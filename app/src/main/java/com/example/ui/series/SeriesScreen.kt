package com.example.ui.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FaselGenre
import com.example.data.FaselMedia
import com.example.data.FaselPage
import com.example.data.GatewayEntry
import com.example.data.HikayeTvApi
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.ui.components.PlayerTopBar
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private const val SRC_ALL = "ALL"
private const val SRC_FASEL = "FASEL"
private const val SRC_HIKAYE = "HIKAYE"
private const val SRC_FIRE = "FIRE"
private const val SRC_CUSTOM = "CUSTOM"

/** النوع الرئيسي: المسلسلات أو الأنمي — تبويبان فوق Some بدل خلط الأنواع. */
private const val KIND_SERIES = "SERIES"
private const val KIND_ANIME = "ANIME"

/** عنصر مسلسل/أنمي موحّد — يوحّد فاصل + حكاية TV + جوجل فاير + محتواي في نوع واحد للعرض. */
private data class SeriesEntry(
    val key: String,
    val title: String,
    val poster: String,
    val subtitle: String,
    val source: String,
    val fasel: FaselMedia? = null,
    val hikaye: HikayeTvApi.HMedia? = null,
    val item: PlaylistItem? = null
)

// ---- مجموعات المكتبة الحقيقية (فاير بيز المستوردة + قوائم المستخدم) ----
private fun PlaylistItem.isFireVod(): Boolean {
    if (isLive) return false
    val g = group.uppercase()
    // FIX #2: "مسلسلات اليوم مش ظاهر" — نوسّع ليشمل أي قسم فيه SERIE/CARTOON/ANIME + بدون بادئة.
    return !isCustom && (
        g.contains("FIREBASE") || g.contains("GOOGLEFIRE") ||
            g.startsWith("SERIES:") || g.startsWith("CARTOONS:") || g.startsWith("ANIME:") ||
            g.contains("SERIE") || g.contains("CARTOON") || g.contains("ANIME") || g.contains("مسلسل")
        )
}

private fun PlaylistItem.fireSerie(): Boolean {
    val g = group.uppercase()
    return isFireVod() && (
        g.startsWith("SERIES:") || g.contains("SERIE") || g.contains("مسلسل")
        ) && !g.contains("CARTOON") && !g.contains("ANIME")
}

private fun PlaylistItem.fireCartoon(): Boolean {
    val g = group.uppercase()
    return isFireVod() && (g.startsWith("CARTOONS:") || g.startsWith("ANIME:") ||
        g.contains("CARTOON") || g.contains("ANIME") || g.contains("كرتون") || g.contains("أنمي"))
}

private fun PlaylistItem.customSerie(): Boolean {
    val g = group.uppercase()
    return !isLive && isCustom && g.contains("SERIE")
}

private fun PlaylistItem.customCartoon(): Boolean {
    val g = group.uppercase()
    return !isLive && isCustom &&
        (g.contains("CARTOON") || g.contains("ANIME") || g.contains("كرتون") || g.contains("أنمي"))
}

/** فلتر النوع داخل نتائج البحث الموحّد — مبني على نوع العمل الحقيقي من السورس. */
private fun faselMatchesKind(m: FaselMedia, k: String): Boolean = when (k) {
    "ALL" -> true
    "serie" -> m.type.contains("serie", true)
    "anime" -> m.type.contains("anime", true)
    else -> m.type.contains("movie", true)
}

private fun hikayeMatchesKind(m: HikayeTvApi.HMedia, k: String): Boolean = when (k) {
    "ALL" -> true
    "serie" -> m.kind.equals("series", true)
    "anime" -> false // حكاية TV مفيهاش نوع أنمي حقيقي — الأنمي من مصادره الحقيقية
    else -> m.kind.equals("movie", true)
}

private fun PlaylistItem.libMatchesKind(k: String): Boolean {
    if (k == "ALL") return true
    val g = group.uppercase()
    return when (k) {
        "serie" -> g.contains("SERIE")
        "anime" -> g.contains("ANIME") || g.contains("CARTOON") || g.contains("أنمي") || g.contains("كرتون")
        else -> !g.contains("SERIE") && !g.contains("ANIME") && !g.contains("CARTOON")
    }
}

/**
 * مسلسلات وأنمي — تبويب مستقل يجمع من **كل المصادر المفعّلة في التطبيق**:
 * فاصل HD (صفحات + تصنيفات حقيقية من السورس) · حكاية TV · اليوم/جوجل فاير · محتواي.
 * تبويب مسلسلات|أنمي فوق — تصنيفات فعلية من الـAPI — بحث موحّد بأيقونة تتمدد
 * (ديبانس 450ms) مع فلاتر نوع حقيقية — تمرير لا نهائي زي التطبيقات العالمية.
 */
@Composable
fun SeriesScreen(
    // فاصل HD — صفحات المسلسلات والأنمي + صفحات التصنيف الحقيقية
    seriesPage: FaselPage?,
    animesPage: FaselPage?,
    seriesGenrePage: FaselPage?,
    animesGenrePage: FaselPage?,
    faselGenres: List<FaselGenre>,
    faselSearchResults: List<FaselMedia>?,
    // حكاية TV
    hikayeSeriesPage: HikayeTvApi.HPage?,
    hikayeSearchResults: List<HikayeTvApi.HMedia>?,
    // مكتبة الجهاز (جوجل فاير + محتواي)
    channels: List<PlaylistItem>,
    libraryItems: List<PlaylistItem>,
    gateways: List<GatewayEntry> = emptyList(),
    hideImages: Boolean = false,
    onHideImagesChange: (Boolean) -> Unit = {},
    onLoadSeries: (Int) -> Unit = {},
    onLoadAnimes: (Int) -> Unit = {},
    onLoadFaselGenre: (String, Int, Int) -> Unit = { _, _, _ -> }, // (kind, genreId, page) — endpoints حقيقية
    onSearchFasel: (String) -> Unit = {},
    onLoadHikayeSeries: (Int, String) -> Unit = { _, _ -> },
    onSearchHikaye: (String) -> Unit = {},
    onOpenFasel: (FaselMedia) -> Unit = {},
    onOpenHikaye: (HikayeTvApi.HMedia) -> Unit = {},
    onOpenPlaylistItem: (PlaylistItem) -> Unit = {},
    onEnsurePortal: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val p = activePlayerPalette()
    val focusReq = remember { FocusRequester() }

    var kind by remember { mutableStateOf(UserSettings.getString("series_kind", KIND_SERIES)) }
    var src by remember { mutableStateOf(UserSettings.getString("series_src", SRC_ALL)) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var kindChip by remember { mutableStateOf("ALL") }
    var faselGenreId by remember { mutableIntStateOf(0) }
    var hikayeMode by remember { mutableStateOf("latest") }
    var hidePics by remember { mutableStateOf(hideImages) }

    LaunchedEffect(kind) { UserSettings.putString("series_kind", kind) }
    LaunchedEffect(src) { UserSettings.putString("series_src", src) }

    val isAnimeKind = kind == KIND_ANIME
    val q = query.trim()
    val searching = q.isNotBlank()
    // genres/list is a global TMDB dictionary; do not show movie-only genres
    // while browsing series/anime. Anime-specific IDs are 10784+ in FaselHD's
    // catalog, while the common TV genres are 10759..10770.
    val visibleFaselGenres = remember(faselGenres, isAnimeKind) {
        faselGenres.filter { g ->
            if (isAnimeKind) g.id in 10784..10920 || g.id == 16
            else g.id in setOf(18, 35, 80, 9648, 10749, 10751, 10759, 10762, 10763, 10764, 10765, 10766, 10767, 10768, 10770)
        }.distinctBy { it.id }
    }

    // أول فتح: حمّل مسلسلات + حكاية — الأنمي يتحمّل أول ما التبويب يتفتح
    LaunchedEffect(Unit) {
        if (seriesPage == null) onLoadSeries(1)
        if (hikayeSeriesPage == null) onLoadHikayeSeries(1, "latest")
        if (animesPage == null && kind == KIND_ANIME) onLoadAnimes(1)
        onEnsurePortal()
    }

    // بحث موحّد على كل المصادر — ديبانس 450ms بدل نداء لكل حرف
    LaunchedEffect(query) {
        val term = query.trim()
        if (term.isBlank()) {
            onSearchFasel("")
            onSearchHikaye("")
            return@LaunchedEffect
        }
        delay(450)
        if (query.trim() == term) {
            onSearchFasel(term)
            onSearchHikaye(term)
        }
    }

    // تصنيف فاصل حقيقي — endpoints مختلفة لكل نوع (genres/series/show · genres/animes/show)
    LaunchedEffect(faselGenreId, kind) {
        if (faselGenreId > 0) onLoadFaselGenre(kind, faselGenreId, 1)
    }

    LaunchedEffect(searchOpen) { if (searchOpen) focusReq.requestFocus() }

    BackHandler {
        when {
            query.isNotBlank() -> query = ""
            searchOpen -> searchOpen = false
            kindChip != "ALL" -> kindChip = "ALL"
            faselGenreId > 0 -> faselGenreId = 0
            src != SRC_ALL -> src = SRC_ALL
            kind != KIND_SERIES -> kind = KIND_SERIES
            else -> onBack()
        }
    }

    // ---- المكتبة المحلية (جوجل فاير + محتواي) ----
    val libAll = remember(channels, libraryItems) { (channels + libraryItems).distinctBy { it.id } }
    val fireSeries = remember(libAll) { libAll.filter { it.fireSerie() } }
    val fireCartoons = remember(libAll) { libAll.filter { it.fireCartoon() } }
    val customSeries = remember(libAll) { libAll.filter { it.customSerie() } }
    val customCartoons = remember(libAll) { libAll.filter { it.customCartoon() } }

    fun PlaylistItem.matches(q: String): Boolean =
        q.isBlank() || name.contains(q, true) || group.contains(q, true)

    // ---- عناصر فاصل: مسلسلات / أنمي حسب التبويب + صفحة التصنيف المختار ----
    val faselKindEntries: List<SeriesEntry> = when {
        searching -> emptyList()
        src != SRC_FASEL && src != SRC_ALL -> emptyList()
        isAnimeKind -> {
            val base = if (faselGenreId > 0) animesGenrePage?.items.orEmpty() else animesPage?.items.orEmpty()
            base.distinctBy { it.id }.map {
                SeriesEntry("f_${it.id}", it.title, it.poster, it.subtitle.ifBlank { "Fasel HD · Anime" }, SRC_FASEL, fasel = it)
            }
        }
        else -> {
            val base = if (faselGenreId > 0) seriesGenrePage?.items.orEmpty() else seriesPage?.items.orEmpty()
            base.distinctBy { it.id }.map {
                SeriesEntry("f_${it.id}", it.title, it.poster, it.subtitle.ifBlank { "Fasel HD" }, SRC_FASEL, fasel = it)
            }
        }
    }

    // ---- حكاية TV ----
    val hikayeActive: Boolean = src == SRC_HIKAYE || (src == SRC_ALL && !isAnimeKind)
    val hikayeEntries: List<SeriesEntry> = when {
        searching || !hikayeActive -> emptyList()
        else -> hikayeSeriesPage?.items.orEmpty().distinctBy { it.kind + "_" + it.id }.map {
            SeriesEntry("h_${it.id}", it.displayTitle, it.poster, it.genreAr.ifBlank { "Hikaye TV" }, SRC_HIKAYE, hikaye = it)
        }
    }

    val fireActive: Boolean = src == SRC_FIRE || src == SRC_ALL
    val fireEntries: List<SeriesEntry> = when {
        searching || !fireActive -> emptyList()
        else -> (if (isAnimeKind) fireCartoons else fireSeries).map {
            SeriesEntry("p_${it.id}", it.name, it.logoUrl, "Prime", SRC_FIRE, item = it)
        }
    }
    val customActive: Boolean = src == SRC_CUSTOM || src == SRC_ALL
    val customEntries: List<SeriesEntry> = when {
        searching || !customActive -> emptyList()
        else -> (if (isAnimeKind) customCartoons else customSeries).map {
            SeriesEntry("c_${it.id}", it.name, it.logoUrl, "محتواي", SRC_CUSTOM, item = it)
        }
    }

    // ---- نتائج البحث الموحّد: فاصل + حكاية + المكتبة كلها ----
    val libSearchBase = when (src) {
        SRC_FIRE -> libAll.filter { it.fireSerie() || it.fireCartoon() }
        SRC_CUSTOM -> libAll.filter { it.customSerie() || it.customCartoon() }
        else -> libAll
    }
    val searchEntries: List<SeriesEntry> = if (!searching) emptyList() else (
        faselSearchResults.orEmpty().filter { faselMatchesKind(it, kindChip) }.map {
            SeriesEntry("f_${it.id}", it.title, it.poster, it.subtitle.ifBlank { "Fasel HD" }, SRC_FASEL, fasel = it)
        } + hikayeSearchResults.orEmpty().filter { hikayeMatchesKind(it, kindChip) }.map {
            SeriesEntry("h_${it.id}", it.displayTitle, it.poster, it.genreAr.ifBlank { "Hikaye TV" }, SRC_HIKAYE, hikaye = it)
        } + libSearchBase.filter { it.matches(q) && it.libMatchesKind(kindChip) }.map {
            SeriesEntry("lib_${it.id}", it.name, it.logoUrl, "مكتبتك", if (it.isFireVod()) SRC_FIRE else SRC_CUSTOM, item = it)
        }
        ).distinctBy { it.key }

    val visible: List<SeriesEntry> = if (searching) searchEntries
    else faselKindEntries + hikayeEntries + fireEntries + customEntries

    val sourceTabs = listOf(
        SRC_ALL to "الكل",
        SRC_FASEL to "Fasel HD",
        SRC_HIKAYE to "Hikaye TV",
        SRC_FIRE to "Prime",
        SRC_CUSTOM to "محتواي"
    ).filter { (key, _) ->
        key == SRC_ALL || gateways.isEmpty() || when (key) {
            SRC_FASEL -> gateways.firstOrNull { it.id == "faselhd" }?.enabled ?: true
            SRC_HIKAYE -> gateways.firstOrNull { it.id == "hikaye" }?.enabled ?: true
            SRC_FIRE -> gateways.firstOrNull { it.id == "googlefire" }?.enabled ?: true
            SRC_CUSTOM -> gateways.firstOrNull { it.id == "custom" }?.enabled ?: true
            else -> true
        }
    }
    val counts = mapOf(
        SRC_ALL to visible.size,
        SRC_FASEL to (if (searching) searchEntries.count { it.source == SRC_FASEL } else if (isAnimeKind) (animesGenrePage?.total ?: animesPage?.total ?: faselKindEntries.size) else (seriesGenrePage?.total ?: seriesPage?.total ?: faselKindEntries.size)),
        SRC_HIKAYE to (if (searching) searchEntries.count { it.source == SRC_HIKAYE } else hikayeSeriesPage?.total ?: hikayeEntries.size),
        SRC_FIRE to (if (searching) searchEntries.count { it.source == SRC_FIRE } else (if (isAnimeKind) fireCartoons else fireSeries).size),
        SRC_CUSTOM to (if (searching) searchEntries.count { it.source == SRC_CUSTOM } else (if (isAnimeKind) customCartoons else customSeries).size)
    )

    val activeFaselPage: FaselPage? = when {
        searching || (src != SRC_FASEL && src != SRC_ALL) -> null
        faselGenreId > 0 -> if (isAnimeKind) animesGenrePage else seriesGenrePage
        else -> if (isAnimeKind) animesPage else seriesPage
    }

    val hikayeModes = listOf(
        "latest" to "🆕 أحدث", "arabic" to "📺 عربي", "foreign" to "🌍 أجنبي",
        "turkish" to "🇹🇷 تركي", "korean" to "🇰🇷 كوري", "anime" to "🎌 أنمي",
        "dubbed" to "🔊 مدبلج", "ramadan" to "🌙 رمضان"
    )
    val kindChips = listOf(
        "ALL" to "الكل", "serie" to "مسلسلات",
        "movie" to "أفلام", "anime" to "أنمي"
    )

    val gridState = rememberLazyGridState()
    var lastAutoFasel by remember(src, kind, faselGenreId, searching) { mutableIntStateOf(1) }
    var lastAutoHikaye by remember(src, kind, hikayeMode, searching) { mutableIntStateOf(1) }

    // ---- تمرير لا نهائي: تحميل الصفحة التالية تلقائي قرب نهاية القائمة ----
    LaunchedEffect(src, kind, faselGenreId, searching, hikayeMode, visible.size, activeFaselPage?.page, hikayeSeriesPage?.page) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx < 0 || visible.isEmpty()) return@collect
                if (!searching && (src == SRC_FASEL || src == SRC_ALL)) {
                    val page = activeFaselPage
                    if (page != null && page.lastPage > page.page) {
                        val next = page.page + 1
                        if (idx >= visible.lastIndex - 4 && lastAutoFasel < next) {
                            lastAutoFasel = next
                            if (faselGenreId > 0) onLoadFaselGenre(kind, faselGenreId, next)
                            else if (isAnimeKind) onLoadAnimes(next) else onLoadSeries(next)
                        }
                    }
                }
                if (!searching && hikayeActive && hikayeEntries.isNotEmpty()) {
                    val page = hikayeSeriesPage ?: return@collect
                    if (page.lastPage > page.page) {
                        val next = page.page + 1
                        if (idx >= visible.lastIndex - 4 && lastAutoHikaye < next) {
                            lastAutoHikaye = next
                            onLoadHikayeSeries(next, hikayeMode)
                        }
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        PlayerTopBar(
            onBack = onBack, onMenu = null, onRefresh = null, onInfo = null,
            slot1 = null, slot2 = null, slot3 = null, showQuickSlots = false
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(4.dp))

            // ---- هيدر مضغوط: العنوان + تبويب مسلسلات|أنمي + بحث + صور ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // تبويب النوع — فوق جنب العنوان
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface.copy(alpha = .85f))
                        .border(1.dp, CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf(KIND_SERIES to "مسلسلات", KIND_ANIME to "أنمي").forEach { (k, label) ->
                        val sel = kind == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (sel) NeonRedContainer else Color.Transparent)
                                .clickable {
                                    if (kind != k) {
                                        kind = k
                                        faselGenreId = 0
                                        if (src == SRC_HIKAYE) onLoadHikayeSeries(1, if (k == KIND_ANIME) "anime" else hikayeMode)
                                        else if (k == KIND_ANIME) onLoadAnimes(1) else onLoadSeries(1)
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(label, color = if (sel) TextPrimary else TextSecondary, fontSize = 12.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // أيقونة بحث بسيطة — تتمدد لحقل كتابة (بدون شريط دائم)
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

                // صور toggle — توفير بيانات
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hidePics) NeonRedContainer.copy(alpha = .7f) else DarkSurface.copy(alpha = .8f))
                        .border(1.dp, if (hidePics) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                        .clickable { hidePics = !hidePics; onHideImagesChange(hidePics) }
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                ) {
                    Text(if (hidePics) "صور🔒" else "صور", color = if (hidePics) NeonRedGlow else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ---- حقل البحث المتمدد ----
            AnimatedVisibility(visible = searchOpen) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp).focusRequester(focusReq),
                        singleLine = true,
                        placeholder = { Text("ابحث في كل المصادر والمكتبة...", color = TextMuted, fontSize = 13.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = p.accentGlow, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                Icon(Icons.Default.Close, "مسح", tint = TextMuted,
                                    modifier = Modifier.size(18.dp).clickable { query = "" })
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(14.dp),
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

            Spacer(Modifier.height(6.dp))

            // ---- مصادر: كل المصادر المفعّلة بعداد حقيقي ----
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(sourceTabs) { (key, label) ->
                    val sel = src == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (sel) NeonRedContainer else DarkSurface.copy(alpha = .78f))
                            .border(1.dp, if (sel) NeonRed else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(13.dp))
                            .clickable {
                                if (src != key) {
                                    src = key
                                    faselGenreId = 0
                                    when {
                                        key == SRC_HIKAYE -> onLoadHikayeSeries(1, if (isAnimeKind) "anime" else hikayeMode)
                                        key == SRC_FASEL || key == SRC_ALL -> if (isAnimeKind) onLoadAnimes(1) else onLoadSeries(1)
                                        else -> onEnsurePortal()
                                    }
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(label, color = if (sel) TextPrimary else TextSecondary, fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                            Text("${counts[key] ?: 0}", color = if (sel) NeonRedGlow else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---- فلتر النوع أثناء البحث (نتائج حقيقية حسب نوع العمل) ----
            if (searching) {
                Spacer(Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(kindChips) { (k, label) ->
                        val sel = kindChip == k
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) TechCyanGlow.copy(alpha = .25f) else DarkSurface.copy(alpha = .7f))
                                .border(1.dp, if (sel) TechCyanGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                .clickable { kindChip = k }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = if (sel) TextPrimary else TextSecondary, fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ---- تصنيفات فاصل الحقيقية من السورس (حسب النوع) ----
            if (!searching && (src == SRC_FASEL || src == SRC_ALL) && visibleFaselGenres.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(visibleFaselGenres) { g ->
                        val sel = faselGenreId == g.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) TechCyanGlow.copy(alpha = .25f) else DarkSurface.copy(alpha = .7f))
                                .border(1.dp, if (sel) TechCyanGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                .clickable { faselGenreId = if (sel) 0 else g.id }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(g.name, color = if (sel) TextPrimary else TextSecondary, fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ---- أوضاع حكاية TV ----
            if (!searching && src == SRC_HIKAYE) {
                Spacer(Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(hikayeModes) { (key, label) ->
                        val sel = hikayeMode == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) TechCyanGlow.copy(alpha = .25f) else DarkSurface.copy(alpha = .7f))
                                .border(1.dp, if (sel) TechCyanGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                .clickable { hikayeMode = key; lastAutoHikaye = 1; onLoadHikayeSeries(1, key) }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = if (sel) TextPrimary else TextSecondary, fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ---- سطر العداد + حالة الصفحة ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    when {
                        searching -> "نتائج البحث"
                        isAnimeKind -> "الأنمي"
                        src == SRC_FASEL -> "Fasel HD Series"
                        src == SRC_HIKAYE -> "Hikaye TV Series"
                        src == SRC_FIRE -> "Prime Series"
                        src == SRC_CUSTOM -> "مسلسلات محتواي"
                        else -> "كل المسلسلات"
                    },
                    color = TechCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Black
                )
                Text("${visible.size} عمل", color = TextMuted, fontSize = 11.sp)
                if (!searching && activeFaselPage != null && activeFaselPage.lastPage > 1) {
                    Text("· صفحة ${activeFaselPage.page}/${activeFaselPage.lastPage}", color = TextMuted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(6.dp))

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when {
                            searching -> "مفيش نتايج للبحث «$q»"
                            faselGenreId > 0 -> "جارٍ تحميل التصنيف من السورس..."
                            else -> "جارٍ التحميل من المصادر..."
                        },
                        color = TextMuted, fontSize = 13.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    gridItems(visible, key = { it.key }) { e ->
                        SeriesPosterCard(e, hidePics) {
                            when {
                                e.fasel != null -> onOpenFasel(e.fasel)
                                e.hikaye != null -> onOpenHikaye(e.hikaye)
                                e.item != null -> onOpenPlaylistItem(e.item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesPosterCard(e: SeriesEntry, hidePics: Boolean, onClick: () -> Unit) {
    val p = activePlayerPalette()
    val ctx = LocalContext.current
    val badge = when (e.source) {
        SRC_FASEL -> "Fasel HD"
        SRC_HIKAYE -> "Hikaye TV"
        SRC_FIRE -> "Prime"
        else -> "محتواي"
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardBg.copy(alpha = .9f))
            .border(1.dp, CrimsonBorder.copy(alpha = .55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)).background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            if (hidePics || e.poster.isBlank()) {
                Text("مسلسل", color = TextMuted, fontSize = 10.sp)
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(e.poster).crossfade(true).build(),
                    contentDescription = e.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(p.accentContainer.copy(alpha = .85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(badge, color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(e.title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
        if (e.subtitle.isNotBlank()) {
            Text(e.subtitle, color = TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
