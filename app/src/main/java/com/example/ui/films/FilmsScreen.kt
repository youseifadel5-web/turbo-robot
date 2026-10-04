package com.example.ui.films

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FaselGenre
import com.example.data.FaselMedia
import com.example.data.FaselNetwork
import com.example.data.FaselPage
import com.example.data.HikayeTvApi
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.TopManageAction
import com.example.ui.components.TechTag
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette
import com.example.ui.theme.glassGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 🎬 المكتبة الموحّدة — تجمع الأفلام + المسلسلات + الأنمي في شاشة واحدة
 * بـ 3 تبويبات رئيسية (movies · series · anime) كل واحد منهم فيه:
 *  • مصادر حقيقية (فاصل HD · حكاية TV · اليوم · محتواي)
 *  • تصنيفات حقيقية من FaselHdApi.genres() و HikayeTvApi.MOVIE_GENRES / SERIES_KEYWORDS
 *  • بحث موحّد بأيقونة تتمدد (debounce 450ms) + فلاتر نوع حقيقية
 */
private const val SECTION_MOVIES = "MOVIES"
private const val SECTION_SERIES = "SERIES"
private const val SECTION_ANIME = "ANIME"

private const val SRC_FASEL = "FASEL"
private const val SRC_HIKAYE = "HIKAYE"
private const val SRC_FIRE = "GOOGLEFIRE"

/** أرقام حقيقية منسّقة: 15000 → 15,000 */
private fun formatCount(n: Int): String =
    if (n < 1000) n.toString() else "%,d".format(java.util.Locale.US, n)
private const val SRC_CUSTOM = "CUSTOM"

@Composable
fun FilmsScreen(
    onBack: () -> Unit = {},
    networks: List<FaselNetwork>,
    genres: List<FaselGenre>,
    moviesPage: FaselPage?,
    seriesPage: FaselPage?,
    animesPage: FaselPage?,
    seriesGenrePage: FaselPage? = null,
    animesGenrePage: FaselPage? = null,
    networkPage: FaselPage?,
    genrePage: FaselPage? = null,
    searchResults: List<FaselMedia>?,
    currentNetwork: String?,
    channels: List<PlaylistItem> = emptyList(),
    customFilms: List<PlaylistItem> = emptyList(),
    countryItems: List<PlaylistItem> = emptyList(),
    hikayeMoviesPage: HikayeTvApi.HPage? = null,
    hikayeSeriesPage: HikayeTvApi.HPage? = null,
    hikayeSearchResults: List<HikayeTvApi.HMedia>? = null,
    gateways: List<com.example.data.GatewayEntry> = emptyList(),
    hideImages: Boolean = false,
    onHideImagesChange: (Boolean) -> Unit = {},
    onLoadMovies: (Int) -> Unit = {},
    onLoadSeries: (Int) -> Unit = {},
    onLoadAnimes: (Int) -> Unit = {},
    onLoadNetwork: (String, Int) -> Unit = { _, _ -> },
    onLoadGenre: (Int, Int) -> Unit = { _, _ -> },
    onLoadFaselKindGenre: (String, Int, Int) -> Unit = { _, _, _ -> },
    onLoadCountry: (Int) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onLoadHikayeMovies: (Int, String) -> Unit = { _, _ -> },
    onLoadHikayeSeries: (Int, String) -> Unit = { _, _ -> },
    onSearchHikaye: (String) -> Unit = {},
    onOpenMedia: (FaselMedia, String) -> Unit = { _, _ -> },
    onOpenHikayeMedia: (HikayeTvApi.HMedia) -> Unit = {},
    onOpenPlaylistItem: (PlaylistItem) -> Unit = {},
    onEnsurePortal: () -> Unit = {},
    onImportVod: (String, String, String) -> Unit = { _, _, _ -> },
    onDeletePlaylistItem: (PlaylistItem) -> Unit = {}
) {
    val p = activePlayerPalette()

    // ---- التبويب الرئيسي (أفلام / مسلسلات / أنمي) — منفصل عن المصدر ----
    // افتراضي: Fasel HD + تصنيف مغامرة (TMDB id 12) بدل «كل الأفلام»
    var section by remember { mutableStateOf(UserSettings.getString("library_section", SECTION_MOVIES)) }
    var sourceFolder by remember { mutableStateOf(UserSettings.getString("library_source", SRC_FASEL)) }
    var folder by remember {
        val saved = UserSettings.getString("library_folder", "G:12")
        // هجرة من الافتراضي القديم «MOVIES» → مغامرة
        mutableStateOf(if (saved == "MOVIES" || saved.isBlank()) "G:12" else saved)
    }
    var hikayeMovieMode by remember { mutableStateOf(UserSettings.getString("library_hikaye_movie_mode", "latest")) }
    var hikayeSeriesMode by remember { mutableStateOf(UserSettings.getString("library_hikaye_series_mode", "latest")) }
    var faselGenreId by remember { mutableStateOf(UserSettings.getInt("library_fasel_genre", 0)) }
    var fireFilter by remember { mutableStateOf("ALL") }

    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var showVodImport by remember { mutableStateOf(false) }
    var vodImportText by remember { mutableStateOf("") }
    var vodPackageName by remember { mutableStateOf("") }
    var hidePics by remember { mutableStateOf(hideImages) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var confirmDeleteCustom by remember { mutableStateOf(false) }
    var hiddenKeys by remember { mutableStateOf(setOf<String>()) }
    var vodFavTick by remember { mutableStateOf(0) }
    val vodFavs = remember(vodFavTick) { UserSettings.vodFavorites() }

    LaunchedEffect(section, sourceFolder, folder) {
        try {
            UserSettings.putString("library_section", section)
            UserSettings.putString("library_source", sourceFolder)
            UserSettings.putString("library_folder", folder)
        } catch (_: Throwable) {}
    }
    LaunchedEffect(hikayeMovieMode, hikayeSeriesMode, faselGenreId) {
        try {
            UserSettings.putString("library_hikaye_movie_mode", hikayeMovieMode)
            UserSettings.putString("library_hikaye_series_mode", hikayeSeriesMode)
            UserSettings.putInt("library_fasel_genre", faselGenreId)
        } catch (_: Throwable) {}
    }

    // ---- عند تغيير التبويب: reset الحالة + حمّل المصادر المناسبة ----
    LaunchedEffect(section) {
        when (section) {
            SECTION_MOVIES -> {
                // افتح على مغامرة (G:12) لو المجلد جاهز لذلك
                if (folder.startsWith("G:")) {
                    val gid = folder.removePrefix("G:").toIntOrNull() ?: 12
                    onLoadGenre(gid, 1)
                } else if (moviesPage == null) {
                    onLoadMovies(1)
                }
                if (hikayeMoviesPage == null) onLoadHikayeMovies(1, hikayeMovieMode)
            }
            SECTION_SERIES -> {
                if (seriesPage == null) onLoadSeries(1)
                if (hikayeSeriesPage == null) onLoadHikayeSeries(1, hikayeSeriesMode)
            }
            SECTION_ANIME -> {
                if (animesPage == null) onLoadAnimes(1)
                if (hikayeSeriesPage == null) onLoadHikayeSeries(1, "anime")
            }
        }
        onEnsurePortal()
    }
    // تحميل تصنيف المغامرة (أو أي G:) عند فتح المكتبة أول مرة
    LaunchedEffect(sourceFolder, folder) {
        if (sourceFolder == SRC_FASEL && section == SECTION_MOVIES && folder.startsWith("G:")) {
            val gid = folder.removePrefix("G:").toIntOrNull() ?: 12
            if (genrePage == null) onLoadGenre(gid, 1)
        }
    }

    // ---- بحث موحّد debounce 450ms — مرة واحدة لكل المصادر المفعّلة ----
    LaunchedEffect(query, section, sourceFolder) {
        val term = query.trim()
        if (term.isBlank()) {
            onSearch("")
            onSearchHikaye("")
            return@LaunchedEffect
        }
        delay(450)
        if (query.trim() != term) return@LaunchedEffect
        val isLocal = sourceFolder == SRC_FIRE || sourceFolder == SRC_CUSTOM
        if (!isLocal) {
            onSearch(term)
            onSearchHikaye(term)
        }
    }

    // ---- تصنيفات حقيقية حسب نوع المحتوى (من FaselHdApi.genres()) ----
    val movieGenreIds = remember(genres) {
        genres.filter { it.id in setOf(12, 14, 16, 18, 27, 28, 35, 36, 37, 53, 80, 99, 878, 9648, 10402, 10749, 10751, 10752, 10770) }
    }
    val seriesGenreIds = remember(genres) {
        genres.filter { it.id in setOf(18, 35, 80, 9648, 10749, 10751, 10759, 10762, 10763, 10764, 10765, 10766, 10767, 10768, 10770) }
    }
    val animeGenreIds = remember(genres) {
        genres.filter { it.id in setOf(16, 10759, 10765, 10784) }
    }

    // ---- عناصر Google Firebase (اليوم) ----
    // نستبعد: مجلدات التصنيف، عناصر بلا بوستر، أسماء = اسم الفولدر فقط
    fun isPlayableFireItem(item: PlaylistItem): Boolean {
        val u = item.url.trim().lowercase()
        if (u.isBlank()) return false
        val folderLabel = item.group.substringAfter(":", "").trim()
        val title = item.name.trim()
        // FIX: لو اسم العنصر = اسم الفئة، نعدّيه لو الـ URL يشير لسيرفر بث
        val isPlayable = u.startsWith("http") || u.startsWith("rtmp") || u.contains(".m3u8") ||
            u.contains(".mpd") || u.contains(".mp4") || u.contains(".mkv") || u.contains(".ts") ||
            u.startsWith("golive") || u.contains("alooytv")
        if (folderLabel.isNotBlank() && title.equals(folderLabel, ignoreCase = true) && !isPlayable) return false
        if (title.length <= 2) return false
        // FIX: البوستر الفاضي أصبح مقبول (Coil بيرجّع placeholder)
        val logo = item.logoUrl.trim().lowercase()
        if (logo.isNotBlank() && !(logo.startsWith("http") || logo.startsWith("content:") || logo.startsWith("data:image"))) return false
        if (u.contains("alooytv") || u.startsWith("golive://")) return true
        if (u.contains(".m3u8") || u.contains(".mpd") || u.contains(".mp4") ||
            u.contains(".mkv") || u.contains(".ts") || u.startsWith("rtmp") ||
            u.startsWith("rtsp") || u.contains("/playlist") || u.contains("manifest")
        ) return true
        return u.startsWith("http")
    }
    val fireItems = remember(customFilms) {
        customFilms.filter { item ->
            val g = item.group.uppercase()
            !item.id.startsWith("ch_") && !item.isLive &&
                (g.startsWith("FILMS:") || g.startsWith("SERIES:") || g.startsWith("CARTOONS:") ||
                    g.startsWith("ANIME:") || g.contains("FIREBASE") || g.contains("GOOGLEFIRE") ||
                    g.contains("SERIE") || g.contains("FILM") || g.contains("CARTOON") || g.contains("ANIME")) &&
                isPlayableFireItem(item)
        }.distinctBy { it.id }
    }
    val fireForSection = remember(fireItems, section) {
        when (section) {
            SECTION_MOVIES -> fireItems.filter {
                val g = it.group.uppercase()
                g.startsWith("FILMS:") || g.startsWith("FILM") ||
                    (!g.startsWith("SERIES:") && !g.contains("SERIE") && !g.contains("CARTOON") && !g.contains("ANIME"))
            }
            SECTION_SERIES -> fireItems.filter {
                val g = it.group.uppercase()
                g.startsWith("SERIES:") || g.contains("SERIE")
            }
            else -> fireItems.filter {
                val g = it.group.uppercase()
                g.startsWith("CARTOONS:") || g.startsWith("ANIME:") || g.contains("CARTOON") || g.contains("ANIME")
            }
        }
    }
    // تصنيفات حقيقية من السورس (زي فاصل/حكاية) مش كروت في الشبكة
    val fireCategoryChips = remember(fireForSection) {
        val cats = fireForSection
            .map { it.group.substringAfter(":", "").trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { (label, n) -> label to n }
        listOf("ALL" to fireForSection.size) + cats
    }
    val filteredFireItems = remember(fireForSection, fireFilter) {
        if (fireFilter == "ALL") fireForSection
        else fireForSection.filter {
            it.group.substringAfter(":", "").trim().equals(fireFilter, ignoreCase = true)
        }
    }
    // محتواي = VOD مخصص فقط — مش قنوات لايف المستوردة
    val myItems = remember(channels, customFilms) {
        val fromChannels = channels.filter { it.isCustom && !it.isLive }
        val fromCustom = customFilms.filter { it.isCustom && !it.isLive && fireItems.none { f -> f.id == it.id } }
        (fromChannels + fromCustom).distinctBy { it.id }
    }
    val myForSection = remember(myItems, section) {
        when (section) {
            SECTION_MOVIES -> myItems.filter {
                val g = it.group.uppercase()
                !g.startsWith("LIVE:") && !g.contains("SERIE") && !g.contains("ANIME") && !g.contains("CARTOON") &&
                    (g.startsWith("FILMS:") || g.startsWith("FILM") || (!g.startsWith("SERIES:") && !g.startsWith("ANIME:")))
            }
            SECTION_SERIES -> myItems.filter {
                val g = it.group.uppercase()
                g.contains("SERIE") && !g.startsWith("LIVE:")
            }
            else -> myItems.filter {
                val g = it.group.uppercase()
                !g.startsWith("LIVE:") && (g.contains("ANIME") || g.contains("CARTOON") || g.contains("كرتون"))
            }
        }
    }

    // ---- المصادر حسب القسم: الأنمي = فاصل (+محتواي) فقط ----
    val sourceFolders = remember(gateways, fireForSection.size, myForSection.size, section) {
        val fireOn = gateways.firstOrNull { it.id == "googlefire" }?.enabled ?: true
        val hikayeOn = gateways.firstOrNull { it.id == "hikaye" }?.enabled ?: true
        val customOn = gateways.firstOrNull { it.id == "custom" }?.enabled ?: true
        buildList {
            add(Triple(SRC_FASEL, "Fasel HD", "faselhd"))
            if (section != SECTION_ANIME) {
                if (hikayeOn) add(Triple(SRC_HIKAYE, "Hikaye TV", "hikaye"))
                if (fireOn) add(Triple(SRC_FIRE, "Prime", "googlefire"))
            }
            if (customOn && myForSection.isNotEmpty()) add(Triple(SRC_CUSTOM, "محتواي", "custom"))
        }
    }
    // لو القسم أنمي والمصدر مش فاصل → رجّع لفاصل
    LaunchedEffect(section, sourceFolder) {
        if (section == SECTION_ANIME && sourceFolder != SRC_FASEL && sourceFolder != SRC_CUSTOM) {
            sourceFolder = SRC_FASEL
            onLoadAnimes(1)
        }
    }

    // ---- أنماط التصفح داخل كل مصدر ----
    val faselMovieFilters = remember(movieGenreIds, networks) {
        val genres = movieGenreIds.map { "G:${it.id}" to "🎭 ${it.name}" }
        val realNets = networks.filter { it.type != "country" }.map { "NET:${it.id}" to "🌐 ${it.name}" }
        val countries = networks.filter { it.type == "country" }.map { "CTY:${it.id}" to "🌍 ${it.name}" }
        genres + realNets + countries
    }
    val faselSeriesFilters = remember(seriesGenreIds) { seriesGenreIds.map { "G:${it.id}" to "🎭 ${it.name}" } }
    val faselAnimeFilters = remember(animeGenreIds) { animeGenreIds.map { "G:${it.id}" to "🎭 ${it.name}" } }

    // اتبدّلت بـ fireCategoryChips من السورس مباشرة

    val hikayeMovieFilters = remember {
        listOf("latest" to "🆕 أحدث", "popular" to "🔥 رائج", "top_rated" to "⭐ الأعلى تقييم",
            "arabic" to "🇸🇦 عربي", "foreign" to "🌍 أجنبي",
            "genre_action" to "أكشن", "genre_comedy" to "كوميدي", "genre_drama" to "دراما",
            "genre_horror" to "رعب", "genre_romance" to "رومانسي", "genre_thriller" to "إثارة")
    }
    val hikayeSeriesFilters = remember {
        listOf("latest" to "🆕 أحدث", "arabic" to "📺 عربي", "foreign" to "🌍 أجنبي",
            "turkish" to "🇹🇷 تركي", "korean" to "🇰🇷 كوري",
            "anime" to "🎌 أنمي", "dubbed" to "🔊 مدبلج", "ramadan" to "🌙 رمضان")
    }

    // ---- عناصر فاصل حسب الـ section + التصنيف المختار ----
    val faselSectionPage: FaselPage? = when {
        sourceFolder != SRC_FASEL -> null
        searchingOrOpen(query).isNotBlank() -> null
        section == SECTION_MOVIES -> when {
            folder.startsWith("G:") -> genrePage
            folder.startsWith("NET:") -> networkPage
            folder.startsWith("CTY:") -> null
            else -> moviesPage
        }
        section == SECTION_SERIES -> when {
            faselGenreId > 0 -> seriesGenrePage
            else -> seriesPage
        }
        section == SECTION_ANIME -> when {
            faselGenreId > 0 -> animesGenrePage
            else -> animesPage
        }
        else -> null
    }
    val faselItems: List<FaselMedia> = faselSectionPage?.items.orEmpty()
    // عدّاد حقيقي فقط — من total السيرفر أو عدد الصفحة الحالية (مفيش تخمين وهمي)
    val faselTotal: Int = faselSectionPage?.total?.takeIf { it > 0 } ?: faselItems.size
    // عدّادات الشريط ثابتة من صفحات كل مصدر (مش بتصفر لما تغيّر المصدر)
    val faselBadgeTotal: Int = when (section) {
        SECTION_MOVIES -> moviesPage?.total?.takeIf { it > 0 } ?: moviesPage?.items?.size ?: faselTotal
        SECTION_SERIES -> seriesPage?.total?.takeIf { it > 0 } ?: seriesPage?.items?.size ?: faselTotal
        SECTION_ANIME -> animesPage?.total?.takeIf { it > 0 } ?: animesPage?.items?.size ?: faselTotal
        else -> faselTotal
    }
    val hikayeBadgeTotal: Int = when (section) {
        SECTION_MOVIES -> hikayeMoviesPage?.total?.takeIf { it > 0 } ?: hikayeMoviesPage?.items?.size ?: 0
        else -> hikayeSeriesPage?.total?.takeIf { it > 0 } ?: hikayeSeriesPage?.items?.size ?: 0
    }
    val faselCurrentPage: Int = faselSectionPage?.page ?: 1
    val faselLastPage: Int = (faselSectionPage?.lastPage ?: 1).coerceAtLeast(1)

    // ---- عناصر حكاية TV حسب الـ section ----
    val hikayeActive = sourceFolder == SRC_HIKAYE
    val hikayeItems: List<HikayeTvApi.HMedia> = when {
        !hikayeActive -> emptyList()
        section == SECTION_MOVIES -> hikayeMoviesPage?.items.orEmpty()
        section == SECTION_SERIES -> hikayeSeriesPage?.items.orEmpty().filter { it.kind.equals("series", true) && !it.genreAr.contains("انمي", true) }
        else -> hikayeSeriesPage?.items.orEmpty().filter { it.genreAr.contains("انمي", true) || it.kind.equals("anime", true) }
    }
    // عدّاد حقيقي من الـ API (مش items.size + total اللي كان بيضاعف الرقم)
    val hikayeTotal: Int = when {
        !hikayeActive -> 0
        section == SECTION_MOVIES -> hikayeMoviesPage?.total?.takeIf { it > 0 } ?: hikayeItems.size
        section == SECTION_SERIES -> hikayeSeriesPage?.total?.takeIf { it > 0 } ?: hikayeItems.size
        else -> hikayeSeriesPage?.total?.takeIf { it > 0 } ?: hikayeItems.size
    }
    val hikayeCurrentPage: Int = when {
        !hikayeActive -> 1
        section == SECTION_MOVIES -> hikayeMoviesPage?.page ?: 1
        else -> hikayeSeriesPage?.page ?: 1
    }
    val hikayeLastPage: Int = when {
        !hikayeActive -> 1
        section == SECTION_MOVIES -> (hikayeMoviesPage?.lastPage ?: 1).coerceAtLeast(1)
        else -> (hikayeSeriesPage?.lastPage ?: 1).coerceAtLeast(1)
    }

    // ---- البحث الموحد ----
    val localQuery = query.trim()
    val searching = localQuery.isNotBlank()

    fun faselKindGenreIds(): List<FaselGenre> = when (section) {
        SECTION_MOVIES -> movieGenreIds
        SECTION_SERIES -> seriesGenreIds
        else -> animeGenreIds
    }

    fun playlistMatches(item: PlaylistItem, q: String): Boolean =
        q.isBlank() || item.name.contains(q, true) || item.group.contains(q, true)

    // ---- عناصر البحث (موحد على كل المصادر المفعّلة) ----
    val searchFaselEntries: List<FaselMedia> = when {
        !searching || sourceFolder == SRC_FIRE || sourceFolder == SRC_CUSTOM -> emptyList()
        section == SECTION_MOVIES -> searchResults.orEmpty().filter { it.type.contains("movie", true) }
        section == SECTION_SERIES -> searchResults.orEmpty().filter { it.type.contains("serie", true) }
        else -> searchResults.orEmpty().filter { it.type.contains("anime", true) }
    }
    val searchHikayeEntries: List<HikayeTvApi.HMedia> = when {
        !searching || sourceFolder == SRC_FIRE || sourceFolder == SRC_CUSTOM -> emptyList()
        section == SECTION_MOVIES -> hikayeSearchResults.orEmpty().filter { it.kind.equals("movie", true) }
        section == SECTION_SERIES -> hikayeSearchResults.orEmpty().filter { it.kind.equals("series", true) && !it.genreAr.contains("انمي", true) }
        else -> hikayeSearchResults.orEmpty().filter { it.genreAr.contains("انمي", true) || it.kind.equals("anime", true) }
    }
    val searchLibEntries: List<PlaylistItem> = when {
        !searching -> emptyList()
        sourceFolder == SRC_FIRE -> filteredFireItems.filter { playlistMatches(it, localQuery) }
        sourceFolder == SRC_CUSTOM -> myForSection.filter { playlistMatches(it, localQuery) && "p:${it.id}" !in hiddenKeys }
        else -> (fireForSection + myForSection).filter { playlistMatches(it, localQuery) }
    }

    // ---- عناصر العرض النهائية (حسب المصدر + البحث أو التصفح العادي) ----
    data class LibEntry(val kind: String, val fasel: FaselMedia? = null, val hikaye: HikayeTvApi.HMedia? = null, val item: PlaylistItem? = null)

    val entriesRaw: List<LibEntry> = when {
        searching -> (searchFaselEntries.map { LibEntry("f", fasel = it) } +
                searchHikayeEntries.map { LibEntry("h", hikaye = it) } +
                searchLibEntries.map { LibEntry("p", item = it) })
        sourceFolder == SRC_FIRE -> filteredFireItems.map { LibEntry("p", item = it) }
        sourceFolder == SRC_CUSTOM -> myForSection.map { LibEntry("p", item = it) }
        sourceFolder == SRC_HIKAYE -> hikayeItems.map { LibEntry("h", hikaye = it) }
        else -> {
            val base = if (section == SECTION_MOVIES && folder.startsWith("CTY:")) {
                countryItems.map { LibEntry("p", item = it) }
            } else faselItems.map { LibEntry("f", fasel = it) }
            base
        }
    }
    // صفحة مفضلة خاصة بالقسم الحالي (أفلام/مسلسلات/أنمي)
    val entries: List<LibEntry> = if (!showFavoritesOnly) entriesRaw else {
        entriesRaw.filter { e ->
            when {
                e.fasel != null -> "f:${e.fasel.id}" in vodFavs
                e.hikaye != null -> "h:${e.hikaye.id}" in vodFavs
                e.item != null -> e.item.isFavorite || "p:${e.item.id}" in vodFavs
                else -> false
            }
        }
    }

    // ---- BackHandler: سلوك ذكي يناسب الشاشة الموحدة الجديدة ----
    BackHandler {
        when {
            query.isNotBlank() -> query = ""
            searchOpen -> searchOpen = false
            faselGenreId > 0 -> faselGenreId = 0
            folder != "MOVIES" && section == SECTION_MOVIES -> folder = "MOVIES"
            sourceFolder != SRC_FASEL -> sourceFolder = SRC_FASEL
            section != SECTION_MOVIES -> section = SECTION_MOVIES
            else -> onBack()
        }
    }

    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = UserSettings.getInt("library_scroll_index_$section", 0).coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = UserSettings.getInt("library_scroll_offset_$section", 0).coerceAtLeast(0)
    )
    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        UserSettings.putInt("library_scroll_index_$section", gridState.firstVisibleItemIndex)
        UserSettings.putInt("library_scroll_offset_$section", gridState.firstVisibleItemScrollOffset)
    }

    Column(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        PlayerTopBar(
            onBack = {
                if (showFavoritesOnly) showFavoritesOnly = false else onBack()
            },
            onMenu = null, onRefresh = null, onInfo = null, onDownloads = null,
            onFavorites = { showFavoritesOnly = !showFavoritesOnly },
            favoritesActive = showFavoritesOnly,
            showThemeButton = false,
            manageActions = listOf(
                TopManageAction(if (selectionMode) "إلغاء التحديد" else "وضع التحديد") {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedKeys = emptySet()
                },
                TopManageAction("إخفاء المحدد") {
                    hiddenKeys = hiddenKeys + selectedKeys
                    selectedKeys = emptySet()
                    selectionMode = false
                },
                TopManageAction("حذف محتواي المستورد", destructive = true) {
                    confirmDeleteCustom = true
                },
                TopManageAction("مسح الإخفاء") { hiddenKeys = emptySet() }
            ),
            slot1 = null, slot2 = null, slot3 = null, showQuickSlots = false
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(2.dp))

            // ============================================================
            // 1) التبويبات الرئيسية: 🎬 أفلام · 📺 مسلسلات · 🎌 أنمي
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface.copy(alpha = .9f))
                        .border(1.dp, CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(14.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        SECTION_MOVIES to ("🎬" to "أفلام"),
                        SECTION_SERIES to ("📺" to "مسلسلات"),
                        SECTION_ANIME to ("🎌" to "أنمي")
                    ).forEach { (k, iconLabel) ->
                        val (icon, label) = iconLabel
                        val sel = section == k
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) NeonRedContainer else Color.Transparent)
                                .clickable {
                                    if (section != k) {
                                        section = k
                                        // reset filters لما يغير القسم
                                        query = ""
                                        searchOpen = false
                                        faselGenreId = 0
                                        when (k) {
                                            SECTION_MOVIES -> { folder = "MOVIES"; onLoadMovies(1); onLoadHikayeMovies(1, hikayeMovieMode) }
                                            SECTION_SERIES -> { onLoadSeries(1); onLoadHikayeSeries(1, hikayeSeriesMode) }
                                            SECTION_ANIME -> { onLoadAnimes(1); onLoadHikayeSeries(1, "anime") }
                                        }
                                    }
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$icon $label", color = if (sel) TextPrimary else TextSecondary,
                                fontSize = 11.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                // أيقونة بحث — تتمدد لحقل كتابة (debounce 450ms)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (searchOpen) NeonRedContainer else DarkSurface.copy(alpha = .8f))
                        .border(1.dp, if (searchOpen) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                        .clickable {
                            searchOpen = !searchOpen
                            if (!searchOpen && query.isNotBlank()) query = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null, tint = if (searchOpen) NeonRedGlow else TextSecondary, modifier = Modifier.size(17.dp))
                }

                // زر صور
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hidePics) NeonRedContainer.copy(alpha = .7f) else DarkSurface.copy(alpha = .8f))
                        .border(1.dp, if (hidePics) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                        .clickable { hidePics = !hidePics; onHideImagesChange(hidePics) }
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                ) {
                    Text(if (hidePics) "صور🔒" else "صور", color = if (hidePics) NeonRedGlow else TextSecondary,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ============================================================
            // 2) حقل البحث المتمدد
            // ============================================================
            AnimatedVisibility(visible = searchOpen) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                when (section) {
                                    SECTION_MOVIES -> "Search Fasel HD · Hikaye TV · Library..."
                                    SECTION_SERIES -> "Search Fasel HD · Hikaye TV · Library..."
                                    else -> "Search Fasel HD · Hikaye TV · Library..."
                                },
                                color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = p.accentGlow, modifier = Modifier.size(17.dp)) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                Icon(Icons.Default.Close, "مسح", tint = TextMuted,
                                    modifier = Modifier.size(17.dp).clickable { query = "" })
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(13.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface.copy(alpha = .85f),
                            unfocusedContainerColor = DarkSurface.copy(alpha = .85f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = p.accentGlow,
                            unfocusedBorderColor = CrimsonBorder.copy(alpha = .7f),
                            cursorColor = p.accentGlow
                        )
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            // ============================================================
            // 3) المصادر: فاصل · حكاية · جوجل فاير · محتواي (مع badge للعدد)
            // ============================================================
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(sourceFolders) { (key, label, _gw) ->
                    val sel = sourceFolder == key
                    val liveCount = when (key) {
                        SRC_FASEL -> faselBadgeTotal
                        SRC_HIKAYE -> hikayeBadgeTotal
                        SRC_FIRE -> fireForSection.size
                        SRC_CUSTOM -> myForSection.size
                        else -> 0
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) NeonRedContainer else DarkSurface.copy(alpha = .78f))
                            .border(1.dp, if (sel) NeonRed else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                            .clickable {
                                if (sourceFolder != key) {
                                    sourceFolder = key
                                    fireFilter = "ALL"
                                    when {
                                        key == SRC_FASEL -> when (section) {
                                            SECTION_MOVIES -> onLoadMovies(1)
                                            SECTION_SERIES -> onLoadSeries(1)
                                            else -> onLoadAnimes(1)
                                        }
                                        key == SRC_HIKAYE -> when (section) {
                                            SECTION_MOVIES -> onLoadHikayeMovies(1, hikayeMovieMode)
                                            SECTION_SERIES -> onLoadHikayeSeries(1, hikayeSeriesMode)
                                            else -> onLoadHikayeSeries(1, "anime")
                                        }
                                        else -> onEnsurePortal()
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(label, color = if (sel) TextPrimary else TextSecondary,
                                fontSize = 10.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                            Text(formatCount(liveCount), color = if (sel) NeonRedGlow else TextMuted,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item(key = "import_plus") {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .55f))))
                            .clickable { showVodImport = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "استيراد", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ============================================================
            // 4) فلاتر المصدر الداخلية الحقيقية
            // ============================================================
            if (!searching) {
                // -- اليوم: تصنيفات من السورس (شيبات زي فاصل/حكاية) — مش كروت حروف
                if (sourceFolder == SRC_FIRE && (section == SECTION_MOVIES || section == SECTION_SERIES)) {
                    if (fireCategoryChips.size > 1) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(fireCategoryChips) { (key, count) ->
                                val sel = fireFilter == key
                                val label = if (key == "ALL") "🔥 الكل" else key
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) NeonRedContainer.copy(alpha = .9f) else DarkSurface.copy(alpha = .7f))
                                        .border(1.dp, if (sel) NeonRedGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                        .clickable { fireFilter = key }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "$label · ${formatCount(count)}",
                                        color = if (sel) TextPrimary else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // -- حكاية TV: أوضاع حقيقية (latest/arabic/foreign/turkish/anime/...) --
                if (sourceFolder == SRC_HIKAYE) {
                    Spacer(Modifier.height(4.dp))
                    val activeFilters = if (section == SECTION_MOVIES) hikayeMovieFilters else hikayeSeriesFilters
                    val activeMode = if (section == SECTION_MOVIES) hikayeMovieMode else hikayeSeriesMode
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(activeFilters) { (key, label) ->
                            val sel = activeMode == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) NeonRedContainer.copy(alpha = .95f) else DarkSurface.copy(alpha = .74f))
                                    .border(1.dp, if (sel) NeonRedGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (section == SECTION_MOVIES) {
                                            hikayeMovieMode = key
                                            onLoadHikayeMovies(1, key)
                                        } else {
                                            hikayeSeriesMode = key
                                            onLoadHikayeSeries(1, key)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (sel) TextPrimary else TextSecondary,
                                    fontSize = 10.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // -- فاصل HD: تصنيفات حقيقية من السورس (genress / networks / countries)
                if (sourceFolder == SRC_FASEL) {
                    val faselFilters = when (section) {
                        SECTION_MOVIES -> faselMovieFilters
                        SECTION_SERIES -> faselSeriesFilters
                        else -> faselAnimeFilters
                    }
                    if (faselFilters.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(faselFilters) { (key, label) ->
                                val sel = if (section == SECTION_MOVIES) folder == key else faselGenreId.toString() == key.removePrefix("G:")
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (sel) TechCyanGlow.copy(alpha = .30f) else DarkSurface.copy(alpha = .72f))
                                        .border(1.dp, if (sel) TechCyanGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (section == SECTION_MOVIES) {
                                                folder = key
                                                when {
                                                    key.startsWith("NET:") -> onLoadNetwork(key.removePrefix("NET:"), 1)
                                                    key.startsWith("CTY:") -> onLoadCountry(key.removePrefix("CTY:").toIntOrNull() ?: 0)
                                                    key.startsWith("G:") -> onLoadGenre(key.removePrefix("G:").toIntOrNull() ?: 0, 1)
                                                }
                                            } else {
                                                val gid = key.removePrefix("G:").toIntOrNull() ?: 0
                                                faselGenreId = if (sel) 0 else gid
                                                onLoadFaselKindGenre(
                                                    if (section == SECTION_SERIES) "SERIES" else "ANIME",
                                                    gid, 1
                                                )
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(label, color = if (sel) TextPrimary else TextSecondary,
                                        fontSize = 10.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ============================================================
            // 5) سطر العداد + حالة الصفحة (لون سماوي accent للاسم)
            // ============================================================
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val sectionLabel = when (section) {
                    SECTION_MOVIES -> "كل الأفلام"
                    SECTION_SERIES -> "كل المسلسلات"
                    else -> "كل الأنمي"
                }
                val sourceLabel = when (sourceFolder) {
                    SRC_HIKAYE -> "Hikaye TV"
                    SRC_FIRE -> "Prime"
                    SRC_CUSTOM -> "محتواي"
                    else -> "Fasel HD"
                }
                Text(
                    when {
                        searching -> "نتائج البحث"
                        else -> "$sectionLabel · $sourceLabel"
                    },
                    color = TechCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("${formatCount(entries.size)} عمل", color = TextMuted, fontSize = 10.sp)
                if (!searching && sourceFolder == SRC_FASEL && faselLastPage > 1) {
                    Text("· صفحة ${faselCurrentPage}/${faselLastPage}", color = TextMuted, fontSize = 9.sp)
                }
                if (!searching && sourceFolder == SRC_HIKAYE && hikayeLastPage > 1) {
                    Text("· صفحة ${hikayeCurrentPage}/${hikayeLastPage}", color = TextMuted, fontSize = 9.sp)
                }
            }

            Spacer(Modifier.height(5.dp))

            // ============================================================
            // 6) الـ Grid الموحّد بنفس الكارد ستايل لكل المصادر
            // ============================================================
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(NeonRedContainer), contentAlignment = Alignment.Center) {
                            val ico = if (section == SECTION_MOVIES) Icons.Default.Movie else Icons.Default.Tv
                            Icon(ico, null, tint = NeonRedGlow, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            when {
                                searching -> "مفيش نتايج للبحث «$localQuery»"
                                else -> "جارٍ التحميل من المصادر..."
                            },
                            color = TextMuted, fontSize = 12.sp
                        )
                    }
                }
            } else {
                // ---- تنقل صفحات صريح (مش تحميل تلقائي) ----
                val canPage = !searching && (
                    (sourceFolder == SRC_FASEL && faselLastPage > 1) ||
                    (sourceFolder == SRC_HIKAYE && hikayeLastPage > 1)
                )
                if (canPage) {
                    val cur = if (sourceFolder == SRC_HIKAYE) hikayeCurrentPage else faselCurrentPage
                    val last = if (sourceFolder == SRC_HIKAYE) hikayeLastPage else faselLastPage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (cur > 1) NeonRedContainer else DarkSurface.copy(alpha = .6f))
                                .border(1.dp, CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(10.dp))
                                .clickable(enabled = cur > 1) {
                                    val p = (cur - 1).coerceAtLeast(1)
                                    when {
                                        sourceFolder == SRC_HIKAYE && section == SECTION_MOVIES -> onLoadHikayeMovies(p, hikayeMovieMode)
                                        sourceFolder == SRC_HIKAYE -> onLoadHikayeSeries(p, if (section == SECTION_ANIME) "anime" else hikayeSeriesMode)
                                        section == SECTION_MOVIES && folder == "MOVIES" -> onLoadMovies(p)
                                        section == SECTION_MOVIES && folder.startsWith("NET:") -> onLoadNetwork(folder.removePrefix("NET:"), p)
                                        section == SECTION_MOVIES && folder.startsWith("G:") -> onLoadGenre(folder.removePrefix("G:").toIntOrNull() ?: 0, p)
                                        section == SECTION_SERIES -> onLoadSeries(p)
                                        section == SECTION_ANIME -> onLoadAnimes(p)
                                        faselGenreId > 0 -> onLoadFaselKindGenre(
                                            when (section) { SECTION_SERIES -> "SERIES"; SECTION_ANIME -> "ANIME"; else -> "MOVIES" },
                                            faselGenreId, p
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text("السابق", color = if (cur > 1) TextPrimary else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                        Text("صفحة $cur / $last", color = TechCyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Black)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (cur < last) NeonRedContainer else DarkSurface.copy(alpha = .6f))
                                .border(1.dp, CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(10.dp))
                                .clickable(enabled = cur < last) {
                                    val p = (cur + 1).coerceAtMost(last)
                                    when {
                                        sourceFolder == SRC_HIKAYE && section == SECTION_MOVIES -> onLoadHikayeMovies(p, hikayeMovieMode)
                                        sourceFolder == SRC_HIKAYE -> onLoadHikayeSeries(p, if (section == SECTION_ANIME) "anime" else hikayeSeriesMode)
                                        section == SECTION_MOVIES && folder == "MOVIES" -> onLoadMovies(p)
                                        section == SECTION_MOVIES && folder.startsWith("NET:") -> onLoadNetwork(folder.removePrefix("NET:"), p)
                                        section == SECTION_MOVIES && folder.startsWith("G:") -> onLoadGenre(folder.removePrefix("G:").toIntOrNull() ?: 0, p)
                                        section == SECTION_SERIES -> onLoadSeries(p)
                                        section == SECTION_ANIME -> onLoadAnimes(p)
                                        faselGenreId > 0 -> onLoadFaselKindGenre(
                                            when (section) { SECTION_SERIES -> "SERIES"; SECTION_ANIME -> "ANIME"; else -> "MOVIES" },
                                            faselGenreId, p
                                        )
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) { Text("التالي", color = if (cur < last) TextPrimary else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp)
                ) {
                    gridItems(entries, key = { e ->
                        when (e.kind) {
                            "f" -> "f_${e.fasel?.id ?: entries.indexOf(e)}"
                            "h" -> "h_${e.hikaye?.id ?: entries.indexOf(e)}"
                            else -> "p_${e.item?.id ?: entries.indexOf(e)}"
                        }
                    }) { entry ->
                        when (entry.kind) {
                            "f" -> {
                                val key = "f:${entry.fasel!!.id}"
                                PosterCard(
                                    m = entry.fasel!!,
                                    hidePics = hidePics,
                                    badge = sourceLabelFor(SRC_FASEL),
                                    isFavorite = key in vodFavs,
                                    onToggleFavorite = {
                                        UserSettings.toggleVodFavorite(key)
                                        vodFavTick++
                                    }
                                ) { onOpenMedia(entry.fasel, section.lowercase()) }
                            }
                            "h" -> {
                                val key = "h:${entry.hikaye!!.id}"
                                HikayePosterCard(
                                    m = entry.hikaye!!,
                                    hidePics = hidePics,
                                    badge = sourceLabelFor(SRC_HIKAYE),
                                    isFavorite = key in vodFavs,
                                    onToggleFavorite = {
                                        UserSettings.toggleVodFavorite(key)
                                        vodFavTick++
                                    }
                                ) { onOpenHikayeMedia(entry.hikaye) }
                            }
                            else -> {
                                val key = "p:${entry.item!!.id}"
                                PlaylistPosterCard(
                                    ch = entry.item!!,
                                    hidePics = hidePics,
                                    badge = sourceLabelFor(sourceFolder),
                                    isFavorite = entry.item!!.isFavorite || key in vodFavs,
                                    onToggleFavorite = {
                                        UserSettings.toggleVodFavorite(key)
                                        vodFavTick++
                                    }
                                ) { onOpenPlaylistItem(entry.item) }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- dialogs: confirm delete custom / vod import ----
    if (confirmDeleteCustom) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCustom = false },
            title = { Text("حذف محتواي؟", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هيتمسح كل العناصر المستوردة في محتواي لهذا الجهاز. فاصل وحكاية مش هيتأثروا.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        myItems.forEach { onDeletePlaylistItem(it) }
                        confirmDeleteCustom = false
                        selectedKeys = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteCustom = false }) { Text("إلغاء", color = TextSecondary) } },
            containerColor = DarkSurface
        )
    }
if (showVodImport) {
        AlertDialog(
            onDismissRequest = { showVodImport = false },
            title = {
                Text(
                    when (section) {
                        SECTION_SERIES -> "استيراد مسلسلات"
                        SECTION_ANIME -> "استيراد أنمي"
                        else -> "استيراد أفلام"
                    },
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("استيراد لمكتبة القسم الحالي فقط (مش قنوات لايف).", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = vodPackageName,
                        onValueChange = { vodPackageName = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        placeholder = { Text("اسم الباقة (اختياري)", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = p.accentGlow, unfocusedBorderColor = CrimsonBorder
                        )
                    )
                    OutlinedTextField(
                        value = vodImportText,
                        onValueChange = { vodImportText = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("https://...m3u أو محتوى القائمة", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = p.accentGlow, unfocusedBorderColor = CrimsonBorder
                        )
                    )
                    val clipMgr = LocalClipboardManager.current
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, p.accentGlow.copy(alpha = .5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val c = clipMgr.getText()?.text?.trim().orEmpty()
                                    if (c.isNotBlank()) vodImportText = c
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📋 لصق من الحافظة", color = p.accentGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = vodImportText.trim()
                        if (text.isNotBlank()) {
                            onImportVod(
                                text,
                                when (section) {
                                    SECTION_SERIES -> "SERIES"
                                    SECTION_ANIME -> "ANIME"
                                    else -> "FILMS"
                                },
                                vodPackageName.trim()
                            )
                            showVodImport = false
                            vodImportText = ""
                            vodPackageName = ""
                            sourceFolder = SRC_CUSTOM
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("استيراد", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showVodImport = false }) { Text("إلغاء", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

}

private fun sourceLabelFor(source: String): String = when (source) {
    SRC_HIKAYE -> "Hikaye TV"
    SRC_FIRE -> "Prime"
    SRC_CUSTOM -> "محتواي"
    else -> "Fasel HD"
}

// ============================================================
// 🎨 Poster Cards (موحّدة بنفس الشكل لكل المصادر — match series card)
// ============================================================


    
@Composable
private fun PosterCard(
    m: FaselMedia,
    hidePics: Boolean,
    badge: String,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val p = activePlayerPalette()
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(DarkCardBg.copy(alpha = .9f))
            .border(1.dp, CrimsonBorder.copy(alpha = .55f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)).background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (!hidePics && m.poster.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(m.poster).crossfade(true).build(),
                    contentDescription = m.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(m.title.take(1).uppercase(), color = p.accentGlow, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(p.accentContainer.copy(alpha = .88f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(badge, color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(DarkSurface.copy(alpha = .72f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) NeonRedGlow else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            m.title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
            if (m.vote > 0) {
                Icon(Icons.Default.Star, null, tint = TechCyanGlow, modifier = Modifier.size(10.dp))
                Text("%.1f".format(m.vote), color = TechCyanGlow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (m.subtitle.isNotBlank() && !m.subtitle.equals("null", true)) {
                TechTag(text = m.subtitle.take(14), color = NeonRedGlow)
            }
        }
    }
}

@Composable
private fun HikayePosterCard(m: HikayeTvApi.HMedia, hidePics: Boolean, badge: String, isFavorite: Boolean = false, onToggleFavorite: (() -> Unit)? = null, onClick: () -> Unit) {
    val p = activePlayerPalette()
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(DarkCardBg.copy(alpha = .9f))
            .border(1.dp, CrimsonBorder.copy(alpha = .55f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)).background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (!hidePics && m.poster.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(m.poster).crossfade(true).build(),
                    contentDescription = m.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(m.displayTitle.take(1).uppercase(), color = p.accentGlow, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(p.accentContainer.copy(alpha = .88f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(badge, color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(DarkSurface.copy(alpha = .72f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) NeonRedGlow else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            m.displayTitle, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
            if (m.rating > 0) {
                Icon(Icons.Default.Star, null, tint = TechCyanGlow, modifier = Modifier.size(10.dp))
                Text("%.1f".format(m.rating), color = TechCyanGlow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (m.year > 0) TechTag(text = "${m.year}", color = NeonRedGlow)
        }
    }
}

@Composable
private fun PlaylistPosterCard(ch: PlaylistItem, hidePics: Boolean, badge: String, isFavorite: Boolean = false, onToggleFavorite: (() -> Unit)? = null, onClick: () -> Unit) {
    val p = activePlayerPalette()
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(DarkCardBg.copy(alpha = .9f))
            .border(1.dp, CrimsonBorder.copy(alpha = .55f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)).background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (!hidePics && ch.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(ch.logoUrl).crossfade(true).build(),
                    contentDescription = ch.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(ch.name.take(1).uppercase(), color = p.accentGlow, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(p.accentContainer.copy(alpha = .88f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(badge, color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(DarkSurface.copy(alpha = .72f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isFavorite) NeonRedGlow else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            ch.name, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2
        )
        val cat = ch.group.substringAfter(":", "").trim().ifBlank { "" }
        if (cat.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                TechTag(text = cat.take(16), color = TechCyanGlow)
            }
        }
    }
}

// helper يكتشف الـ searching من query بدون throw
private fun searchingOrOpen(query: String): String = query.trim()
