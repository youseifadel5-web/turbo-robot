package com.example.player

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.mediarouter.app.MediaRouteChooserDialog
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import org.json.JSONObject

/**
 * مساعد البث الحقيقي (Google Cast / Chromecast).
 * - يفتح قائمة الأجهزة المتوافقة
 * - يحمّل الرابط الحالي على الجهاز عند الاتصال
 */
object CastHelper {
    private const val TAG = "CastHelper"
    @Volatile private var initialized = false
    @Volatile private var pendingUrl: String? = null
    @Volatile private var pendingTitle: String = "Youseif Player"
    @Volatile private var pendingIsLive: Boolean = true
    @Volatile private var pendingContentType: String = "application/x-mpegURL"

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Log.i(TAG, "Cast session started: $sessionId")
            loadPending(session)
        }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            loadPending(session)
        }
        override fun onSessionEnded(session: CastSession, error: Int) {
            Log.i(TAG, "Cast session ended error=$error")
            try { CastProxy.stop() } catch (_: Throwable) {}
        }
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.w(TAG, "Cast session start failed: $error")
            try { CastProxy.stop() } catch (_: Throwable) {}
        }
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    fun init(context: Context) {
        if (initialized) return
        try {
            val castContext = CastContext.getSharedInstance(context.applicationContext)
            castContext.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
            initialized = true
            Log.i(TAG, "CastContext initialized")
        } catch (t: Throwable) {
            Log.e(TAG, "Cast init failed — Google Play Services missing?", t)
        }
    }

    /**
     * يفتح نافذة اختيار جهاز البث، ويجهّز الرابط للتحميل بعد الاتصال.
     */
    fun showDevicePicker(
        activity: Activity,
        mediaUrl: String?,
        title: String,
        isLive: Boolean,
        referer: String? = null,
        userAgent: String? = null
    ) {
        if (mediaUrl.isNullOrBlank()) {
            Toast.makeText(activity, "لا يوجد بث للتشغيل على الشاشة", Toast.LENGTH_SHORT).show()
            return
        }
        // Route the stream through the local proxy so the TV receives the SAME
        // Referer / User-Agent the phone player uses. Many IPTV streams return 403
        // to the TV (which sends no headers) and only play through the proxy.
        val proxied = try { CastProxy.proxyUrl(mediaUrl, referer, userAgent) } catch (_: Throwable) { null }
        pendingUrl = proxied ?: mediaUrl
        pendingTitle = title.ifBlank { "Youseif Player" }
        pendingIsLive = isLive
        pendingContentType = guessContentType(mediaUrl)

        try {
            init(activity)
            val castContext = try {
                CastContext.getSharedInstance(activity.applicationContext)
            } catch (t: Throwable) {
                Toast.makeText(
                    activity,
                    "خدمات Google Play غير متوفرة على هذا الجهاز",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val current = castContext.sessionManager.currentCastSession
            if (current != null && current.isConnected) {
                loadPending(current)
                Toast.makeText(activity, "جاري البث على ${current.castDevice?.friendlyName ?: "الجهاز"}", Toast.LENGTH_SHORT).show()
                return
            }

            val selector = MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(
                    com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                        com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                    )
                )
                .build()

            // Pass an AppCompat-descendant theme with an OPAQUE colorPrimary. The chooser
            // is an AppCompatDialog and MediaRouterThemeHelper reads the theme's colorPrimary;
            // with the app's plain Material theme that value was #0, so the dialog threw
            // "background can not be translucent: #0" instead of opening.
            val dialog = MediaRouteChooserDialog(
                activity,
                com.example.R.style.Theme_Youseif_CastDialog
            )
            dialog.routeSelector = selector
            dialog.setTitle("اختر جهاز البث")
            // Fix: MediaRouteChooserDialog window requires an OPAQUE background — otherwise
            // Android throws "background can not be translucent: #0". Force the window BG.
            try {
                val w = dialog.window
                if (w != null) {
                    w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#101012")))
                }
            } catch (_: Throwable) {}
            dialog.show()
        } catch (t: Throwable) {
            Log.e(TAG, "showDevicePicker failed", t)
            Toast.makeText(
                activity,
                "تعذر فتح قائمة الأجهزة: ${t.message ?: "خطأ"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadPending(session: CastSession) {
        val url = pendingUrl ?: return
        try {
            val remote = session.remoteMediaClient ?: run {
                Log.w(TAG, "No remoteMediaClient")
                return
            }
            val streamType = if (pendingIsLive) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED
            val contentType = pendingContentType.ifBlank { guessContentType(url) }
            val metadata = MediaMetadata(
                if (contentType.startsWith("audio")) MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
                else MediaMetadata.MEDIA_TYPE_MOVIE
            )
            metadata.putString(MediaMetadata.KEY_TITLE, pendingTitle)
            // صورة افتراضية اختيارية — مش مطلوبة

            val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(streamType)
                .setContentType(contentType)
                .setMetadata(metadata)
                .build()

            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()

            remote.load(request)
            Log.i(TAG, "Loaded media on Cast: $url")
        } catch (t: Throwable) {
            Log.e(TAG, "loadPending failed", t)
        }
    }

    private fun guessContentType(url: String): String {
        val u = url.lowercase()
        return when {
            u.contains(".m3u8") || u.contains("m3u8") -> "application/x-mpegURL"
            u.contains(".mpd") -> "application/dash+xml"
            u.contains(".mp3") || u.contains(".aac") || u.contains(".m4a") -> "audio/mp4"
            u.contains(".mp4") -> "video/mp4"
            u.contains(".mkv") -> "video/x-matroska"
            u.contains(".webm") -> "video/webm"
            else -> "application/x-mpegURL"
        }
    }

    fun isCasting(context: Context): Boolean {
        return try {
            CastContext.getSharedInstance(context.applicationContext)
                .sessionManager.currentCastSession?.isConnected == true
        } catch (_: Throwable) {
            false
        }
    }
}
