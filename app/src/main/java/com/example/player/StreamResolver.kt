package com.example.player

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.util.regex.Pattern

/**
 * Resolves any stream URL for Media3 with strong IPTV-friendly headers.
 */
object StreamResolver {

    data class ResolvedStream(
        val url: String,
        val type: StreamType,
        val mimeType: String? = null,
        val headers: Map<String, String> = emptyMap()
    )

    /** Widely accepted by IPTV CDNs */
    const val UA_VLC = "VLC/3.0.21 LibVLC/3.0.21"
    const val UA_EXOPLAYER = "ExoPlayerLib/2.19.1 (Linux;Android 14) YouseifPlayer/12"
    const val UA_BROWSER =
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    suspend fun resolve(
        rawUrl: String,
        userAgent: String? = null,
        referer: String? = null
    ): ResolvedStream = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
            .removePrefix("\uFEFF")
            .replace(" ", "%20")
        if (trimmed.isEmpty()) {
            return@withContext ResolvedStream(trimmed, StreamType.UNKNOWN)
        }

        val lower = trimmed.lowercase(Locale.US)
        val uri = try { Uri.parse(trimmed) } catch (_: Throwable) { null }
        val path = (uri?.path ?: lower).lowercase(Locale.US)
        val host = uri?.host?.lowercase(Locale.US).orEmpty()

        val headers = linkedMapOf<String, String>()
        val ua = userAgent?.takeIf { it.isNotBlank() } ?: UA_VLC
        headers["User-Agent"] = ua
        headers["Accept"] = "*/*"
        headers["Accept-Encoding"] = "identity"
        headers["Connection"] = "keep-alive"
        headers["Icy-MetaData"] = "1"

        val originRef = referer?.takeIf { it.isNotBlank() } ?: run {
            if (uri?.scheme != null && !host.isNullOrEmpty()) {
                "${uri.scheme}://$host/"
            } else null
        }
        if (originRef != null) {
            headers["Referer"] = originRef
            headers["Origin"] = originRef.trimEnd('/')
        }

        if (host.contains("alooytv") && path.contains("/watch/")) {
            resolvePortalPage(trimmed, headers, 0)?.let { return@withContext it }
        }

        // RTSP / RTMP first (scheme-based)
        when {
            lower.startsWith("rtsp://") ->
                return@withContext ResolvedStream(trimmed, StreamType.RTSP, null, headers)
            lower.startsWith("rtmp://") || lower.startsWith("rtmps://") ->
                return@withContext ResolvedStream(trimmed, StreamType.RTMP, null, headers)
        }

        // Strong HLS signals
        val isHls = path.endsWith(".m3u8") || path.endsWith(".m3u") ||
            lower.contains(".m3u8") || lower.contains("format=m3u8") ||
            lower.contains("type=m3u8") || lower.contains("/hls/") ||
            lower.contains("playlist.m3u8") || lower.contains("index.m3u8") ||
            lower.contains("master.m3u8") || lower.contains("chunklist") ||
            host.contains("mux.dev") || host.contains("akamai") && lower.contains("hls")

        // بصمة أقوى للـ HLS: تغطي روابط بتوكن/كويري وبورتات ومسارات CDN بدون امتداد واضح
        val isHlsStrong = isHls ||
            lower.contains(".m3u8") || lower.contains(".m3u") ||
            lower.contains("/hls2/") || lower.contains("/hls/") ||
            lower.contains("chunklist") || lower.contains("playlist.m3u8") ||
            lower.contains("index.m3u8") || lower.contains("master.m3u8") ||
            lower.contains("format=m3u8") || lower.contains("type=m3u8") ||
            lower.contains("output=m3u8") || lower.contains("/live/") && lower.contains("m3u")

        if (isHlsStrong) {
            return@withContext ResolvedStream(
                trimmed, StreamType.HLS, MimeTypesCompat.M3U8, headers
            )
        }

        // DASH
        if (path.endsWith(".mpd") || lower.contains(".mpd") || lower.contains("manifest.mpd") ||
            lower.contains("format=mpd")
        ) {
            return@withContext ResolvedStream(
                trimmed, StreamType.DASH, MimeTypesCompat.MPD, headers
            )
        }

        // Progressive by extension
        val videoExt = listOf(
            ".mp4", ".webm", ".mkv", ".mov", ".flv", ".avi", ".ts", ".m4v",
            ".3gp", ".mpg", ".mpeg", ".wmv", ".m2ts", ".mts"
        )
        if (videoExt.any { path.endsWith(it) || lower.contains("$it?") }) {
            return@withContext ResolvedStream(trimmed, StreamType.PROGRESSIVE, null, headers)
        }

        val audioExt = listOf(".mp3", ".m4a", ".aac", ".ogg", ".oga", ".wav", ".flac", ".opus")
        if (audioExt.any { path.endsWith(it) || lower.contains("$it?") }) {
            return@withContext ResolvedStream(trimmed, StreamType.PROGRESSIVE, null, headers)
        }

        if (path.endsWith(".html") || path.endsWith(".htm") ||
            lower.contains("youtube.com/embed") ||
            lower.contains("/embed/") || lower.contains("/e/") ||
            lower.contains("streamwish") || lower.contains("filemoon") ||
            lower.contains("doodstream") || lower.contains("dood.") ||
            lower.contains("uqload") || lower.contains("vidoba") ||
            lower.contains("mixdrop") || lower.contains("streamtape") ||
            lower.contains("updown") || lower.contains("voe.sx")
        ) {
            return@withContext ResolvedStream(trimmed, StreamType.WEB_EMBED, "text/html", headers)
        }

        // No extension / tokenized live URLs → UNKNOWN (Media3 content sniffing)
        ResolvedStream(trimmed, StreamType.UNKNOWN, null, headers)
    }

    private fun resolvePortalPage(url: String, headers: Map<String, String>, depth: Int): ResolvedStream? {
        if (depth > 2) return null
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", UA_BROWSER)
            conn.setRequestProperty("Referer", url.substringBefore("/watch/") + "/")
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val streamPattern = Pattern.compile("https?://[^\\\"'\\s<>]+\\.(?:m3u8|mp4|mpd|ts)(?:\\?[^\\\"'\\s<>]*)?", Pattern.CASE_INSENSITIVE)
            val stream = streamPattern.matcher(html)
            if (stream.find()) {
                val media = stream.group()
                val lower = media.lowercase(Locale.US)
                val type = if (lower.contains(".m3u8")) StreamType.HLS else if (lower.contains(".mpd")) StreamType.DASH else StreamType.PROGRESSIVE
                return ResolvedStream(media, type, if (type == StreamType.HLS) MimeTypesCompat.M3U8 else if (type == StreamType.DASH) MimeTypesCompat.MPD else null, headers + ("Referer" to url))
            }
            val keyPattern = Pattern.compile("href=\\\"([^\\\"]*(?:\\?|&)key=[^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE)
            val keys = keyPattern.matcher(html)
            if (keys.find()) return resolvePortalPage(URI(url).resolve(keys.group(1)).toString(), headers, depth + 1)
            null
        } catch (_: Throwable) { null }
    }
}

/** Local mime constants to avoid import noise in resolver */
private object MimeTypesCompat {
    const val M3U8 = "application/x-mpegURL"
    const val MPD = "application/dash+xml"
}
