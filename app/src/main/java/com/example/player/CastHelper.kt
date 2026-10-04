package com.example.player

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * مساعد البث الحقيقي (Google Cast / Chromecast).
 *
 * بدل ما نستخدم MediaRouteChooserDialog (اللي كان بيقع بسبب ثيم التطبيق:
 * "background can not be translucent" بعدين NullPointerException جوه الـTextView)،
 * بنعمل قائمة أجهزتنا بنفسنا من MediaRouter — أوضح وأأمن.
 *
 * - startDiscovery()  → يبدأ البحث عن أجهزة على نفس الشبكة ويملأ devices
 * - devices           → قائمة الأجهزة المتاحة (تتحدّث لحظيًا)
 * - selectRoute(id)   → يتصل بالجهاز المختار
 * - prepareCast(...)  → يجهّز الرابط (عن طريق البروكسي المحلي) ويرجّع true لو لازم نعرض القائمة
 */
object CastHelper {
    private const val TAG = "CastHelper"
    @Volatile private var initialized = false
    @Volatile private var pendingUrl: String? = null
    @Volatile private var pendingTitle: String = "Youseif Player"
    @Volatile private var pendingIsLive: Boolean = true
    @Volatile private var pendingContentType: String = "application/x-mpegURL"

    data class CastDeviceInfo(val id: String, val name: String, val connected: Boolean)

    private val _devices = MutableStateFlow<List<CastDeviceInfo>>(emptyList())
    val devices: StateFlow<List<CastDeviceInfo>> = _devices.asStateFlow()

    @Volatile private var routerCallback: MediaRouter.Callback? = null

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

    private fun selector(): MediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .addControlCategory(
            com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
            )
        )
        .build()

    private fun refreshDevices(context: Context) {
        try {
            val router = MediaRouter.getInstance(context.applicationContext)
            val sel = selector()
            _devices.value = router.routes
                .filter { it.matchesSelector(sel) && !it.isDefault }
                .map { CastDeviceInfo(it.id, it.name, it.isSelected) }
        } catch (t: Throwable) {
            Log.w(TAG, "refreshDevices: ${t.message}")
        }
    }

    /** Starts an active scan for cast devices on the local network. */
    fun startDiscovery(context: Context) {
        try {
            init(context)
            val router = MediaRouter.getInstance(context.applicationContext)
            val cb = routerCallback ?: object : MediaRouter.Callback() {
                override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) { refreshDevices(context) }
                override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) { refreshDevices(context) }
                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) { refreshDevices(context) }
                override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) { refreshDevices(context) }
            }.also { routerCallback = it }
            router.addCallback(selector(), cb, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
            refreshDevices(context)
        } catch (t: Throwable) {
            Log.w(TAG, "startDiscovery: ${t.message}")
        }
    }

    /** Stops the active scan (call when the picker is dismissed). */
    fun stopDiscovery(context: Context) {
        try {
            val router = MediaRouter.getInstance(context.applicationContext)
            routerCallback?.let { router.removeCallback(it) }
            _devices.value = emptyList()
        } catch (_: Throwable) {}
    }

    /** Connects to a route previously returned in [devices]. */
    fun selectRoute(context: Context, routeId: String): Boolean {
        return try {
            val router = MediaRouter.getInstance(context.applicationContext)
            val route = router.routes.firstOrNull { it.id == routeId } ?: return false
            route.select()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "selectRoute: ${t.message}")
            false
        }
    }

    /**
     * يجهّز البث (عن طريق البروكسي المحلي عشان الهيدرز) ويرجّع:
     *  - false لو مفيش رابط / GMS مش موجود / اتصلنا بجهاز بالفعل وبدأنا البث
     *  - true لو المفروض نعرض قائمة الأجهزة (startDiscovery اتنده)
     */
    fun prepareCast(
        activity: Activity,
        mediaUrl: String?,
        title: String,
        isLive: Boolean,
        referer: String? = null,
        userAgent: String? = null
    ): Boolean {
        if (mediaUrl.isNullOrBlank()) {
            Toast.makeText(activity, "لا يوجد بث للتشغيل على الشاشة", Toast.LENGTH_SHORT).show()
            return false
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
                return false
            }

            val current = castContext.sessionManager.currentCastSession
            if (current != null && current.isConnected) {
                loadPending(current)
                Toast.makeText(activity, "جاري البث على ${current.castDevice?.friendlyName ?: "الجهاز"}", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "prepareCast failed", t)
            Toast.makeText(activity, "تعذر تجهيز البث: ${t.message ?: "خطأ"}", Toast.LENGTH_LONG).show()
            return false
        }

        startDiscovery(activity)
        return true
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
