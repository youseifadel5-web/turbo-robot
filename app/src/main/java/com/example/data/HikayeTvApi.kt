package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object HikayeTvApi {
    private const val BASE_URL = "https://admin.golive-pro.online/api"
    private const val CACHE_TTL_MS = 75_000L
    /** حجم الصفحة — 35 عنصر يوفر بيانات ويبقي القائمة خفيفة */
    private const val PAGE_LIMIT = 35

    val MOVIE_CATEGORIES: List<Triple<String, String, String>> = listOf(
        Triple("arabic", "أفلام عربية", "2185cd2d-f379-4584-8caa-5884bced7150"),
        Triple("foreign", "أفلام أجنبية", "9ec354e5-4707-4161-9dab-b51f899b29d8"),
        Triple("arabic_all", "كل الأفلام العربية", "a41d4764-d74e-4df3-a5aa-51e1725fe42e"),
        Triple("foreign_general", "أفلام أجنبية عامة", "7bd112fc-a3c2-49ab-a3d7-5287ffd1d045")
    )

    val SERIES_CATEGORIES: List<Triple<String, String, String>> = listOf(
        Triple("arabic", "مسلسلات عربية", "dd185bc6-1dfd-45c2-9189-13c47f672e5a")
    )

    val MOVIE_GENRES: Map<String, String> = linkedMapOf(
        "action" to "أكشن",
        "comedy" to "كوميدي",
        "drama" to "دراما",
        "horror" to "رعب",
        "sci_fi" to "خيال علمي",
        "romance" to "رومانسي",
        "thriller" to "إثارة",
        "adventure" to "مغامرة",
        "animation" to "أنيميشن",
        "crime" to "جريمة",
        "documentary" to "وثائقي",
        "family" to "عائلي"
    )

    val SERIES_KEYWORDS: Map<String, String> = linkedMapOf(
        "foreign" to "اجنبي",
        "turkish" to "تركي",
        "korean" to "كوري",
        "anime" to "انمي",
        "dubbed" to "مدبلج",
        "ramadan" to "رمضان"
    )

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private data class CacheEntry(val timeMs: Long, val value: String?)
    private val responseCache = ConcurrentHashMap<String, CacheEntry>()

    data class HMedia(
        val id: String,
        val title: String,
        val titleAr: String = "",
        val titleEn: String = "",
        val slug: String = "",
        val poster: String = "",
        val backdrop: String = "",
        val year: Int = 0,
        val rating: Double = 0.0,
        val genreAr: String = "",
        val overview: String = "",
        val kind: String = "movie"
    ) {
        val displayTitle: String get() = firstNonBlank(titleAr, titleEn, title, deriveFromSlug(slug), deriveFromPoster(poster), "عنوان غير متوفر")
    }

    data class HPage(
        val items: List<HMedia>,
        val page: Int,
        val lastPage: Int,
        val total: Int
    )

    data class HSource(
        val label: String,
        val url: String,
        val quality: String = "",
        val format: String = ""
    )

    data class HEpisode(
        val id: String,
        val number: Int,
        val season: Int,
        val title: String,
        val sources: List<HSource>
    )

    data class HSeason(val number: Int, val name: String, val episodes: List<HEpisode>)

    data class HSeriesDetail(
        val id: String,
        val title: String,
        val poster: String = "",
        val overview: String = "",
        val seasons: List<HSeason> = emptyList()
    )

    private fun cleanText(value: Any?): String {
        val s = value?.toString()?.trim().orEmpty()
        return if (s.isBlank() || s.equals("null", true) || s.equals("none", true) || s.equals("undefined", true)) "" else s
    }

    private fun firstNonBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() } ?: ""

    private fun deriveFromSlug(slug: String): String {
        val raw = cleanText(slug)
        if (raw.isBlank()) return ""
        return raw.substringAfterLast('/').substringBeforeLast('.')
            .replace('-', ' ')
            .replace('_', ' ')
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { part -> part.lowercase().replaceFirstChar { ch -> ch.titlecase() } }
    }

    private fun deriveFromPoster(url: String): String {
        val raw = cleanText(url)
        if (raw.isBlank()) return ""
        val name = raw.substringAfterLast('/').substringBeforeLast('.')
            .substringBefore('?').substringBefore('#')
        return name.replace('-', ' ').replace('_', ' ').trim()
    }

    private fun get(path: String, useCache: Boolean = true): String? {
        return try {
            val now = System.currentTimeMillis()
            if (useCache) {
                val cached = responseCache[path]
                if (cached != null && now - cached.timeMs <= CACHE_TTL_MS) cached.value else {
                    val req = Request.Builder()
                        .url(BASE_URL + path)
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 HikayeTV/3.0")
                        .header("Accept-Language", "ar,en;q=0.8")
                        .build()
                    val value = client.newCall(req).execute().use { r -> if (!r.isSuccessful) null else r.body?.string() }
                    responseCache[path] = CacheEntry(now, value)
                    value
                }
            } else {
                val req = Request.Builder()
                    .url(BASE_URL + path)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 HikayeTV/3.0")
                    .header("Accept-Language", "ar,en;q=0.8")
                    .build()
                client.newCall(req).execute().use { r -> if (!r.isSuccessful) null else r.body?.string() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun sources(arr: JSONArray?): List<HSource> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val u = firstNonBlank(cleanText(o.opt("streamUrl")), cleanText(o.opt("url")))
            if (u.isBlank()) null
            else HSource(
                label = cleanText(o.opt("label")).ifBlank { "سيرفر مشاهدة" },
                url = u,
                quality = cleanText(o.opt("quality")),
                format = cleanText(o.opt("format"))
            )
        }
    }

    private fun media(o: JSONObject?, kind: String): HMedia? {
        if (o == null) return null
        val id = cleanText(o.opt("id"))
        if (id.isBlank()) return null
        val titleAr = cleanText(o.opt("titleAr"))
        val titleEn = cleanText(o.opt("titleEn"))
        val title = cleanText(o.opt("title"))
        val slug = firstNonBlank(cleanText(o.opt("slug")), cleanText(o.opt("seoSlug")))
        val poster = cleanText(o.opt("posterUrl"))
        val backdrop = cleanText(o.opt("backdropUrl"))
        val safeTitle = firstNonBlank(titleAr, titleEn, title, deriveFromSlug(slug), deriveFromPoster(poster))
        if (safeTitle.isBlank()) return null
        return HMedia(
            id = id,
            title = safeTitle,
            titleAr = titleAr,
            titleEn = titleEn,
            slug = slug,
            poster = poster,
            backdrop = backdrop,
            year = o.optInt("year", 0),
            rating = o.optDouble("rating", 0.0),
            genreAr = cleanText(o.opt("genreAr")),
            overview = firstNonBlank(cleanText(o.opt("descriptionAr")), cleanText(o.opt("description"))),
            kind = kind
        )
    }

    private fun page(json: String?, kind: String): HPage {
        if (json == null) return HPage(emptyList(), 1, 1, 0)
        return try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("data") ?: JSONArray()
            val meta = root.optJSONObject("meta") ?: JSONObject()
            val items = (0 until arr.length()).mapNotNull { i -> media(arr.optJSONObject(i), kind) }
            HPage(
                items = items,
                page = meta.optInt("page", 1),
                lastPage = maxOf(1, meta.optInt("totalPages", 1)),
                total = meta.optInt("total", items.size)
            )
        } catch (_: Throwable) {
            HPage(emptyList(), 1, 1, 0)
        }
    }

    private fun popularMovies(page: Int): HPage {
        val json = get("/content/movies/most-viewed?limit=$PAGE_LIMIT") ?: return HPage(emptyList(), page, 1, 0)
        return try {
            val arr = JSONArray(json)
            val items = (0 until arr.length()).mapNotNull { i -> media(arr.optJSONObject(i), "movie") }
            HPage(items = items, page = 1, lastPage = 1, total = items.size)
        } catch (_: Throwable) {
            HPage(emptyList(), page, 1, 0)
        }
    }

    suspend fun movies(page: Int = 1, mode: String = "latest", search: String = ""): HPage = withContext(Dispatchers.IO) {
        if (mode == "popular" && search.isBlank()) return@withContext popularMovies(page)
        val q = StringBuilder("/content/movies?page=$page&limit=$PAGE_LIMIT")
        when {
            search.isNotBlank() -> q.append("&search=").append(enc(search))
            mode == "arabic" -> q.append("&categoryId=").append(enc(MOVIE_CATEGORIES.first { it.first == "arabic" }.third))
            mode == "foreign" -> q.append("&categoryId=").append(enc(MOVIE_CATEGORIES.first { it.first == "foreign" }.third))
            mode.startsWith("genre_") -> MOVIE_GENRES[mode.removePrefix("genre_")]?.let { q.append("&genre=").append(enc(it)) }
            mode.startsWith("year_") -> mode.removePrefix("year_").toIntOrNull()?.let { q.append("&year=").append(it) }
            mode == "top_rated" -> q.append("&ratingMin=7&sortBy=rating&sortOrder=desc")
        }
        page(get(q.toString()), "movie")
    }

    suspend fun series(page: Int = 1, mode: String = "latest", search: String = ""): HPage = withContext(Dispatchers.IO) {
        val q = StringBuilder("/content/series?page=$page&limit=$PAGE_LIMIT")
        when {
            search.isNotBlank() -> q.append("&search=").append(enc(search))
            mode == "arabic" -> q.append("&categoryId=").append(enc(SERIES_CATEGORIES.first { it.first == "arabic" }.third))
            SERIES_KEYWORDS.containsKey(mode) -> q.append("&search=").append(enc(SERIES_KEYWORDS.getValue(mode)))
        }
        page(get(q.toString()), "series")
    }

    suspend fun search(query: String): List<HMedia> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val m = try { movies(1, search = query).items } catch (_: Throwable) { emptyList<HMedia>() }
        val s = try { series(1, search = query).items } catch (_: Throwable) { emptyList<HMedia>() }
        (m + s).distinctBy { it.kind + "_" + it.id }
    }

    suspend fun movieDetail(id: String): Pair<HMedia, List<HSource>>? = withContext(Dispatchers.IO) {
        val json = get("/content/movies/$id", useCache = true) ?: return@withContext null
        try {
            val root = JSONObject(json)
            val m = media(root, "movie") ?: return@withContext null
            m to sources(root.optJSONArray("sources"))
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun seriesDetail(id: String): HSeriesDetail? = withContext(Dispatchers.IO) {
        val json = get("/content/series/$id", useCache = true) ?: return@withContext null
        try {
            val root = JSONObject(json)
            val epsArr = root.optJSONArray("episodes") ?: JSONArray()
            val bySeason = LinkedHashMap<Int, MutableList<HEpisode>>()
            for (i in 0 until epsArr.length()) {
                val e = epsArr.optJSONObject(i) ?: continue
                val season = e.optInt("seasonNumber", 1)
                val list = bySeason.getOrPut(season) { mutableListOf() }
                list += HEpisode(
                    id = cleanText(e.opt("id")),
                    number = e.optInt("episodeNumber", 0),
                    season = season,
                    title = firstNonBlank(cleanText(e.opt("titleAr")), cleanText(e.opt("title")), "الحلقة ${e.optInt("episodeNumber", 0)}"),
                    sources = sources(e.optJSONArray("sources"))
                )
            }
            HSeriesDetail(
                id = cleanText(root.opt("id")).ifBlank { id },
                title = firstNonBlank(cleanText(root.opt("titleAr")), cleanText(root.opt("title")), cleanText(root.opt("titleEn")), "مسلسل"),
                poster = cleanText(root.opt("posterUrl")),
                overview = firstNonBlank(cleanText(root.opt("descriptionAr")), cleanText(root.opt("description"))),
                seasons = bySeason.map { (s, eps) -> HSeason(s, "الموسم $s", eps.sortedBy { it.number }) }.sortedBy { it.number }
            )
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun resolve(url: String): String = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext url
        try {
            val json = get("/stream/resolve?url=" + enc(url), useCache = true)
            val root = json?.let { JSONObject(it) }
            if (root != null && root.optBoolean("ok") && !cleanText(root.opt("resolved")).isBlank()) cleanText(root.opt("resolved")) else url
        } catch (_: Throwable) {
            url
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
