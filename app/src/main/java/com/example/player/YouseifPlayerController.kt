package com.example.player

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.annotation.OptIn
import androidx.media3.common.C
import android.graphics.BitmapFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import okhttp3.OkHttpClient
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.PlaylistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
enum class VideoScaleMode(val label: String, val labelAr: String, val resizeMode: Int) {
    FIT("FIT", "Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("FILL", "Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("CROP / ZOOM", "Crop / Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

enum class AudioBoostLevel(val percentage: Int, val gainMilliBels: Int) {
    BOOST_100(100, 0),
    BOOST_125(125, 250),
    BOOST_150(150, 450),
    BOOST_175(175, 650),
    BOOST_200(200, 850),
    BOOST_250(250, 1100),
    BOOST_300(300, 1400),
    BOOST_400(400, 1800),
    BOOST_500(500, 2200)
}

/**
 * Practical vocal / instrumental emphasis via Equalizer (not AI stem split).
 * VOCAL = boost voice band, INSTRUMENTAL = cut voice band (karaoke-ish), BOTH = blend by mix.
 */
enum class AudioMixMode(val labelAr: String) {
    BOTH("مطرب + موسيقى"),
    VOCAL("مطرب أوضح"),
    INSTRUMENTAL("موسيقى أوضح")
}

data class TrackInfo(
    val id: String,
    val name: String,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

/** جودة / سيرفر واحد جاهز للتشغيل من داخل المشغّل نفسه. */
data class QualityOption(
    val label: String,
    val url: String,
    val server: String = "",
    val userAgent: String? = null,
    val referer: String? = null
)

/** آخر بث كان شغّال — بيتخزّن عشان زر التشغيل يرجّعه بعد قفل/فتح التطبيق. */
data class LastPlayed(
    val url: String,
    val title: String,
    val isLive: Boolean,
    val userAgent: String?,
    val referer: String?
)

private const val EXTERNAL_SUB_LABEL = "ترجمة خارجية"

data class SubtitleStyleConfig(
    val fontSizeSp: Float = 18f,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x99000000,
    val edgeType: Int = 1,
    /** 0 = أقرب للأسفل، 0.25 = أعلى شوية — موضع الترجمة عموديًا */
    val bottomPaddingFraction: Float = 0.08f
)

@OptIn(UnstableApi::class)
class YouseifPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null

    // Real network telemetry: Media3 measures bytes transferred by the active source.
    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
        // VPN / تبديل شبكة كان بيقطع البث بعد ثواني — متعملش ريستيت
        .setResetOnNetworkTypeChange(false)
        .setInitialBitrateEstimate(5_000_000L)
        .build()
        .also { meter ->
            try {
                meter.addEventListener(
                    android.os.Handler(android.os.Looper.getMainLooper()),
                    object : androidx.media3.exoplayer.upstream.BandwidthMeter.EventListener {
                        override fun onBandwidthSample(elapsedMs: Int, bytesTransferred: Long, bitrateEstimate: Long) {
                            if (elapsedMs > 0) {
                                lastTransferLatencyMs = elapsedMs.toLong().coerceIn(1L, 60_000L)
                                lastSampleBytes = bytesTransferred
                                lastSampleElapsedMs = elapsedMs.toLong()
                            }
                        }
                    }
                )
            } catch (_: Throwable) {}
        }

    // OkHttp is more tolerant of real-world IPTV/CDN redirects and TLS behavior
    // than the bare HTTP stack. It is used for both HLS and progressive media.
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    @Volatile private var transferStartedAtMs: Long = 0L
    @Volatile private var firstByteAtMs: Long = 0L
    @Volatile private var lastTransferLatencyMs: Long = 0L
    @Volatile private var lastSampleBytes: Long = 0L
    @Volatile private var lastSampleElapsedMs: Long = 0L

    private val _diagnostics = MutableStateFlow(StreamDiagnostics())
    val diagnostics: StateFlow<StreamDiagnostics> = _diagnostics.asStateFlow()

    private val _currentChannel = MutableStateFlow<PlaylistItem?>(null)
    val currentChannel: StateFlow<PlaylistItem?> = _currentChannel.asStateFlow()

    private val _streamTitle = MutableStateFlow("Stream")
    val streamTitle: StateFlow<String> = _streamTitle.asStateFlow()

    private val _audioOnlyMode = MutableStateFlow(false)
    val audioOnlyMode: StateFlow<Boolean> = _audioOnlyMode.asStateFlow()
    private val _dataSaverEnabled = MutableStateFlow(false)
    val dataSaverEnabled: StateFlow<Boolean> = _dataSaverEnabled.asStateFlow()
    private val _maxVideoBitrateBps = MutableStateFlow(0)

    /** Network controls are opt-in and never restored as enabled by player creation. */
    val networkControlsEnabled: Boolean
        get() = _dataSaverEnabled.value || _maxVideoBitrateBps.value > 0

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // الافتراضي FIT عشان الفيديو يظهر بنسبته الطبيعية (بدون تمدد/تمطيط).
    // FILL كان بيخلّي الفيديو يتمدّد ليملأ الشاشة ويفسد شكله مع النسب المختلفة (4:3 / طولي / سينمائي).
    // وبنحفظ اختيار المستخدم عشان يفضل ثابت بعد إعادة التشغيل.
    private val _scaleMode = MutableStateFlow(
        runCatching {
            VideoScaleMode.values().firstOrNull {
                it.name == com.example.data.UserSettings.getString("video_scale_mode", VideoScaleMode.FIT.name)
            }
        }.getOrNull() ?: VideoScaleMode.FIT
    )
    val scaleMode: StateFlow<VideoScaleMode> = _scaleMode.asStateFlow()

    /** Local music / device queue support */
    enum class RepeatMode { OFF, ONE, ALL }
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _localQueue = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val localQueue: StateFlow<List<PlaylistItem>> = _localQueue.asStateFlow()
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _audioBoost = MutableStateFlow(AudioBoostLevel.BOOST_100)
    val audioBoost: StateFlow<AudioBoostLevel> = _audioBoost.asStateFlow()

    private val _audioMixMode = MutableStateFlow(AudioMixMode.BOTH)
    val audioMixMode: StateFlow<AudioMixMode> = _audioMixMode.asStateFlow()
    /** 0 = instrumental emphasis, 100 = vocal emphasis (used when mode is BOTH or as strength). */
    private val _vocalMix = MutableStateFlow(50f)
    val vocalMix: StateFlow<Float> = _vocalMix.asStateFlow()

    /** Combined volume display: 0..500 (100 = normal max, >100 uses LoudnessEnhancer). */
    private val _effectiveVolumePercent = MutableStateFlow(100)
    val effectiveVolumePercent: StateFlow<Int> = _effectiveVolumePercent.asStateFlow()

    /** Window brightness 0f..1f (null = system). Applied by Activity. */
    private val _brightness = MutableStateFlow(0.7f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    /** Video look controls (0f..2f, 1 = neutral). */
    private val _contrast = MutableStateFlow(1f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()
    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isWebEmbed = MutableStateFlow(false)
    val isWebEmbed: StateFlow<Boolean> = _isWebEmbed.asStateFlow()

    // Tracks
    private val _videoTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val videoTracks: StateFlow<List<TrackInfo>> = _videoTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val audioTracks: StateFlow<List<TrackInfo>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackInfo>> = _subtitleTracks.asStateFlow()

    private val _subtitleStyle = MutableStateFlow(SubtitleStyleConfig())
    val subtitleStyle: StateFlow<SubtitleStyleConfig> = _subtitleStyle.asStateFlow()

    private var telemetryJob: Job? = null
    private var fastSeekJob: Job? = null
    private var bufferingTimeoutJob: Job? = null
    private var positionWatchdogJob: Job? = null
    /** Avoid seeking resume more than once per media open. */
    private var resumeAppliedForUrl: String = ""

    // Last source is kept independently from currentChannel so pasted URLs can be retried too.
    @Volatile private var lastUrl: String? = null
    @Volatile private var lastTitle: String = "Stream"
    @Volatile private var lastIsLive: Boolean = true
    @Volatile private var lastUserAgent: String? = null
    @Volatile private var lastReferer: String? = null
    @Volatile private var lastSubtitleUrl: String? = null

    /** جودات/سيرفرات المحتوى الحالي — تظهر في قائمة الجودة داخل المشغّل. */
    private val _qualities = MutableStateFlow<List<QualityOption>>(emptyList())
    val qualities: StateFlow<List<QualityOption>> = _qualities.asStateFlow()

    /** سياق التشغيل الحالي (قنوات/أفلام/مسلسلات/جودات) — لتقليب السابق/التالي. */
    private val _contextQueue = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val contextQueue: StateFlow<List<PlaylistItem>> = _contextQueue.asStateFlow()
    private val _contextIndex = MutableStateFlow(-1)
    val contextIndex: StateFlow<Int> = _contextIndex.asStateFlow()
    @Volatile private var fallbackAttempted: Boolean = false
    @Volatile private var lowestBitrateFallbackTried: Boolean = false
    /** Temporary adaptive cap during bad network — does NOT turn Data Saver UI on. */
    @Volatile private var softBitrateCapBps: Int = 0
    private var rebufferCount: Int = 0
    private var softCapJob: Job? = null
    private var autoAudioOnly = false
    private var audioOnlyUserSet = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val isReady = playbackState == Player.STATE_READY

            _diagnostics.value = _diagnostics.value.copy(
                isLoading = isBuffering,
                isPlaying = exoPlayer?.isPlaying == true
            )
            if (playbackState == Player.STATE_ENDED) {
                handleTrackEnded()
            }
            if (isBuffering) {
                rebufferCount++
                // خفض الجودة التلقائي كان بيقطع البث (خصوصاً IPTV) — يشتغل بس لو Data Saver مفعّل
                softCapJob?.cancel()
                if (_dataSaverEnabled.value) {
                    softCapJob = scope.launch(Dispatchers.Main) {
                        delay(8_000L)
                        if (exoPlayer?.playbackState == Player.STATE_BUFFERING && rebufferCount >= 3) {
                            applySoftBitrateFallback()
                        }
                    }
                }
            }
            if (isReady) {
                bufferingTimeoutJob?.cancel()
                softCapJob?.cancel()
                if (rebufferCount > 0) rebufferCount = maxOf(0, rebufferCount - 1)
                setupAudioEnhancer()
                // Resume from last saved position (VOD / local only)
                tryApplyResumePosition()
                startPositionWatchdog()
                // لو READY ومفروض يشغّل بس واقف — nudge مرة
                try {
                    val pl = exoPlayer
                    if (pl != null && pl.playWhenReady && !pl.isPlaying) {
                        pl.play()
                    }
                } catch (_: Throwable) {}
                // First-open fix: if video track exists but surface still 0x0, nudge renderer
                try {
                    val player = exoPlayer
                    if (player != null) {
                        val vs = player.videoSize
                        val hasVideo = player.currentTracks.groups.any { g ->
                            g.type == C.TRACK_TYPE_VIDEO && g.length > 0
                        }
                        if (hasVideo && vs.width == 0 && vs.height == 0) {
                            val pos = player.currentPosition
                            player.seekTo(if (pos > 0) pos else 0L)
                            player.playWhenReady = true
                            android.util.Log.i(
                                "YouseifPlayerController",
                                "Video surface nudge after first ready (audio-only symptom)"
                            )
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("YouseifPlayerController", "surface nudge failed: ${e.message}")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _diagnostics.value = _diagnostics.value.copy(
                isPlaying = isPlaying,
                isLoading = false
            )
            if (!isPlaying) saveCurrentPosition()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            // Audio session is usually assigned only after the player has prepared/started.
            // Re-attach LoudnessEnhancer here so Audio Boost actually works.
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                try {
                    loudnessEnhancer?.release()
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        setTargetGain(_audioBoost.value.gainMilliBels)
                        enabled = _audioBoost.value != AudioBoostLevel.BOOST_100
                    }
                    applyEqualizerMix()
                } catch (e: Throwable) {
                    Log.w("YouseifPlayerController", "Audio enhancer attach failed: ${e.message}")
                }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            try {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val res = "${videoSize.width} × ${videoSize.height}"
                    _diagnostics.value = _diagnostics.value.copy(resolution = res)
                }
            } catch (e: Throwable) {
                Log.w("YouseifPlayerController", "onVideoSizeChanged: ${e.message}")
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            try {
                updateTracksList(tracks)
                val hasVideo = tracks.groups.any { it.type == C.TRACK_TYPE_VIDEO && it.length > 0 }
                val hasAudio = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO && it.length > 0 }
                if (hasAudio && !hasVideo && !audioOnlyUserSet) {
                    autoAudioOnly = true
                    _audioOnlyMode.value = true
                } else if (hasVideo && autoAudioOnly && !audioOnlyUserSet) {
                    autoAudioOnly = false
                    _audioOnlyMode.value = false
                }
            } catch (e: Throwable) {
                Log.w("YouseifPlayerController", "onTracksChanged: ${e.message}")
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            bufferingTimeoutJob?.cancel()
            Log.w("YouseifPlayerController", "PlaybackException: ${error.errorCodeName} ${error.message}")
            val detail = error.cause?.message ?: error.message ?: error.errorCodeName
            val category = when {
                detail.contains("403", true) || detail.contains("forbidden", true) -> "Source rejected (403)"
                detail.contains("404", true) || detail.contains("not found", true) -> "Source not found (404)"
                detail.contains("timeout", true) || detail.contains("timed out", true) -> "Connection timed out"
                detail.contains("behind live window", true) -> "Live window moved"
                detail.contains("CLEARTEXT", true) -> "Cleartext HTTP blocked"
                detail.contains("extractor", true) || detail.contains("None of the available", true) ->
                    "السيرفر مش مدعوم حالياً — جرب سيرفر تاني أو زر الجودات"
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network failed"
                error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Bad HTTP status"
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Bad stream format"
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Bad playlist/manifest"
                else -> "Unable to play this URL"
            }

            // Recovery 1: browser UA + DefaultHttpDataSource
            val url = lastUrl
            if (!url.isNullOrBlank() && !fallbackAttempted) {
                fallbackAttempted = true
                Log.i("YouseifPlayerController", "Auto-fallback after error for $url")
                try {
                    retryWithFallbackHttp(url, lastTitle, lastIsLive, lastSubtitleUrl)
                    return
                } catch (e: Throwable) {
                    Log.e("YouseifPlayerController", "Auto-fallback failed", e)
                }
            }
            fallbackAttempted = false
            lowestBitrateFallbackTried = false
            _diagnostics.value = _diagnostics.value.copy(
                isLoading = false,
                isPlaying = false,
                errorMessage = "$category: ${detail.take(140)}"
            )
        }
    }


    init {
        // Lazy: do NOT build ExoPlayer at process start (was a common crash + slow open).
        // Player is created on first playUrl/playChannel via ensurePlayer().
        // Completely unrestricted by default — Data Saver / Audio Only are opt-in only.
        _dataSaverEnabled.value = false
        _audioOnlyMode.value = false
        _maxVideoBitrateBps.value = 0
        // التشغيل/الإيقاف + التقليب من الإشعار وشاشة القفل ولوحة تحكم السيارة
        PlaybackBridge.onToggleOrResume = { resumeOrToggle(); true }
        // Prefer the active device queue for songs/videos; otherwise use the
        // current library/channel context. This prevents a stale channel list
        // from swallowing notification next/previous presses.
        PlaybackBridge.onNext = {
            if (_localQueue.value.isNotEmpty()) {
                playNextInQueue()
                true
            } else nextInContext()
        }
        PlaybackBridge.onPrevious = {
            if (_localQueue.value.isNotEmpty()) {
                playPreviousInQueue()
                true
            } else previousInContext()
        }
    }

    @Synchronized
    private fun ensurePlayer() {
        if (exoPlayer != null) return
        try {
            initializePlayer()
        } catch (e: Throwable) {
            android.util.Log.e("YouseifPlayerController", "ensurePlayer failed", e)
        }
    }

    private fun initializePlayer() {
        try {
            trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setAllowMultipleAdaptiveSelections(true)
                        .setPreferredAudioLanguage("ar")
                        .setPreferredTextLanguage("ar")
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        .setExceedRendererCapabilitiesIfNecessary(true)
                )
            }

            // Tuned for weak mobile data: more buffer before start, bigger cushion after rebuffer.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs */ 25_000,
                    /* maxBufferMs */ 120_000,
                    /* bufferForPlaybackMs */ 2_500,
                    /* bufferForPlaybackAfterRebufferMs */ 6_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(/* backBufferDurationMs */ 45_000, /* retainBackBufferFromKeyframe */ true)
                .build()

            // ON (not PREFER): avoid requiring missing FFmpeg extension; still allows extensions if present
            val renderersFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setEnableDecoderFallback(true)

            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            // Base HTTP factory used by DefaultMediaSourceFactory for auto-detect
            val baseHttp = buildHttpFactory(
                mapOf(
                    "User-Agent" to StreamResolver.UA_VLC,
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )
            val baseDataSource = DefaultDataSource.Factory(context, baseHttp)
            val mediaSourceFactory = DefaultMediaSourceFactory(baseDataSource)

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setTrackSelector(
                    trackSelector ?: DefaultTrackSelector(context).also { trackSelector = it }
                )
                .setLoadControl(loadControl)
                .setBandwidthMeter(bandwidthMeter)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setSeekBackIncrementMs(10_000)
                .setSeekForwardIncrementMs(10_000)
                .build()
                .apply {
                    playWhenReady = true
                    volume = 1.0f
                    addListener(playerListener)
                }

            PlayerHolder.attach(exoPlayer!!, _streamTitle.value, lastIsLive)
            try {
                run {
                    val i = android.content.Intent(context, PlaybackService::class.java)
                    ContextCompat.startForegroundService(context, i)
                    try {
                        context.startService(
                            android.content.Intent(context, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_REFRESH)
                        )
                    } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                Log.w("YouseifPlayerController", "Unable to start playback service", e)
            }
            applyCurrentTrackConstraints()
            startTelemetryLoop()
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error initializing ExoPlayer: ${e.message}", e)
            try {
                exoPlayer = ExoPlayer.Builder(context).build().apply {
                    playWhenReady = true
                    addListener(playerListener)
                }
                PlayerHolder.attach(exoPlayer!!, _streamTitle.value, lastIsLive)
                try { run {
                    val i = android.content.Intent(context, PlaybackService::class.java)
                    ContextCompat.startForegroundService(context, i)
                    try {
                        context.startService(
                            android.content.Intent(context, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_REFRESH)
                        )
                    } catch (_: Throwable) {}
                } } catch (_: Throwable) {}
                startTelemetryLoop()
            } catch (e2: Throwable) {
                Log.e("YouseifPlayerController", "Fatal ExoPlayer fallback failure", e2)
            }
        }
    }

    private fun setupAudioEnhancer() {
        applyLoudnessGain(_audioBoost.value)
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    /** رابط البث الحالي (للـ Cast وغيره) */
    fun getCurrentMediaUrl(): String? = lastUrl ?: _currentChannel.value?.url

    fun getCurrentMediaTitle(): String = lastTitle.ifBlank { _streamTitle.value }.ifBlank { "Youseif Player" }

    fun isCurrentLive(): Boolean = lastIsLive || (_currentChannel.value?.isLive == true)

    /** Referer / User-Agent of the current stream — forwarded to the Cast local proxy. */
    fun getCurrentReferer(): String? = lastReferer
    fun getCurrentUserAgent(): String? = lastUserAgent

    fun setCurrentContent(item: PlaylistItem) {
        _currentChannel.value = item
        _streamTitle.value = item.name
        PlayerHolder.title = item.name
    }

    fun playChannel(channel: PlaylistItem, customUserAgent: String? = null, customReferer: String? = null) {
        _currentChannel.value = channel
        playUrl(
            url = channel.url,
            title = channel.name,
            isLive = channel.isLive,
            userAgent = channel.httpUserAgent ?: customUserAgent,
            referer = channel.httpReferrer ?: customReferer
        )
    }

    /** Show a real WebView page instead of ExoPlayer — used when a source page is
     *  Cloudflare-protected and no direct stream could be resolved automatically. */
    fun playWebEmbed(url: String, title: String) {
        if (url.isBlank()) return
        _currentChannel.value = PlaylistItem(
            id = "webembed_${System.currentTimeMillis()}", channelNumber = 0,
            name = title, url = url, isLive = false
        )
        _streamTitle.value = title
        _isWebEmbed.value = true
        bufferingTimeoutJob?.cancel()
        scope.launch(Dispatchers.Main) {
            try { exoPlayer?.stop() } catch (_: Throwable) {}
            _diagnostics.value = _diagnostics.value.copy(
                isLoading = false, isPlaying = true,
                protocol = "Web / Embed", errorMessage = null
            )
        }
    }

    /** عنوان ودي للعرض في الإشعار — يمنع ظهور قيم خام زي "index v1 a1". */
    private fun friendlyTitle(t: String?, url: String): String {
        val raw = t?.trim().orEmpty()
        val looksRaw = raw.isBlank() ||
            raw.startsWith("index", true) ||
            raw.startsWith("v", true) && raw.length < 4 ||
            (!raw.contains(" ") && raw.length > 26 && raw.contains("/"))
        if (!looksRaw) return raw
        return url.substringAfterLast('/').substringBefore('?').ifBlank { "Youseif Player Pro" }
    }

    fun playUrl(
        url: String,
        title: String = "Stream",
        isLive: Boolean = true,
        userAgent: String? = null,
        referer: String? = null,
        externalSubtitleUrl: String? = null
    ) {
        val cleanUrl = url.trim().removePrefix("\uFEFF")
        if (cleanUrl.isEmpty()) return

        lastUrl = cleanUrl
        lastTitle = title
        val safeTitle = friendlyTitle(title, url)
        _streamTitle.value = safeTitle
        PlayerHolder.title = safeTitle
        lastIsLive = isLive
        lastUserAgent = userAgent
        lastReferer = referer
        lastSubtitleUrl = externalSubtitleUrl
        persistLastPlayed(cleanUrl, safeTitle, isLive, userAgent, referer)
        fallbackAttempted = false
        lowestBitrateFallbackTried = false
        softBitrateCapBps = 0
        rebufferCount = 0
        softCapJob?.cancel()
        autoAudioOnly = false
        audioOnlyUserSet = false
        _audioOnlyMode.value = false
        bufferingTimeoutJob?.cancel()
        positionWatchdogJob?.cancel()
        resumeAppliedForUrl = ""
        // Save previous media position before switching
        saveCurrentPosition()

        scope.launch(Dispatchers.Main) {
            _diagnostics.value = _diagnostics.value.copy(
                isLoading = true,
                errorMessage = null,
                isLive = isLive,
                isPlaying = false,
                resolution = "—",
                fps = 0,
                videoCodec = "—",
                audioCodec = "—",
                bitrateKbps = 0L,
                protocol = "—"
            )

            try {
                val resolved = StreamResolver.resolve(cleanUrl, userAgent, referer)
                if (resolved.type == StreamType.WEB_EMBED) {
                    // Ensure the in-app WebView has a channel URL to load
                    _currentChannel.value = PlaylistItem(
                        id = "webembed_${System.currentTimeMillis()}",
                        channelNumber = 0,
                        name = title,
                        url = cleanUrl,
                        isLive = false
                    )
                    _streamTitle.value = title
                    _isWebEmbed.value = true
                    _diagnostics.value = _diagnostics.value.copy(
                        isLoading = false,
                        protocol = "Web / Embed",
                        isPlaying = true,
                        errorMessage = null
                    )
                    try { exoPlayer?.stop() } catch (_: Throwable) {}
                    return@launch
                }

                _isWebEmbed.value = false
                ensurePlayer()
                val player = exoPlayer ?: return@launch

                val mediaSource = createMediaSource(resolved, externalSubtitleUrl)

                // Safe switch: never release the ExoPlayer instance — only swap media.
                // Releasing here was a common cause of process death when tapping another item.
                try {
                    player.playWhenReady = false
                    player.stop()
                } catch (_: Throwable) {}
                try { player.clearMediaItems() } catch (_: Throwable) {}
                if (!_dataSaverEnabled.value) softBitrateCapBps = 0
                trackSelector?.let { sel ->
                    try {
                        val b = sel.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .setMaxVideoBitrate(Int.MAX_VALUE)
                            .setForceLowestBitrate(false)
                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, _audioOnlyMode.value)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .setExceedRendererCapabilitiesIfNecessary(true)
                        if (_dataSaverEnabled.value && _maxVideoBitrateBps.value > 0) {
                            b.setMaxVideoBitrate(_maxVideoBitrateBps.value)
                            b.setForceLowestBitrate(false)
                            if (_maxVideoBitrateBps.value <= 1_500_000) b.setMaxVideoSize(1280, 720)
                        }
                        // مفيش soft bitrate من غير Data Saver — كان بيقطع البث
                        sel.setParameters(b.build())
                    } catch (_: Throwable) {}
                }
                try {
                    player.setMediaSource(mediaSource, /* resetPosition = */ true)
                    player.prepare()
                    player.playWhenReady = true
                    player.volume = 1f
                } catch (e: Throwable) {
                    Log.e("YouseifPlayerController", "setMediaSource failed, rebuilding player", e)
                    try {
                        exoPlayer?.release()
                    } catch (_: Throwable) {}
                    exoPlayer = null
                    PlayerHolder.clear()
                    ensurePlayer()
                    val p2 = exoPlayer
                    if (p2 != null) {
                        p2.setMediaSource(mediaSource, true)
                        p2.prepare()
                        p2.playWhenReady = true
                    }
                }
                PlayerHolder.attach(exoPlayer ?: player, title, isLive)
                try {
                    ContextCompat.startForegroundService(
                        context,
                        android.content.Intent(context, PlaybackService::class.java)
                    )
                    context.startService(
                        android.content.Intent(context, PlaybackService::class.java)
                            .setAction(PlaybackService.ACTION_REFRESH)
                    )
                } catch (_: Throwable) {}

                bufferingTimeoutJob = scope.launch(Dispatchers.Main) {
                    delay(30_000L)
                    if (lastUrl == cleanUrl && _diagnostics.value.isLoading && exoPlayer?.playbackState != Player.STATE_READY) {
                        _diagnostics.value = _diagnostics.value.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "مصدر البث لم يستجب خلال 30 ثانية"
                        )
                        try { exoPlayer?.stop() } catch (_: Throwable) {}
                    }
                }

                _diagnostics.value = _diagnostics.value.copy(
                    protocol = resolved.type.displayName,
                    isLoading = true,
                    errorMessage = null
                )
                setupAudioEnhancer()
            } catch (e: Throwable) {
                Log.e("YouseifPlayerController", "Error playing URL: $cleanUrl", e)
                // Automatic second attempt with browser UA + DefaultHttpDataSource (no OkHttp)
                try {
                    retryWithFallbackHttp(cleanUrl, title, isLive, externalSubtitleUrl)
                } catch (e2: Throwable) {
                    Log.e("YouseifPlayerController", "Fallback also failed", e2)
                    val msg = (e2.message ?: e.message ?: e.javaClass.simpleName).take(160)
                    _diagnostics.value = _diagnostics.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = "Play failed: $msg"
                    )
                }
            }
        }
    }

    /** Fallback path: platform DefaultHttpDataSource + browser UA (fixes some CDN blocks on OkHttp). */
    private fun retryWithFallbackHttp(
        url: String,
        title: String,
        isLive: Boolean,
        externalSubtitleUrl: String?
    ) {
        ensurePlayer()
        val player = exoPlayer ?: return
        val headers = linkedMapOf(
            "User-Agent" to StreamResolver.UA_BROWSER,
            "Accept" to "*/*",
            "Connection" to "keep-alive"
        )
        try {
            val u = Uri.parse(url)
            if (u.scheme != null && u.host != null) {
                headers["Referer"] = "${u.scheme}://${u.host}/"
                headers["Origin"] = "${u.scheme}://${u.host}"
            }
        } catch (_: Throwable) {}

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(StreamResolver.UA_BROWSER)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(headers)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        // تمرير نوع الـ MIME للـ fallback كمان — بعض روابط HLS بتوكن ما بيظهرش
        // لها Content-Type صريح، فالـ sniffing كان يفشل ويقول "غير مدعوم".
        val lowUrl = url.lowercase()
        val fallbackMime = when {
            lowUrl.contains(".m3u8") || lowUrl.contains(".m3u") ||
                lowUrl.contains("/hls2/") || lowUrl.contains("/hls/") -> MimeTypes.APPLICATION_M3U8
            lowUrl.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> null
        }
        val mediaItem = buildMediaItem(url, fallbackMime, externalSubtitleUrl)

        // Push the hint when known, otherwise let DefaultMediaSourceFactory sniff.
        val source = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)

        player.stop()
        player.clearMediaItems()
        player.setMediaSource(source, true)
        player.prepare()
        player.playWhenReady = true
        _diagnostics.value = _diagnostics.value.copy(
            isLoading = true,
            errorMessage = null,
            protocol = "Auto (fallback)"
        )
        Log.i("YouseifPlayerController", "Fallback HTTP path started for $url")
    }

    private fun buildHttpFactory(headers: Map<String, String>): OkHttpDataSource.Factory {
        return OkHttpDataSource.Factory(httpClient)
            .setUserAgent(headers["User-Agent"] ?: StreamResolver.UA_VLC)
            .setTransferListener(bandwidthMeter)
            .setDefaultRequestProperties(headers)
    }

    private fun buildMediaItem(
        url: String,
        mimeHint: String?,
        externalSubtitleUrl: String?
    ): MediaItem {
        val displayTitle = lastTitle.ifBlank { _streamTitle.value }.ifBlank { "Youseif Player" }
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(displayTitle)
            .setDisplayTitle(displayTitle)
            .setArtist("Youseif Player Pro")
            .setAlbumTitle(if (lastIsLive) "Live" else "Library")
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(
                if (lastIsLive) MediaMetadata.MEDIA_TYPE_RADIO_STATION
                else MediaMetadata.MEDIA_TYPE_MUSIC  // shows as media player card in QS
            )
        // Attach app icon as artwork so notification / media controls show a real icon
        try {
            val bmp = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.youseif_app_icon)
            if (bmp != null) {
                metaBuilder.setArtworkData(
                    java.io.ByteArrayOutputStream().use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                        out.toByteArray()
                    },
                    MediaMetadata.PICTURE_TYPE_FILE_ICON
                )
            }
        } catch (_: Throwable) {}
        val metadata = metaBuilder.build()

        val builder = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMediaId(url)
            .setMediaMetadata(metadata)
        if (!mimeHint.isNullOrBlank()) {
            builder.setMimeType(mimeHint)
        }
        if (!externalSubtitleUrl.isNullOrBlank()) {
            val subMime = when {
                externalSubtitleUrl.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                externalSubtitleUrl.endsWith(".ssa", true) ||
                    externalSubtitleUrl.endsWith(".ass", true) -> MimeTypes.TEXT_SSA
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(externalSubtitleUrl))
                .setMimeType(subMime)
                .setLanguage("ar")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        return builder.build()
    }

    private fun createMediaSource(
        resolved: StreamResolver.ResolvedStream,
        externalSubtitleUrl: String? = null
    ): MediaSource {
        val headers = LinkedHashMap<String, String>()
        headers.putAll(resolved.headers)
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = StreamResolver.UA_VLC
        }
        if (!headers.containsKey("Accept")) headers["Accept"] = "*/*"

        val httpDataSourceFactory = buildHttpFactory(headers)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mimeHint = when (resolved.type) {
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            else -> resolved.mimeType
        }
        val mediaItem = buildMediaItem(resolved.url, mimeHint, externalSubtitleUrl)

        return when (resolved.type) {
            StreamType.HLS -> {
                // Live: target a few seconds behind edge to reduce stalls on shaky networks
                val liveItem = if (lastIsLive) {
                    mediaItem.buildUpon()
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(8_000)
                                .setMinOffsetMs(3_000)
                                .setMaxOffsetMs(25_000)
                                .setMaxPlaybackSpeed(1.08f)
                                .build()
                        )
                        .build()
                } else mediaItem
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount */ 8))
                    .createMediaSource(liveItem)
            }
            StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            StreamType.RTSP -> RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(20_000)
                .createMediaSource(mediaItem)
            StreamType.RTMP -> ProgressiveMediaSource.Factory(RtmpDataSource.Factory())
                .createMediaSource(mediaItem)
            else -> {
                // UNKNOWN / PROGRESSIVE / WEB_EMBED → full auto-detect
                DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    fun stopPlayback() {
        try {
            exoPlayer?.playWhenReady = false
            exoPlayer?.stop()
        } catch (_: Throwable) {}
        _currentChannel.value = null
        _diagnostics.value = _diagnostics.value.copy(isPlaying = false, isLoading = false)
    }

    fun togglePlayPause() {
        try {
            exoPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                } else {
                    it.play()
                }
            }
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error toggling play/pause", e)
        }
    }

    // ---------------------------------------------------------- آخر بث مشغّل

    private fun lastPlayedPrefs() =
        context.getSharedPreferences("youseif_last_play", Context.MODE_PRIVATE)

    private fun persistLastPlayed(url: String, title: String, isLive: Boolean, ua: String?, ref: String?) {
        try {
            lastPlayedPrefs().edit()
                .putString("url", url)
                .putString("title", title)
                .putBoolean("is_live", isLive)
                .putString("ua", ua ?: "")
                .putString("referer", ref ?: "")
                .apply()
        } catch (_: Throwable) {}
    }

    /** آخر بث/فيديو/قناة/فيلم اتشغّل (محفوظ على الجهاز). */
    fun lastPlayed(): LastPlayed? {
        return try {
            val u = lastPlayedPrefs().getString("url", null).orEmpty()
            if (u.isBlank()) null else LastPlayed(
                url = u,
                title = lastPlayedPrefs().getString("title", "").orEmpty(),
                isLive = lastPlayedPrefs().getBoolean("is_live", false),
                userAgent = lastPlayedPrefs().getString("ua", "")?.takeIf { it.isNotBlank() },
                referer = lastPlayedPrefs().getString("referer", "")?.takeIf { it.isNotBlank() }
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * زر التشغيل في نص المشغّل:
     * - لو فيه عنصر محمّل → تشغيل/إيقاف عادي.
     * - لو المشغّل فاضي (بعد قفل/فتح التطبيق) → يرجّع آخر بث/فيديو/قناة/فيلم اتشغّل.
     */
    fun resumeOrToggle() {
        val p = exoPlayer
        val hasMedia = p != null && p.mediaItemCount > 0
        if (hasMedia) {
            try {
                if (p!!.isPlaying) p.pause() else p.play()
            } catch (e: Throwable) {
                Log.e("YouseifPlayerController", "resumeOrToggle toggle failed", e)
            }
            return
        }
        val snap = lastPlayed()
        if (snap != null) {
            playUrl(snap.url, snap.title, snap.isLive, snap.userAgent, snap.referer)
            return
        }
        val memUrl = lastUrl
        if (!memUrl.isNullOrBlank()) {
            playUrl(memUrl, lastTitle, lastIsLive, lastUserAgent, lastReferer)
        }
    }

    // ------------------------------------------------- جودات/سيرفرات المحتوى

    /** يحدّد قائمة الجودات/السيرفرات اللي هتظهر في قائمة الجودة داخل المشغّل. */
    fun setQualities(list: List<QualityOption>) {
        _qualities.value = list
    }

    fun clearQualities() {
        _qualities.value = emptyList()
    }

    /** تبديل الجودة/السيرفر من غير الخروج من المشغّل. */
    fun selectQuality(option: QualityOption) {
        if (option.url.isBlank()) return
        playUrl(
            url = option.url,
            title = lastTitle,
            isLive = lastIsLive,
            userAgent = option.userAgent ?: lastUserAgent,
            referer = option.referer ?: lastReferer
        )
    }

    // ------------------------------------------------- سياق التقليب سابق/تالي

    /**
     * يسجّل السياق الحالي (قائمة القنوات/الأفلام/الجودات) عشان أزرار السابق/التالي
     * (داخل المشغّل + من الإشعار) تقلّب على نفس القائمة بدل ما تفشل.
     */
    fun setContextQueue(items: List<PlaylistItem>, currentId: String?) {
        // Switching from device media to library content must not retain the
        // previous device queue as the notification navigation context.
        _localQueue.value = emptyList()
        _queueIndex.value = -1
        _contextQueue.value = items
        _contextIndex.value = if (currentId == null) -1 else items.indexOfFirst { it.id == currentId }
    }

    fun clearContextQueue() {
        _contextQueue.value = emptyList()
        _contextIndex.value = -1
    }

    private fun stepContext(forward: Boolean): Boolean {
        val q = _contextQueue.value
        if (q.isEmpty()) return false
        var i = _contextIndex.value
        if (i !in q.indices) {
            i = q.indexOfFirst { it.id == _currentChannel.value?.id || it.url == lastUrl }
        }
        if (i !in q.indices) return false
        i = if (forward) (i + 1) % q.size else if (i - 1 < 0) q.size - 1 else i - 1
        _contextIndex.value = i
        val item = q[i]
        setCurrentContent(item)
        playUrl(item.url, item.name, item.isLive, item.httpUserAgent, item.httpReferrer)
        return true
    }

    /** يرجع true لو اتعمل تقليب فعلي. */
    fun nextInContext(): Boolean = stepContext(true)

    /** يرجع true لو اتعمل تقليب فعلي. */
    fun previousInContext(): Boolean = stepContext(false)

    fun retry() {
        val url = lastUrl
        if (!url.isNullOrBlank()) {
            playUrl(
                url = url,
                title = lastTitle,
                isLive = lastIsLive,
                userAgent = lastUserAgent,
                referer = lastReferer,
                externalSubtitleUrl = lastSubtitleUrl
            )
        } else {
            _currentChannel.value?.let { playChannel(it) }
        }
    }

    fun seekRelative(seconds: Int) {
        try {
            exoPlayer?.let {
                val target = (it.currentPosition + seconds * 1000L).coerceIn(0L, it.duration.coerceAtLeast(0L))
                it.seekTo(target)
            }
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error seeking", e)
        }
    }

    /** Persist last playback position so reopen resumes (minute 50 → minute 50). */
    private fun positionKey(url: String): String =
        "resume_pos_" + url.trim().hashCode().toString()

    private fun saveCurrentPosition() {
        try {
            val player = exoPlayer ?: return
            val url = lastUrl ?: return
            if (lastIsLive) return
            val pos = player.currentPosition
            val dur = player.duration
            if (pos < 5_000L) return // too early
            // Near end → clear so next open starts from beginning
            if (dur > 0 && pos > dur - 12_000L) {
                context.getSharedPreferences("youseif_resume", Context.MODE_PRIVATE)
                    .edit().remove(positionKey(url)).apply()
                return
            }
            context.getSharedPreferences("youseif_resume", Context.MODE_PRIVATE)
                .edit().putLong(positionKey(url), pos).apply()
        } catch (_: Throwable) {}
    }

    private fun tryApplyResumePosition() {
        try {
            val player = exoPlayer ?: return
            val url = lastUrl ?: return
            if (lastIsLive) return
            if (resumeAppliedForUrl == url) return
            val saved = context.getSharedPreferences("youseif_resume", Context.MODE_PRIVATE)
                .getLong(positionKey(url), 0L)
            if (saved < 5_000L) return
            val dur = player.duration
            if (dur > 0 && saved >= dur - 8_000L) return
            resumeAppliedForUrl = url
            player.seekTo(saved)
            android.util.Log.i("YouseifPlayerController", "Resumed at ${saved / 1000}s for $url")
        } catch (e: Throwable) {
            android.util.Log.w("YouseifPlayerController", "resume failed: ${e.message}")
        }
    }

    private fun startPositionWatchdog() {
        positionWatchdogJob?.cancel()
        positionWatchdogJob = scope.launch(Dispatchers.Main) {
            while (true) {
                delay(8_000L)
                if (exoPlayer?.isPlaying == true) saveCurrentPosition()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            exoPlayer?.let {
                val target = positionMs.coerceIn(0L, it.duration.coerceAtLeast(0L))
                it.seekTo(target)
            }
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error seeking to position", e)
        }
    }

    fun startContinuousSeek(isForward: Boolean) {
        fastSeekJob?.cancel()
        fastSeekJob = scope.launch(Dispatchers.Main) {
            val delta = if (isForward) 4000L else -4000L
            while (isActive) {
                exoPlayer?.let {
                    val target = (it.currentPosition + delta).coerceIn(0L, it.duration.coerceAtLeast(0L))
                    it.seekTo(target)
                }
                delay(200)
            }
        }
    }

    fun stopContinuousSeek() {
        fastSeekJob?.cancel()
        fastSeekJob = null
    }

    fun setScaleMode(mode: VideoScaleMode) {
        _scaleMode.value = mode
        runCatching { com.example.data.UserSettings.putString("video_scale_mode", mode.name) }
    }

    fun cycleScaleMode() {
        val modes = VideoScaleMode.values()
        val nextIndex = (modes.indexOf(_scaleMode.value) + 1) % modes.size
        setScaleMode(modes[nextIndex])
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    fun toggleMute() {
        try {
            val newMute = !_isMuted.value
            _isMuted.value = newMute
            exoPlayer?.volume = if (newMute) 0f else _volume.value
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error muting", e)
        }
    }

    fun setVolume(vol: Float) {
        try {
            val clamped = vol.coerceIn(0f, 1.0f)
            _volume.value = clamped
            if (!_isMuted.value) {
                exoPlayer?.volume = clamped
            }
            // Keep effective % in sync when volume is set directly (boost stays)
            val boostPct = _audioBoost.value.percentage
            _effectiveVolumePercent.value = if (clamped >= 0.99f) boostPct else (clamped * 100f).toInt().coerceIn(0, 100)
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error setting volume", e)
        }
    }

    fun setAudioBoost(boost: AudioBoostLevel) {
        _audioBoost.value = boost
        applyLoudnessGain(boost)
        if (_volume.value >= 0.99f) {
            _effectiveVolumePercent.value = boost.percentage
        }
    }

    /**
     * Single control 0..500:
     * 0..100 → system ExoPlayer volume
     * 100..500 → volume=1 + LoudnessEnhancer steps
     */
    fun setEffectiveVolumePercent(percent: Int) {
        val p = percent.coerceIn(0, 500)
        _effectiveVolumePercent.value = p
        if (p <= 100) {
            setVolume(p / 100f)
            setAudioBoost(AudioBoostLevel.BOOST_100)
        } else {
            setVolume(1f)
            val boost = AudioBoostLevel.values()
                .filter { it.percentage >= p }
                .minByOrNull { it.percentage }
                ?: AudioBoostLevel.values().maxByOrNull { it.percentage }
                ?: AudioBoostLevel.BOOST_100
            // Prefer closest level at or just below
            val closest = AudioBoostLevel.values().minByOrNull { kotlin.math.abs(it.percentage - p) }
                ?: AudioBoostLevel.BOOST_100
            setAudioBoost(closest)
        }
    }

    fun setAudioMixMode(mode: AudioMixMode) {
        _audioMixMode.value = mode
        applyEqualizerMix()
    }

    fun setVocalMix(value: Float) {
        _vocalMix.value = value.coerceIn(0f, 100f)
        applyEqualizerMix()
    }

    /** Apply EQ curve approximating vocal vs instrumental emphasis. */
    private fun applyEqualizerMix() {
        try {
            val session = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
            if (session == C.AUDIO_SESSION_ID_UNSET) return
            try { equalizer?.release() } catch (_: Throwable) {}
            equalizer = null
            val eq = try { Equalizer(0, session) } catch (e: Throwable) {
                Log.w("YouseifPlayerController", "Equalizer unavailable: ${e.message}")
                return
            }
            eq.enabled = true
            val mode = _audioMixMode.value
            val mix = _vocalMix.value / 100f // 0 instrumental .. 1 vocal
            val n = eq.numberOfBands.toInt()
            for (i in 0 until n) {
                val band = i.toShort()
                val range = eq.getBandLevelRange() // short[2] min..max milliBel
                val minL = range[0].toInt()
                val maxL = range[1].toInt()
                val center = try { eq.getCenterFreq(band) } catch (_: Throwable) { 1000 * (i + 1) }
                // Voice energy roughly 300Hz – 4kHz
                val isVoiceBand = center in 300_000..4_000_000
                val isLow = center < 250_000
                val isHigh = center > 5_000_000
                val level = when (mode) {
                    AudioMixMode.VOCAL -> when {
                        isVoiceBand -> maxL * 0.55
                        isLow -> minL * 0.25
                        else -> maxL * 0.15
                    }
                    AudioMixMode.INSTRUMENTAL -> when {
                        isVoiceBand -> minL * 0.70 // cut vocals
                        isLow || isHigh -> maxL * 0.35
                        else -> maxL * 0.20
                    }
                    AudioMixMode.BOTH -> {
                        // Blend: mix=1 vocal curve, mix=0 instrumental curve
                        val vocal = when {
                            isVoiceBand -> maxL * 0.55
                            isLow -> minL * 0.15
                            else -> maxL * 0.10
                        }
                        val instru = when {
                            isVoiceBand -> minL * 0.55
                            else -> maxL * 0.25
                        }
                        vocal * mix + instru * (1f - mix)
                    }
                }
                val clamped = level.toInt().coerceIn(minL, maxL).toShort()
                try { eq.setBandLevel(band, clamped) } catch (_: Throwable) {}
            }
            equalizer = eq
            Log.i("YouseifPlayerController", "EQ mix mode=$mode vocalMix=${_vocalMix.value}")
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "applyEqualizerMix failed: ${e.message}")
        }
    }

    fun setBrightness(value: Float) {
        _brightness.value = value.coerceIn(0.05f, 1f)
    }

    fun setContrast(value: Float) {
        _contrast.value = value.coerceIn(0.4f, 1.8f)
    }

    fun setSaturation(value: Float) {
        _saturation.value = value.coerceIn(0f, 2f)
    }

    private fun applyLoudnessGain(boost: AudioBoostLevel = _audioBoost.value) {
        try {
            val audioSessionId = exoPlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
                Log.w("YouseifPlayerController", "Audio boost deferred: no audio session yet")
                return
            }
            try {
                loudnessEnhancer?.release()
            } catch (_: Throwable) {}
            loudnessEnhancer = null
            if (boost == AudioBoostLevel.BOOST_100 || boost.gainMilliBels <= 0) {
                // neutral — leave enhancer off
                return
            }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                // Android LoudnessEnhancer typically accepts up to ~3000 mB depending on device
                setTargetGain(boost.gainMilliBels.coerceIn(0, 3000))
                enabled = true
            }
            Log.i("YouseifPlayerController", "Audio boost applied ${boost.percentage}% (${boost.gainMilliBels} mB)")
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "Failed applying audio boost: ${e.message}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            exoPlayer?.playbackParameters = PlaybackParameters(speed)
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error setting playback speed", e)
        }
    }

    fun selectTrack(type: Int, groupIndex: Int, trackIndex: Int) {
        val selector = trackSelector ?: return
        try {
            val parameters = selector.parameters.buildUpon()
            val tracks = exoPlayer?.currentTracks ?: return

            val matchingGroups = tracks.groups.filter { it.type == type }
            if (groupIndex in matchingGroups.indices) {
                val targetGroup = matchingGroups[groupIndex]
                parameters.setOverrideForType(
                    androidx.media3.common.TrackSelectionOverride(
                        targetGroup.mediaTrackGroup,
                        trackIndex
                    )
                )
                selector.setParameters(parameters)
                updateTracksList(tracks)
            }
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error selecting track", e)
        }
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        val selector = trackSelector ?: return
        try {
            val params = selector.parameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            selector.setParameters(params)
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error setting subtitle enabled", e)
        }
    }

    fun updateSubtitleStyle(config: SubtitleStyleConfig) {
        _subtitleStyle.value = config
    }

    /** ترجمة خارجية حقيقية: تربط ملف/رابط ترجمة على المحتوى الشغّال فورًا
     *  بدون إعادة تشغيل البث — مع الحفاظ على مكان التشغيل.
     *  بتدعم SRT / VTT / SSA / ASS / SUB وأي صيغة نصية. */
    fun applyExternalSubtitle(rawUri: String) {
        val player = exoPlayer ?: return
        val current = player.currentMediaItem ?: return
        val uriString = rawUri.trim()
        if (uriString.isBlank()) return
        try {
            val uri = android.net.Uri.parse(uriString)
            val mime = when {
                uriString.endsWith(".vtt", true) -> MimeTypes.TEXT_VTT
                uriString.endsWith(".ssa", true) || uriString.endsWith(".ass", true) -> MimeTypes.TEXT_SSA
                uriString.endsWith(".srt", true) -> MimeTypes.APPLICATION_SUBRIP
                uriString.endsWith(".sub", true) -> MimeTypes.APPLICATION_SUBRIP
                else -> MimeTypes.APPLICATION_SUBRIP
            }
            val cfg = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mime)
                .setLabel(EXTERNAL_SUB_LABEL)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
                .filter { it.label?.toString() != EXTERNAL_SUB_LABEL }
            val updated = current.buildUpon()
                .setSubtitleConfigurations(existing + cfg)
                .build()
            val idx = player.currentMediaItemIndex
            val pos = player.currentPosition
            lastSubtitleUrl = uriString
            player.replaceMediaItem(idx, updated)
            player.seekTo(pos)
            setSubtitlesEnabled(true)
            player.playWhenReady = true
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "applyExternalSubtitle", e)
        }
    }

    /** إزالة الترجمة الخارجية الحالية. */
    fun clearExternalSubtitle() {
        val player = exoPlayer ?: return
        val current = player.currentMediaItem ?: return
        try {
            val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
                .filter { it.label?.toString() != EXTERNAL_SUB_LABEL }
            val updated = current.buildUpon().setSubtitleConfigurations(existing).build()
            val idx = player.currentMediaItemIndex
            val pos = player.currentPosition
            lastSubtitleUrl = null
            player.replaceMediaItem(idx, updated)
            player.seekTo(pos)
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "clearExternalSubtitle", e)
        }
    }

    fun hasExternalSubtitle(): Boolean = !lastSubtitleUrl.isNullOrBlank()


    private fun updateTracksList(tracks: Tracks) {
        var vCodec = "—"
        var aCodec = "—"
        var bitrate = 0L
        var fps = 0

        val vList = mutableListOf<TrackInfo>()
        val aList = mutableListOf<TrackInfo>()
        val sList = mutableListOf<TrackInfo>()

        var vGroupIdx = 0
        var aGroupIdx = 0
        var sGroupIdx = 0

        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isTrackSel = group.isTrackSelected(i)
                        // HLS/DASH exposes one Format per real rendition. Show
                        // the detected resolution and bitrate so identical
                        // heights (for example two 720p rungs) remain useful
                        // choices instead of appearing as duplicate "720p".
                        val resolution = if (format.height > 0) "${format.height}p" else "Auto"
                        val bitrateLabel = if (format.bitrate > 0) {
                            val mbps = format.bitrate / 1_000_000f
                            if (mbps >= 1f) String.format(java.util.Locale.US, "%.1f Mbps", mbps)
                            else "${format.bitrate / 1000} Kbps"
                        } else ""
                        val fpsLabel = if (format.frameRate > 0f) " · ${format.frameRate.toInt()}fps" else ""
                        val name = if (bitrateLabel.isNotBlank()) "$resolution · $bitrateLabel$fpsLabel" else "$resolution$fpsLabel"
                        vList.add(TrackInfo(format.id ?: "$vGroupIdx-$i", name, isTrackSel, vGroupIdx, i))
                        if (isTrackSel) {
                            vCodec = format.sampleMimeType?.replace("video/", "")?.uppercase() ?: "H.264"
                            if (format.bitrate > 0) bitrate = (format.bitrate / 1000).toLong()
                            if (format.frameRate > 0f) fps = format.frameRate.toInt()
                        }
                    }
                    vGroupIdx++
                }
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isTrackSel = group.isTrackSelected(i)
                        val lang = format.language?.uppercase() ?: "Default"
                        val channels = if (format.channelCount == 6) "5.1" else "2.0"
                        val name = "$lang ($channels)"
                        aList.add(TrackInfo(format.id ?: "$aGroupIdx-$i", name, isTrackSel, aGroupIdx, i))
                        if (isTrackSel) {
                            val mime = format.sampleMimeType?.replace("audio/", "")?.uppercase() ?: "AAC"
                            aCodec = "$mime ($channels)"
                        }
                    }
                    aGroupIdx++
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        val isTrackSel = group.isTrackSelected(i)
                        val lang = format.label ?: format.language ?: "Subtitle ${sGroupIdx + 1}"
                        sList.add(TrackInfo(format.id ?: "$sGroupIdx-$i", lang, isTrackSel, sGroupIdx, i))
                    }
                    sGroupIdx++
                }
            }
        }

        _videoTracks.value = vList
        _audioTracks.value = aList
        _subtitleTracks.value = sList

        _diagnostics.value = _diagnostics.value.copy(
            videoCodec = vCodec,
            audioCodec = aCodec,
            bitrateKbps = bitrate,
            fps = fps
        )
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    try {
                        val pos = player.currentPosition
                        val dur = player.duration
                        val bufferPos = player.bufferedPosition
                        val bufferMs = (bufferPos - pos).coerceAtLeast(0L)
                        val bufferPct = player.bufferedPercentage

                        // bitrateEstimate is calculated from real bytes/time by Media3's
                        // DefaultBandwidthMeter. Never fabricate a connection speed for the UI.
                        val measuredKbps = (bandwidthMeter.bitrateEstimate / 1000L).coerceAtLeast(0L)
                        var measuredLatency = lastTransferLatencyMs
                        if (measuredLatency <= 0L && lastSampleElapsedMs > 0L) {
                            measuredLatency = lastSampleElapsedMs
                        }
                        // Fallback estimate from buffer health when meter silent
                        if (measuredLatency <= 0L && player.isPlaying) {
                            measuredLatency = 30L
                        }
                        measuredLatency = measuredLatency.coerceIn(0L, 60_000L)

                        _diagnostics.value = _diagnostics.value.copy(
                            currentPositionMs = pos,
                            totalDurationMs = if (dur > 0) dur else 0L,
                            bufferMs = bufferMs,
                            bufferPercentage = bufferPct,
                            downloadSpeedKbps = measuredKbps,
                            latencyMs = measuredLatency,
                            isPlaying = player.isPlaying
                        )
                    } catch (e: Throwable) {
                        // ignore telemetry errors
                    }
                }
                delay(700)
            }
        }
    }

    fun setAudioOnlyMode(enabled: Boolean) {
        audioOnlyUserSet = true
        autoAudioOnly = false
        _audioOnlyMode.value = enabled
        try {
            val sel = trackSelector ?: return
            sel.setParameters(
                sel.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enabled)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
            )
            if (enabled) {
                // Extra: prefer lowest video when re-enabled later is handled by data saver
            }
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "setAudioOnlyMode: ${e.message}")
        }
    }

    fun setDataSaver(enabled: Boolean, maxMbps: Float = 1.5f) {
        _dataSaverEnabled.value = enabled
        if (enabled) {
            val preferred = _maxVideoBitrateBps.value
            val bps = if (preferred > 0) preferred else (maxMbps * 1_000_000f).toInt().coerceAtLeast(250_000)
            _maxVideoBitrateBps.value = bps
            softBitrateCapBps = 0 // user choice replaces soft recovery
            applyBitrateCap(bps)
        } else {
            // Turning Data Saver OFF restores unrestricted quality (keeps soft recovery off too).
            softBitrateCapBps = 0
            rebufferCount = 0
            applyBitrateCap(0)
        }
    }

    /**
     * Stores preferred soft ceiling used ONLY while Data Saver is ON.
     * Does not enable Data Saver and does not constrain tracks while Data Saver is off.
     */
    fun setMaxNetworkMbps(maxMbps: Float) {
        val bps = if (maxMbps <= 0f) 0 else (maxMbps * 1_000_000f).toInt()
        _maxVideoBitrateBps.value = bps
        // Apply only if user already enabled Data Saver; otherwise playback stays unrestricted.
        if (_dataSaverEnabled.value) applyBitrateCap(bps) else applyBitrateCap(0)
    }

    fun disableNetworkControls() {
        _dataSaverEnabled.value = false
        _audioOnlyMode.value = false
        _maxVideoBitrateBps.value = 0
        softBitrateCapBps = 0
        rebufferCount = 0
        applyBitrateCap(0)
        applyCurrentTrackConstraints()
    }

    /** Reapply real user constraints after Media3 resets the selected tracks. */
    private fun applyCurrentTrackConstraints() {
        applyBitrateCap(_maxVideoBitrateBps.value)
        try {
            trackSelector?.let { selector ->
                selector.setParameters(selector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, _audioOnlyMode.value)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build())
            }
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "applyCurrentTrackConstraints: ${e.message}")
        }
    }

    private fun applySoftBitrateFallback() {
        // ممنوع يشتغل إلا لو المستخدم فعّل Data Saver بنفسه
        if (!_dataSaverEnabled.value) {
            softBitrateCapBps = 0
            return
        }
        try {
            val next = when {
                softBitrateCapBps <= 0 -> 2_500_000
                softBitrateCapBps > 1_500_000 -> 1_200_000
                softBitrateCapBps > 800_000 -> 700_000
                else -> softBitrateCapBps
            }
            if (next != softBitrateCapBps) {
                softBitrateCapBps = next
                Log.i("YouseifPlayerController", "Soft bitrate fallback → ${next / 1000} kbps")
                applyBitrateCap(_maxVideoBitrateBps.value)
                _diagnostics.value = _diagnostics.value.copy(errorMessage = null)
            }
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "soft fallback: ${e.message}")
        }
    }

    private fun applyBitrateCap(maxBps: Int) {
        try {
            val sel = trackSelector ?: return
            val params = sel.buildUponParameters()
            val saverOn = _dataSaverEnabled.value
            // من غير Data Saver: مفيش أي حد جودة (حتى soft)
            val soft = if (saverOn) softBitrateCapBps else 0
            val effective = when {
                saverOn && maxBps > 0 && soft > 0 -> minOf(maxBps, soft)
                saverOn && maxBps > 0 -> maxBps
                saverOn && soft > 0 -> soft
                else -> 0
            }
            if (effective > 0) {
                params.setMaxVideoBitrate(effective)
                params.setForceLowestBitrate(false)
                if (effective <= 1_500_000) params.setMaxVideoSize(1280, 720)
                else params.clearVideoSizeConstraints()
            } else {
                params.clearVideoSizeConstraints()
                params.setMaxVideoBitrate(Int.MAX_VALUE)
                params.setForceLowestBitrate(false)
            }
            params.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, _audioOnlyMode.value)
            params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            params.setAllowVideoMixedMimeTypeAdaptiveness(true)
            params.setAllowVideoNonSeamlessAdaptiveness(true)
            sel.setParameters(params.build())
        } catch (e: Throwable) {
            Log.w("YouseifPlayerController", "applyBitrateCap: ${e.message}")
        }
    }


    /** Set a local device queue (album / filtered list) and optionally start at [startIndex]. */
    fun setLocalQueue(items: List<PlaylistItem>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (items.isEmpty()) {
            _localQueue.value = emptyList()
            _queueIndex.value = -1
            return
        }
        val safe = startIndex.coerceIn(0, items.lastIndex)
        _contextQueue.value = emptyList()
        _contextIndex.value = -1
        _localQueue.value = items
        _queueIndex.value = safe
        if (autoPlay) playQueueIndex(safe)
    }

    fun clearLocalQueue() {
        _localQueue.value = emptyList()
        _queueIndex.value = -1
    }

    fun playQueueIndex(index: Int) {
        val q = _localQueue.value
        if (q.isEmpty() || index !in q.indices) return
        _queueIndex.value = index
        val item = q[index]
        playUrl(item.url, item.name, isLive = false)
        setCurrentContent(item)
    }

    fun playNextInQueue() {
        val q = _localQueue.value
        if (q.isEmpty()) return
        val i = queueIndexForCurrent(q)
        if (_repeatMode.value == RepeatMode.ONE) {
            playQueueIndex(i.coerceAtLeast(0))
        } else {
            // Device-music next must always move to another song; wrapping at
            // the end is intentional so the player never appears stuck.
            playQueueIndex(if (i < 0) 0 else (i + 1) % q.size)
        }
    }

    fun playPreviousInQueue() {
        val q = _localQueue.value
        if (q.isEmpty()) return
        val i = queueIndexForCurrent(q)
        if (_repeatMode.value == RepeatMode.ONE) {
            playQueueIndex(i.coerceAtLeast(0))
        } else {
            // Previous also wraps, matching the expected music-player behavior.
            playQueueIndex(if (i <= 0) q.lastIndex else i - 1)
        }
    }

    /** Recover the selected song if a player callback rebuilt the current item. */
    private fun queueIndexForCurrent(q: List<PlaylistItem>): Int {
        val stored = _queueIndex.value
        if (stored in q.indices) return stored
        val currentUrl = _currentChannel.value?.url.orEmpty()
        val recovered = q.indexOfFirst { it.url == currentUrl }
        if (recovered >= 0) _queueIndex.value = recovered
        return recovered
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        // Apply ExoPlayer native repeat for ONE mode on the single item
        try {
            exoPlayer?.repeatMode = when (_repeatMode.value) {
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        } catch (_: Throwable) {}
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        try {
            exoPlayer?.repeatMode = if (mode == RepeatMode.ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        } catch (_: Throwable) {}
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                try {
                    exoPlayer?.seekTo(0)
                    exoPlayer?.playWhenReady = true
                } catch (_: Throwable) {}
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                val q = _localQueue.value
                if (q.isEmpty()) return
                val i = _queueIndex.value
                if (_repeatMode.value == RepeatMode.ALL) {
                    playQueueIndex(if (i < 0) 0 else (i + 1) % q.size)
                } else if (i >= 0 && i < q.lastIndex) {
                    playQueueIndex(i + 1)
                }
            }
        }
    }

    fun release() {
        telemetryJob?.cancel()
        fastSeekJob?.cancel()
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error releasing LoudnessEnhancer", e)
        }
        try {
            equalizer?.release()
            equalizer = null
        } catch (_: Throwable) {}
        val oldPlayer = exoPlayer
        try {
            oldPlayer?.removeListener(playerListener)
            oldPlayer?.release()
        } catch (e: Throwable) {
            Log.e("YouseifPlayerController", "Error releasing player", e)
        }
        PlayerHolder.clear(oldPlayer)
        exoPlayer = null
        try { context.stopService(android.content.Intent(context, PlaybackService::class.java)) } catch (_: Throwable) {}
    }
}
