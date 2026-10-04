package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.regex.Pattern

data class FirebaseCatalogResult(
    val channels: List<PlaylistItem>,
    val films: List<PlaylistItem>,
    val series: List<PlaylistItem>,
    val cartoons: List<PlaylistItem>
)

/** Read-only importer for the owner's Firebase catalog. No admin key is bundled. */
object FirebaseCatalogImporter {
    private val DATABASE_URL: String = SecretVault.FIREBASE_URL
    private val ROOT: String = SecretVault.FIREBASE_ROOT
    private val SCRAPING_DOMAIN: String = SecretVault.SCRAPING_DOMAIN
    private val client = OkHttpClient.Builder().build()

    suspend fun import(code: String): FirebaseCatalogResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$DATABASE_URL/$ROOT.json")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Catalog request failed: HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank() || body == "null") return@withContext FirebaseCatalogResult(emptyList(), emptyList(), emptyList(), emptyList())
            val root = JSONObject(body)
            val base = parseCatalog(root)
            val scraped = fetchMovieItems(root)
            FirebaseCatalogResult(
                channels = base.channels,
                films = (base.films + scraped.filter { it.group.startsWith("FILMS:") }),
                series = (base.series + scraped.filter { it.group.startsWith("SERIES:") }),
                cartoons = (base.cartoons + scraped.filter { it.group.startsWith("CARTOONS:") })
            )
        }
    }

    private fun fetchMovieItems(root: JSONObject): List<PlaylistItem> {
        val result = ArrayList<PlaylistItem>()
        val categories = root.optJSONArray("catalog_categories") ?: return result
        // The original البصري TV card is .movie-img: image (data-src), play link, then movie-title.
        // Matching the whole card is important; the image is not inside .movie-title.
        val blockPattern = Pattern.compile("(?s)<div[^>]*class=\\\"[^\\\"]*movie-img[^\\\"]*\\\"[^>]*>(.*?)</div>\\s*</div>\\s*</div>", Pattern.CASE_INSENSITIVE)
        // Slice-based card scan: anchor on the card class then read the next
        // 2600 characters. The old fixed "</div></div></div>" nesting only
        // matched about half of the real cards, so half the library was lost.
        val anchorPattern = Pattern.compile("class=\"[^\"]*movie-img[^\"]*\"", Pattern.CASE_INSENSITIVE)
        val linkPattern = Pattern.compile("(?s)<h3[^>]*>.*?<a[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE)
        val imagePattern = Pattern.compile("(?s)<img[^>]+(?:data-src|data-original|data-lazy-src)=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE)
        val fallbackImagePattern = Pattern.compile("(?s)<img[^>]+src=\\\"([^\\\"]+)", Pattern.CASE_INSENSITIVE)
        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            if (category.optString("scope").uppercase() != "MOVIES") continue
            val categoryTitle = category.optString("title", "Films")
            val target = normalizeScrapingUrl(category.optString("targetUrl").trim())
            if (target.isBlank()) continue
            try {
                val request = Request.Builder().url(target).header("User-Agent", "Mozilla/5.0 Android").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val html = response.body?.string().orEmpty()
                    val blocks = anchorPattern.matcher(html)
                    while (blocks.find() && result.size < 400) {
                        val block = html.substring(blocks.start(), minOf(html.length, blocks.start() + 2600))
                        val link = linkPattern.matcher(block)
                        if (!link.find()) continue
                        val url = link.group(1).orEmpty().trim()
                        val title = link.group(2).orEmpty().replace(Regex("<[^>]+>"), "").trim()
                        if (url.isBlank() || title.isBlank()) continue
                        val image = imagePattern.matcher(block).let {
                            if (it.find()) it.group(1).orEmpty()
                            else fallbackImagePattern.matcher(block).let { f -> if (f.find()) f.group(1).orEmpty() else "" }
                        }
                        val type = when {
                            categoryTitle.contains("انمي", true) || categoryTitle.contains("أنمي", true) -> "CARTOONS"
                            categoryTitle.contains("مسلسل", true) || categoryTitle.contains("series", true) -> "SERIES"
                            else -> "FILMS"
                        }
                        // A "blank_thumbnail.jpg" placeholder is not a poster at all.
                        val cleanImage = if (image.contains("blank_thumbnail", true)) "" else image
                        val slug = url.substringAfterLast('/').substringBefore(".html").ifBlank { "v${result.size + 1}" }
                        result += PlaylistItem(
                            id = "vod_${type.lowercase()}_$slug",
                            channelNumber = result.size + 1,
                            name = title,
                            url = url,
                            group = "$type:$categoryTitle",
                            logoUrl = if (cleanImage.isBlank()) "" else normalizeImageUrl(cleanImage),
                            language = "AR",
                            isLive = false,
                            isCustom = false
                        )
                    }
                }
            } catch (_: Throwable) {
                // One unavailable category must not prevent the other categories from loading.
            }
        }
        return result
    }

    private fun normalizeScrapingUrl(url: String): String {
        if (url.isBlank()) return url
        val path = url.substringAfter(".xyz", "")
        return if (path.isNotBlank()) SCRAPING_DOMAIN + path else url
    }

    private fun parseCatalog(root: JSONObject): FirebaseCatalogResult {
        val categoryById = HashMap<String, Pair<String, String>>()
        val categoryOrder = HashMap<String, Int>()
        val categoryImageById = HashMap<String, String>()
        val movieCategoryCards = ArrayList<PlaylistItem>()
        val categories = root.optJSONArray("catalog_categories") ?: JSONArray()
        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            val id = category.optString("id")
            if (id.isNotBlank()) {
                val title = category.optString("title", "GENERAL")
                val scope = category.optString("scope", "LIVE").uppercase()
                categoryById[id] = Pair(title, scope)
                categoryOrder[title] = category.optInt("order", i)
                categoryImageById[id] = imageValue(category)
                val target = normalizeScrapingUrl(category.optString("targetUrl").trim())
                if (scope != "LIVE" && title.isNotBlank() && target.isNotBlank()) {
                    movieCategoryCards += PlaylistItem(
                        id = "category_$id",
                        channelNumber = movieCategoryCards.size + 1,
                        name = title,
                        url = target,
                        group = vodGroup(title, scope),
                        logoUrl = imageValue(category),
                        language = "AR",
                        isLive = false,
                        isCustom = false
                    )
                }
            }
        }
        val items = parseChannels(root.opt("catalog_channels"), categoryById, categoryOrder, categoryImageById)
        val allVods = movieCategoryCards + items.filter { !it.isLive }
        fun isCartoon(item: PlaylistItem) = item.group.contains("انمي", true) || item.group.contains("أنمي", true) || item.group.contains("كرتون", true) || item.group.contains("رسوم", true)
        fun isSeries(item: PlaylistItem) = item.group.contains("مسلسل", true) || item.group.contains("series", true)
        return FirebaseCatalogResult(
            channels = items.filter { it.isLive },
            films = allVods.filter { !isSeries(it) && !isCartoon(it) },
            series = allVods.filter { isSeries(it) },
            cartoons = allVods.filter { isCartoon(it) }
        )
    }

    private fun parseChannels(raw: Any?, categoryById: Map<String, Pair<String, String>>, categoryOrder: Map<String, Int>, categoryImageById: Map<String, String> = emptyMap()): List<PlaylistItem> {
        val result = ArrayList<PlaylistItem>()
        val array = raw as? JSONArray
        val rootObject = raw as? JSONObject
        val count = array?.length() ?: rootObject?.length() ?: 0
        var number = 1
        for (index in 0 until count) {
            val key = if (array != null) index.toString() else rootObject!!.names()?.optString(index).orEmpty()
            val channel = if (array != null) array.optJSONObject(index) else rootObject!!.optJSONObject(key)
            if (channel == null) continue
            val categoryId = channel.optString("categoryId")
            val title = channel.optString("title").ifBlank { channel.optString("name") }.ifBlank { "Channel $number" }
            val category = categoryById[categoryId]
            val categoryTitle = category?.first ?: inferPackage(title)
            val scope = category?.second ?: "LIVE"
            // A channel with no picture of its own falls back to its category logo,
            // so no card is ever left blank (same behaviour as البصري).
            val logo = imageValue(channel).ifBlank { categoryImageById[categoryId].orEmpty() }
            val servers = channel.optJSONArray("servers") ?: JSONArray()
            var added = false
            for (i in 0 until servers.length()) {
                val server = servers.optJSONObject(i) ?: continue
                val url = server.optString("url").trim()
                if (url.isBlank()) continue
                result += PlaylistItem(
                    // Stable ids (no random UUID) so a Room REPLACE refreshes the row
                    // instead of stacking a brand-new copy of every channel each refresh.
                    id = if (i == 0) "firebase_$key" else "firebase_${key}_s$i",
                    channelNumber = number++,
                    // Only the first server carries the bare channel name; extras are
                    // suffixed so the UI can collapse them into a single card.
                    name = if (i == 0) title else "$title · ${server.optString("name").ifBlank { "Server ${i + 1}" }}",
                    url = url,
                    group = vodGroup(categoryTitle, scope),
                    logoUrl = logo,
                    language = "AR",
                    isLive = !isVodScope(scope),
                    httpUserAgent = server.optString("userAgent").ifBlank { null },
                    httpReferrer = server.optString("referer").ifBlank { null },
                    isCustom = false
                )
                added = true
            }
            if (!added) {
                val url = channel.optString("url").trim()
                if (url.isNotBlank()) result += PlaylistItem(
                    id = "firebase_$key",
                    channelNumber = number++,
                    name = title,
                    url = url,
                    group = vodGroup(categoryTitle, scope),
                    logoUrl = logo,
                    language = "AR",
                    isLive = !isVodScope(scope),
                    isCustom = false
                )
            }
        }
        return result.sortedWith(compareBy({ categoryOrder[rawGroup(it.group)] ?: Int.MAX_VALUE }, { it.channelNumber }))
    }

    private fun rawGroup(group: String): String = when {
        group.startsWith("SERIES:") -> group.removePrefix("SERIES:")
        group.startsWith("CARTOONS:") -> group.removePrefix("CARTOONS:")
        group.startsWith("FILMS:") -> group.removePrefix("FILMS:")
        else -> group
    }

    private fun isVodScope(scope: String): Boolean {
        val s = scope.uppercase()
        return s == "MOVIES" || s == "SERIES" || s == "CARTOONS" || s == "ANIME" || s == "VOD" || s == "FILMS"
    }

    private fun vodGroup(title: String, scope: String): String {
        val s = scope.uppercase(); val t = title.uppercase()
        return when {
            s == "SERIES" || s == "SERIE" || t.contains("SERIES") || title.contains("مسلسل") -> "SERIES:$title"
            s == "CARTOONS" || s == "ANIME" || t.contains("ANIME") || title.contains("أنمي") || title.contains("كرتون") -> "CARTOONS:$title"
            isVodScope(s) -> "FILMS:$title"
            else -> title
        }
    }

    private fun imageValue(o: JSONObject): String = sequenceOf(
        "imageUrl", "image_url", "logoUrl", "logo_url", "posterUrl", "poster_url",
        "thumbnailUrl", "thumbnail_url", "image", "logo", "poster", "icon"
    ).map { o.optString(it).trim() }.firstOrNull { it.isNotBlank() && it != "null" }
        ?.let { normalizeImageUrl(it) } ?: ""

    private fun normalizeImageUrl(value: String): String {
        if (value.startsWith("//")) return "https:$value"
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val path = if (value.startsWith("/")) value else "/$value"
        return SCRAPING_DOMAIN.trimEnd('/') + path
    }

    private fun inferPackage(title: String): String {
        val t = title.uppercase()
        return when {
            "BEIN" in t -> "BEIN SPORT"
            "TOD" in t -> "TOD Sports"
            "SHAHID" in t -> "SHAHID SPORT"
            "MBC" in t -> "MBC GROUP"
            "ROTANA" in t -> "Rotana"
            "ALKASS" in t || "الكأس" in title -> "ALKASS"
            "ABU DHABI" in t -> "Abu Dhabi Sports"
            "NEWS" in t -> "NEWS"
            else -> "OTHER"
        }
    }
}
