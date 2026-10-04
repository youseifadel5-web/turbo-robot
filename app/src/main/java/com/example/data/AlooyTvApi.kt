package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.regex.Pattern

/** Read-only parser matching the البصري TV web flow: genre cards -> /watch page -> Ep# links. */
object AlooyTvApi {
    private val cardImage = Pattern.compile("(?s)<img[^>]+(?:data-src|data-original|data-lazy-src)=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE)
    private val poster = Pattern.compile("(?s)<(?:video|img)[^>]+(?:poster|src)=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE)
    private val source = Pattern.compile("(?s)<source[^>]+src=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE)
    private val episode = Pattern.compile("(?s)<a[^>]+href=\\\"([^\\\"]+)\\\"[^>]*class=\\\"[^\\\"]*btn-ep[^\\\"]*\\\"[^>]*>\\s*(.*?)\\s*</a>", Pattern.CASE_INSENSITIVE)
    private val title = Pattern.compile("(?s)<h1[^>]*>(.*?)</h1>|<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE)

    suspend fun loadSeries(pageUrl: String, fallbackTitle: String): FaselSeries? = withContext(Dispatchers.IO) {
        val first = get(pageUrl) ?: return@withContext null
        val posterUrl = poster.matcher(first).let { if (it.find()) absolute(it.group(1).orEmpty(), pageUrl) else "" }
        val pageTitle = title.matcher(first).let {
            if (it.find()) clean(it.group(1).orEmpty().ifBlank { it.group(2).orEmpty() }) else fallbackTitle
        }.ifBlank { fallbackTitle }
        val eps = ArrayList<FaselEpisode>()
        val matcher = episode.matcher(first)
        while (matcher.find() && eps.size < 120) {
            val epUrl = absolute(matcher.group(1).orEmpty(), pageUrl)
            val epName = clean(matcher.group(2).orEmpty()).ifBlank { "الحلقة ${eps.size + 1}" }
            val body = get(epUrl).orEmpty()
            val stream = source.matcher(body).let { if (it.find()) absolute(it.group(1).orEmpty(), epUrl) else "" }
            eps += FaselEpisode(eps.size + 1, eps.size + 1, epName, still = posterUrl,
                videos = if (stream.isBlank()) emptyList() else listOf(FaselVideo("البصري", stream, referer = origin(epUrl), hd = true, hls = stream.contains(".m3u8", true))))
        }
        if (eps.isEmpty()) {
            val stream = source.matcher(first).let { if (it.find()) absolute(it.group(1).orEmpty(), pageUrl) else "" }
            if (stream.isNotBlank()) eps += FaselEpisode(1, 1, "الحلقة 1", still = posterUrl,
                videos = listOf(FaselVideo("البصري", stream, referer = origin(pageUrl), hd = true, hls = stream.contains(".m3u8", true))))
        }
        FaselSeries(0, pageTitle, poster = posterUrl, seasons = listOf(FaselSeason(1, "الموسم الأول", eps)))
    }

    data class AlooyCard(val title: String, val pageUrl: String, val poster: String, val episodes: Int)

    private val genreLink = Pattern.compile("(?s)<h3>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE)
    private val anyImage = Pattern.compile("(?s)<img[^>]+(?:data-src|data-original|data-lazy-src|src)=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    private val cardAnchor = Regex("class=\"movie-img\"", RegexOption.IGNORE_CASE)
    private val episodeCount = Regex("(\\d+)\\s*عدد الحلقات")

    /** Reads one genre page (e.g. /genre/arabic.html) into title + link + poster. */
    suspend fun loadGenre(genreUrl: String): List<AlooyCard> = withContext(Dispatchers.IO) {
        val html = get(genreUrl) ?: return@withContext emptyList()
        val out = ArrayList<AlooyCard>()
        var from = 0
        while (out.size < 300) {
            val m = cardAnchor.find(html, from) ?: break
            from = m.range.last + 1
            val block = html.substring(m.range.first, minOf(html.length, m.range.first + 2600))
            val link = genreLink.matcher(block)
            if (!link.find()) continue
            val url = absolute(link.group(1).orEmpty().trim(), genreUrl)
            val name = clean(link.group(2).orEmpty())
            if (url.isBlank() || name.isBlank()) continue
            var image = anyImage.matcher(block).let { if (it.find()) it.group(1).orEmpty() else "" }
            if (image.contains("blank_thumbnail", true)) image = ""
            out += AlooyCard(
                title = name,
                pageUrl = url,
                poster = if (image.isBlank()) "" else absolute(image, genreUrl),
                episodes = episodeCount.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            )
        }
        out.distinctBy { it.pageUrl }
    }

    fun imageFromCard(html: String): String = cardImage.matcher(html).let { if (it.find()) it.group(1).orEmpty() else "" }

    private fun get(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 Android").header("Accept", "text/html,*/*").build()
        FaselHdApi.client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
    } catch (_: Throwable) { null }

    private fun clean(s: String) = s.replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
    private fun absolute(value: String, base: String): String = when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> base.substringBefore("/", "https:") + value
        else -> base.substringBeforeLast("/", base) + "/" + value
    }
    private fun origin(url: String): String = try { val u = java.net.URI(url); "${u.scheme}://${u.host}/" } catch (_: Throwable) { "https://k.alooytv10.com/" }
}
