package com.example.ui.channels

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.player.YouseifPlayerController
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.ui.MainViewModel
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.TopManageAction
import com.example.ui.components.TechTag
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette
import java.util.UUID

private data class PackageBucket(
    val name: String,
    val count: Int,
    val logoUrl: String,
    val kind: String, // "brand" | "content"
    val iconHint: String = ""
)

private const val TAB_CHANNELS = "CHANNELS"
private const val TAB_RADIO = "RADIO"

private data class SourceBucket(val id: String, val label: String)

/** استبعاد دراما لايف — حذف كامل من السورس والعرض */
private fun isDramaLive(item: PlaylistItem): Boolean {
    val v = "${item.group} ${item.name} ${item.id} ${item.url}".lowercase()
    return v.contains("dramalive") || v.contains("drama live") || v.contains("دراما لايف") ||
        v.contains("drama_live") || v.contains("drama-live") || v.contains("dram live") ||
        v.contains("drama.live") || (v.contains("دراما") && v.contains("لايف"))
}

private fun detectSource(item: PlaylistItem): SourceBucket {
    val blob = "${item.group} ${item.name} ${item.url} ${item.id}".lowercase()
    val g = item.group.trim()
    return when {
        blob.contains("firebase") || blob.contains("googlefire") || blob.contains("portal") ||
            blob.contains("اليوم") -> SourceBucket("GOOGLEFIRE", "فاير باس")
        blob.contains("fasel") || blob.contains("فاصل") -> SourceBucket("FASEL", "فاصل")
        // كل باقة مستوردة = شريحة لوحدها باسم الباقة
        g.startsWith("LIVE:", ignoreCase = true) -> {
            val pkg = g.substringAfter(":").trim().ifBlank { "مستورد" }
            SourceBucket("PKG:$pkg", pkg)
        }
        item.isCustom -> {
            val pkg = g.substringAfter(":").trim().ifBlank { g.ifBlank { "مستورد" } }
            SourceBucket("PKG:$pkg", pkg)
        }
        else -> SourceBucket("OTHER", "أخرى")
    }
}

/** ماركات القنوات — باقة مستقلة */
private fun brandLabel(group: String, channelName: String = ""): String? {
    val full = "$group $channelName".lowercase()
    if (full.contains("dramalive") || full.contains("دراما لايف") || full.contains("drama live")) return null
    val brands = listOf(
        "bein sport" to "BEIN SPORT",
        "beinsport" to "BEIN SPORT",
        "bein entertainment" to "beIN ENTERTAINMENT",
        "alkass" to "ALKASS",
        "alwan" to "ALWAN SPORT",
        "starzplay" to "Starz Sport",
        "starz sport" to "Starz Sport",
        "shahid sport" to "SHAHID SPORT",
        "shahid" to "SHAHID SPORT",
        "abu dhabi" to "Abu Dhabi Sport",
        "أبوظبي" to "Abu Dhabi Sport",
        "ssc" to "SSC",
        "fajer" to "Fajer TV",
        "الفجر" to "Fajer TV",
        "mbc" to "MBC GROUP",
        "rotana" to "Rotana",
        "majestic" to "Majestic",
        "osn" to "OSN",
        "nile" to "NILE",
        "الثامنة" to "الثامنة",
        "global sports" to "الثامنة",
        "bein" to "BEIN",
        "ياسين" to "BEIN SPORT"
    )
    for ((key, label) in brands) {
        if (full.contains(key)) return label
    }
    return null
}

/**
 * تصنيف المحتوى عبر كل الباقات:
 * أفلام / مسلسلات / أطفال / أخبار / أغاني / رياضة
 */
private fun contentCategory(group: String, channelName: String = ""): String? {
    val full = "$group $channelName".lowercase()
    if (full.contains("dramalive") || full.contains("دراما لايف")) return null
    return when {
        full.contains("kid") || full.contains("طفل") || full.contains("اطفال") ||
            full.contains("cartoon") || full.contains("spacetoon") || full.contains("baraem") ||
            full.contains("jeem") || full.contains("karameesh") || full.contains("طيور") ||
            full.contains("toy or") -> "أطفال"
        full.contains("movie") || full.contains("فيلم") || full.contains("افلام") ||
            full.contains("cinema") || full.contains("netflix") || full.contains("film") -> "أفلام"
        full.contains("series") || full.contains("مسلسل") || full.contains("serie") -> "مسلسلات"
        full.contains("news") || full.contains("أخبار") || full.contains("اخبار") ||
            full.contains("إخباري") || full.contains("اخبارية") -> "أخبار"
        full.contains("music") || full.contains("اغاني") || full.contains("أغاني") ||
            full.contains("أغاني") || full.contains("مازيكا") -> "أغاني"
        full.contains("دين") || full.contains("quran") || full.contains("قرآن") ||
            full.contains("مكة") || full.contains("المدينة") -> "دينية"
        full.contains("sport") || full.contains("رياض") || full.contains("كورة") ||
            full.contains("bein") || full.contains("alkass") || full.contains("ssc") -> "رياضة"
        else -> null
    }
}

/** اسم الباقة للعرض/التجميع — ماركة أولاً وإلا تصنيف محتوى وإلا اسم الجروب */
private fun packageLabel(group: String, channelName: String = ""): String {
    val g = group.trim()
    if (g.startsWith("LIVE:", ignoreCase = true)) {
        val pkg = g.substringAfter(":").trim()
        if (pkg.isNotBlank()) return pkg
    }
    brandLabel(group, channelName)?.let { return it }
    contentCategory(group, channelName)?.let { return it }
    val raw = group.trim().substringAfterLast('/').trim()
    val generic = setOf("", "general", "live", "iptv", "channels", "channel", "tv", "stream", "all")
    if (raw.lowercase() !in generic && raw.length >= 2) return raw
    return group.trim().ifBlank { "أخرى" }
}

private fun isBrandPackage(name: String): Boolean {
    val brands = setOf(
        "BEIN", "BEIN SPORT", "ALWAN SPORT", "beIN ENTERTAINMENT", "Starz Sport",
        "SHAHID SPORT", "ALKASS", "Abu Dhabi Sport", "Fajer TV", "MBC GROUP",
        "Rotana", "Majestic", "OSN", "NILE", "الثامنة", "SSC", "Rakuten TV", "NETFLIX"
    )
    return brands.any { name.equals(it, true) }
}

private fun qualityLabelOf(variant: PlaylistItem, index: Int, total: Int): String {
    val suffix = variant.name.substringAfter(" · ", "").trim()
    val url = variant.url.lowercase()
    val q = when {
        suffix.contains("1080") || url.contains("1080") || suffix.contains("fhd", true) -> "1080p FHD"
        suffix.contains("720") || url.contains("720") -> "720p HD"
        suffix.contains("480") || url.contains("480") -> "480p SD"
        suffix.contains("360") || url.contains("360") -> "360p"
        suffix.isNotBlank() -> suffix
        else -> "سيرفر ${index + 1}"
    }
    return if (total > 1) "$q  ·  ${index + 1}/$total" else q
}

@Composable
fun ChannelsScreen(
    channels: List<PlaylistItem>,
    currentPlayingChannel: PlaylistItem?,
    controller: YouseifPlayerController? = null,
    onExpandPlayer: () -> Unit = {},
    channelHealth: Map<String, MainViewModel.ChannelHealth> = emptyMap(),
    onCheckChannelStatus: (PlaylistItem) -> Unit = {},
    onProbeAllChannels: (List<PlaylistItem>) -> Unit = {},
    onChannelSelected: (PlaylistItem) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onAddCustomChannel: (PlaylistItem) -> Unit,
    onImportM3U: (String) -> Unit,
    onImportRadio: (String) -> Unit = onImportM3U,
    onImportZipBytes: (ByteArray) -> Unit = {},
    /** text, kind (CHANNELS/RADIO/FILMS/...), packageName */
    onImportSmart: (String, String, String) -> Unit = { text, kind, _ ->
        if (kind == "RADIO") onImportRadio(text) else onImportM3U(text)
    },
    lastImportSummary: com.example.ui.MainViewModel.ImportSummary? = null,
    onDeleteChannel: (PlaylistItem) -> Unit,
    onDeleteChannels: (List<String>) -> Unit = { ids -> ids.forEach { id -> channels.find { it.id == id }?.let(onDeleteChannel) } },
    onDeleteAllCustom: () -> Unit = {
        channels.filter { it.isCustom }.forEach(onDeleteChannel)
    },
    onNavigateToFilms: () -> Unit = {},
    onOpenRadio: () -> Unit = {},
    onOpenDevice: (android.net.Uri, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    onRefreshCatalog: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val p = activePlayerPalette()
    val liveDiagnostics = controller?.diagnostics?.collectAsState()?.value
    val isLivePlaying = liveDiagnostics?.isPlaying == true
    val showLiveMini = currentPlayingChannel != null && controller != null &&
        (currentPlayingChannel.isLive || currentPlayingChannel.group.contains("RADIO", true) ||
            currentPlayingChannel.group.startsWith("LIVE:", true))

    fun documentTitle(uri: android.net.Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0).orEmpty() else ""
            }.orEmpty().ifBlank { uri.lastPathSegment?.substringAfterLast('/') ?: "Opened file" }
        } catch (_: Throwable) {
            uri.lastPathSegment?.substringAfterLast('/') ?: "Opened file"
        }
    }

    val devicePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Throwable) {}
        onOpenDevice(uri, documentTitle(uri))
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var sectionTab by remember { mutableStateOf(UserSettings.getString("channels_tab", TAB_CHANNELS)) }
    var selectedSource by remember { mutableStateOf(UserSettings.getString("channels_source", "ALL")) }
    var openPackage by remember { mutableStateOf<String?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedPackages by remember { mutableStateOf(setOf<String>()) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importKind by remember { mutableStateOf("CHANNELS") }
    var importTargetRadio by remember { mutableStateOf(false) }
    var importPackageName by remember { mutableStateOf("") }
    val importFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                val isZip = uri.toString().lowercase().endsWith(".zip") ||
                    (bytes.size > 1 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte())
                if (isZip) onImportZipBytes(bytes) else {
                    val text = String(bytes)
                    if (text.isNotBlank()) onImportSmart(text, importKind, importPackageName.trim())
                }
            }
            showImportDialog = false
        }
    }
    var collapseServers by remember { mutableStateOf(UserSettings.getString("channels_collapse_servers", "1") == "1") }
    var hidePics by remember { mutableStateOf(UserSettings.getString("channels_hide_pics", "0") == "1") }

    // كل القنوات الحية بدون دراما لايف
    val liveChannels = remember(channels) {
        channels.filter { it.isLive && !isDramaLive(it) }.distinctBy { it.id }
    }

    // PERF: احسب مصدر كل قناة مرة واحدة — بدل إعادة بناء strings لكل قناة عند كل إعادة رسم
    val sourceById = remember(liveChannels) {
        liveChannels.associate { it.id to detectSource(it) }
    }

    val sourceScoped = remember(liveChannels, sourceById, selectedSource, channelHealth) {
        liveChannels.filter {
            when {
                selectedSource == "ALL" -> true
                selectedSource == "AVAILABLE" -> channelHealth[it.id] == MainViewModel.ChannelHealth.ONLINE
                else -> sourceById[it.id]?.id == selectedSource
            }
        }
    }

    val sourceRegistry = remember(sourceById) {
        val packages = sourceById.values
            .filter { it.id.startsWith("PKG:") }
            .distinctBy { it.id }
            .sortedBy { it.label }
        listOf(
            SourceBucket("ALL", "الكل"),
            SourceBucket("AVAILABLE", "قنوات شغّالة"),
            SourceBucket("GOOGLEFIRE", "فاير باس"),
            SourceBucket("FASEL", "فاصل")
        ) + packages
    }

    // PERF: عدّاد كل مصدر يُحسب مرة واحدة لكل تغيير حالة — بدل count مع detectSource لكل chip عند كل فريم
    val sourceCounts = remember(liveChannels, sourceById, channelHealth) {
        val m = HashMap<String, Int>()
        liveChannels.forEach { ch ->
            val sid = sourceById[ch.id]?.id ?: "OTHER"
            m[sid] = (m[sid] ?: 0) + 1
            m["ALL"] = (m["ALL"] ?: 0) + 1
            if (channelHealth[ch.id] == MainViewModel.ChannelHealth.ONLINE) {
                m["AVAILABLE"] = (m["AVAILABLE"] ?: 0) + 1
            }
        }
        m
    }

    // باقات ماركات فوق · تصنيفات محتوى تحت (بأيقونة/لوجو من جوه)
    val packages = remember(sourceScoped) {
        val brands = sourceScoped
            .mapNotNull { ch -> brandLabel(ch.group, ch.name)?.let { it to ch } }
            .groupBy({ it.first }, { it.second })
            .map { (label, entries) ->
                PackageBucket(
                    name = label,
                    count = entries.size,
                    logoUrl = entries.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl.orEmpty(),
                    kind = "brand"
                )
            }
        // باقات مستوردة (LIVE:اسم)
        val imported = sourceScoped
            .filter { it.group.startsWith("LIVE:", ignoreCase = true) }
            .groupBy { it.group.substringAfter(":").trim().ifBlank { "مستورد" } }
            .map { (label, entries) ->
                PackageBucket(
                    name = label,
                    count = entries.distinctBy { it.id }.size,
                    logoUrl = entries.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl.orEmpty(),
                    kind = "brand"
                )
            }
            .filter { b -> brands.none { it.name.equals(b.name, true) } }
        val contents = sourceScoped
            .mapNotNull { ch -> contentCategory(ch.group, ch.name)?.let { it to ch } }
            .groupBy({ it.first }, { it.second })
            .map { (label, entries) ->
                val uniq = entries.distinctBy { it.id }
                PackageBucket(
                    name = label,
                    count = uniq.size,
                    logoUrl = pickCategoryLogo(label, uniq),
                    kind = "content",
                    iconHint = label
                )
            }
            .sortedByDescending { it.count }
        (brands + imported).sortedWith(
            compareByDescending<PackageBucket> { preferredPackageRank(it.name) }.thenByDescending { it.count }
        ) + contents
    }

    val packageChannels = remember(sourceScoped, openPackage, searchQuery, showFavoritesOnly) {
        if (openPackage == null) emptyList()
        else sourceScoped.filter { ch ->
            val match = when {
                isBrandPackage(openPackage!!) || brandLabel(ch.group, ch.name) == openPackage ||
                    ch.group.equals("LIVE:$openPackage", true) || packageLabel(ch.group, ch.name) == openPackage ->
                    brandLabel(ch.group, ch.name) == openPackage ||
                        ch.group.equals("LIVE:$openPackage", true) ||
                        packageLabel(ch.group, ch.name) == openPackage
                else ->
                    contentCategory(ch.group, ch.name) == openPackage
            }
            match &&
                (if (showFavoritesOnly) ch.isFavorite else true) &&
                (searchQuery.isBlank() || ch.name.contains(searchQuery, true) || ch.group.contains(searchQuery, true))
        }.distinctBy { it.id }.sortedWith(
            compareBy<PlaylistItem> { it.channelNumber }.thenBy { it.name }
        )
    }

    val channelVariants = remember(packageChannels) {
        packageChannels.groupBy { it.name.substringBefore(" · ").trim().ifBlank { it.name } }
    }
    val shownChannels = remember(packageChannels, collapseServers) {
        if (!collapseServers) packageChannels
        else channelVariants.map { (_, list) -> list.first() }
    }

    // بحث على مستوى الباقات كمان
    val shownPackages = remember(packages, searchQuery, showFavoritesOnly) {
        if (showFavoritesOnly) emptyList() // المفضلة = شبكة قنوات مش باقات
        else if (searchQuery.isBlank()) packages.filter { it.name.isNotBlank() }
        else packages.filter { it.name.isNotBlank() && it.name.contains(searchQuery, true) }
    }
    // في وضع المفضلة: اعرض كل القنوات المفضلة مباشرة
    val favoriteChannels = remember(liveChannels, searchQuery) {
        liveChannels.filter { it.isFavorite }.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.group.contains(searchQuery, true)
        }
    }

    LaunchedEffect(collapseServers, hidePics, sectionTab, openPackage, selectedSource) {
        try {
            UserSettings.putString("channels_collapse_servers", if (collapseServers) "1" else "0")
            UserSettings.putString("channels_hide_pics", if (hidePics) "1" else "0")
            UserSettings.putString("channels_tab", sectionTab)
            UserSettings.putString("channels_package", openPackage ?: "")
            UserSettings.putString("channels_source", selectedSource)
        } catch (_: Throwable) {}
    }

    // فحص خفيف مرّة واحدة عند فتح الباقة فقط — مش حلقة مستمرة (كانت بتهلك البطارية)
    LaunchedEffect(openPackage) {
        if (openPackage == null) return@LaunchedEffect
        val scope = packageChannels
            .filter { channelHealth[it.id] == null || channelHealth[it.id] == MainViewModel.ChannelHealth.IDLE }
            .take(40)
        if (scope.isNotEmpty()) onProbeAllChannels(scope)
    }

    val handleBack = {
        when {
            searchOpen && searchQuery.isNotBlank() -> searchQuery = ""
            searchOpen -> searchOpen = false
            openPackage != null -> openPackage = null
            showFavoritesOnly -> showFavoritesOnly = false
            else -> onBack()
        }
    }
    BackHandler { handleBack() }

    var qualityPickChannel by remember { mutableStateOf<PlaylistItem?>(null) }
    val qualityVariants = remember(qualityPickChannel, channelVariants) {
        val ch = qualityPickChannel ?: return@remember emptyList()
        val key = ch.name.substringBefore(" · ").trim().ifBlank { ch.name }
        channelVariants[key].orEmpty().ifEmpty { listOf(ch) }
    }

    fun playChannel(channel: PlaylistItem) {
        if (selectionMode) {
            selectedIds = if (channel.id in selectedIds) selectedIds - channel.id else selectedIds + channel.id
            return
        }
        val key = channel.name.substringBefore(" · ").trim().ifBlank { channel.name }
        val variants = channelVariants[key].orEmpty()
        if (collapseServers && variants.size > 1) qualityPickChannel = channel
        else onChannelSelected(channel)
    }

    fun deleteSelection() {
        // قنوات محددة جوّه الباقة — المستوردة فقط (isCustom) عشان فاصل/فاير باس/اليوم/حكاية ما يتأثروش
        val ids = selectedIds.filter { id ->
            channels.find { it.id == id }?.isCustom == true
        }
        // باقات محددة من الشبكة — فقط قنوات LIVE: أو isCustom اللي اسم باقتها مطابق
        val fromPackages = if (selectedPackages.isEmpty()) emptyList() else {
            channels.filter { ch ->
                if (!ch.isCustom && !ch.group.startsWith("LIVE:", ignoreCase = true)) return@filter false
                val pkg = packageLabel(ch.group, ch.name)
                val brand = brandLabel(ch.group, ch.name)
                pkg in selectedPackages || brand in selectedPackages ||
                    selectedPackages.any { ch.group.equals("LIVE:$it", true) }
            }.map { it.id }
        }
        val all = (ids + fromPackages).distinct()
        if (all.isNotEmpty()) onDeleteChannels(all)
        selectedIds = emptySet()
        selectedPackages = emptySet()
        selectionMode = false
        // لو الباقة الحالية فضيت ارجع للشبكة
        openPackage = null
    }

    Column(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        PlayerTopBar(
            onBack = handleBack,
            onMenu = null,
            onRefresh = onRefreshCatalog,
            onInfo = null,
            onDownloads = null,
            onFavorites = {
                showFavoritesOnly = !showFavoritesOnly
                if (showFavoritesOnly) openPackage = null
            },
            favoritesActive = showFavoritesOnly,
            showThemeButton = false,
            manageActions = listOf(
                TopManageAction("تحديث القائمة") { onRefreshCatalog() },
                TopManageAction(if (selectionMode) "إنهاء التحديد" else "تحديد باقات / قنوات") {
                    selectionMode = !selectionMode
                    if (!selectionMode) {
                        selectedIds = emptySet()
                        selectedPackages = emptySet()
                    }
                },
                TopManageAction("تحديد كل الظاهر") {
                    selectionMode = true
                    if (openPackage == null) {
                        selectedPackages = shownPackages.map { it.name }.toSet()
                    } else {
                        selectedIds = shownChannels.map { it.id }.toSet()
                    }
                },
                TopManageAction(
                    "حذف المحدد (${selectedIds.size + selectedPackages.size})",
                    destructive = true
                ) {
                    if (selectedIds.isNotEmpty() || selectedPackages.isNotEmpty()) {
                        confirmDeleteSelected = true
                    }
                },
                TopManageAction("حذف كل المستورد", destructive = true) {
                    confirmDeleteAll = true
                }
            )
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            // شريط زي المكتبة: [قنوات | راديو] + بحث + صور — من غير عنوان كبير
            if (openPackage == null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface.copy(alpha = .9f))
                            .border(1.dp, CrimsonBorder.copy(alpha = .65f), RoundedCornerShape(14.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(TAB_CHANNELS to "القنوات", TAB_RADIO to "راديو").forEach { (key, label) ->
                            val sel = sectionTab == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (sel) NeonRedContainer else Color.Transparent)
                                    .clickable {
                                        sectionTab = key
                                        openPackage = null
                                        showFavoritesOnly = false
                                        searchQuery = ""
                                        searchOpen = false
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (sel) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (searchOpen) NeonRedContainer else DarkSurface.copy(alpha = .8f))
                            .border(1.dp, if (searchOpen) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                            .clickable {
                                searchOpen = !searchOpen
                                if (!searchOpen) searchQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, null, tint = if (searchOpen) NeonRedGlow else TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hidePics) NeonRedContainer.copy(alpha = .7f) else DarkSurface.copy(alpha = .8f))
                            .border(1.dp, if (hidePics) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                            .clickable { hidePics = !hidePics }
                            .padding(horizontal = 9.dp, vertical = 8.dp)
                    ) {
                        Text(if (hidePics) "صور🔒" else "صور", color = if (hidePics) NeonRedGlow else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // جوّه باقة: اسم الباقة + بحث + صور
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        openPackage!!,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (searchOpen) NeonRedContainer else DarkSurface.copy(alpha = .8f))
                            .border(1.dp, if (searchOpen) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                            .clickable {
                                searchOpen = !searchOpen
                                if (!searchOpen) searchQuery = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, null, tint = if (searchOpen) NeonRedGlow else TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hidePics) NeonRedContainer.copy(alpha = .7f) else DarkSurface.copy(alpha = .8f))
                            .border(1.dp, if (hidePics) NeonRedGlow else CrimsonBorder.copy(alpha = .9f), RoundedCornerShape(12.dp))
                            .clickable { hidePics = !hidePics }
                            .padding(horizontal = 9.dp, vertical = 8.dp)
                    ) {
                        Text(if (hidePics) "صور🔒" else "صور", color = if (hidePics) NeonRedGlow else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = searchOpen) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                when {
                                    sectionTab == TAB_RADIO -> "ابحث في الراديو..."
                                    openPackage != null -> "ابحث داخل الباقة..."
                                    else -> "ابحث عن باقة أو قناة..."
                                },
                                color = TextMuted, fontSize = 12.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = p.accentGlow, modifier = Modifier.size(17.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(17.dp).clickable { searchQuery = "" })
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

            // مصادر: الكل · فاير · فاصل · شغّالة — بس في تبويب القنوات
            if (openPackage == null && sectionTab == TAB_CHANNELS) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sourceRegistry.forEach { src ->
                        val sel = selectedSource == src.id
                        val cnt = sourceCounts[src.id] ?: 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) NeonRedContainer else DarkSurface.copy(alpha = .78f))
                                .border(1.dp, if (sel) NeonRed else CrimsonBorder.copy(alpha = .85f), RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedSource = src.id
                                    openPackage = null
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${src.label} · ${formatCount(cnt)}",
                                color = if (sel) TextPrimary else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                    // زر + استيراد (رابط / M3U / ZIP / GitHub)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .6f))))
                            .clickable {
                                importTargetRadio = false
                                importKind = "CHANNELS"
                                showImportDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "استيراد", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            } else if (sectionTab == TAB_CHANNELS) {
                // جوّه الباقة: أدوات بسيطة
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (collapseServers) NeonRedContainer else DarkSurface.copy(alpha = .8f))
                            .border(1.dp, if (collapseServers) NeonRed else CrimsonBorder.copy(alpha = .8f), RoundedCornerShape(12.dp))
                            .clickable { collapseServers = !collapseServers }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (collapseServers) "سيرفرات: مدموجة" else "سيرفرات: كلها",
                            color = if (collapseServers) TextPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${formatCount(shownChannels.size)} قناة", color = NeonRedGlow, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .55f))))
                            .clickable { showImportDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("M3U", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (sectionTab == TAB_RADIO && openPackage == null && !showFavoritesOnly) {
                // راديو جوّه تبويب القنوات — تصنيفات عربية جميلة + توسيع المحتوى
                val radioCats = remember { com.example.data.RadioCatalog.load(context) }
                var radioExpanded by remember {
                    mutableStateOf(UserSettings.getString("radio_expanded", radioCats.firstOrNull()?.type.orEmpty()))
                }
                val q = searchQuery.trim()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "تصنيفات الإذاعة",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .55f))))
                            .clickable {
                                importTargetRadio = true
                                importKind = "RADIO"
                                showImportDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("استيراد راديو", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val customRadio = remember(showImportDialog) {
                    UserSettings.getString("custom_radio_stations", "")
                        .lines()
                        .mapNotNull { line ->
                            val p2 = line.split("|||")
                            if (p2.size >= 2) p2[0].trim() to p2[1].trim() else null
                        }
                        .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    if (customRadio.isNotEmpty()) {
                        item(key = "custom_radio_header") {
                            Text("محطاتي", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        customRadio.forEach { (name, url) ->
                            item(key = "cr_$url") {
                                RadioStationRow(
                                    name = name,
                                    isPlaying = currentPlayingChannel?.url == url,
                                    accent = p.accentGlow,
                                    onPlay = {
                                        onChannelSelected(
                                            PlaylistItem(
                                                id = "radio_custom_${url.hashCode()}",
                                                channelNumber = 0,
                                                name = name,
                                                url = url,
                                                group = "RADIO/CUSTOM",
                                                logoUrl = "",
                                                isLive = true,
                                                isCustom = true
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                    radioCats.forEach { category ->
                        val stations = category.items.filter {
                            q.isBlank() || it.name.contains(q, true) || category.type.contains(q, true) || category.name.contains(q, true)
                        }
                        if (stations.isEmpty() && q.isNotBlank()) return@forEach
                        val isExp = radioExpanded == category.type
                        // اسم عربي جميل مناسب للمحتوى (مش type الإنجليزي)
                        val displayName = category.name
                            .replace(Regex("^[🟢📻🎤🤲📖\\s]+"), "")
                            .trim()
                            .ifBlank { category.name }
                        val catIcon = when {
                            category.type.contains("modern", true) || category.type.contains("classic", true) -> Icons.Default.Person
                            category.type.contains("quran", true) || category.type.contains("radio_quran", true) -> Icons.Default.MenuBook
                            category.type.contains("islamic", true) -> Icons.Default.Favorite
                            category.type.contains("music_artists", true) -> Icons.Default.Mic
                            category.type.contains("music", true) -> Icons.Default.Radio
                            else -> Icons.Default.Radio
                        }
                        item(key = "rc_${category.type}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isExp) NeonRedContainer.copy(alpha = .22f)
                                        else DarkCardBg.copy(alpha = .92f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isExp) p.accentGlow.copy(alpha = .7f) else CrimsonBorder.copy(alpha = .55f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        radioExpanded = if (isExp) "" else category.type
                                        try { UserSettings.putString("radio_expanded", radioExpanded) } catch (_: Throwable) {}
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(p.accentContainer.copy(alpha = 0.5f))
                                            .border(1.dp, p.accentGlow.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(catIcon, null, tint = p.accentGlow, modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            displayName,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${stations.size} محطة",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        if (isExp) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                        contentDescription = null,
                                        tint = p.accentGlow,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                if (isExp) {
                                    Spacer(Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        stations.forEach { st ->
                                            RadioStationRow(
                                                name = st.name,
                                                isPlaying = currentPlayingChannel?.url == st.url,
                                                accent = p.accentGlow,
                                                onPlay = {
                                                    onChannelSelected(
                                                        PlaylistItem(
                                                            id = "radio_${st.url.hashCode()}",
                                                            channelNumber = 0,
                                                            name = st.name,
                                                            url = st.url,
                                                            group = "RADIO/${category.type}",
                                                            logoUrl = "",
                                                            isLive = true
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (showFavoritesOnly && openPackage == null) {
                // ===== صفحة مفضلة القنوات فقط =====
                if (favoriteChannels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد قنوات في المفضلة", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favoriteChannels, key = { it.id }) { channel ->
                            ChannelCard(
                                selected = channel.id in selectedIds,
                                channel = channel,
                                isPlaying = currentPlayingChannel?.id == channel.id,
                                health = channelHealth[channel.id] ?: MainViewModel.ChannelHealth.IDLE,
                                hideLogo = hidePics,
                                qualityCount = 1,
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = if (channel.id in selectedIds) selectedIds - channel.id else selectedIds + channel.id
                                    } else onChannelSelected(channel)
                                },
                                onToggleFavorite = { onToggleFavorite(channel.id, !channel.isFavorite) }
                            )
                        }
                    }
                }
            } else if (openPackage == null) {
                // ===== شبكة الباقات من برّه =====
                if (shownPackages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد باقات في هذا التبويب", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shownPackages, key = { "${it.kind}_${it.name}" }) { pkg ->
                            PackageCard(
                                name = pkg.name,
                                count = pkg.count,
                                logoUrl = if (hidePics) "" else pkg.logoUrl,
                                kind = pkg.kind,
                                selected = pkg.name in selectedPackages,
                                onOpen = {
                                    if (selectionMode) {
                                        selectedPackages = if (pkg.name in selectedPackages)
                                            selectedPackages - pkg.name else selectedPackages + pkg.name
                                    } else {
                                        openPackage = pkg.name
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ===== قنوات جوّه الباقة =====
                if (shownChannels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد قنوات في هذه الباقة", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shownChannels, key = { it.id }) { channel ->
                            val key = channel.name.substringBefore(" · ").trim().ifBlank { channel.name }
                            val variantCount = channelVariants[key]?.size ?: 1
                            ChannelCard(
                                selected = channel.id in selectedIds,
                                channel = channel,
                                isPlaying = currentPlayingChannel?.id == channel.id ||
                                    (currentPlayingChannel != null &&
                                        currentPlayingChannel.name.substringBefore(" · ").trim() == key),
                                health = channelHealth[channel.id] ?: MainViewModel.ChannelHealth.IDLE,
                                hideLogo = hidePics,
                                qualityCount = if (collapseServers) variantCount else 1,
                                onClick = { playChannel(channel) },
                                onToggleFavorite = { onToggleFavorite(channel.id, !channel.isFavorite) }
                            )
                        }
                    }
                }
            }
        }
    }

    // اختيار الجودة
    if (qualityPickChannel != null && qualityVariants.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { qualityPickChannel = null },
            title = { Text("اختر الجودة / السيرفر", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        qualityPickChannel!!.name.substringBefore(" · ").trim(),
                        color = TextSecondary, fontSize = 12.sp
                    )
                    qualityVariants.forEachIndexed { index, variant ->
                        val label = qualityLabelOf(variant, index, qualityVariants.size)
                        val health = channelHealth[variant.id] ?: MainViewModel.ChannelHealth.IDLE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface.copy(alpha = .85f))
                                .border(1.dp, CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(12.dp))
                                .clickable {
                                    onChannelSelected(variant)
                                    qualityPickChannel = null
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val host = try {
                                        java.net.URI(variant.url).host?.removePrefix("www.")?.take(28).orEmpty()
                                    } catch (_: Throwable) { "" }
                                    if (host.isNotBlank()) Text(host, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                SignalBars(health)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { qualityPickChannel = null }) { Text("إلغاء", color = TextSecondary) }
            },
            containerColor = DarkCardBg
        )
    }



    // ميني بلاير قنوات / راديو
    if (showLiveMini && !selectionMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 14.dp, start = 10.dp, end = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LiveMiniPlayerBar(
                channel = currentPlayingChannel!!,
                isPlaying = isLivePlaying,
                controller = controller!!,
                onExpand = onExpandPlayer,
                onPlayPause = { controller?.togglePlayPause() },
                onStop = {
                    try { controller?.stopPlayback() } catch (_: Throwable) {}
                }
            )
        }
    }

    // شريط تحديد عائم
    if (selectionMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 18.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCardBg.copy(alpha = 0.96f))
                    .border(1.dp, NeonRedGlow.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${selectedIds.size + selectedPackages.size} محدد",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, CrimsonBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            if (openPackage == null) {
                                selectedPackages = shownPackages.map { it.name }.toSet()
                            } else {
                                selectedIds = shownChannels.map { it.id }.toSet()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text("تحديد الكل", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonRed.copy(alpha = 0.9f))
                        .clickable {
                            if (selectedIds.isNotEmpty() || selectedPackages.isNotEmpty()) {
                                confirmDeleteSelected = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("حذف", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .clickable {
                            selectionMode = false
                            selectedIds = emptySet()
                            selectedPackages = emptySet()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text("استيراد قائمة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // نتيجة حقيقية لآخر استرداد — عدد فعلي
                    lastImportSummary?.let { s ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface.copy(alpha = .95f))
                                .border(1.dp, p.accentGlow.copy(alpha = .6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (s.kind == "ERROR") "✕ ${s.label}"
                                else "✔ تم استرداد ${s.count} ${s.label}" + (if (s.packageName.isNotBlank()) " ← ${s.packageName}" else ""),
                                color = if (s.kind == "ERROR") NeonRedGlow else p.accentGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text("اختر المكان + اسم الباقة، ثم الصق الرابط أو اختر ملف.", color = TextSecondary, fontSize = 12.sp)
                    // نوع الاستيراد
                    Text("نوع الاستيراد", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "CHANNELS" to "قنوات",
                            "RADIO" to "راديو",
                            "FILMS" to "أفلام",
                            "SERIES" to "مسلسلات",
                            "ANIME" to "أنمي"
                        ).forEach { (key, label) ->
                            val sel = importKind == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) NeonRedContainer else DarkSurface.copy(alpha = .85f))
                                    .border(1.dp, if (sel) NeonRedGlow else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(10.dp))
                                    .clickable { importKind = key; importTargetRadio = key == "RADIO" }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = if (sel) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = importPackageName,
                        onValueChange = { importPackageName = it },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        singleLine = true,
                        placeholder = { Text("اسم الباقة (اختياري) مثل: نتفليكس · beIN", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = p.accentGlow, unfocusedBorderColor = CrimsonBorder,
                            focusedContainerColor = DarkSurface.copy(alpha = .9f),
                            unfocusedContainerColor = DarkSurface.copy(alpha = .9f)
                        )
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("https://...m3u  أو  #EXTM3U", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = p.accentGlow, unfocusedBorderColor = CrimsonBorder,
                            focusedContainerColor = DarkSurface.copy(alpha = .9f),
                            unfocusedContainerColor = DarkSurface.copy(alpha = .9f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface.copy(alpha = .95f))
                                .border(1.dp, p.accentGlow.copy(alpha = .6f), RoundedCornerShape(14.dp))
                                .clickable {
                                    importFilePicker.launch("*/*")
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, null, tint = p.accentGlow, modifier = Modifier.size(22.dp))
                                Text("ملف", color = p.accentGlow, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text("M3U · ZIP · TXT", color = TextMuted, fontSize = 9.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface.copy(alpha = .95f))
                                .border(1.dp, NeonRedGlow.copy(alpha = .45f), RoundedCornerShape(14.dp))
                                .clickable {
                                    val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (clip.isNotBlank()) importText = clip
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.ContentPaste, null, tint = NeonRedGlow, modifier = Modifier.size(22.dp))
                                Text("لصق", color = NeonRedGlow, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text("من الحافظة", color = TextMuted, fontSize = 9.sp)
                            }
                        }
                    }
                    Text("هيتفك ويتضاف للباقة/المكان المختار — بدون تشغيل تلقائي.", color = TextMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val text = if (importText.isNotBlank()) importText.trim() else clipboardManager.getText()?.text?.trim().orEmpty()
                    if (text.isNotBlank()) {
                        onImportSmart(text, importKind, importPackageName.trim())
                        showImportDialog = false
                        importText = ""
                        importPackageName = ""
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NeonRed)) { Text("استيراد", fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("إلغاء", color = TextSecondary) } },
            containerColor = DarkCardBg
        )
    }

    // ---- dialogs: confirm delete selected / delete all imported ----
    if (confirmDeleteSelected) {
        val n = selectedIds.size + selectedPackages.size
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("تأكيد الحذف", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "حذف $n عنصر محدد (باقات/قنوات)؟ لن يختفي باقي المحتوى.",
                    color = TextSecondary, fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteSelection()
                        confirmDeleteSelected = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSelected = false }) { Text("إلغاء", color = TextSecondary) } },
            containerColor = DarkCardBg
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("حذف كل المستورد؟", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هيتمسح القنوات المستوردة فقط. فاصل وفاير باس وباقي المصادر تفضل.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllCustom()
                        confirmDeleteAll = false
                        selectedIds = emptySet()
                        selectedPackages = emptySet()
                        selectionMode = false
                        openPackage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) { Text("حذف المستورد") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("إلغاء", color = TextSecondary) } },
            containerColor = DarkCardBg
        )
    }

    // راديو جوّه تبويب القنوات فقط — بدون overlay كامل (عشان تبويب القنوات|راديو يفضل ظاهر)
}


/** ترتيب الباقات الشهيرة أولاً زي المرجع */

private fun preferredPackageRank(name: String): Int {
    val order = listOf(
        "BEIN", "BEIN SPORT", "ALWAN SPORT", "الثامنة", "beIN ENTERTAINMENT", "Starz Sport",
        "SHAHID SPORT", "ALKASS", "Abu Dhabi Sport", "Fajer TV", "MBC GROUP", "NEWS",
        "Rotana", "Majestic", "KIDS CHANNELS", "SSC", "OSN", "Rakuten TV"
    )
    val i = order.indexOfFirst { name.equals(it, true) }
    return if (i >= 0) 1000 - i else 0
}

/** لوجو من قناة مشهورة جوّه التصنيف (سبيستون / براعم / beIN …) */
private fun pickCategoryLogo(category: String, entries: List<PlaylistItem>): String {
    val prefer = when (category) {
        "أطفال" -> listOf(
            "spacetoon", "سبيس", "سبيستون", "baraem", "براعم", "cartoon", "كرتون",
            "nick", "disney", "mbc3", "mbc 3", "طيور", "karameesh", "كراميش", "jeem", "جيم"
        )
        "رياضة" -> listOf("bein", "بي إن", "ssc", "alkass", "الكأس", "alwan", "ألوان", "starz")
        "أفلام" -> listOf("netflix", "osn", "shahid", "شاهد", "rotana", "روتانا", "cinema", "movie")
        "مسلسلات" -> listOf("shahid", "شاهد", "osn", "mbc", "netflix", "series")
        "أخبار" -> listOf("aljazeera", "الجزيرة", "arabia", "العربية", "sky news", "bbc", "cnn", "extra")
        "أغاني" -> listOf("music", "mazzika", "مازيكا", "rotana", "روتانا", "melody")
        "دينية" -> listOf("quran", "قرآن", "مكة", "المدينة", "iqra", "اقرأ")
        else -> emptyList()
    }
    val scored = entries.map { ch ->
        val blob = "${ch.name} ${ch.group}".lowercase()
        val score = prefer.indexOfFirst { blob.contains(it) }.let { if (it < 0) 99 else it }
        score to ch
    }.sortedBy { it.first }
    return scored.firstOrNull { it.second.logoUrl.isNotBlank() }?.second?.logoUrl
        ?: entries.firstOrNull { it.logoUrl.isNotBlank() }?.logoUrl.orEmpty()
}

private fun packageIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        n.contains("طفل") || n.contains("kid") || n.contains("cartoon") -> Icons.Default.ChildCare
        n.contains("فيلم") || n.contains("أفلام") || n.contains("movie") || n.contains("cinema") -> Icons.Default.Movie
        n.contains("مسلسل") -> Icons.Default.LiveTv
        n.contains("news") || n.contains("أخبار") || n.contains("اخبار") -> Icons.Default.Newspaper
        n.contains("أغاني") || n.contains("اغاني") || n.contains("music") -> Icons.Default.Whatshot
        n.contains("دين") -> Icons.Default.LiveTv
        n.contains("رياض") || n.contains("sport") || n.contains("bein") || n.contains("alkass") || n.contains("ssc") -> Icons.Default.SportsSoccer
        else -> Icons.Default.LiveTv
    }
}

@Composable
private fun SignalBars(health: MainViewModel.ChannelHealth, modifier: Modifier = Modifier) {
    val active = when (health) {
        MainViewModel.ChannelHealth.ONLINE -> 3
        MainViewModel.ChannelHealth.CHECKING -> 2
        MainViewModel.ChannelHealth.OFFLINE -> 1
        MainViewModel.ChannelHealth.IDLE -> 0
    }
    val color = when (health) {
        MainViewModel.ChannelHealth.ONLINE -> LiveGreen
        MainViewModel.ChannelHealth.CHECKING -> NeonRedGlow
        MainViewModel.ChannelHealth.OFFLINE -> LiveRed
        MainViewModel.ChannelHealth.IDLE -> TextMuted
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(6.dp, 9.dp, 12.dp).forEachIndexed { i, h ->
            Box(
                Modifier.width(3.dp).height(h).clip(RoundedCornerShape(1.dp))
                    .background(if (i < active) color else color.copy(alpha = .22f))
            )
        }
    }
}

private fun formatCount(n: Int): String =
    if (n < 1000) n.toString() else "%,d".format(java.util.Locale.US, n)

@Composable
private fun PackageCard(
    name: String,
    count: Int,
    logoUrl: String,
    kind: String = "brand",
    selected: Boolean = false,
    onOpen: () -> Unit
) {
    val p = activePlayerPalette()
    val isContent = kind == "content"
    val borderColor = when {
        selected -> NeonRedGlow
        isContent -> p.accent.copy(alpha = .55f)
        else -> CrimsonBorder.copy(alpha = .75f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) NeonRedContainer.copy(alpha = .85f) else DarkCardBg.copy(alpha = .94f))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(8.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface.copy(alpha = .95f))
                    .border(
                        1.dp,
                        if (isContent) p.accentGlow.copy(alpha = .35f) else CrimsonBorder.copy(alpha = .45f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // لوجو من قناة جوّه التصنيف أو الماركة
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(logoUrl)
                            .size(384).memoryCacheKey(logoUrl).diskCacheKey(logoUrl).crossfade(true).build(),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        packageIcon(name),
                        name,
                        tint = p.accentGlow,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                name,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            TechTag(
                text = "${formatCount(count)} قناة",
                color = p.accentGlow
            )
        }
    }
}

@Composable
private fun ChannelCard(
    selected: Boolean = false,
    channel: PlaylistItem,
    isPlaying: Boolean,
    health: MainViewModel.ChannelHealth,
    hideLogo: Boolean,
    qualityCount: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val ctx = LocalContext.current
    val p = activePlayerPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    selected -> NeonRedContainer.copy(alpha = .88f)
                    isPlaying -> NeonRedContainer.copy(alpha = .75f)
                    else -> DarkCardBg.copy(alpha = .92f)
                }
            )
            .border(
                1.5.dp,
                when {
                    selected -> NeonRedGlow
                    isPlaying -> NeonRedGlow
                    else -> CrimsonBorder.copy(alpha = .75f)
                },
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPlaying) NeonRed.copy(alpha = .28f) else DarkSurface)
                    .border(1.dp, if (isPlaying) NeonRedGlow.copy(alpha = .65f) else CrimsonBorder.copy(alpha = .45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!hideLogo && channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx).data(channel.logoUrl)
                            .size(384).memoryCacheKey(channel.logoUrl).diskCacheKey(channel.logoUrl).crossfade(false).build(),
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.LiveTv, null, tint = if (isPlaying) NeonRedGlow else TechCyanGlow, modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier.align(Alignment.TopStart).padding(4.dp).size(18.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(DarkSurface.copy(alpha = .65f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (channel.isFavorite) LiveRed else TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) { SignalBars(health) }
                if (qualityCount > 1) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(4.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(NeonRed.copy(alpha = .88f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("${qualityCount} جودات", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(
                channel.name.substringBefore(" · ").trim(),
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun LiveMiniPlayerBar(
    channel: PlaylistItem,
    isPlaying: Boolean,
    controller: YouseifPlayerController,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit
) {
    val p = activePlayerPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCardBg.copy(alpha = 0.97f))
            .border(
                1.dp,
                if (isPlaying) LiveGreen.copy(alpha = 0.55f) else NeonRedGlow.copy(alpha = 0.4f),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onExpand)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // صورة مصغّرة من المشغّل
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .border(1.dp, CrimsonBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        setKeepContentOnPlayerReset(true)
                        try { player = controller.getPlayer() } catch (_: Throwable) {}
                    }
                },
                update = { view ->
                    try {
                        val pl = controller.getPlayer()
                        if (view.player !== pl) view.player = pl
                    } catch (_: Throwable) {}
                },
                modifier = Modifier.fillMaxSize()
            )
            if (!isPlaying) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                channel.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (channel.group.contains("RADIO", true)) "راديو مباشر"
                else if (isPlaying) "بث مباشر · شغال" else "متوقف",
                color = if (isPlaying) LiveGreen else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(p.accent)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .clickable(onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}


@Composable
private fun RadioStationRow(
    name: String,
    isPlaying: Boolean,
    accent: Color,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface.copy(alpha = .92f))
            .border(
                1.dp,
                if (isPlaying) LiveGreen.copy(alpha = .6f) else CrimsonBorder.copy(alpha = .4f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isPlaying) LiveGreen.copy(alpha = 0.18f) else accent.copy(alpha = 0.18f))
                .border(1.dp, if (isPlaying) LiveGreen.copy(alpha = 0.5f) else accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Radio,
                contentDescription = null,
                tint = if (isPlaying) LiveGreen else accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            name,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isPlaying) LiveGreen.copy(alpha = 0.25f)
                    else accent.copy(alpha = 0.85f)
                )
                .border(1.dp, if (isPlaying) LiveGreen else accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .clickable(onClick = onPlay)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    null,
                    tint = if (isPlaying) LiveGreen else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    if (isPlaying) "شغال" else "Play",
                    color = if (isPlaying) LiveGreen else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
