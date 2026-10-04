package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.HostResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * Headless-WebView resolver: loads FaselHD short server links (fasel-hd.com/?p=…,
 * plusfas.com/…) inside a real WebView so the Cloudflare "Just a moment…" JS
 * challenge is passed with a genuine browser fingerprint + clearance cookies,
 * then harvests stream qualities from:
 *   - intercepted player network requests (.m3u8 / .mpd / .mp4)
 *   - dumped page HTML ( {"file":…,"label":…} pairs + raw media URLs )
 * Returns an empty list when the challenge cannot be passed (caller falls back).
 */
object WebViewResolver {

    private val MEDIA: Pattern = Pattern.compile(
        "https?://[^\\\"'\\\\\\s<>]+\\.(?:m3u8|mpd|mp4|mkv|m4v|webm|ts)(?:\\?[^\\\"'\\\\\\s<>]*)?",
        Pattern.CASE_INSENSITIVE
    )
    /** jwplayer / videojs / clappr / generic player source fields */
    private val SRC_FIELD: Pattern = Pattern.compile(
        "(?:\"(?:file|src|source|url|link|stream|hls|dash)\"\\s*:\\s*\"(https?://[^\"]+)\"|'file'\\s*:\\s*'(https?://[^']+)'|src\\s*=\\s*\"(https?://[^\"]+\\.(?:m3u8|mp4|mpd)[^\"]*)\")",
        Pattern.CASE_INSENSITIVE
    )
    private val PAIR: Pattern = Pattern.compile(
        "\\{\\s*\\\"?file\\\"?\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"?label\\\"?\\s*:\\s*\\\"(.*?)\\\"\\s*\\}",
        Pattern.CASE_INSENSITIVE
    )

    private class Bridge {
        @Volatile var html: String = ""
        @JavascriptInterface
        fun put(s: String?) {
            if (s != null && s.length > html.length) html = s
        }
    }

    /** Qualities discovered for the (possibly protected) page. Runs WebView on Main. */
    suspend fun resolve(
        context: Context,
        link: String,
        timeoutMs: Long = 28_000L
    ): List<HostResolver.Quality> {
        val urls = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) { loadAndCollect(context, link) }
        }.orEmpty()

        val qs = ArrayList<HostResolver.Quality>()
        for (u in urls) {
            if (u.contains(".m3u8", true)) {
                val variants = parseMasterWithCookies(u)
                if (variants.isNotEmpty()) qs.addAll(variants)
                else qs += HostResolver.Quality("تلقائي", u)
            } else {
                qs += HostResolver.Quality(
                    if (u.contains(".mpd", true)) "DASH" else "مباشر", u
                )
            }
        }
        return qs.distinctBy { it.url }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadAndCollect(context: Context, link: String): List<String> {
        val bridge = Bridge()
        val done = AtomicBoolean(false)
        val found: MutableList<String> = Collections.synchronizedList(ArrayList<String>())
        val main = Handler(Looper.getMainLooper())

        return suspendCancellableCoroutine { cont ->
            var web: WebView? = null
            fun destroyOnMain() {
                val w = web
                main.post {
                    try { w?.stopLoading() } catch (_: Throwable) {}
                    try { w?.destroy() } catch (_: Throwable) {}
                }
            }
            fun finishOnce() {
                if (done.compareAndSet(false, true)) {
                    destroyOnMain()
                    if (cont.isActive) cont.resume(found.toList()) { }
                }
            }

            try {
                CookieManager.getInstance().setAcceptCookie(true)
                val wv = WebView(context)
                web = wv
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = StreamResolver.UA_BROWSER
                    blockNetworkImage = true
                    loadsImagesAutomatically = false
                }
                try { CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true) } catch (_: Throwable) {}
                wv.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val u = request.url.toString()
                        if (MEDIA.matcher(u).find() && found.none { it == u }) found.add(u)
                        return null
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        // Dump full HTML + try to surface common player configs
                        val js = """
                            (function(){
                              try{
                                var h = document.documentElement.innerHTML;
                                try{
                                  if(window.jwplayer){var p=jwplayer();if(p&&p.getPlaylist){h+='\n'+JSON.stringify(p.getPlaylist())}}
                                }catch(e){}
                                try{
                                  if(window.player && window.player.options){h+='\n'+JSON.stringify(window.player.options)}
                                }catch(e){}
                                try{
                                  var vids=document.querySelectorAll('video,source');
                                  for(var i=0;i<vids.length;i++){var s=vids[i].src||vids[i].getAttribute('src');if(s)h+='\n"file":"'+s+'"'}
                                }catch(e){}
                                window.YPL.put(h);
                              }catch(e){}
                            })()
                        """.trimIndent()
                        try { view.evaluateJavascript(js, null) } catch (_: Throwable) {}
                        // second + third scrape after challenge / player JS settles
                        main.postDelayed({
                            try { view.evaluateJavascript(js, null) } catch (_: Throwable) {}
                        }, 3500L)
                        main.postDelayed({
                            try { view.evaluateJavascript(js, null) } catch (_: Throwable) {}
                        }, 8000L)
                    }
                }
                wv.addJavascriptInterface(bridge, "YPL")

                // grace stop: enough results, or hard deadline
                main.postDelayed({ if (found.isNotEmpty()) finishOnce() }, 12_000L)
                main.postDelayed({ finishOnce() }, 28_000L)

                // watcher thread: parse HTML dumps as they arrive
                Thread {
                    var ticks = 0
                    while (!done.get() && ticks < 34) {
                        try { Thread.sleep(700) } catch (_: InterruptedException) { break }
                        ticks++
                        val html = bridge.html
                        if (html.length > 500) {
                            parseAll(html).forEach { q ->
                                if (found.none { it.equals(q.url, ignoreCase = true) }) found.add(q.url)
                            }
                            if (found.size >= 2) { finishOnce(); break }
                        }
                    }
                }.apply { isDaemon = true; start() }

                cont.invokeOnCancellation { finishOnce() }
                wv.loadUrl(link)
            } catch (_: Throwable) {
                destroyOnMain()
                if (cont.isActive) cont.resume(emptyList()) { }
            }
        }
    }

    private fun parseAll(html: String): List<HostResolver.Quality> {
        val out = ArrayList<HostResolver.Quality>()
        try {
            val p = PAIR.matcher(html)
            while (p.find()) {
                val u = p.group(1)?.trim().orEmpty()
                val l = p.group(2)?.trim().orEmpty()
                if (u.startsWith("http")) out += HostResolver.Quality(l.ifBlank { "جودة" }, u)
            }
            val m = MEDIA.matcher(html)
            while (m.find()) out += HostResolver.Quality("مباشر", m.group())
            val s = SRC_FIELD.matcher(html)
            while (s.find()) {
                val u = (s.group(1) ?: s.group(2) ?: s.group(3) ?: "").trim()
                if (u.startsWith("http") && (u.contains(".m3u8", true) || u.contains(".mp4", true) ||
                        u.contains(".mpd", true) || u.contains("/hls") || u.contains("playlist"))) {
                    out += HostResolver.Quality(
                        when {
                            u.contains("1080") -> "1080p"
                            u.contains("720") -> "720p"
                            u.contains("480") -> "480p"
                            u.contains("360") -> "360p"
                            else -> "مباشر"
                        },
                        u
                    )
                }
            }
        } catch (_: Throwable) { }
        return out.distinctBy { it.url }
    }

    /** Fetch an m3u8 master with the WebView's Cloudflare cookies and split variants. */
    private fun parseMasterWithCookies(url: String): List<HostResolver.Quality> = try {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 12000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", StreamResolver.UA_BROWSER)
        conn.setRequestProperty("Accept", "*/*")
        try {
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) conn.setRequestProperty("Cookie", cookie)
        } catch (_: Throwable) { }
        if (conn.responseCode in 200..299) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = HostResolver.parseMaster(body, url)
            parsed
        } else emptyList()
    } catch (_: Throwable) { emptyList() }
}
