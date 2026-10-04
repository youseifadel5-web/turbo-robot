package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.example.MainActivity
import com.example.R

/**
 * إشعار ميديا حديث (زي تيك توك / يوتيوب ميوزك / سبوتيفاي):
 * - أزرار السابق / تشغيل-إيقاف / التالي ظاهرة مباشرة في الصف المضغوط.
 * - شريط التقدّم (seekbar) بيظهر لأن الإشعار مربوط بـ MediaSessionCompat Token
 *   وBePlaybackState بتاعة media3 بتنشر position/duration + ACTION_SEEK_TO.
 * - كبير أيقونة (artwork) + لون البراند (colorized) + عنوان ووصف حقيقيين.
 * - مفيش "جاري التحضير…" واقف — بيتحدث فورًا مع حالة التشغيل.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val mainHandler by lazy { android.os.Handler(mainLooper) }
    private var appIconBitmap: Bitmap? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refreshNotification()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshNotification()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            refreshNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        try {
            appIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.youseif_app_icon)
        } catch (_: Throwable) {}

        // مزوّد مخصص: الإشعار بيتبني عندنا بالشكل الحديث بدل الافتراضي الفقير
        try {
            setMediaNotificationProvider(YouseifNotificationProvider())
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "custom provider failed: ${t.message}")
        }

        // foreground فوري (مطلوب أندرويد 12+) — بنفس الإشعار الحديث
        try {
            val boot = buildRichNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, boot, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, boot)
            }
        } catch (t: Throwable) {
            android.util.Log.e(TAG, "startForeground: ${t.message}", t)
        }

        attachSessionWhenReady()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        // قناة جديدة عشان النظام ما يفضلش الشكل القديم
        try { nm.deleteNotificationChannel("youseif_playback") } catch (_: Throwable) {}
        try { nm.deleteNotificationChannel("youseif_playback_v2") } catch (_: Throwable) {}
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الوسائط",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "مشغّل الوسائط في لوحة التحكم"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    /** الإشعار الحديث — بيتستخدم للستارت واللوك سكرين ولوحة التحكم. */
    private fun buildRichNotification(session: MediaSession? = mediaSession): Notification {
        val player = PlayerHolder.player
        val rawTitle = PlayerHolder.title.ifBlank {
            player?.mediaMetadata?.title?.toString()
        }.orEmpty().trim()
        val title = rawTitle.ifBlank { "Youseif Player Pro" }
        val isPlaying = (player?.isPlaying == true) || (player?.playWhenReady == true)
        val buffering = player?.playbackState == Player.STATE_BUFFERING

        // Use the session passed by Media3 when the provider is first created;
        // the service field may not be assigned yet at that exact moment.
        val token = try { session?.sessionCompatToken } catch (_: Throwable) { null }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(
                when {
                    buffering -> "جاري التحضير…"
                    isPlaying -> "يُشغّل الآن · Youseif Player Pro"
                    else -> "متوقف مؤقتًا · Youseif Player Pro"
                }
            )
            .setSubText("Youseif Player Pro")
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(appIconBitmap)
            .setContentIntent(activityIntent())
            .setDeleteIntent(serviceIntent(ACTION_STOP, REQ_STOP))
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(ACCENT_COLOR)
            .setColorized(true)
            .addAction(R.drawable.ic_notif_prev, "السابق", serviceIntent(ACTION_PREV, REQ_PREV))
            .addAction(
                if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play,
                if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                serviceIntent(ACTION_TOGGLE, REQ_TOGGLE)
            )
            .addAction(R.drawable.ic_notif_next, "التالي", serviceIntent(ACTION_NEXT, REQ_NEXT))
            .addAction(R.drawable.ic_notif_stop, "إغلاق", serviceIntent(ACTION_STOP, REQ_CLOSE))

        val style = MediaStyle().setShowActionsInCompactView(0, 1, 2)
        if (token != null) style.setMediaSession(token)
        builder.setStyle(style)

        return builder.build()
    }

    private fun refreshNotification() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        try {
            nm.notify(NOTIF_ID, buildRichNotification())
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "notify: ${t.message}")
        }
    }

    private fun activityIntent(): PendingIntent = PendingIntent.getActivity(
        this, REQ_OPEN,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceIntent(action: String, req: Int): PendingIntent = PendingIntent.getService(
        this, req,
        Intent(this, PlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun attachSessionWhenReady() {
        var attempts = 0
        val task = object : Runnable {
            override fun run() {
                val player = PlayerHolder.player
                if (player != null) {
                    try {
                        bindSession(player)
                        return
                    } catch (t: Throwable) {
                        android.util.Log.w(TAG, "bindSession: ${t.message}")
                    }
                }
                if (attempts++ < 100) mainHandler.postDelayed(this, 80L)
            }
        }
        mainHandler.post(task)
    }

    private fun pushMetadataAndNotify() {
        val player = PlayerHolder.player ?: return
        val title = PlayerHolder.title.ifBlank {
            player.mediaMetadata.title?.toString()
        }.orEmpty().ifBlank { "Youseif Player" }
        try {
            val current = player.currentMediaItem
            val meta = MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .setArtist("Youseif Player Pro")
                .setAlbumTitle(if (PlayerHolder.isLive) "بث مباشر" else "المكتبة")
                .setIsPlayable(true)
                .setMediaType(
                    if (PlayerHolder.isLive) MediaMetadata.MEDIA_TYPE_RADIO_STATION
                    else MediaMetadata.MEDIA_TYPE_MUSIC
                )
                .apply {
                    try {
                        val bmp = appIconBitmap
                        if (bmp != null) {
                            val bytes = java.io.ByteArrayOutputStream().use { out ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                                out.toByteArray()
                            }
                            setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        }
                    } catch (_: Throwable) {}
                }
                .build()
            if (current != null) {
                val idx = player.currentMediaItemIndex
                if (idx >= 0) {
                    val updated = current.buildUpon().setMediaMetadata(meta).build()
                    val oldTitle = current.mediaMetadata.title?.toString().orEmpty()
                    if (!oldTitle.equals(title, true) || current.mediaMetadata.artworkData == null) {
                        player.replaceMediaItem(idx, updated)
                    }
                }
            }
        } catch (t: Throwable) {
            android.util.Log.w(TAG, "pushMetadata: ${t.message}")
        }
        refreshNotification()
    }

    private fun bindSession(player: Player) {
        mediaSession?.let { old ->
            // Same underlying ExoPlayer already bound (via our wrapper) — just refresh.
            if (unwrapPlayer(old.player) === player) {
                pushMetadataAndNotify()
                return
            }
            try { old.player.removeListener(playerListener) } catch (_: Throwable) {}
            try { old.release() } catch (_: Throwable) {}
            mediaSession = null
        }
        try { player.removeListener(playerListener) } catch (_: Throwable) {}
        player.addListener(playerListener)
        pushMetadataAndNotify()

        // Wrap so system UI (shade / lock screen) always enables prev/next.
        // ExoPlayer with a single MediaItem reports SEEK_TO_NEXT as unavailable,
        // which greys out the right arrow even though PlaybackBridge can flip.
        val sessionPlayer = AlwaysNavigablePlayer(player)

        // أزرار السابق/التالي تظهر كمان في شاشة القفل ولوحة التحكم (Custom Layout)
        val cmdNext = SessionCommand(ACTION_SESSION_NEXT, Bundle.EMPTY)
        val cmdPrev = SessionCommand(ACTION_SESSION_PREV, Bundle.EMPTY)
        val sessionButtons = ImmutableList.of(
            CommandButton.Builder()
                .setSessionCommand(cmdPrev)
                .setIconResId(R.drawable.ic_notif_prev)
                .setDisplayName("السابق")
                .build(),
            CommandButton.Builder()
                .setSessionCommand(cmdNext)
                .setIconResId(R.drawable.ic_notif_next)
                .setDisplayName("التالي")
                .build()
        )
        mediaSession = MediaSession.Builder(this, sessionPlayer)
            .setId("YouseifPlayerSession")
            .setSessionActivity(activityIntent())
            .setCustomLayout(sessionButtons)
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    // Lock-screen/system media controls send standard Media3
                    // commands, not our private PendingIntent actions. Handle
                    // them here so navigation works while the Activity is
                    // completely in the background.
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT_WINDOW -> {
                            if (PlaybackBridge.next()) return Player.COMMAND_INVALID
                        }
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS_WINDOW -> {
                            if (PlaybackBridge.previous()) return Player.COMMAND_INVALID
                        }
                        Player.COMMAND_PLAY_PAUSE -> {
                            // Prefer bridge so resume-after-empty works from shade.
                            if (PlaybackBridge.toggleOrResume()) return Player.COMMAND_INVALID
                        }
                    }
                    return playerCommand
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        ACTION_SESSION_NEXT -> PlaybackBridge.next()
                        ACTION_SESSION_PREV -> PlaybackBridge.previous()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()

        android.util.Log.i(TAG, "MediaSession ready title=${PlayerHolder.title} playing=${player.isPlaying}")
    }

    /** Unwrap AlwaysNavigablePlayer → real ExoPlayer for identity checks. */
    private fun unwrapPlayer(p: Player?): Player? = when (p) {
        is AlwaysNavigablePlayer -> p.delegate
        else -> p
    }

    /**
     * ForwardingPlayer that always advertises next/previous as available so the
     * system media controls never grey them out. Actual navigation is handled by
     * PlaybackBridge (context queue), not by ExoPlayer's single-item timeline.
     */
    private class AlwaysNavigablePlayer(val delegate: Player) :
        androidx.media3.common.ForwardingPlayer(delegate) {
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands()
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_PLAY_PAUSE -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun hasNextMediaItem(): Boolean = true
        override fun hasPreviousMediaItem(): Boolean = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> mainHandler.post {
                PlayerHolder.player?.let {
                    try { bindSession(it) } catch (t: Throwable) {
                        android.util.Log.w(TAG, "refresh: ${t.message}")
                    }
                }
                refreshNotification()
            }
            ACTION_TOGGLE -> {
                // لو المشغّل فاضي: يرجّع آخر بث كان شغّال. غير كده: تشغيل/إيقاف عادي.
                val handled = PlaybackBridge.toggleOrResume()
                if (!handled) PlayerHolder.player?.let {
                    if (it.isPlaying || it.playWhenReady) it.pause() else it.play()
                }
                refreshNotification()
            }
            ACTION_STOP -> {
                try { PlayerHolder.player?.pause() } catch (_: Throwable) {}
                try { PlayerHolder.player?.stop() } catch (_: Throwable) {}
                stopSelf()
            }
            ACTION_NEXT -> {
                // تقليب حقيقي على السياق (قناة/فيلم/جودة/أغنية) — مش No-op على عنصر واحد
                val done = PlaybackBridge.next()
                if (!done) runCatching {
                    val p = PlayerHolder.player
                    if (p != null && p.hasNextMediaItem()) p.seekToNextMediaItem()
                }
                refreshNotification()
            }
            ACTION_PREV -> {
                val done = PlaybackBridge.previous()
                if (!done) runCatching {
                    val p = PlayerHolder.player
                    if (p != null && p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
                }
                refreshNotification()
            }
            else -> {
                if (mediaSession == null) attachSessionWhenReady()
                refreshNotification()
            }
        }
        // لازم نرجّع START_STICKY مع تشغيل الـ foreground مستمر
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlayerHolder.player
        val keep = player != null && (
            player.isPlaying ||
                player.playbackState == Player.STATE_BUFFERING ||
                player.playWhenReady
            )
        if (!keep) {
            try { mediaSession?.release() } catch (_: Throwable) {}
            mediaSession = null
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        try { mediaSession?.player?.removeListener(playerListener) } catch (_: Throwable) {}
        try { mediaSession?.release() } catch (_: Throwable) {}
        mediaSession = null
        try {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
            else @Suppress("DEPRECATION") stopForeground(true)
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    /** مزوّد إشعارات مخصص — يستبدل الافتراضي الفقير بإشعار فيه أزرار + seekbar. */
    @UnstableApi
    private inner class YouseifNotificationProvider : MediaNotification.Provider {

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback
        ): MediaNotification {
            return MediaNotification(NOTIF_ID, buildRichNotification(mediaSession))
        }

        override fun handleCustomCommand(
            mediaSession: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean = false
    }

    companion object {
        private const val TAG = "YouseifPlayback"
        const val CHANNEL_ID = "youseif_media_v3"
        const val NOTIF_ID = 4401
        const val ACTION_REFRESH = "com.youseif.player.SESSION_REFRESH"
        private const val ACTION_TOGGLE = "com.youseif.player.NOTIF_TOGGLE"
        private const val ACTION_STOP = "com.youseif.player.NOTIF_STOP"
        private const val ACTION_NEXT = "com.youseif.player.NOTIF_NEXT"
        private const val ACTION_PREV = "com.youseif.player.NOTIF_PREV"
        private const val ACTION_SESSION_NEXT = "com.youseif.player.SESSION_NEXT"
        private const val ACTION_SESSION_PREV = "com.youseif.player.SESSION_PREV"
        // Gold-orange brand color; intentionally not the old yellow accent.
        private const val ACCENT_COLOR = 0xFFE88900.toInt()
        private const val REQ_OPEN = 10
        private const val REQ_PREV = 11
        private const val REQ_TOGGLE = 12
        private const val REQ_NEXT = 13
        private const val REQ_STOP = 14
        private const val REQ_CLOSE = 15
    }
}
