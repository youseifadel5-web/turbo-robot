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

/**
 * Full Fasel HD read adapter. Every method maps 1:1 to a real endpoint verified
 * against live responses (fashd.com/faselhd15/public/api). Search, networks,
 * genres, detail, seasons, hosts config and the host resolver chain are all
 * implemented here — the old version only consumed media/homecontent + 3 pages.
 */
data class FaselMedia(
    val id: Int,
    val title: String,
    val type: String,          // movie | serie | anime | streaming
    val poster: String = "",
    val backdrop: String = "",
    val subtitle: String = "", // "1080p WEB-DL" quality label
    val vote: Double = 0.0,
    val release: String = "",
    val overview: String = "",
    val genres: List<String> = emptyList()
)

data class FaselPage(
    val items: List<FaselMedia>,
    val page: Int,
    val lastPage: Int,
    val total: Int,
    val perPage: Int
)

data class FaselNetwork(
    val id: String,
    val name: String,
    val logo: String = "",
    val type: String = ""
)

data class FaselGenre(val id: Int, val name: String)

data class FaselVideo(
    val server: String,
    val link: String,
    val userAgent: String? = null,
    val referer: String? = null,
    val hd: Boolean,
    val hls: Boolean,
    val lang: String = "",
    val downloadOnly: Boolean = false
)

data class FaselQuality(val label: String, val url: String)

data class FaselDetail(
    val id: Int,
    val title: String,
    val overview: String = "",
    val poster: String = "",
    val backdrop: String = "",
    val vote: Double = 0.0,
    val release: String = "",
    val subtitle: String = "",
    val runtime: String = "",
    val cast: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val networks: List<FaselNetwork> = emptyList(),
    val videos: List<FaselVideo> = emptyList(),
    val substitles: List<String> = emptyList(),
    val related: List<FaselMedia> = emptyList()
)

data class FaselEpisode(
    val id: Int,
    val number: Int,
    val name: String,
    val imdb: String = "",
    val still: String = "",
    val videos: List<FaselVideo> = emptyList()
)

data class FaselSeason(val id: Int, val name: String, val episodes: List<FaselEpisode> = emptyList())

data class FaselSeries(
    val id: Int,
    val title: String,
    val overview: String = "",
    val poster: String = "",
    val vote: Double = 0.0,
    val genres: List<String> = emptyList(),
    val seasons: List<FaselSeason> = emptyList()
)

data class HostConfig(
    val hostId: String,
    val domains: List<String> = emptyList(),
    val urlSite: String = "",
    val referer: String = "",
    val userAgent: String = "",
    val enabled: Boolean = true
)

object FaselHdApi {
    private const val BASE_URL = "https://fashd.com/faselhd15/public/api/"
    private const val CACHE_TTL_MS = 75_000L
    private val APP_TOKEN: String = SecretVault.FASEL_TOKEN
    private val PLATFORM_HEADERS: String = "packagename: " + SecretVault.FASEL_PACKAGE
    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500/"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private data class CacheEntry(val timeMs: Long, val body: String)
    private val responseCache = ConcurrentHashMap<String, CacheEntry>()

    private fun rawRequest(url: String): String {
        val now = System.currentTimeMillis()
        responseCache[url]?.let { cached ->
            if (now - cached.timeMs <= CACHE_TTL_MS) return cached.body
        }
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("packagename", SecretVault.FASEL_PACKAGE)
            .header("User-Agent", "EasyPlex (Android 15; RMX3710; realme REE2ADL1; ar)")
            .build()
        client.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("FaselHD HTTP ${response.code}: $url")
            responseCache[url] = CacheEntry(now, body)
            return body
        }
    }

    private fun requestJson(path: String): JSONObject = JSONObject(rawRequest(BASE_URL + path))

    private fun requestArray(path: String): JSONArray = JSONArray(rawRequest(BASE_URL + path))

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    // ---------------------------------------------------------------- search
    suspend fun search(query: String): List<FaselMedia> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val root = requestJson("search/${enc(query)}/$APP_TOKEN")
            val arr = root.optJSONArray("search") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                mediaFromJson(o)
            }
        } catch (e: Throwable) { emptyList() }
    }

    // -------------------------------------------------------------- paged lists
    suspend fun moviesPage(page: Int): FaselPage =
        paged("genres/movies/all/$APP_TOKEN?page=$page").filterByKind("movie")
    suspend fun seriesPage(page: Int): FaselPage =
        paged("genres/series/all/$APP_TOKEN?page=$page").filterByKind("serie")
    suspend fun animesPage(page: Int): FaselPage =
        paged("genres/animes/all/$APP_TOKEN?page=$page")
            .filterByKind("anime")
            .filterAnimeCatalog()
    suspend fun networkPage(networkId: String, page: Int): FaselPage =
        paged("networks/media/show/${enc(networkId)}/$APP_TOKEN?page=$page")
    /** Movie titles for a real genre folder (reference API: genres/movies/show). */
    suspend fun genrePage(genreId: Int, page: Int): FaselPage =
        paged("genres/movies/show/$genreId/$APP_TOKEN?page=$page")

    /** Real genre pages per kind — endpoints verified live:
     *  SERIES → genres/series/show/{id} · ANIME → genres/animes/show/{id}.
     *  (The old code used the movies endpoint for every kind → wrong titles.) */
    suspend fun kindGenrePage(kind: String, genreId: Int, page: Int): FaselPage = when {
        kind.equals("ANIME", true) -> paged("genres/animes/show/$genreId/$APP_TOKEN?page=$page")
            .filterByKind("anime")
            .filterAnimeCatalog()
        kind.equals("SERIE", true) || kind.equals("SERIES", true) ->
            paged("genres/series/show/$genreId/$APP_TOKEN?page=$page")
                .filterByKind("serie")
        else -> paged("genres/movies/show/$genreId/$APP_TOKEN?page=$page")
            .filterByKind("movie")
    }

    /** فلتر نوع — مارجّعش القائمة الأصلية لو فضيت (كانت بتخلي مسلسلات في الأنمي) */
    private fun FaselPage.filterByKind(expected: String): FaselPage {
        val filtered = items.filter {
            val t = it.type.trim().lowercase()
            when (expected) {
                "anime" -> t.contains("anime") || t.contains("cartoon") || t.contains("انمي") || t.contains("أنمي")
                "serie" -> t.contains("serie") || t == "series" || t.contains("مسلسل")
                "movie" -> t.contains("movie") || t.contains("film") || t.contains("فيلم") || t.isBlank()
                else -> t.equals(expected, true) || t.isBlank()
            }
        }
        return copy(items = filtered)
    }

    /** استبعاد دراما/مدبلج اتسرّبت لجدول الأنمي */
    private fun FaselPage.filterAnimeCatalog(): FaselPage {
        val rejectTitle = listOf(
            "مدبلج", "عهد العمر", "ويبقى الحب", "رمضان", "مسلسل تركي", "مسلسل كوري",
            "تركي", "خليجي", "سوري", "لبناني"
        )
        val animeGenre = listOf("انمي", "أنمي", "anime", "شونين", "شوجو", "ميكا", "كرتون", "رسوم", "متحركة")
        val filtered = items.filter { item ->
            val t = item.type.trim().lowercase()
            if (!(t.contains("anime") || t.contains("cartoon") || t.contains("انمي") || t.contains("أنمي"))) return@filter false
            val title = item.title.lowercase()
            if (rejectTitle.any { title.contains(it) } && animeGenre.none { title.contains(it) }) return@filter false
            val names = item.genres.map { it.trim().lowercase() }
            if (names.isNotEmpty() &&
                names.any { g -> g in listOf("تركي", "كوري", "دراما", "رومانسي", "رومانسية") } &&
                names.none { g -> animeGenre.any { g.contains(it) } }
            ) return@filter false
            true
        }
        return copy(items = filtered)
    }

    private suspend fun paged(path: String): FaselPage = withContext(Dispatchers.IO) {
        val root = requestJson(path)
        val arr = root.optJSONArray("data") ?: JSONArray()
        val items = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            mediaFromJson(o)
        }
        FaselPage(
            items = items,
            page = root.optInt("current_page", 1),
            lastPage = root.optInt("last_page", 1).coerceAtLeast(1),
            total = root.optInt("total", items.size),
            perPage = root.optInt("per_page", 12).coerceAtLeast(1)
        )
    }

    // ----------------------------------------------------------------- networks
    suspend fun networks(): List<FaselNetwork> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("networks/lists/$APP_TOKEN")
            val arr = root.optJSONArray("networks") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                FaselNetwork(
                    id = o.optString("id").ifBlank { o.optInt("id", 0).toString() },
                    name = o.optString("name", "قسم"),
                    logo = normalizeImage(o.optString("logo_path")),
                    type = o.optString("type")
                )
            }.filter { it.id.isNotBlank() && it.id != "0" }
        } catch (e: Throwable) { emptyList() }
    }

    // ------------------------------------------------------------------ genres
    suspend fun genres(): List<FaselGenre> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("genres/list/$APP_TOKEN")
            val arr = root.optJSONArray("genres") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                FaselGenre(o.optInt("id"), o.optString("name"))
            }.filter { it.name.isNotBlank() }
        } catch (e: Throwable) { emptyList() }
    }

    // ------------------------------------------------------------------ detail
    suspend fun detail(id: Int): FaselDetail? = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("media/detail/$id/$APP_TOKEN")
            val media = root.optJSONObject("media") ?: root
            val videoArray = root.optJSONArray("videos") ?: media.optJSONArray("videos") ?: media.optJSONArray("sources") ?: JSONArray()
            val videos = videoArray.let { arr ->
                (0 until arr.length()).mapNotNull { i -> videoFromJson(arr.optJSONObject(i)) }
            }
            val cast = (root.optJSONArray("casterslist") ?: JSONArray()).let { arr ->
                (0 until arr.length()).map { arr.optJSONObject(it)?.optString("name", "") ?: "" }.filter { it.isNotBlank() }
            }
            val genres = (root.optJSONArray("genres") ?: JSONArray()).let { arr ->
                (0 until arr.length()).map { arr.optJSONObject(it)?.optString("name", "") ?: "" }.filter { it.isNotBlank() }
            }
            val nets = (root.optJSONArray("networkslist") ?: JSONArray()).let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    FaselNetwork(o.optInt("id", 0).toString(), o.optString("name", ""), "", o.optString("type"))
                }
            }
            val subs = (root.optJSONArray("substitles") ?: JSONArray()).let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    o.optString("subDownloadLink").ifBlank { o.optString("zipDownloadLink") }.ifBlank {
                        o.optString("link")
                    }
                }
            }
            val related = try {
                val r = requestJson("media/relateds/$id/$APP_TOKEN")
                (r.optJSONArray("relateds") ?: JSONArray()).let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        val o = arr.optJSONObject(i) ?: return@mapNotNull null
                        mediaFromJson(o)
                    }
                }
            } catch (e: Throwable) { emptyList() }

            FaselDetail(
                id = media.optInt("id", id),
                title = media.optString("title").ifBlank { media.optString("name") },
                overview = media.optString("overview"),
                poster = normalizeImage(media.optString("poster_path")),
                backdrop = normalizeImage(media.optString("backdrop_path").ifBlank { media.optString("backdrop_path_tv") }),
                vote = media.optDouble("vote_average", 0.0),
                release = media.optString("release_date"),
                subtitle = media.optString("subtitle"),
                runtime = media.optString("runtime"),
                cast = cast,
                genres = genres,
                networks = nets,
                videos = videos,
                substitles = subs,
                related = related
            )
        } catch (e: Throwable) { null }
    }

    // ----------------------------------------------------------------- series
    suspend fun series(id: Int): FaselSeries? = withContext(Dispatchers.IO) {
        try {
            val dest = requestJson("series/show/$id/$APP_TOKEN")
            val seasonsArr = dest.optJSONArray("seasons") ?: JSONArray()
            val seasons = (0 until seasonsArr.length()).mapNotNull { i ->
                val s = seasonsArr.optJSONObject(i) ?: return@mapNotNull null
                val sid = s.optInt("id", s.optString("id").toIntOrNull() ?: 0)
                if (sid == 0) return@mapNotNull null
                val seasonBody = try { requestJson("series/season/$sid/$APP_TOKEN") } catch (e: Throwable) { JSONObject() }
                val epsArr = seasonBody.optJSONArray("episodes") ?: JSONArray()
                val eps = (0 until epsArr.length()).mapNotNull { j ->
                    val e = epsArr.optJSONObject(j) ?: return@mapNotNull null
                    val imdb = e.optString("imdb_external_id").ifBlank { e.optString("imdb_id") }
                    val inlineVideos = (e.optJSONArray("videos") ?: e.optJSONArray("sources") ?: JSONArray()).let { v ->
                        (0 until v.length()).mapNotNull { k -> videoFromJson(v.optJSONObject(k)) }
                    }
                    val streamVideos = if (inlineVideos.isNotEmpty() || imdb.isBlank()) inlineVideos else try {
                        val stream = requestJson("series/episode/$imdb/$APP_TOKEN")
                        (stream.optJSONArray("videos") ?: stream.optJSONArray("sources") ?: stream.optJSONObject("episode")?.optJSONArray("videos") ?: JSONArray()).let { v -> (0 until v.length()).mapNotNull { k -> videoFromJson(v.optJSONObject(k)) } }
                    } catch (_: Throwable) { emptyList() }
                    FaselEpisode(
                        id = e.optInt("id", 0),
                        number = e.optInt("episode_number", j + 1),
                        name = e.optString("name").ifBlank { "الحلقة ${j + 1}" },
                        imdb = imdb,
                        still = normalizeImage(e.optString("still_path")),
                        videos = streamVideos
                    )
                }
                FaselSeason(sid, s.optString("name").ifBlank { "الموسم ${i + 1}" }, eps)
            }
            val genres = (dest.optJSONArray("genreslist") ?: JSONArray()).let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            }
            FaselSeries(
                id = dest.optInt("id", id),
                title = dest.optString("name").ifBlank { dest.optString("title") },
                overview = dest.optString("overview"),
                poster = normalizeImage(dest.optString("poster_path")),
                vote = dest.optDouble("vote_average", 0.0),
                genres = genres,
                seasons = seasons
            )
        } catch (e: Throwable) { null }
    }

    suspend fun animeDetail(id: Int): FaselSeries? = withContext(Dispatchers.IO) {
        try {
            // animes/show returns 500 Server Error for most ids on this deployment;
            // fall back to series/show which serves the same season/episode structure.
            val body = try {
                rawRequest(BASE_URL + "animes/show/$id/$APP_TOKEN")
            } catch (e: Throwable) {
                rawRequest(BASE_URL + "series/show/$id/$APP_TOKEN")
            }
            val dest = JSONObject(body)
            val seasonsArr = dest.optJSONArray("seasons") ?: JSONArray()
            val seasons = (0 until seasonsArr.length()).mapNotNull { i ->
                val s = seasonsArr.optJSONObject(i) ?: return@mapNotNull null
                val sid = s.optInt("id", s.optString("id").toIntOrNull() ?: 0)
                if (sid == 0) return@mapNotNull null
                // Anime seasons use a different API namespace; series/season returns 404.
                val seasonBody = try { requestJson("animes/season/$sid/$APP_TOKEN") } catch (e: Throwable) { JSONObject() }
                val epsArr = seasonBody.optJSONArray("episodes") ?: JSONArray()
                val eps = (0 until epsArr.length()).mapNotNull { j ->
                    val e = epsArr.optJSONObject(j) ?: return@mapNotNull null
                    val imdb = e.optString("imdb_external_id").ifBlank { e.optString("imdb_id") }
                    val inlineVideos = (e.optJSONArray("videos") ?: e.optJSONArray("sources") ?: JSONArray()).let { v ->
                        (0 until v.length()).mapNotNull { k -> videoFromJson(v.optJSONObject(k)) }
                    }
                    val streamVideos = if (inlineVideos.isNotEmpty() || imdb.isBlank()) inlineVideos else try {
                        val stream = requestJson("animes/episode/$imdb/$APP_TOKEN")
                        (stream.optJSONArray("videos") ?: stream.optJSONArray("sources") ?: stream.optJSONObject("episode")?.optJSONArray("videos") ?: JSONArray()).let { v -> (0 until v.length()).mapNotNull { k -> videoFromJson(v.optJSONObject(k)) } }
                    } catch (_: Throwable) { emptyList() }
                    FaselEpisode(
                        id = e.optInt("id", 0),
                        number = e.optInt("episode_number", j + 1),
                        name = e.optString("name").ifBlank { "الحلقة ${j + 1}" },
                        imdb = imdb,
                        still = normalizeImage(e.optString("still_path")),
                        videos = streamVideos
                    )
                }
                FaselSeason(sid, s.optString("name").ifBlank { "الموسم ${i + 1}" }, eps)
            }
            FaselSeries(
                id = dest.optInt("id", id),
                title = dest.optString("name").ifBlank { dest.optString("title") },
                overview = dest.optString("overview"),
                poster = normalizeImage(dest.optString("poster_path")),
                vote = dest.optDouble("vote_average", 0.0),
                seasons = seasons
            )
        } catch (e: Throwable) { null }
    }

    // ---------------------------------------------------------------- upcoming
    suspend fun upcoming(): List<FaselMedia> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("upcoming/latest/$APP_TOKEN")
            (root.optJSONArray("upcoming") ?: JSONArray()).let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    mediaFromJson(o)
                }
            }
        } catch (e: Throwable) { emptyList() }
    }

    // -------------------------------------------------------------- hosts conf
    private fun channelFromJson(o: JSONObject, group: String, idPrefix: String, number: Int): PlaylistItem? {
        val id = o.optInt("id", 0)
        if (id == 0) return null
        val videos = o.optJSONArray("videos") ?: return null
        val v = videos.optJSONObject(0) ?: return null
        val url = v.optString("link").trim()
        if (url.isBlank() || !(url.startsWith("http") || url.startsWith("rtmp"))) return null
        val logo = listOf(
                "poster_path", "logo_path", "backdrop_path", "backdrop_path_tv",
                "image", "logo", "poster", "icon", "channel_logo", "channelLogo", "thumbnail"
            )
            .map { o.optString(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .ifBlank {
                listOf("logo", "logo_url", "logoUrl", "image", "poster", "icon", "poster_path")
                    .map { v.optString(it) }
                    .firstOrNull { it.isNotBlank() }
                    .orEmpty()
            }
        return PlaylistItem(
            id = "$idPrefix$id",
            channelNumber = number,
            name = o.optString("name", "Live"),
            url = url,
            group = group,
            logoUrl = normalizeImage(logo),
            language = "AR",
            isLive = true,
            httpUserAgent = v.optString("useragent").ifBlank { null },
            httpReferrer = v.optString("header").ifBlank { null },
            isCustom = false
        )
    }

    /** Real live TV — most watched channels (livetv/mostwatched, verified live). */
    suspend fun livetvMostWatched(): List<PlaylistItem> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("livetv/mostwatched/$APP_TOKEN")
            val arr = root.optJSONArray("watched") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                channelFromJson(o, "FASEL HD / LIVE / الأكثر مشاهدة", "faselhd_mv_", i + 1)
            }
        } catch (e: Throwable) { emptyList() }
    }

    /** Real country categories (categories/list, verified live: مصر/الجزائر/العراق...). */
    suspend fun countries(): List<FaselNetwork> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("categories/list/$APP_TOKEN")
            val arr = root.optJSONArray("categories") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optInt("id", 0)
                val name = o.optString("name").trim()
                if (id == 0 || name.isBlank()) null
                else FaselNetwork(id = id.toString(), name = name, type = "country")
            }
        } catch (e: Throwable) { emptyList() }
    }

    /** Channels of a country (categories/streaming/show/{id}, verified live with real HLS links). */
    suspend fun countryLive(categoryId: Int): List<PlaylistItem> = withContext(Dispatchers.IO) {
        try {
            val root = requestJson("categories/streaming/show/$categoryId/$APP_TOKEN")
            val arr = root.optJSONArray("data") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                channelFromJson(o, "FASEL HD / LIVE / قنوات الدول", "faselhd_cty${categoryId}_", i + 1)
            }
        } catch (e: Throwable) { emptyList() }
    }

    suspend fun hostsConfig(): List<HostConfig> = withContext(Dispatchers.IO) {
        // Primary API + mirrors used by private Fasel APKs (same hosts/config payload).
        val bases = listOf(
            BASE_URL,
            "https://kahitdgku.com/faselhd15/public/api/",
            "https://hrrejhp.com/egybestanto/public/api/"
        )
        for (base in bases) {
            try {
                val body = rawRequest(base + "hosts/config")
                val arr = JSONArray(body)
                val list = (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val domains = (o.optJSONArray("domains") ?: JSONArray()).let { d ->
                        (0 until d.length()).map { d.optString(it) }.filter { it.isNotBlank() }
                    }
                    // Sanitize: corrupted configs sometimes ship a whole JSON blob inside
                    // urlsite — such garbage hosts must be dropped before use (hang fix).
                    var site = o.optString("urlsite").trim()
                    if (!site.startsWith("http") || site.length > 300) site = ""
                    // enableded sometimes holds the real working scraper URL after TRUE|
                    val enableded = o.optString("enableded")
                    if (site.isBlank() && enableded.contains("http")) {
                        val m = Regex("https?://\\S+").find(enableded)
                        if (m != null) site = m.value.trim().trimEnd('|', ' ')
                    }
                    if (site.isBlank()) return@mapNotNull null
                    HostConfig(
                        hostId = o.optString("host_id", "host$i"),
                        domains = domains,
                        urlSite = site,
                        referer = o.optString("referer"),
                        userAgent = o.optString("useragent"),
                        enabled = o.optBoolean("enabled", true)
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Throwable) {
                android.util.Log.w("FaselHdApi", "hostsConfig fail $base: ${e.message}")
            }
        }
        emptyList()
    }

    // ---------------------------------------------------------------- parsing
    private fun mediaFromJson(o: JSONObject): FaselMedia? {
        val id = o.optInt("id", 0)
        if (id == 0) return null
        val title = o.optString("title").ifBlank { o.optString("name").ifBlank { return null } }
        var type = o.optString("type").trim().lowercase()
        if (o.optInt("is_anime", 0) == 1 || o.optBoolean("is_anime", false)) type = "anime"
        if (type == "series" || type == "tv") type = "serie"
        if (type.isBlank()) type = "movie"
        val genres = (o.optJSONArray("genreslist") ?: o.optJSONArray("genres")).let { a ->
            if (a == null) emptyList() else (0 until a.length()).mapNotNull { j ->
                val g = a.optJSONObject(j)?.optString("name") ?: a.optString(j)
                g.ifBlank { null }
            }
        }
        return FaselMedia(
            id = id,
            title = title,
            type = type,
            poster = normalizeImage(o.optString("poster_path")),
            backdrop = normalizeImage(o.optString("backdrop_path").ifBlank { o.optString("backdrop_path_tv") }),
            subtitle = o.optString("subtitle"),
            vote = o.optDouble("vote_average", 0.0),
            release = o.optString("release_date").ifBlank { o.optString("first_air_date") },
            overview = o.optString("overview"),
            genres = genres
        )
    }

    private fun videoFromJson(o: JSONObject?): FaselVideo? {
        if (o == null) return null
        val link = o.optString("link")
            .ifBlank { o.optString("url") }
            .ifBlank { o.optString("file") }
            .ifBlank { o.optString("src") }
            .ifBlank { o.optString("video_url") }
            .ifBlank { o.optString("download_url") }
            .trim()
        if (link.isBlank()) return null
        if (o.optInt("status", 1) == 0) return null
        return FaselVideo(
            server = o.optString("server").ifBlank { o.optString("name", "سيرفر") },
            link = link,
            userAgent = o.optString("useragent").ifBlank { o.optString("user_agent") }.ifBlank { null },
            referer = o.optString("header").ifBlank { o.optString("referer") }.ifBlank { o.optString("referrer") }.ifBlank { null },
            hd = o.optInt("hd", 0) == 1 || o.optString("quality").contains("hd", true) || o.optString("label").contains("1080", true) || o.optString("label").contains("720", true),
            hls = o.optInt("hls", 0) == 1 || link.contains(".m3u8", true),
            lang = o.optString("lang", ""),
            downloadOnly = o.optInt("downloadonly", 0) == 1 || o.optBoolean("download_only", false)
        )
    }

    fun normalizeImage(value: String): String {
        if (value.isBlank()) return ""
        return when {
            value.startsWith("http://") -> "https://" + value.removePrefix("http://")
            value.startsWith("https://") -> value
            value.startsWith("/") -> IMAGE_BASE + value.removePrefix("/")
            else -> IMAGE_BASE + value
        }
    }

    // ----------------------------------------------------------- home catalog
    // Backward-compatible high-level catalog that feeds the classic VOD tabs.
    // Kept in a SEPARATE keyed collection so it never replaces imported catalogs
    // (this was the root cause of "بيفخي محتوى فاصل لو جبت فاير" — one list
    // overwriting the other instead of merging).
    data class Catalog(
        val channels: List<PlaylistItem>,
        val films: List<PlaylistItem>,
        val series: List<PlaylistItem>,
        val cartoons: List<PlaylistItem>
    )

    suspend fun loadHomeCatalog(): Catalog = withContext(Dispatchers.IO) {
        val home = requestJson("media/homecontent/$APP_TOKEN")
        val films = ArrayList<PlaylistItem>()
        val series = ArrayList<PlaylistItem>()
        val cartoons = ArrayList<PlaylistItem>()
        val channels = ArrayList<PlaylistItem>()

        addHomeSection(home.optJSONArray("featured"), "FEATURED", films, series, cartoons)
        addHomeSection(home.optJSONArray("latest"), "LATEST", films, series, cartoons)
        addHomeSection(home.optJSONArray("choosed"), "CHOOSED", films, series, cartoons)
        addHomeSection(home.optJSONArray("recommended"), "RECOMMENDED", films, series, cartoons)
        addHomeSection(home.optJSONArray("thisweek"), "THIS WEEK", films, series, cartoons)
        addHomeSection(home.optJSONArray("trending"), "TRENDING", films, series, cartoons)
        addHomeSection(home.optJSONArray("pinned"), "PINNED", films, series, cartoons)
        addHomeSection(home.optJSONArray("top10"), "TOP 10", films, series, cartoons)
        addHomeSection(home.optJSONArray("popular"), "POPULAR", films, series, cartoons)
        addHomeSection(home.optJSONArray("recents"), "RECENT", films, series, cartoons)
        addHomeSection(home.optJSONArray("popularSeries"), "POPULAR SERIES", films, series, cartoons)
        addHomeSection(home.optJSONArray("anime"), "ANIME", films, series, cartoons)
        addLiveSection(home.optJSONArray("livetv"), channels)

        Catalog(
            channels = channels.distinctBy { it.id },
            films = films.distinctBy { it.id },
            series = series.distinctBy { it.id },
            cartoons = cartoons.distinctBy { it.id }
        )
    }

    private fun addHomeSection(
        array: JSONArray?,
        section: String,
        films: MutableList<PlaylistItem>,
        series: MutableList<PlaylistItem>,
        cartoons: MutableList<PlaylistItem>
    ) {
        if (array == null) return
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val media = mediaFromJson(o) ?: continue
            val isAnime = media.type.contains("anime") || section == "ANIME"
            val isSerie = !isAnime && (media.type.contains("serie") || section.contains("SERIES"))
            val kind = if (isAnime) "ANIME" else if (isSerie) "SERIES" else "FILMS"
            val markerType = when (kind) { "SERIES" -> "series"; "ANIME" -> "anime"; else -> "movie" }
            val group = "FASEL HD / $kind / $section"
            val item = PlaylistItem(
                id = "faselhd_${kind.lowercase()}_${media.id}",
                channelNumber = i + 1,
                name = media.title,
                url = "faselhd://$markerType/${media.id}",
                group = group,
                logoUrl = media.poster,
                language = "AR",
                isLive = false,
                isCustom = false
            )
            when { isAnime -> cartoons += item; isSerie -> series += item; else -> films += item }
        }
    }

    private fun addLiveSection(array: JSONArray?, out: MutableList<PlaylistItem>) {
        if (array == null) return
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optInt("id", 0)
            val videos = o.optJSONArray("videos") ?: continue
            val v = videos.optJSONObject(0) ?: continue
            val url = v.optString("link").trim()
            if (id == 0 || url.isBlank()) continue
            out += PlaylistItem(
                id = "faselhd_live_$id",
                channelNumber = i + 1,
                name = o.optString("name", "Live"),
                url = url,
                group = "FASEL HD / LIVE TV",
                logoUrl = normalizeImage(
                    o.optString("poster_path")
                        .ifBlank { o.optString("backdrop_path") }
                        .ifBlank { o.optString("backdrop_path_tv") }
                        .ifBlank { o.optString("logo_path") }
                        .ifBlank { o.optString("image") }
                ),
                language = v.optString("lang", "AR"),
                isLive = true,
                httpUserAgent = v.optString("useragent").ifBlank { null },
                httpReferrer = v.optString("header").ifBlank { null },
                isCustom = false
            )
        }
    }

// --- Pagination fix: load ALL pages, not only the first ---
suspend fun allMovies(maxPages: Int = 80): List<FaselMedia> = withContext(Dispatchers.IO) {
    val first = runCatching { paged("genres/movies/all/$APP_TOKEN?page=1").filterByKind("movie") }.getOrNull()
    val out = ArrayList<FaselMedia>(first?.items.orEmpty())
    if (first != null) {
        val last = first.lastPage.coerceAtMost(maxPages)
        for (p in 2..last) {
            try { out += paged("genres/movies/all/$APP_TOKEN?page=$p").filterByKind("movie").items } catch (_: Throwable) {}
        }
    }
    out.distinctBy { it.id }
}
suspend fun allSeries(maxPages: Int = 80): List<FaselMedia> = withContext(Dispatchers.IO) {
    val first = runCatching { paged("genres/series/all/$APP_TOKEN?page=1").filterByKind("serie") }.getOrNull()
    val out = ArrayList<FaselMedia>(first?.items.orEmpty())
    if (first != null) {
        val last = first.lastPage.coerceAtMost(maxPages)
        for (p in 2..last) {
            try { out += paged("genres/series/all/$APP_TOKEN?page=$p").filterByKind("serie").items } catch (_: Throwable) {}
        }
    }
    out.distinctBy { it.id }
}
suspend fun allAnimes(maxPages: Int = 80): List<FaselMedia> = withContext(Dispatchers.IO) {
    val first = runCatching { paged("genres/animes/all/$APP_TOKEN?page=1").filterByKind("anime").filterAnimeCatalog() }.getOrNull()
    val out = ArrayList<FaselMedia>(first?.items.orEmpty())
    if (first != null) {
        val last = first.lastPage.coerceAtMost(maxPages)
        for (p in 2..last) {
            try { out += paged("genres/animes/all/$APP_TOKEN?page=$p").filterByKind("anime").filterAnimeCatalog().items } catch (_: Throwable) {}
        }
    }
    out.distinctBy { it.id }
}
}
