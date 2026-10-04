package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.data.PlaylistItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.player.AudioBoostLevel
import com.example.player.AudioMixMode
import com.example.player.SubtitleStyleConfig
import com.example.player.VideoScaleMode
import com.example.player.YouseifPlayerController
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    controller: YouseifPlayerController,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    isInPipMode: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onPreviousChannel: () -> Unit = {},
    onNextChannel: () -> Unit = {},
    onOpenPlaylist: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    suggestionItems: List<PlaylistItem> = emptyList(),
    onPlaySuggested: (PlaylistItem) -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    /** كل القنوات الحية — عشان نعرف سيرفرات/جودات نفس القناة (· S1 / · HD) */
    allLiveChannels: List<PlaylistItem> = emptyList(),
    onSwitchChannelServer: (PlaylistItem) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = run {
        var current: android.content.Context = context
        while (current is android.content.ContextWrapper) {
            if (current is Activity) break
            current = current.baseContext
        }
        current as? Activity
    }
    val palette = activePlayerPalette()
    val diagnostics by controller.diagnostics.collectAsState()
    val currentChannel by controller.currentChannel.collectAsState()
    val streamTitle by controller.streamTitle.collectAsState()
    val isLocked by controller.isLocked.collectAsState()
    val scaleMode by controller.scaleMode.collectAsState()
    val isMuted by controller.isMuted.collectAsState()
    val audioOnlyMode by controller.audioOnlyMode.collectAsState()
    val volume by controller.volume.collectAsState()
    val audioBoost by controller.audioBoost.collectAsState()
    val audioMixMode by controller.audioMixMode.collectAsState()
    val vocalMix by controller.vocalMix.collectAsState()
    val effectiveVolumePercent by controller.effectiveVolumePercent.collectAsState()
    val playbackSpeed by controller.playbackSpeed.collectAsState()
    val isWebEmbed by controller.isWebEmbed.collectAsState()
    val videoTracks by controller.videoTracks.collectAsState()
    val contentQualities by controller.qualities.collectAsState()
    val audioTracks by controller.audioTracks.collectAsState()
    val subtitleTracks by controller.subtitleTracks.collectAsState()
    val subtitleStyle by controller.subtitleStyle.collectAsState()
    val rawPlayerTitle = streamTitle.ifBlank { currentChannel?.name ?: "Ready" }.trim()
    val compactPlayerTitle = if (rawPlayerTitle.length > 30) rawPlayerTitle.take(27) + "…" else rawPlayerTitle

    // أولوية للمستخدم: أي ترجمة استوردها تتطبّق تلقائيًا على المحتوى الجديد
    LaunchedEffect(controller.getCurrentMediaUrl()) {
        try {
            val prefs = context.getSharedPreferences("youseif_subtitles", android.content.Context.MODE_PRIVATE)
            val saved = prefs.getString("subtitle_uri", null) ?: prefs.getString("subtitle_url", null)
            if (!saved.isNullOrBlank() && controller.getCurrentMediaUrl() != null) {
                controller.applyExternalSubtitle(saved)
            }
        } catch (_: Throwable) {}
    }

    var showControls by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showCastPicker by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistOverlay by remember { mutableStateOf(false) }

    // Gesture feedback indicators
    var gestureOverlayText by remember { mutableStateOf<String?>(null) }
    var gestureOverlayIcon by remember { mutableStateOf<String?>(null) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var isSeekingForwardContinuous by remember { mutableStateOf(false) }
    var isSeekingBackwardContinuous by remember { mutableStateOf(false) }

    // Brightness state (0..1)
    var currentBrightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f)
    }

    // Auto-hide controls after a short delay while playing (longer in fullscreen so icons stay usable)
    LaunchedEffect(showControls, diagnostics.isPlaying, isLocked, isFullscreen, isInPipMode) {
        if (isInPipMode) {
            showControls = false
            return@LaunchedEffect
        }
        if (showControls && diagnostics.isPlaying && !isLocked) {
            delay(if (isFullscreen) 7000 else 4500)
            showControls = false
        }
    }

    // Clear gesture feedback after delay
    LaunchedEffect(gestureOverlayText) {
        if (gestureOverlayText != null) {
            delay(1200)
            gestureOverlayText = null
            gestureOverlayIcon = null
        }
    }

    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(700)
            doubleTapFeedback = null
        }
    }

    Box(
        modifier = modifier
            .background(AmoledBlack)
            .border(
                width = if (isFullscreen) 0.dp else 1.dp,
                color = if (isFullscreen) Color.Transparent else CrimsonBorder,
                shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp)
            )
            .clip(if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp))
    ) {
        if (isWebEmbed) {
            // Web / HTML5 Embed Fallback — desktop UA + referer + mixed content
            // so hosts like VidTube / StreamWish / UpDown actually start the player.
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            // Critical for third-party embed players
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowContentAccess = true
                            allowFileAccess = true
                            loadsImagesAutomatically = true
                            blockNetworkImage = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = false
                            displayZoomControls = false
                            // Desktop Chrome UA — many hosts reject Android WebView UA
                            userAgentString =
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
                            // Hardware layer helps video decode in WebView
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        }
                        try {
                            android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        } catch (_: Throwable) {}
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean = false
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(
                                view: android.view.View?,
                                callback: CustomViewCallback?
                            ) {
                                // Allow fullscreen video from embed players
                                super.onShowCustomView(view, callback)
                            }
                        }
                    }
                },
                update = { wv ->
                    val u = currentChannel?.url.orEmpty()
                    if (u.isNotBlank() && u != wv.url) {
                        // Send Referer so domain-restricted embeds (VidTube etc.) accept the request
                        val headers = HashMap<String, String>()
                        try {
                            val host = java.net.URI(u).host
                            if (!host.isNullOrBlank()) {
                                headers["Referer"] = "https://$host/"
                            }
                        } catch (_: Throwable) {}
                        headers["Accept-Language"] = "ar,en;q=0.9"
                        if (headers.isNotEmpty()) {
                            wv.loadUrl(u, headers)
                        } else {
                            wv.loadUrl(u)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // ExoPlayer View — TextureView (via XML) so first play renders video inside Compose
            // SurfaceView often shows audio-only until activity restart.
            AndroidView(
                factory = { ctx ->
                    val view = try {
                        android.view.LayoutInflater.from(ctx)
                            .inflate(com.example.R.layout.player_view_texture, null, false) as PlayerView
                    } catch (e: Throwable) {
                        android.util.Log.e("VideoPlayerView", "texture inflate failed, fallback", e)
                        PlayerView(ctx)
                    }
                    view.apply {
                        useController = false
                        resizeMode = scaleMode.resizeMode
                        keepScreenOn = true
                        try {
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        } catch (_: Throwable) {}
                        player = controller.getPlayer()
                        // Force a layout pass so the texture surface is created before first frame
                        post {
                            try {
                                player = null
                                player = controller.getPlayer()
                                requestLayout()
                            } catch (_: Throwable) {}
                        }
                        subtitleView?.apply {
                            setStyle(
                                CaptionStyleCompat(
                                    subtitleStyle.textColor.toInt(),
                                    subtitleStyle.backgroundColor.toInt(),
                                    0x00000000,
                                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    0xFF000000.toInt(),
                                    null
                                )
                            )
                            setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleStyle.fontSizeSp)
                            try { setBottomPaddingFraction(subtitleStyle.bottomPaddingFraction.coerceIn(0f, 0.4f)) } catch (_: Throwable) {}
                        }
                    }
                },
                update = { playerView ->
                    val exo = controller.getPlayer()
                    // Always re-bind when player instance changes OR surface was lost
                    if (playerView.player !== exo) {
                        playerView.player = exo
                    }
                    playerView.resizeMode = scaleMode.resizeMode
                    playerView.subtitleView?.apply {
                        setStyle(
                            CaptionStyleCompat(
                                subtitleStyle.textColor.toInt(),
                                subtitleStyle.backgroundColor.toInt(),
                                0x00000000,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                0xFF000000.toInt(),
                                null
                            )
                        )
                        setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, subtitleStyle.fontSizeSp)
                        try { setBottomPaddingFraction(subtitleStyle.bottomPaddingFraction.coerceIn(0f, 0.4f)) } catch (_: Throwable) {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tap & Gesture Detector Layer (underneath controls, over video)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        // Unified drag: vertical = volume/brightness, horizontal = next/prev
                        var totalDx = 0f
                        var totalDy = 0f
                        var axisLocked: Int = 0 // 0 none, 1 horizontal, 2 vertical
                        detectDragGestures(
                            onDragStart = {
                                totalDx = 0f
                                totalDy = 0f
                                axisLocked = 0
                            },
                            onDragEnd = {
                                if (axisLocked == 1) {
                                    // Strong horizontal swipe → change track / channel
                                    val threshold = size.width * 0.12f
                                    when {
                                        totalDx <= -threshold -> {
                                            onNextChannel()
                                            gestureOverlayText = "التالي  »"
                                            gestureOverlayIcon = "next"
                                        }
                                        totalDx >= threshold -> {
                                            onPreviousChannel()
                                            gestureOverlayText = "«  السابق"
                                            gestureOverlayIcon = "prev"
                                        }
                                    }
                                }
                                axisLocked = 0
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDx += dragAmount.x
                                totalDy += dragAmount.y
                                if (axisLocked == 0) {
                                    if (kotlin.math.abs(totalDx) > 24f || kotlin.math.abs(totalDy) > 24f) {
                                        axisLocked = if (kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)) 1 else 2
                                    }
                                }
                                if (axisLocked == 2) {
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val delta = -dragAmount.y / size.height.toFloat().coerceAtLeast(1f)
                                    if (change.position.x > w * 0.5f) {
                                        // Volume 0..500 continuous
                                        val cur = try { controller.effectiveVolumePercent.value } catch (_: Throwable) { 100 }
                                        val next = (cur + (delta * 380f).toInt()).coerceIn(0, 500)
                                        controller.setEffectiveVolumePercent(next)
                                        gestureOverlayText = "الصوت $next%"
                                        gestureOverlayIcon = "vol"
                                    } else {
                                        try {
                                            val act = context as? android.app.Activity
                                            val win = act?.window
                                            if (win != null) {
                                                val lp = win.attributes
                                                val curB = if (lp.screenBrightness < 0f) 0.45f else lp.screenBrightness
                                                val nextBrightness = (curB + delta * 1.3f).coerceIn(0.01f, 1f)
                                                lp.screenBrightness = nextBrightness
                                                win.attributes = lp
                                                gestureOverlayText = "السطوع ${((nextBrightness * 100f).toInt())}%"
                                                gestureOverlayIcon = "bright"
                                            }
                                        } catch (_: Throwable) {}
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures {
                            showControls = !showControls
                        }
                    } else {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { offset ->
                                val isRightSide = offset.x > size.width / 2
                                if (isRightSide) {
                                    controller.seekRelative(10)
                                    doubleTapFeedback = "+10"
                                } else {
                                    controller.seekRelative(-10)
                                    doubleTapFeedback = "-10"
                                }
                            }
                        )
                    }
                }
        )

        if (audioOnlyMode && !isInPipMode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(palette.accentContainer.copy(alpha = .72f), palette.background, Color.Black)
                        )
                    )
                    // Forward taps so fullscreen controls can reappear
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showControls = !showControls })
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Box(
                        Modifier.size(116.dp).background(
                            Brush.radialGradient(listOf(palette.accentGlow.copy(alpha = .55f), Color.Transparent)),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Audio only", tint = palette.accentGlow, modifier = Modifier.size(58.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("𝑌𝑜𝑢𝑠𝑒𝑖𝑓 𝑝𝑙𝑎𝑦𝑒𝑟 𝑝𝑟𝑜", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    // اسم البث: سطر واحد فقط مع تمرير (marquee) للأسماء الطويلة
                    val displayTitle = streamTitle.ifBlank { "Audio Only" }
                    Text(
                        text = displayTitle,
                        color = palette.accentGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        // الاسم في النص بالظبط (كان مائل لليسار/الآخر)
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                    Text("AUDIO ONLY  •  DATA SAVER", color = palette.secondaryText, fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
            }
        }

        // Gesture Overlay Feedback (Brightness, Volume, Fast Seeking)
        AnimatedVisibility(
            visible = gestureOverlayText != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            gestureOverlayText?.let { text ->
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = text,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Double Tap +/-10s Indicators
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(
                if (doubleTapFeedback == "+10") Alignment.CenterEnd else Alignment.CenterStart
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .size(48.dp)
                    .background(NeonRed.copy(alpha = 0.3f), CircleShape)
                    .border(2.dp, NeonRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doubleTapFeedback ?: "",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }

        // Loading Indicator
        if (diagnostics.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NeonRed,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        // Error Banner
        if (diagnostics.errorMessage != null && !diagnostics.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "تعذر تشغيل الرابط أو البث",
                        color = NeonRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = diagnostics.errorMessage ?: "",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            controller.retry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRedContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(1.dp, NeonRed, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "إعادة المحاولة",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Main Player Controls Overlay (hidden completely in PiP — only video surface)
        AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    // Keep icons clear of notch / gesture bar in immersive fullscreen
                    .then(
                        if (isFullscreen) {
                            Modifier.windowInsetsPadding(
                                WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            )
                        } else Modifier
                    )
                    // Tap empty area to hide controls again
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showControls = false })
                    }
            ) {
                // Compact icons (slightly larger only in fullscreen for visibility)
                val barIcon = if (isFullscreen) 32.dp else 24.dp
                val barIconInner = if (isFullscreen) 18.dp else 15.dp

                if (isLocked) {
                    // Locked overlay: show only unlock button
                    IconButton(
                        onClick = { controller.toggleLock() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(NeonRedContainer, CircleShape)
                            .border(1.dp, NeonRed, CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Controls",
                            tint = NeonRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Top Player Bar — full width, no fixed height (was clipping icons in fullscreen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = if (isFullscreen) 8.dp else 6.dp, vertical = if (isFullscreen) 6.dp else 4.dp)
                            .height(if (isFullscreen) 44.dp else 36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Back, Channel Name, LIVE badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            if (isFullscreen) {
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.widthIn(max = if (isFullscreen) 220.dp else 150.dp)) {
                                // Only the active stream/channel name — never the app brand here
                                Text(
                                    text = if (diagnostics.isPlaying) rawPlayerTitle else compactPlayerTitle,
                                    color = palette.accentGlow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = if (diagnostics.isPlaying && rawPlayerTitle.length > 22) Modifier.basicMarquee() else Modifier
                                )
                                if (diagnostics.isLive) {
                                    Text(
                                        text = "LIVE STREAM",
                                        color = palette.secondaryText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (diagnostics.isLive) {
                                LiveBadge(isLive = true)
                            }
                        }

                        // Right: Cast, PiP, Subtitles, Quality, Audio, More (larger in fullscreen)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 6.dp else 2.dp)
                        ) {
                            // Cast حقيقي — قائمة أجهزة Chromecast + تحميل البث الحالي
                            IconButton(
                                onClick = {
                                    val act = activity
                                    if (act == null) {
                                        Toast.makeText(context, "تعذر فتح البث", Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    val url = controller.getCurrentMediaUrl() ?: currentChannel?.url
                                    val title = controller.getCurrentMediaTitle()
                                    val isLive = controller.isCurrentLive()
                                    // prepareCast: يجهّز البث (عبر البروكسي) ويرجّع true لو لازم نعرض قائمة الأجهزة
                                    val needPicker = com.example.player.CastHelper.prepareCast(
                                        activity = act,
                                        mediaUrl = url,
                                        title = title,
                                        isLive = isLive,
                                        referer = controller.getCurrentReferer(),
                                        userAgent = controller.getCurrentUserAgent()
                                    )
                                    if (needPicker) showCastPicker = true
                                },
                                modifier = Modifier.size(barIcon)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = "Cast",
                                    tint = if (com.example.player.CastHelper.isCasting(context)) NeonRed else TextSecondary,
                                    modifier = Modifier.size(barIconInner)
                                )
                            }

                            // PiP Button
                            IconButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        try {
                                            val enter = {
                                                // Prefer real video aspect from "1920x1080" style resolution; fall back to 16:9
                                                val resParts = diagnostics.resolution.lowercase()
                                                    .replace("×", "x").split("x")
                                                val vw = resParts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull()?.takeIf { it > 0 } ?: 16
                                                val vh = resParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.takeIf { it > 0 } ?: 9
                                                val ratio = try {
                                                    Rational(vw.coerceAtLeast(1), vh.coerceAtLeast(1))
                                                } catch (_: Throwable) { Rational(16, 9) }
                                                val decor = activity?.window?.decorView
                                                val hint = android.graphics.Rect()
                                                try { decor?.getGlobalVisibleRect(hint) } catch (_: Throwable) {}
                                                val b = PictureInPictureParams.Builder().setAspectRatio(ratio)
                                                if (!hint.isEmpty) b.setSourceRectHint(hint)
                                                if (android.os.Build.VERSION.SDK_INT >= 31) {
                                                    try { b.setAutoEnterEnabled(false) } catch (_: Throwable) {}
                                                    try { b.setSeamlessResizeEnabled(true) } catch (_: Throwable) {}
                                                }
                                                activity?.enterPictureInPictureMode(b.build())
                                                Unit
                                            }
                                            if (!isFullscreen) {
                                                onToggleFullscreen()
                                                activity?.window?.decorView?.postDelayed(enter, 180L)
                                            } else enter()
                                        } catch (e: Throwable) {
                                            Toast.makeText(context, "Picture-in-picture is not supported", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(barIcon)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(barIconInner)
                                )
                            }

                            // Subtitles (CC) — fixed-size control in the top row.
                            Box(
                                modifier = Modifier
                                    .size(barIcon)
                                    .clip(CircleShape)
                                    .background(if (subtitleTracks.any { it.isSelected }) palette.accentContainer else Color.Black.copy(alpha = .28f))
                                    .border(1.dp, if (subtitleTracks.any { it.isSelected }) palette.accentGlow else palette.border.copy(alpha = .7f), CircleShape)
                                    .clickable { showSubtitleDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitleTracks.any { it.isSelected }) palette.accentGlow else TextSecondary,
                                    modifier = Modifier.size(barIconInner)
                                )
                            }

                            // HD / Quality Button — fixed-size control in the top row.
                            IconButton(
                                onClick = { showQualityDialog = true },
                                modifier = Modifier.size(barIcon)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "Quality HD",
                                    tint = NeonRed,
                                    modifier = Modifier.size(barIconInner)
                                )
                            }

                            // Audio Boost & Tracks Button — fixed-size control in the top row.
                            IconButton(
                                onClick = { showAudioDialog = true },
                                modifier = Modifier.size(barIcon)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Audio Boost",
                                    tint = if (audioBoost != AudioBoostLevel.BOOST_100) NeonRed else TextSecondary,
                                    modifier = Modifier.size(barIconInner)
                                )
                            }

                            // More Menu Button
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier.size(barIcon)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(barIconInner)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier
                                        .background(DarkSurface)
                                        .border(1.dp, CrimsonBorder, RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Playback speed (${playbackSpeed}x)", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = NeonRed) },
                                        onClick = {
                                            showMoreMenu = false
                                            showSpeedDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Screen mode (${scaleMode.label})", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.AspectRatio, contentDescription = null, tint = NeonRed) },
                                        onClick = {
                                            showMoreMenu = false
                                            controller.cycleScaleMode()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Lock controls", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonRed) },
                                        onClick = {
                                            showMoreMenu = false
                                            controller.toggleLock()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Settings", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = NeonRed) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Upper Right: Orientation + Playlist (below top bar, always visible with controls)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = if (isFullscreen) 56.dp else 44.dp,
                                end = if (isFullscreen) 14.dp else 10.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onToggleOrientation,
                            modifier = Modifier
                                .background(DarkSurface.copy(alpha = 0.8f), CircleShape)
                                .border(1.dp, CrimsonBorder, CircleShape)
                                .size(if (isFullscreen) 36.dp else 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate screen",
                                tint = NeonRed,
                                modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showPlaylistOverlay = true },
                            modifier = Modifier
                                .background(DarkSurface.copy(alpha = 0.8f), CircleShape)
                                .border(1.dp, CrimsonBorder, CircleShape)
                                .size(if (isFullscreen) 36.dp else 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = "Playlist",
                                tint = NeonRed,
                                modifier = Modifier.size(if (isFullscreen) 20.dp else 18.dp)
                            )
                        }
                    }

                    // Center Playback Controls — clear of top bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 28.dp, bottom = 36.dp)
                            .fillMaxWidth(0.78f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Previous Channel / Rewind
                        IconButton(
                            onClick = onPreviousChannel,
                            modifier = Modifier
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Channel",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // -10s Back
                        IconButton(
                            onClick = { controller.seekRelative(-10) },
                            modifier = Modifier
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "10s Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // EXACT CENTER MAIN PLAY / PAUSE BUTTON WITH NEON GLOW
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(NeonRed, NeonRed.copy(alpha = 0.4f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                                .border(1.5.dp, NeonRed, CircleShape)
                                // زر التشغيل الأوسط: لو مفيش حاجة محمّلة يرجّع آخر بث/قناة/فيلم/فيديو
                                .clickable { controller.resumeOrToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (diagnostics.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (diagnostics.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // +10s Forward
                        IconButton(
                            onClick = { controller.seekRelative(10) },
                            modifier = Modifier
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "10s Forward",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Next Channel / Forward
                        IconButton(
                            onClick = onNextChannel,
                            modifier = Modifier
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Channel",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Bottom Bar: progress + Fit / mute / lock / fullscreen — always with safe padding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(
                                horizontal = if (isFullscreen) 16.dp else 10.dp,
                                vertical = if (isFullscreen) 12.dp else 6.dp
                            )
                    ) {
                        RealTimeProgressBar(
                            positionMs = diagnostics.currentPositionMs,
                            durationMs = diagnostics.totalDurationMs,
                            isLive = diagnostics.isLive,
                            palette = palette,
                            onSeek = controller::seekTo
                        )

                        // One bottom row: prev/play/next on LEFT · Fit / mute / lock / FS on RIGHT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 4.dp else 0.dp)
                            ) {
                                IconButton(onClick = onPreviousChannel, modifier = Modifier.size(barIcon)) {
                                    Icon(Icons.Default.SkipPrevious, "Previous", tint = TextPrimary, modifier = Modifier.size(barIconInner))
                                }
                                IconButton(
                                    onClick = { controller.resumeOrToggle() },
                                    modifier = Modifier.size(barIcon)
                                ) {
                                    Icon(
                                        imageVector = if (diagnostics.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (diagnostics.isPlaying) "إيقاف مؤقت" else "تشغيل",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(barIconInner)
                                    )
                                }
                                IconButton(onClick = onNextChannel, modifier = Modifier.size(barIcon)) {
                                    Icon(Icons.Default.SkipNext, "Next", tint = TextPrimary, modifier = Modifier.size(barIconInner))
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isFullscreen) 8.dp else 4.dp)
                            ) {
                                // Three Screen Modes Toggle Button
                                Surface(
                                    color = palette.accentContainer.copy(alpha = 0.42f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.accentGlow),
                                    modifier = Modifier.clickable { controller.cycleScaleMode() }
                                ) {
                                    Text(
                                        text = scaleMode.labelAr,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Mute Button
                                IconButton(
                                    onClick = { controller.toggleMute() },
                                    modifier = Modifier.size(barIcon)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) NeonRed else TextSecondary,
                                        modifier = Modifier.size(barIconInner)
                                    )
                                }

                                // Lock Button
                                IconButton(
                                    onClick = { controller.toggleLock() },
                                    modifier = Modifier.size(barIcon)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Lock",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(barIconInner)
                                    )
                                }

                                // Fullscreen Button
                                IconButton(
                                    onClick = onToggleFullscreen,
                                    modifier = Modifier.size(barIcon)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(barIconInner)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlaylistOverlay) {
        val current = currentChannel
        val suggestions = remember(current, suggestionItems) {
            val base = suggestionItems.filter { it.id != current?.id }
            val sameGroup = base.filter { current != null && it.group.equals(current.group, ignoreCase = true) }
            val sameKind = base.filter {
                current != null && it.isLive == current.isLive &&
                    !it.group.equals(current.group, ignoreCase = true)
            }
            (sameGroup + sameKind + base)
                .distinctBy { it.id }
                .sortedByDescending { it.logoUrl.isNotBlank() }
                .take(16)
        }
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)).clickable { showPlaylistOverlay = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceVariant, palette.background)))
                    .border(1.dp, palette.accentGlow.copy(alpha = .45f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable { }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("المحتوى المقترح", color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(if (current?.isLive == true) "قنوات مشابهة" else "أفلام ومسلسلات مشابهة", color = palette.secondaryText, fontSize = 11.sp)
                    }
                    IconButton(onClick = { showPlaylistOverlay = false }) {
                        Icon(Icons.Default.Close, "Close", tint = palette.accentGlow)
                    }
                }
                if (suggestions.isEmpty()) {
                    Text("لا توجد اقتراحات متاحة حاليًا", color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(suggestions) { item ->
                            Column(
                                Modifier.width(150.dp).clip(RoundedCornerShape(16.dp))
                                    .background(palette.cardDeep.copy(alpha = .92f))
                                    .border(1.dp, palette.border.copy(alpha = .8f), RoundedCornerShape(16.dp))
                                    .clickable { showPlaylistOverlay = false; onPlaySuggested(item) }
                                    .padding(10.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(12.dp))
                                        .background(palette.accentContainer.copy(alpha = .35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val logo = item.logoUrl.trim()
                                    if (logo.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(logo)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            if (item.isLive) Icons.Default.PlaylistPlay else Icons.Default.Info,
                                            null,
                                            tint = palette.accentGlow,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(item.name, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(item.group.substringAfterLast('/').trim(), color = palette.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    // QUALITY (HD) SELECTION DIALOG
    // بيعرض: 1) سيرفرات/جودات نفس القناة (· S1 / · HD)  2) تراكات HLS التكيفية
    if (showQualityDialog) {
        val cur = currentChannel
        val baseName = cur?.name?.substringBefore(" · ")?.trim().orEmpty()
        val serverVariants = remember(cur?.id, allLiveChannels) {
            if (cur == null || !cur.isLive || baseName.isBlank()) emptyList()
            else allLiveChannels
                .filter { it.isLive && it.name.substringBefore(" · ").trim().equals(baseName, ignoreCase = true) }
                .distinctBy { it.id }
                .sortedBy { it.name }
        }
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    if (serverVariants.size > 1) "جودة / سيرفر القناة" else "Video quality / resolution",
                    color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // ---- سيرفرات متعددة لنفس القناة (BEIN · S1 / S2 / HD) ----
                    if (serverVariants.size > 1) {
                        Text("سيرفرات القناة (${serverVariants.size})", color = TextSecondary, fontSize = 12.sp)
                        serverVariants.forEachIndexed { index, variant ->
                            val isCurrent = variant.id == cur?.id || variant.url == cur?.url
                            val label = variant.name.substringAfter(" · ", "").ifBlank {
                                when {
                                    variant.url.contains("1080", true) -> "1080p"
                                    variant.url.contains("720", true) -> "720p"
                                    variant.url.contains("480", true) -> "480p"
                                    variant.url.contains("360", true) -> "360p"
                                    else -> "سيرفر ${index + 1}"
                                }
                            }.ifBlank { "سيرفر ${index + 1}" }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        if (!isCurrent) onSwitchChannelServer(variant)
                                        showQualityDialog = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, color = if (isCurrent) NeonRed else TextPrimary, fontSize = 14.sp)
                                if (isCurrent) {
                                    Text("الحالي", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("دقة الستريم (HLS)", color = TextSecondary, fontSize = 12.sp)
                    }
                    // ---- الجودات/السيرفرات الحقيقية للمحتوى (1080p / 720p / 360p وسيرفرات متعددة) ----
                    if (contentQualities.isNotEmpty()) {
                        Text(
                            "الجودات / السيرفرات (${contentQualities.size})",
                            color = TextSecondary, fontSize = 12.sp
                        )
                        contentQualities.forEach { q ->
                            val isCur = q.url == cur?.url || q.url == controller.getCurrentMediaUrl()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCur) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        if (!isCur) controller.selectQuality(q)
                                        showQualityDialog = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (q.server.isBlank()) q.label else "${q.label} · ${q.server}",
                                    color = if (isCur) NeonRed else TextPrimary, fontSize = 14.sp
                                )
                                if (isCur) {
                                    Text("Selected", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("دقة الستريم (HLS)", color = TextSecondary, fontSize = 12.sp)
                    }
                    // ---- تراكات الفيديو التكيفية من ExoPlayer ----
                    if (videoTracks.isEmpty()) {
                        Text("Auto Adaptive HD", color = TextSecondary, fontSize = 14.sp)
                    } else {
                        videoTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (track.isSelected) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        controller.selectTrack(C.TRACK_TYPE_VIDEO, track.groupIndex, track.trackIndex)
                                        showQualityDialog = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(track.name, color = if (track.isSelected) NeonRed else TextPrimary, fontSize = 14.sp)
                                if (track.isSelected) {
                                    Text("Selected", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close", color = NeonRed)
                }
            }
        )
    }

    // AUDIO BOOST & TRACKS DIALOG
    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("الصوت والتعزيز", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("مستوى التعزيز", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioBoostLevel.values().forEach { level ->
                            val isSel = audioBoost == level
                            Surface(
                                color = if (isSel) NeonRed else DarkCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonRed else CrimsonBorder),
                                modifier = Modifier.clickable { controller.setAudioBoost(level) }
                            ) {
                                Text(
                                    text = "${level.percentage}%",
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text("مطرب / موسيقى (EQ تقريبي)", color = TextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AudioMixMode.values().forEach { mode ->
                            val isSel = audioMixMode == mode
                            Surface(
                                color = if (isSel) NeonRed else DarkCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonRed else CrimsonBorder),
                                modifier = Modifier.weight(1f).clickable { controller.setAudioMixMode(mode) }
                            ) {
                                Text(
                                    text = mode.labelAr,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp).fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Text("مزج: ${vocalMix.toInt()}%  ·  فعّال: $effectiveVolumePercent%", color = TextMuted, fontSize = 11.sp)
                    Slider(
                        value = vocalMix,
                        onValueChange = { controller.setVocalMix(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                    )

                    if (audioTracks.isNotEmpty()) {
                        Text("مسار الصوت", color = TextSecondary, fontSize = 12.sp)
                        audioTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (track.isSelected) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        controller.selectTrack(C.TRACK_TYPE_AUDIO, track.groupIndex, track.trackIndex)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(track.name, color = if (track.isSelected) NeonRed else TextPrimary, fontSize = 12.sp)
                                if (track.isSelected) {
                                    Text("Active", color = NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text("تم", color = NeonRed)
                }
            }
        )
    }

    // SUBTITLE CONTROLS — مضغوط + ألوان + موضع
    if (showSubtitleDialog) {
        var externalSubUrl by remember { mutableStateOf("") }
        val subFilePicker = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Throwable) {}
                controller.applyExternalSubtitle(uri.toString())
                showSubtitleDialog = false
            }
        }
        val subColors = listOf(
            "أبيض" to 0xFFFFFFFFL,
            "أصفر" to 0xFFFFFF00L,
            "سماوي" to 0xFF00E5FFL,
            "أخضر" to 0xFF69F0AEL,
            "برتقالي" to 0xFFFFAB40L,
            "أحمر" to 0xFFFF5252L
        )
        val sizeOptions = listOf("صغير" to 14f, "وسط" to 18f, "كبير" to 22f, "أكبر" to 28f)

        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("الترجمة (CC)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الترجمة المدمجة", color = TextSecondary, fontSize = 12.sp)
                        Surface(
                            color = if (subtitleTracks.any { it.isSelected }) NeonRed else DarkCardBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                            modifier = Modifier.clickable {
                                val hasActive = subtitleTracks.any { it.isSelected }
                                controller.setSubtitlesEnabled(!hasActive)
                            }
                        ) {
                            Text(
                                if (subtitleTracks.any { it.isSelected }) "إيقاف" else "تشغيل",
                                color = if (subtitleTracks.any { it.isSelected }) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (subtitleTracks.isNotEmpty()) {
                        subtitleTracks.forEach { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (track.isSelected) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        controller.selectTrack(C.TRACK_TYPE_TEXT, track.groupIndex, track.trackIndex)
                                    }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(track.name, color = if (track.isSelected) NeonRed else TextPrimary, fontSize = 12.sp)
                                if (track.isSelected) Text("مفعّل", color = NeonRed, fontSize = 10.sp)
                            }
                        }
                    }

                    Text("لون النص", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subColors.forEach { (label, argb) ->
                            val isSel = subtitleStyle.textColor == argb
                            Surface(
                                color = Color(argb.toInt()).copy(alpha = if (isSel) 1f else 0.35f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSel) 2.dp else 1.dp,
                                    if (isSel) NeonRed else CrimsonBorder
                                ),
                                modifier = Modifier.clickable {
                                    controller.updateSubtitleStyle(subtitleStyle.copy(textColor = argb))
                                }
                            ) {
                                Text(
                                    label,
                                    color = if (argb == 0xFFFFFFFFL || argb == 0xFFFFFF00L) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text("حجم الخط", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sizeOptions.forEach { (label, size) ->
                            val isSel = subtitleStyle.fontSizeSp == size
                            Surface(
                                color = if (isSel) NeonRed else DarkCardBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) NeonRed else CrimsonBorder),
                                modifier = Modifier.weight(1f).clickable {
                                    controller.updateSubtitleStyle(subtitleStyle.copy(fontSizeSp = size))
                                }
                            ) {
                                Text(
                                    label,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Text("موضع الترجمة (أسفل ← أعلى)", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = subtitleStyle.bottomPaddingFraction,
                        onValueChange = {
                            controller.updateSubtitleStyle(
                                subtitleStyle.copy(bottomPaddingFraction = it.coerceIn(0.02f, 0.35f))
                            )
                        },
                        valueRange = 0.02f..0.35f,
                        colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                    )

                    Text("تحميل ترجمة من ملف أو رابط (SRT · VTT · ASS · أي صيغة)", color = TextSecondary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = externalSubUrl,
                        onValueChange = { externalSubUrl = it },
                        placeholder = { Text("https://…/sub.srt", color = TextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonRed,
                            unfocusedBorderColor = CrimsonBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { subFilePicker.launch(arrayOf("*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRedContainer),
                            modifier = Modifier.weight(1f)
                        ) { Text("فتح ملف ترجمة", color = TextPrimary, fontSize = 12.sp) }
                        Button(
                            onClick = {
                                controller.clearExternalSubtitle()
                                showSubtitleDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBg),
                            modifier = Modifier.weight(1f)
                        ) { Text("إزالة الترجمة", color = TextPrimary, fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (externalSubUrl.isNotBlank()) {
                        // تطبيق حقيقي على المحتوى الشغّال — بدون إعادة تشغيل
                        controller.applyExternalSubtitle(externalSubUrl.trim())
                    }
                    showSubtitleDialog = false
                }) {
                    Text("تطبيق", color = NeonRed)
                }
            }
        )
    }

    // PLAYBACK SPEED DIALOG
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = DarkSurface,
            title = {
                Text("Playback speed", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSel = playbackSpeed == speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonRedContainer else Color.Transparent)
                                .clickable {
                                    controller.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${speed}x", color = if (isSel) NeonRed else TextPrimary, fontSize = 14.sp)
                            if (isSel) {
                                Text("Current speed", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel", color = NeonRed)
                }
            }
        )
    }

    if (showCastPicker) {
        CastDevicePickerDialog(
            onDismiss = {
                showCastPicker = false
                com.example.player.CastHelper.stopDiscovery(context)
            },
            onPick = { id ->
                com.example.player.CastHelper.selectRoute(context, id)
                showCastPicker = false
                com.example.player.CastHelper.stopDiscovery(context)
            }
        )
    }
}

/**
 * Always-visible progress control. VOD uses Media3 duration/position directly.
 * For an open-ended live timeline, the thumb follows the real elapsed playback
 * position inside a rolling minute window and no artificial duration is shown.
 */
@Composable
private fun RealTimeProgressBar(
    positionMs: Long,
    durationMs: Long,
    isLive: Boolean,
    palette: com.example.ui.theme.PlayerPalette,
    onSeek: (Long) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    val hasDuration = durationMs > 0L
    val liveWindowMs = 60_000L
    val engineProgress = if (hasDuration) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        // A rolling timeline, not a fabricated duration: the displayed label is elapsed LIVE time.
        ((positionMs % liveWindowMs).toFloat() / liveWindowMs.toFloat()).coerceIn(0f, 1f)
    }
    var dragProgress by remember(engineProgress) { mutableFloatStateOf(engineProgress) }
    val progress = if (dragging) dragProgress else engineProgress
    val shownTime = formatPlayerTime(positionMs)
    val totalTime = if (hasDuration) formatPlayerTime(durationMs) else null

    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(shownTime, color = palette.text, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(42.dp))
        Canvas(
            Modifier.weight(1f).height(if (dragging) 26.dp else 20.dp)
                .pointerInput(durationMs, isLive) {
                    detectTapGestures { offset ->
                        val tapped = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragProgress = tapped
                        if (hasDuration) onSeek((tapped * durationMs).toLong())
                    }
                }
                .pointerInput(durationMs, isLive) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragCancel = { dragging = false },
                        onDragEnd = {
                            if (hasDuration) onSeek((dragProgress * durationMs).toLong())
                            dragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val y = size.height / 2f
            val start = 4.dp.toPx()
            val end = size.width - 4.dp.toPx()
            val trackWidth = (end - start).coerceAtLeast(1f)
            val x = start + trackWidth * progress
            drawLine(palette.surfaceVariant.copy(alpha = .95f), Offset(start, y), Offset(end, y), 5.dp.toPx(), StrokeCap.Round)
            drawLine(palette.accent.copy(alpha = .95f), Offset(start, y), Offset(x, y), 5.dp.toPx(), StrokeCap.Round)
            drawCircle(palette.accentGlow.copy(alpha = .22f), 12.dp.toPx(), Offset(x, y))
            drawCircle(palette.accentGlow, if (dragging) 7.dp.toPx() else 5.dp.toPx(), Offset(x, y))
            drawCircle(Color.White.copy(alpha = .9f), if (dragging) 3.dp.toPx() else 2.dp.toPx(), Offset(x, y))
        }
        if (hasDuration) {
            Text(totalTime ?: "--:--", color = palette.secondaryText, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
        } else if (isLive) {
            Spacer(Modifier.width(8.dp))
            Text("LIVE", color = palette.accentGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

/**
 * قائمة اختيار جهاز البث — بديلنا الخاص لـ MediaRouteChooserDialog اللي كان بيقع.
 * بتقرا الأجهزة من CastHelper.devices وتتحدّث لحظيًا وقت البحث.
 */
@Composable
private fun CastDevicePickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val devices by com.example.player.CastHelper.devices.collectAsState()
    val palette = activePlayerPalette()
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkCardBg)
                .border(1.dp, CrimsonBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .clickable(enabled = false, onClick = {})
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("اختر جهاز البث", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (devices.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = palette.accentGlow
                    )
                    Text(
                        "بيدوّر على أجهزة على نفس شبكة الواي فاي…",
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
            } else {
                devices.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (d.connected) palette.accentContainer.copy(alpha = .5f) else Color.Transparent)
                            .clickable { onPick(d.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cast, contentDescription = null,
                            tint = palette.accentGlow, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            d.name, color = TextPrimary, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                        )
                        if (d.connected) {
                            Text("متصل", color = palette.accentGlow, fontSize = 11.sp)
                        }
                    }
                }
            }
            Text(
                "لو مش لاقي التلفزيون: اتأكد إنه على نفس الواي فاي، واقفل الـVPN.",
                color = TextMuted, fontSize = 11.sp
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("إغلاق", color = palette.accentGlow)
            }
        }
    }
}
