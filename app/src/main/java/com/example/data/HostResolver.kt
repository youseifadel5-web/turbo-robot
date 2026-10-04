package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import com.example.player.WebViewResolver
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Resolves Fasel HD server links into playable qualities (HLS m3u8 master /
 * variant playlists) and direct download URLs, using the same chain the real
 * Fasel HD app uses:
 *
 *   hosts/config  ->  mawdhou3.com/scrapefinal/{host}.php?api={link}
 *                 ->  {file:"...",label:"1080p"}  pairs
 *
 * Plus an m3u8-variant parser for links that point straight at a master playlist,
 * and a final WebView pass (WebViewResolver) that defeats Cloudflare-challenge
 * pages (fasel-hd.com/?p=…, plusfas.com/…) with a real browser fingerprint.
 */
object HostResolver {

    data class Quality(val label: String, val url: String)

    data class Resolved(
        val qualities: List<Quality> = emptyList(),
        val directUrl: String? = null,
        val source: String = "none"
    )

    private val PAIR = Pattern.compile("\\{\\s*\"?file\"?\\s*:\\s*\"(.*?)\"\\s*,\\s*\"?label\"?\\s*:\\s*\"(.*?)\"\\s*}")
    private val OLD_PAIR = Pattern.compile("file\\s*:\\s*\"(.*?)\"\\s*,\\s*label\\s*:\\s*\"(.*?)\"")
    private val FALLBACK_WORKER: String = com.example.data.SecretVault.FALLBACK_WORKER

    /** Multiquality hosts from the official Fasel app + known working sources.
     *  Short links carry ?p={id} and these expose /api/source/{id} quality lists. */
    private val SOURCE_HOSTS = listOf(
        "multiquality.host", "suzihaza.com", "gavid.xyz", "zapurl.xyz",
        "mrdhan.com", "diampokusy.com", "diasfem.com", "gdstream.net",
        "easyplex.xyz", "ff-dns.xyz", "ll-dns.xyz", "pp-dns.xyz", "psadns.xyz",
        "iplhd.cyou", "kotakajair.xyz", "manasx.xyz", "mifilm.xyz",
        "mycineplay.co", "oracleclouds.live", "otcplay.fun", "playto1.com",
        "pocketnow.xyz", "sbplay.xyz", "kawaiifansub.com"
    )
    /** Third-party embed hosts that rarely answer scrapers — force WebView early. */
    private val EMBED_HOST_HINTS = listOf(
        "streamwish", "swishsrv", "wishfast", "streamhg", "hlsplayer",
        "updown", "upstream", "upsb", "upload", "vidoza", "vidoba",
        "uqload", "dood", "doodstream", "filemoon", "moonplayer",
        "mixdrop", "mixdroop", "mp4upload", "mp4u", "voe.sx", "voe-network",
        "streamtape", "strtape", "wolfstream", "luluvdo", "luluvid",
        "vidmoly", "vidhide", "vidguard", "vgembed", "vembed",
        "anafast", "anavids", "serveregy", "egybest", "egy.best",
        "ok.ru", "okru", "vk.com", "vkvideo", "youtube.com/embed",
        "myvi", "rutube", "mail.ru", "cloudvideo", "supervideo",
        "streamsb", "sbplay", "sbfull", "sbanh", "sblona",
        "embed.", "/e/", "/embed/", "/v/", "/f/"
    )
    private const val CHAIN_BUDGET_MS = 10_000L
    private const val MAX_SCRAPERS = 6

    /**
     * Scrapers extracted from the official/private Fasel APK (same chain that
     * unlocks VIP + Server Egy / CimaClub style embeds into file/label pairs).
     */
    /** All known scrapers — ordered lists are picked by server name for speed. */
    private val SCRAPER_VIP = listOf(
        "https://mawdhou3.com/scripttestfasel.php?api=",
        "https://autumn-dust-1a31.flechlivraison.workers.dev/?url=",
        "https://mawdhou3.com/test.php?api=",
        "https://mawdhou3.com/scrapefinal/faselpost.php?api=",
        "https://abcdef.flech.tn/test.php?api="
    )
    private val SCRAPER_SHAHED = listOf(
        "https://mawdhou3.com/scrapefinal/faselpost.php?api=",
        "https://mawdhou3.com/scrapefinal/vidtubepost.php?api=",
        "https://mawdhou3.com/test.php?api=",
        "https://flech.tn/recuperepost.php?api=",
        "https://shahed4uapp.com/akwam/test.php?api="
    )
    private val SCRAPER_UPDOWN = listOf(
        "https://mawdhou3.com/scrapefinal/updown.php?api=",
        "https://mawdhou3.com/test.php?api="
    )
    private val SCRAPER_WISH = listOf(
        "https://mawdhou3.com/scrapefinal/earnvids.php?api=",
        "https://mawdhou3.com/test.php?api="
    )
    // cimaclub.php is dead (404). Server Egy works via BaseVed POST on scriptEgybest
    // + test.php + flech recuperepost — NOT the old GET api= chain alone.
    private val SCRAPER_EGY = listOf(
        "https://mawdhou3.com/scrapeamine/scriptEgybest.php?api=",
        "https://mawdhou3.com/test.php?api=",
        "https://flech.tn/recuperepost.php?api=",
        "https://mawdhou3.com/scrapefinal/faselpost.php?api="
    )
    private val SCRAPER_GENERIC = listOf(
        "https://mawdhou3.com/scripttestfasel.php?api=",
        "https://mawdhou3.com/test.php?api=",
        "https://mawdhou3.com/scrapefinal/earnvids.php?api=",
        "https://mawdhou3.com/scrapefinal/updown.php?api=",
        "https://mawdhou3.com/scrapefinal/uqload.php?api=",
        "https://mawdhou3.com/scrapefinal/faselpost.php?api=",
        "https://mawdhou3.com/scrapefinal/vidtubepost.php?api=",
        "https://mawdhou3.com/scrapeamine/scriptEgybest.php?api=",
        "https://flech.tn/recuperepost.php?api="
    )

    private fun isLikelyEmbed(link: String): Boolean {
        val l = link.lowercase()
        return EMBED_HOST_HINTS.any { l.contains(it) }
    }

    /** Pick scraper list by server label / link host — avoids trying 16 dead endpoints. */
    private fun scrapersFor(server: String?, link: String): List<String> {
        val s = (server ?: "").lowercase()
        val l = link.lowercase()
        return when {
            // VIP EGYBEST / VIP Fast MUST be before "egy" — name contains both words
            s.contains("vip") || l.contains("plusfas") || l.contains("fasel-hd") ||
                Regex("[?&]p=\\d+").containsMatchIn(l) -> SCRAPER_VIP
            s.contains("shahed") || s.contains("شاهد") || l.contains("shahed") || l.contains("vidtube") ||
                l.contains("fdewsdc") || l.contains("flech") -> SCRAPER_SHAHED
            s.contains("updown") || l.contains("updown") -> SCRAPER_UPDOWN
            s.contains("wish") || s.contains("streamwish") || l.contains("streamwish") ||
                l.contains("stmruby") || l.contains("earnvids") -> SCRAPER_WISH
            // Server Egy / Cima only (not VIP EGYBEST)
            (s.contains("server egy") || s.contains("cima") || s == "egy" ||
                (s.contains("egy") && !s.contains("vip"))) ||
                l.contains("cimaclub") -> SCRAPER_EGY
            else -> SCRAPER_GENERIC
        }
    }

    /**
     * Fast parallel scrapers: short timeouts, 1–2 referers, race first success.
     * Was sequential 16×5×2 requests → could take minutes; now ~3–6s typical.
     */
    /**
     * BaseVed "TRUE|postUrl" endpoints from private Fasel APK hosts/config.
     * These expect POST content=<embed_html>&url=<embed> not api=.
     */
    private fun baseVedPostEndpoints(server: String?, link: String): List<String> {
        val s = (server ?: "").lowercase()
        val l = link.lowercase()
        return when {
            s.contains("updown") || l.contains("updown") -> listOf(
                "https://mawdhou3.com/scrapefinal/updown.php",
                "https://mawdhou3.com/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            s.contains("wish") || l.contains("streamwish") || l.contains("stmruby") ||
                l.contains("earnvids") -> listOf(
                "https://mawdhou3.com/scrapefinal/earnvids.php",
                "https://mawdhou3.com/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            s.contains("shahed") || s.contains("شاهد") || l.contains("shahed") ||
                l.contains("vidtube") || l.contains("fdewsdc") -> listOf(
                "https://mawdhou3.com/scrapefinal/faselpost.php",
                "https://mawdhou3.com/scrapefinal/vidtubepost.php",
                "https://flech.tn/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            // Server Egy / Cima — cimaclub is dead; official path is scriptEgybest POST
            s.contains("egy") || s.contains("cima") || l.contains("egybest") ||
                l.contains("cimaclub") || l.contains("serveregy") -> listOf(
                "https://mawdhou3.com/scrapeamine/scriptEgybest.php",
                "https://mawdhou3.com/test.php",
                "https://flech.tn/recuperepost.php",
                "https://mawdhou3.com/scrapefinal/faselpost.php"
            )
            else -> listOf(
                "https://mawdhou3.com/test.php",
                "https://mawdhou3.com/recuperepost.php",
                "https://flech.tn/recuperepost.php",
                "https://mawdhou3.com/scrapeamine/scriptEgybest.php",
                "https://mawdhou3.com/scrapefinal/earnvids.php",
                "https://mawdhou3.com/scrapefinal/updown.php"
            )
        }
    }

    private suspend fun tryOfficialScrapers(
        link: String,
        ua: String?,
        ref: String?,
        server: String? = null
    ): Resolved? = kotlinx.coroutines.coroutineScope {
        val encoded = URLEncoder.encode(link, "UTF-8")
        val userAgent = ua?.ifBlank { null }
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        val referer = ref?.ifBlank { null }
            ?: when {
                (server ?: "").contains("shahed", true) -> "https://shaaheid4u.net/"
                (server ?: "").contains("egy", true) -> "https://flech.tn/"
                (server ?: "").contains("updown", true) -> "https://topcinema.media/"
                (server ?: "").contains("wish", true) -> "https://topcinema.media/"
                else -> "https://faselhd.center/"
            }

        // ── Path A: EasyPlex BaseVed (GET page → POST content+url) — UpDown / Wish / Server Egy / Shahed ──
        // Skip VIP EGYBEST (name contains "egy" but needs scripttestfasel, not BaseVed)
        val needsBaseVed = run {
            val s = (server ?: "").lowercase()
            val isVip = s.contains("vip")
            !isVip && (s.contains("updown") || s.contains("wish") ||
                s.contains("server egy") || s.contains("cima") ||
                s.contains("shahed") || s.contains("شاهد") ||
                (s.contains("egy") && !s.contains("vip")) ||
                link.contains("updown", true) || link.contains("streamwish", true) ||
                link.contains("stmruby", true) || link.contains("shahed", true) ||
                link.contains("vidtube", true) || link.contains("fdewsdc", true))
        }
        if (needsBaseVed) {
            val posts = baseVedPostEndpoints(server, link)
            val vedJobs = posts.map { endpoint ->
                async(Dispatchers.IO) {
                    try {
                        val qs = resolveViaPagePost(link, endpoint, userAgent, referer)
                        if (qs.isEmpty()) null
                        else Resolved(
                            qualities = qs,
                            directUrl = qs.firstOrNull { it.url.contains(".m3u8", true) }?.url
                                ?: qs.first().url,
                            source = "baseved:" + endpoint.substringAfterLast('/')
                        )
                    } catch (_: Throwable) { null }
                }
            }
            try {
                kotlinx.coroutines.withTimeout(12_000L) {
                    val pending = vedJobs.toMutableList()
                    while (pending.isNotEmpty()) {
                        val done = pending.filter { it.isCompleted }
                        for (j in done) {
                            pending.remove(j)
                            val r = j.await()
                            if (r != null) {
                                pending.forEach { it.cancel() }
                                return@withTimeout r
                            }
                        }
                        if (pending.isEmpty()) break
                        kotlinx.coroutines.delay(40)
                    }
                    null
                }?.let { return@coroutineScope it }
            } catch (_: Throwable) {
                vedJobs.forEach { it.cancel() }
            }
        }

        // ── Path B: classic api= scrapers (Shahed / VIP / fallback) ──
        val scrapers = scrapersFor(server, link)
        val jobs = scrapers.map { base ->
            async(Dispatchers.IO) {
                try {
                    val body = httpGetFast(base + encoded, userAgent, referer)
                        ?: httpGetFast(base + link, userAgent, referer)
                        ?: httpPostFast(base, link, userAgent, referer)
                    if (body.isNullOrBlank()) return@async null
                    val parsed = parsePairs(body).ifEmpty { parseBaseVedResponse(body) }
                    if (parsed.isEmpty()) return@async null
                    Resolved(
                        qualities = dedupe(parsed),
                        directUrl = parsed.firstOrNull { it.url.contains(".m3u8", true) }?.url
                            ?: parsed.first().url,
                        source = "scraper:" + base.substringAfter("scrapefinal/").substringBefore(".php")
                            .ifBlank { base.substringAfter("://").substringBefore("/") }
                    )
                } catch (_: Throwable) {
                    null
                }
            }
        }
        try {
            kotlinx.coroutines.withTimeout(9_000L) {
                val pending = jobs.toMutableList()
                while (pending.isNotEmpty()) {
                    val done = pending.filter { it.isCompleted }
                    for (j in done) {
                        pending.remove(j)
                        val r = j.await()
                        if (r != null) {
                            pending.forEach { it.cancel() }
                            return@withTimeout r
                        }
                    }
                    if (pending.isEmpty()) break
                    kotlinx.coroutines.delay(50)
                }
                null
            }
        } catch (_: Throwable) {
            jobs.forEach { it.cancel() }
            null
        }
    }

    /** Short-timeout GET for scraper racing (fail fast on dead hosts). */
    private fun httpGetFast(url: String, ua: String?, ref: String?): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                ua?.ifBlank { null }
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
            )
            if (!ref.isNullOrBlank()) conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Accept", "*/*")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Official EasyPlex BaseVed flow (from private Fasel APK BaseVedEasyPlex.java):
     * 1) GET embed page HTML (try several referers — CF often blocks the first)
     * 2) POST content=<html>&url=<embed> to postEndpoint
     * 3) Parse status/Quality/filtered_content
     * Also tries POST url= alone when HTML fetch fails (some endpoints accept it).
     */
    private fun resolveViaPagePost(
        embedUrl: String,
        postEndpoint: String,
        ua: String?,
        ref: String?
    ): List<Quality> {
        return try {
            val userAgent = ua?.ifBlank { null }
                ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
            val referers = listOfNotNull(
                ref?.ifBlank { null },
                "https://flech.tn/",
                "https://topcinema.media/",
                "https://faselhd.center/",
                "https://shaaheid4u.net/",
                "https://www.google.com/"
            ).distinct()
            var html: String? = null
            var usedRef = referers.first()
            for (r in referers) {
                html = httpGetFast(embedUrl, userAgent, r)
                if (!html.isNullOrBlank() && html!!.length >= 80 &&
                    !html!!.contains("Just a moment", true) &&
                    !html!!.contains("cf-browser-verification", true)
                ) {
                    usedRef = r
                    break
                }
                html = null
            }
            val endpoint = postEndpoint.trim().substringBefore("?").trimEnd('/')
            // Path 1: full BaseVed with page HTML
            if (!html.isNullOrBlank()) {
                val bodyParams = "content=" + URLEncoder.encode(html, "UTF-8") +
                    "&url=" + URLEncoder.encode(embedUrl, "UTF-8")
                postForm(endpoint, bodyParams, userAgent, usedRef)?.let { resp ->
                    val parsed = parseBaseVedResponse(resp)
                    if (parsed.isNotEmpty()) return parsed
                    val pairs = parsePairs(resp)
                    if (pairs.isNotEmpty()) return pairs
                }
            }
            // Path 2: POST url= only (scriptEgybest / test.php sometimes accept this)
            val urlOnly = "url=" + URLEncoder.encode(embedUrl, "UTF-8")
            postForm(endpoint, urlOnly, userAgent, usedRef)?.let { resp ->
                val parsed = parseBaseVedResponse(resp)
                if (parsed.isNotEmpty()) return parsed
                val pairs = parsePairs(resp)
                if (pairs.isNotEmpty()) return pairs
            }
            // Path 3: GET ?api= as last try on the same endpoint
            httpGetFast(endpoint + "?api=" + URLEncoder.encode(embedUrl, "UTF-8"), userAgent, usedRef)?.let { resp ->
                val parsed = parseBaseVedResponse(resp).ifEmpty { parsePairs(resp) }
                if (parsed.isNotEmpty()) return parsed
            }
            emptyList()
        } catch (e: Throwable) {
            android.util.Log.w("HostResolver", "resolveViaPagePost: ${e.message}")
            emptyList()
        }
    }

    private fun postForm(endpoint: String, body: String, ua: String, ref: String): String? {
        return try {
            val conn = java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 7_000
            conn.readTimeout = 14_000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", ua)
            conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseBaseVedResponse(text: String): List<Quality> {
        val out = ArrayList<Quality>()
        try {
            val root = org.json.JSONObject(text.trim())
            val status = root.optString("status")
            if (status.isNotBlank() && !status.equals("success", true)) return emptyList()
            val quals = root.optJSONArray("Quality") ?: root.optJSONArray("qualities")
            val urls = root.optJSONArray("filtered_content")
                ?: root.optJSONArray("urls")
                ?: root.optJSONArray("files")
            if (urls != null) {
                for (i in 0 until urls.length()) {
                    val u = urls.optString(i, "").trim()
                    if (!u.startsWith("http")) continue
                    val lab = quals?.optString(i, "")?.takeIf { it.isNotBlank() }
                        ?: when {
                            "1080" in u -> "1080p"
                            "720" in u -> "720p"
                            "480" in u -> "480p"
                            "360" in u -> "360p"
                            else -> "جودة"
                        }
                    out += Quality(lab, u)
                }
            }
            if (out.isEmpty()) out.addAll(parsePairs(text))
        } catch (_: Throwable) {
            out.addAll(parsePairs(text))
        }
        return dedupe(out)
    }

    /** POST for scrapers that return "Méthode non autorisée" on GET (earnvids / updown). */
    private fun httpPostFast(base: String, link: String, ua: String?, ref: String?): String? {
        return try {
            val encoded = URLEncoder.encode(link, "UTF-8")
            val url = when {
                base.endsWith("api=") || base.endsWith("url=") -> base + encoded
                base.contains("?") -> "$base&api=$encoded"
                else -> "$base?api=$encoded"
            }
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty(
                "User-Agent",
                ua?.ifBlank { null }
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
            )
            if (!ref.isNullOrBlank()) conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "*/*")
            conn.outputStream.use { os ->
                os.write("api=$encoded&url=$encoded&link=$encoded".toByteArray())
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    /** Expand single master.m3u8 (Server Egy 1080-only) into 1080/720/480/360. */
    private suspend fun expandQualities(resolved: Resolved, ua: String?, ref: String?): Resolved {
        if (resolved.qualities.size >= 2) return resolved
        val only = resolved.qualities.firstOrNull() ?: return resolved
        if (!only.url.contains(".m3u8", true) && !only.url.contains("/hls", true)) return resolved
        val variants = tryParseMaster(only.url, ua, ref)
        if (variants.size <= 1) return resolved
        val labeled = variants.map { q ->
            val lab = q.label.lowercase()
            val nice = when {
                "1080" in lab || "1920" in lab -> "1080p"
                "720" in lab || "1280" in lab -> "720p"
                "480" in lab || "854" in lab -> "480p"
                "360" in lab || "640" in lab -> "360p"
                else -> q.label
            }
            Quality(nice, q.url)
        }
        return Resolved(
            qualities = dedupe(labeled),
            directUrl = resolved.directUrl ?: labeled.first().url,
            source = resolved.source + "+master"
        )
    }

    suspend fun resolve(
        video: FaselVideo,
        hosts: List<HostConfig>,
        appContext: Context? = null
    ): Resolved = withContext(Dispatchers.IO) {
        try {
            expandQualities(resolveInner(video, hosts, appContext), video.userAgent, video.referer)
        } catch (e: Throwable) {
            android.util.Log.e("HostResolver", "resolve crashed: ${e.message}", e)
            val link = video.link.trim().ifBlank { "" }
            Resolved(
                qualities = if (link.startsWith("http")) listOf(Quality("Original", link)) else emptyList(),
                directUrl = link.ifBlank { null },
                source = "error:${e.javaClass.simpleName}"
            )
        }
    }

    private suspend fun resolveInner(
        video: FaselVideo,
        hosts: List<HostConfig>,
        appContext: Context?
    ): Resolved {
        var link = video.link.trim().removePrefix("\uFEFF")
        if (link.isBlank()) return Resolved(source = "none")
        if (!link.startsWith("http")) link = "https://" + link

        val lower = link.lowercase()
        val t0 = System.currentTimeMillis()

        // ── Fast parallel scrapers (picked by server name: Shahed / UpDown / Wish / VIP / Egy) ──
        try {
            val official = tryOfficialScrapers(link, video.userAgent, video.referer, video.server)
            if (official != null) return official
        } catch (e: Throwable) {
            android.util.Log.w("HostResolver", "official scrapers failed: ${e.message}")
        }

        // Server Egy / Shahed often need a real browser when scrapers miss.
        // Jump to WebView early (before the long hosts/config chain burns the budget).
        val serverLow = video.server.lowercase()
        val needsEarlyWebView = appContext != null && (
            serverLow.contains("server egy") || serverLow.contains("shahed") ||
                serverLow.contains("شاهد") ||
                (serverLow.contains("egy") && !serverLow.contains("vip")) ||
                lower.contains("serveregy") || lower.contains("cimaclub") ||
                lower.contains("fdewsdc") || lower.contains("vidtube")
            )
        if (needsEarlyWebView) {
            try {
                val browser = WebViewResolver.resolve(appContext!!, link, 16_000L)
                if (browser.isNotEmpty()) {
                    val qs = browser.map { it.copy(url = it.url.trim()) }.distinctBy { it.url }
                    return Resolved(
                        qualities = qs,
                        directUrl = qs.firstOrNull { !it.url.contains(".m3u8", true) }?.url
                            ?: qs.first().url,
                        source = "webview-early"
                    )
                }
            } catch (e: Throwable) {
                android.util.Log.w("HostResolver", "early server webview failed: ${e.message}")
            }
        }

        val isFaselPage = Regex("[?&]p=\\d+").containsMatchIn(link) ||
            lower.contains("plusfas") || lower.contains("fasel-hd") || lower.contains("fashd.com") ||
            lower.contains("faselhd") || lower.contains("faselhds") ||
            lower.contains("egybest") || lower.contains("cimaclub") ||
            lower.contains("serveregy")
        if (isFaselPage) {
            try {
                val worker = resolveWorker(link, video.userAgent, video.referer)
                if (worker != null) return worker
            } catch (e: Throwable) {
                android.util.Log.w("HostResolver", "worker resolver failed: ${e.message}")
            }
            // Browser fallback for Cloudflare-protected plusfas/fasel pages
            if (appContext != null) {
                try {
                    val browser = WebViewResolver.resolve(appContext, link, 14_000L)
                    if (browser.isNotEmpty()) {
                        val qs = browser.map { it.copy(url = it.url.trim()) }.distinctBy { it.url }
                        return Resolved(
                            qualities = qs,
                            directUrl = qs.firstOrNull { !it.url.contains(".m3u8", true) }?.url ?: qs.first().url,
                            source = "webview"
                        )
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("HostResolver", "webview resolver failed: ${e.message}")
                }
            }
        }

        // 0.5) Direct progressive file — no scraping needed
        if (Regex("\\.(mp4|mkv|webm|m4v|mov|ts)(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(lower) &&
            !lower.contains("embed") && !lower.contains("/e/")) {
            return Resolved(
                qualities = listOf(Quality(if (video.hd) "HD" else "مباشر", link)),
                directUrl = link,
                source = "direct-file"
            )
        }

        // 1) Link already HLS -> try to parse variant playlist, else use as-is
        if (lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("playlist")) {
            val parsed = tryParseMaster(link, video.userAgent, video.referer)
            if (parsed.isNotEmpty()) {
                return Resolved(
                    qualities = parsed,
                    source = "playlist"
                )
            }
            return Resolved(
                qualities = listOf(Quality(if (video.hd) "HD" else "متوسط", link)),
                directUrl = link,
                source = "direct-hls"
            )
        }

        // 1.2) Known third-party embeds (StreamWish, UpDown, Dood, Filemoon…):
        // scrapers almost never work — go to WebView first while the budget is full.
        if (isLikelyEmbed(link) && appContext != null) {
            try {
                val browser = WebViewResolver.resolve(appContext, link, 14_000L)
                if (browser.isNotEmpty()) {
                    val qs = browser.map { it.copy(url = it.url.trim()) }.distinctBy { it.url }
                    return Resolved(
                        qualities = qs,
                        directUrl = qs.firstOrNull { !it.url.contains(".m3u8", true) }?.url
                            ?: qs.first().url,
                        source = "webview-embed"
                    )
                }
            } catch (e: Throwable) {
                android.util.Log.w("HostResolver", "early embed webview failed: ${e.message}")
            }
        }

        // 1.5) Cloudflare worker JSON style (e.g. test-stream.developer-pro.workers.dev/video?url=…)
        // returns {"availableQualities":[{"quality":"1080p","url":"..."}]}
        if (lower.contains("workers.dev") && (lower.contains("/video") || lower.contains("get-links"))) {
            val body = httpGet(link, video.userAgent, video.referer)
            if (!body.isNullOrBlank() && body.contains("availableQualities")) {
                try {
                    val root = org.json.JSONObject(body)
                    val arr = root.optJSONArray("availableQualities") ?: JSONArray0()
                    val qs = ArrayList<Quality>()
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val u = o.optString("url", "")
                        if (u.startsWith("http")) qs += Quality(o.optString("quality", "جودة").ifBlank { "جودة" }, u)
                    }
                    if (qs.isNotEmpty()) {
                        return Resolved(qualities = dedupe(qs), directUrl = qs.first().url, source = "worker-json")
                    }
                } catch (_: Throwable) { }
            }
            // Not JSON — treat as a direct stream (server may redirect to media)
            return Resolved(
                qualities = listOf(Quality(if (video.hd) "HD" else "مباشر", link)),
                directUrl = link, source = "direct-worker"
            )
        }

        // 1.7) Multiquality /api/source chain — the exact mechanism the reference
        // app uses for fasel-hd short links: /api/source/{p-id} → quality list.
        Regex("[?&]p=(\\d+)").find(link)?.groupValues?.get(1)?.let { pId ->
            for (h in SOURCE_HOSTS) {
                if (System.currentTimeMillis() - t0 > 10_000L) break
                try {
                    val body = httpGet("https://$h/api/source/$pId", null, "https://www.fasel-hd.com/") ?: continue
                    val qs = ArrayList<Quality>()
                    try {
                        val root = org.json.JSONObject(body)
                        val arr = root.optJSONArray("qualities")
                            ?: root.optJSONArray("availableQualities")
                            ?: JSONArray0()
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val u = o.optString("url", "")
                            if (u.startsWith("http")) qs += Quality(o.optString("quality", "جودة").ifBlank { "جودة" }, u)
                        }
                    } catch (_: Throwable) { }
                    if (qs.isEmpty()) qs.addAll(parsePairs(body))
                    if (qs.isNotEmpty()) {
                        return Resolved(
                            qualities = dedupe(qs),
                            directUrl = qs.firstOrNull { !it.url.lowercase().contains(".m3u8") }?.url ?: qs.first().url,
                            source = "api-source"
                        )
                    }
                } catch (_: Throwable) { continue }
            }
        }

        // 2) Scraper chain (same as the official app) — bounded by a time budget
        //    and a scraper cap so a burst of dead hosts can never hang playback.
        val candidates = ArrayList<Quality>()
        var direct: String? = null
        val started = System.currentTimeMillis()
        // Always try the known-working official scraper first (even for non-fasel embeds)
        try {
            val encodedLink = URLEncoder.encode(link, "UTF-8")
            val body = httpGet(
                "https://mawdhou3.com/scripttestfasel.php?api=$encodedLink",
                video.userAgent ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/137.0.0.0 Safari/537.36",
                video.referer ?: "https://faselhd.center/"
            )
            if (!body.isNullOrBlank()) {
                val parsed = parsePairs(body)
                if (parsed.isNotEmpty()) {
                    return Resolved(
                        qualities = dedupe(parsed),
                        directUrl = parsed.firstOrNull { it.url.contains(".m3u8", true) }?.url ?: parsed.first().url,
                        source = "scripttestfasel-chain"
                    )
                }
            }
        } catch (_: Throwable) { }

        val usable = hosts.filter { it.enabled && it.urlSite.startsWith("http") && it.urlSite.length <= 300 }
        // Prefer hosts whose domain list matches the link (Server Shahed / UpDown / Wish…)
        val linkHost = try { java.net.URI(link).host?.lowercase().orEmpty() } catch (_: Throwable) { "" }
        val ordered = usable.sortedByDescending { h ->
            h.domains.any { d -> linkHost.contains(d.lowercase()) || lower.contains(d.lowercase()) }
        }
        for (host in ordered) {
            if (candidates.isNotEmpty()) break
            if (System.currentTimeMillis() - started > CHAIN_BUDGET_MS) break
            val site = host.urlSite.trim()
            if (site.isBlank() || !site.contains("http")) continue
            try {
                // The live hosts/config endpoint already returns URLs such as
                // ".../ukrcdn.php?api=". Appending another "api=" produced
                // "?api=&api=...", which made every quality lookup fail and
                // sent the UI to the protected-page fallback.
                val encodedLink = URLEncoder.encode(link, "UTF-8")
                val apiMarker = Regex("([?&])api=", RegexOption.IGNORE_CASE).find(site)
                val apiUrl = if (apiMarker != null) {
                    site.substring(0, apiMarker.range.last + 1) + encodedLink
                } else if (site.contains("workers.dev") || site.endsWith("url=")) {
                    site + encodedLink
                } else {
                    val sep = if (site.contains("?")) "&" else "?"
                    site + sep + "api=" + encodedLink
                }
                val text = httpGet(
                    apiUrl,
                    host.userAgent.ifBlank { video.userAgent },
                    host.referer.ifBlank { video.referer ?: "https://shaaheid4u.net/" }
                ) ?: continue
                val parsed = parsePairs(text)
                if (parsed.isNotEmpty()) {
                    candidates.addAll(parsed)
                    direct = parsed.firstOrNull { !it.url.lowercase().contains(".m3u8") }?.url ?: parsed.first().url
                    break
                }
            } catch (_: Throwable) { continue }
        }
        if (candidates.isNotEmpty()) {
            return Resolved(
                qualities = dedupe(candidates),
                directUrl = direct,
                source = "resolver"
            )
        }

        // 3) Last resort: generic worker proxy
        try {
            val text = httpGet(FALLBACK_WORKER + URLEncoder.encode(link, "UTF-8"), video.userAgent, video.referer)
            val parsed = parsePairs(text.orEmpty())
            if (parsed.isNotEmpty()) {
                return Resolved(
                    qualities = dedupe(parsed),
                    directUrl = parsed.firstOrNull { !it.url.lowercase().contains(".m3u8") }?.url,
                    source = "worker"
                )
            }
        } catch (_: Throwable) { }

        // 4) General WebView fallback for ANY remaining host.
        // Official Fasel scrapers on mawdhou3 are often stale; a real browser pass
        // is the most reliable way left to harvest .m3u8 / .mp4 from protected embeds.
        if (appContext != null) {
            try {
                val browser = WebViewResolver.resolve(appContext, link, 14_000L)
                if (browser.isNotEmpty()) {
                    val qs = browser.map { it.copy(url = it.url.trim()) }.distinctBy { it.url }
                    return Resolved(
                        qualities = qs,
                        directUrl = qs.firstOrNull { !it.url.contains(".m3u8", true) }?.url
                            ?: qs.first().url,
                        source = "webview-general"
                    )
                }
            } catch (e: Throwable) {
                android.util.Log.w("HostResolver", "general webview failed: ${e.message}")
            }
        }

        // Last resort: keep Original so the UI can still offer play + web-embed fallback.
        // Label stays "Original" (not a fake quality). Caller sets webEmbedFallback.
        return Resolved(
            qualities = listOf(Quality("Original", link)),
            directUrl = link,
            source = "original"
        )
    }

    /** Exact worker flow observed in the original app's HAR capture. */
    private suspend fun resolveWorker(link: String, ua: String?, ref: String?): Resolved? {
        val encoded = URLEncoder.encode(link, "UTF-8")
        // The HAR shows the raw nested query and Chrome/117 UA. Do not add
        // Referer, Accept, cookies, or the Android UA used by the new app.
        val urls = listOf(FALLBACK_WORKER + link, FALLBACK_WORKER + encoded).distinct()
        for (url in urls) {
            val body = workerGet(url) ?: continue
            val parsed = parsePairs(body)
            if (parsed.isNotEmpty()) {
                val direct = parsed.firstOrNull { !it.url.lowercase().contains(".m3u8") }?.url
                    ?: parsed.first().url
                return Resolved(qualities = dedupe(parsed), directUrl = direct, source = "original-worker")
            }
        }
        return null
    }

    fun parsePairs(text: String): List<Quality> {
        val out = ArrayList<Quality>()
        try {
            val m1 = PAIR.matcher(text)
            val m2 = OLD_PAIR.matcher(text)
            while (m1.find()) addPair(out, m1.group(1), m1.group(2))
            while (m2.find()) addPair(out, m2.group(1), m2.group(2))
            parseJsonQualities(text, out)
            // Some workers return newline-delimited file:"...",label:"..."
            // without JSON braces. Accept those records exactly as the old app did.
            val linePair = Pattern.compile("file\\s*:\\s*[\\\"'](.*?)[\\\"']\\s*,\\s*label\\s*:\\s*[\\\"'](.*?)[\\\"']", Pattern.CASE_INSENSITIVE)
            val lm = linePair.matcher(text)
            while (lm.find()) addPair(out, lm.group(1), lm.group(2))
        } catch (_: Throwable) { }
        return dedupe(out)
    }

    private fun parseJsonQualities(text: String, out: MutableList<Quality>) {
        val root = try { org.json.JSONTokener(text.trim()).nextValue() } catch (_: Throwable) { return }
        val arrays = ArrayList<org.json.JSONArray>()
        when (root) {
            is org.json.JSONArray -> arrays += root
            is org.json.JSONObject -> listOf("qualities", "availableQualities", "sources", "links", "data")
                .forEach { root.optJSONArray(it)?.let(arrays::add) }
        }
        for (arr in arrays) for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val url = item.optString("url").ifBlank { item.optString("file") }
                .ifBlank { item.optString("link") }.ifBlank { item.optString("src") }
            val label = item.optString("quality").ifBlank { item.optString("label") }
                .ifBlank { item.optString("name") }.ifBlank { item.optString("resolution") }
            addPair(out, url, label)
        }
    }

    private fun JSONArray0(): org.json.JSONArray = org.json.JSONArray()

    private fun addPair(out: MutableList<Quality>, url: String?, label: String?) {
        val u = url?.trim().orEmpty()
        if (!u.startsWith("http") || u.length > 2000) return // guard against garbage configs
        val l = label?.trim().orEmpty()
            .replace("\"", "").replace("،", "/")
        out += Quality(if (l.isBlank()) "جودة" else l, u)
    }

    private fun dedupe(list: List<Quality>): List<Quality> {
        val seen = HashSet<String>()
        val out = ArrayList<Quality>()
        for (q in list) if (seen.add(q.url)) out += q
        return out
    }

    /** Parse an m3u8 master playlist into variant qualities. (public: reused by WebViewResolver) */
    fun parseMaster(text: String, baseUrl: String): List<Quality> {
        val out = ArrayList<Quality>()
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val band = Regex("BANDWIDTH=(\\d+)").find(line)?.groupValues?.get(1)
                val res = Regex("RESOLUTION=(\\d+x\\d+)").find(line)?.groupValues?.get(1)
                var uri: String? = null
                if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) uri = lines[i + 1]
                uri?.let { u ->
                    val abs = try { java.net.URI(baseUrl).resolve(u).toString() } catch (_: Throwable) { u }
                    val label = res ?: (if (band != null) "${(band.toLong() / 1000)}k" else "جودة")
                    out += Quality(label, abs)
                }
                i++
            } else i++
        }
        return out
    }

    private suspend fun tryParseMaster(url: String, ua: String?, ref: String?): List<Quality> {
        val body = httpGet(url, ua, ref) ?: return emptyList()
        if (!body.startsWith("#EXTM3U") && !body.contains("#EXT-X-STREAM-INF")) return emptyList()
        return parseMaster(body, url)
    }

    /** Plain HttpURLConnection — avoids OkHttp/R8 NoClassDefFoundError on release builds. */
    private suspend fun httpGet(url: String, ua: String?, ref: String?): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                ua?.ifBlank { null }
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
            )
            if (!ref.isNullOrBlank()) conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Accept-Language", "ar,en;q=0.9")
            val code = conn.responseCode
            if (code !in 200..299) {
                android.util.Log.w("HostResolver", "httpGet $code for $url")
                return null
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Throwable) {
            android.util.Log.w("HostResolver", "httpGet fail: ${e.message} url=$url")
            null
        }
    }

    private fun workerGet(url: String): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"
            )
            conn.setRequestProperty("Accept", "*/*")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) { null }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
