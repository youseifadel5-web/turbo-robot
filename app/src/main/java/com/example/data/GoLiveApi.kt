package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** GoLive catalog: movies, series and episode details are separate endpoints. */
object GoLiveApi {
    private const val BASE = "https://admin.golive-pro.online/api"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS).followRedirects(true).build()

    data class Source(val label: String, val url: String, val quality: String = "", val format: String = "")
    data class Media(val id: String, val title: String, val poster: String, val overview: String, val year: Int, val category: String, val isSeries: Boolean)
    data class Episode(val id: String, val number: Int, val title: String, val season: Int, val sources: List<Source>)
    data class SeriesDetail(val media: Media, val episodes: List<Episode>)
    data class MovieDetail(val media: Media, val sources: List<Source>)

    private fun get(path: String): String? = try {
        val req = Request.Builder().url(BASE + path).header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 YouseifPlayer/25.2").build()
        client.newCall(req).execute().use { r -> if (!r.isSuccessful) null else r.body?.string() }
    } catch (_: Throwable) { null }

    private fun text(o: JSONObject, vararg keys: String): String = keys.firstNotNullOfOrNull { k ->
        o.optString(k).trim().takeIf { it.isNotBlank() && it != "null" }
    } ?: ""
    private fun source(o: JSONObject): Source? {
        val u = text(o, "streamUrl", "url", "link")
        return if (u.isBlank()) null else Source(text(o, "label").ifBlank { "سيرفر مشاهدة" }, u, text(o, "quality"), text(o, "format"))
    }
    private fun media(o: JSONObject, isSeries: Boolean): Media = Media(
        id = text(o, "id"), title = text(o, "titleAr", "title", "titleEn").ifBlank { "بدون عنوان" },
        poster = text(o, "posterUrl"), overview = text(o, "descriptionAr", "description"),
        year = o.optInt("year", 0), category = text(o.optJSONObject("category") ?: JSONObject(), "nameAr", "name"), isSeries = isSeries
    )

    suspend fun catalog(): Pair<List<Media>, List<Media>> = withContext(Dispatchers.IO) {
        val movies = parseList(get("/content/movies"), false)
        val series = parseList(get("/content/series"), true)
        movies to series
    }
    private fun parseList(raw: String?, isSeries: Boolean): List<Media> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = try {
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
        } catch (_: Throwable) { JSONArray() }
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o -> media(o, isSeries) } }
    }
    suspend fun seriesDetail(id: String): SeriesDetail? = withContext(Dispatchers.IO) {
        val root = try { JSONObject(get("/content/series/${enc(id)}") ?: return@withContext null) } catch (_: Throwable) { return@withContext null }
        val m = media(root, true)
        val eps = (0 until (root.optJSONArray("episodes")?.length() ?: 0)).mapNotNull { i ->
            val e = root.optJSONArray("episodes")!!.optJSONObject(i) ?: return@mapNotNull null
            val sources = (0 until (e.optJSONArray("sources")?.length() ?: 0)).mapNotNull { j -> source(e.optJSONArray("sources")!!.optJSONObject(j) ?: JSONObject()) }
            Episode(text(e, "id"), e.optInt("episodeNumber", i + 1), text(e, "titleAr", "title", "titleEn").ifBlank { "الحلقة ${i + 1}" }, e.optInt("seasonNumber", 1), sources)
        }
        SeriesDetail(m, eps)
    }
    suspend fun movieDetail(id: String): MovieDetail? = withContext(Dispatchers.IO) {
        val root = try { JSONObject(get("/content/movies/${enc(id)}") ?: return@withContext null) } catch (_: Throwable) { return@withContext null }
        val sources = (0 until (root.optJSONArray("sources")?.length() ?: 0)).mapNotNull { i -> source(root.optJSONArray("sources")!!.optJSONObject(i) ?: JSONObject()) }
        MovieDetail(media(root, false), sources)
    }
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
