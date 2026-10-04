package com.example.ui

import android.app.Activity
import android.provider.OpenableColumns
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.FaselGenre
import com.example.data.FaselMedia
import com.example.data.RadioCatalog
import com.example.data.FaselNetwork
import com.example.data.FaselPage
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.player.YouseifPlayerController
import com.example.ui.channels.ChannelsScreen
import com.example.ui.components.VideoPlayerView
import com.example.ui.films.FilmsScreen
import com.example.ui.films.MediaDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.LocalMediaScreen
import com.example.ui.radio.RadioScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyan
import com.example.ui.theme.GlobalIconStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.availablePlayerThemes
import com.example.ui.theme.applyPlayerTheme
import kotlinx.coroutines.delay
import com.example.ui.theme.activePlayerPalette
import com.example.ui.theme.glassGradient

private enum class OverlayScreen { NONE, RADIO, VIDEOS, MUSIC, DOWNLOADS }

private enum class AppTab(
    val label: String,
    val iconRes: Int,      // SVG vector — لونه بيتغير مع الثيم
    val icon3dRes: Int     // 3D PNG
) {
    HOME("الرئيسية", R.drawable.ic_nav_home, R.drawable.nav_home_3d),
    CHANNELS("القنوات", R.drawable.ic_nav_channels, R.drawable.nav_channels_3d),
    LIBRARY("المكتبة", R.drawable.ic_nav_films, R.drawable.nav_films_3d),
    DEVICE("جهازي", R.drawable.ic_nav_downloads, R.drawable.nav_downloads_3d),
    SETTINGS("الإعدادات", R.drawable.ic_nav_settings, R.drawable.nav_settings_3d)
}

@Composable
fun MainScreen(
    controller: YouseifPlayerController,
    isInPipMode: Boolean = false,
    channels: List<PlaylistItem>,
    films: List<PlaylistItem>,
    series: List<PlaylistItem> = emptyList(),
    cartoons: List<PlaylistItem>,
    recentHistory: List<com.example.data.HistoryItem> = emptyList(),
    selectedTheme: String = "AURORA FLOW",
    onThemeSelected: (String) -> Unit = {},
    onCustomColor: (Long) -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit,
    onContentSelected: (PlaylistItem) -> Unit = {},
    onAddCustomChannel: (PlaylistItem) -> Unit,
    onImportM3U: (String) -> Unit,
    onImportFromUrl: (String) -> Unit = {},
    onImportZipBytes: (ByteArray) -> Unit = {},
    onImportPortalCode: (String) -> Unit = {},
    onDeleteChannel: (PlaylistItem) -> Unit,
    onResetChannels: () -> Unit,
    onClearCache: () -> Unit,
    onRefreshCatalog: () -> Unit = {},
    onClearChannelCache: () -> Unit = {},
    onHistoryItem: (PlaylistItem) -> Unit = {},
    onHistoryUrl: (String, String, String) -> Unit = { _, _, _ -> },
    onClearHistory: () -> Unit = {},
    onSubtitleUrlSaved: (String) -> Unit = {},
    onSubtitleFileSelected: (android.net.Uri) -> Unit = {},
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val p = activePlayerPalette()
    val vm = viewModel
    var currentTab by remember { mutableStateOf(runCatching { AppTab.valueOf(UserSettings.getString("main_tab", AppTab.HOME.name)) }.getOrDefault(AppTab.HOME)) }
    LaunchedEffect(currentTab) {
        if (currentTab == AppTab.CHANNELS) {
            try { vm?.ensureLiveChannelsLoaded() } catch (_: Throwable) {}
        }
    }
    var overlayScreen by remember { mutableStateOf(runCatching { OverlayScreen.valueOf(UserSettings.getString("overlay_screen", OverlayScreen.NONE.name)) }.getOrDefault(OverlayScreen.NONE)) }
    var showQuickDrawer by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showMediaDetail by remember { mutableStateOf(false) }
    var autoThemeEnabled by remember { mutableStateOf(false) } // مش تلقائي إلا بعد دوسة مطوّلة
    var autoThemeSpeedMs by remember { mutableStateOf(UserSettings.getInt("auto_theme_speed_ms", 2500).coerceIn(100, 60000)) }
    var offlineMode by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    val currentPlayingChannel by controller.currentChannel.collectAsState()
    val tabHistory = remember { mutableStateListOf<AppTab>() }
    val allThemes = remember { availablePlayerThemes() }
    // Remember last library tab (FILMS / CHANNELS) so back from player returns there
    var lastLibraryTab by remember {
        mutableStateOf(
            runCatching { AppTab.valueOf(UserSettings.getString("last_library_tab", AppTab.LIBRARY.name)) }
                .getOrDefault(AppTab.LIBRARY)
        )
    }

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
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Throwable) {}
        val title = documentTitle(uri)
        controller.playUrl(uri.toString(), title = title, isLive = false)
        onHistoryUrl(title, uri.toString(), "VIDEO")
        currentTab = AppTab.HOME
        showQuickDrawer = false
        overlayScreen = OverlayScreen.NONE
    }

    fun navigateTo(tab: AppTab) {
        if (tab == AppTab.CHANNELS) {
            try { vm?.ensureLiveChannelsLoaded() } catch (_: Throwable) {}
        }
        if (currentTab != tab) {
            tabHistory.add(currentTab)
            currentTab = tab
        }
        if (tab == AppTab.LIBRARY || tab == AppTab.CHANNELS) {
            lastLibraryTab = tab
            UserSettings.putString("last_library_tab", tab.name)
        }
        UserSettings.putString("main_tab", tab.name)
        overlayScreen = OverlayScreen.NONE
        showQuickDrawer = false
    }

    fun openOverlay(screen: OverlayScreen) {
        overlayScreen = screen
        showQuickDrawer = false
    }

    fun goBack() {
        if (showQuickDrawer) {
            showQuickDrawer = false
            return
        }
        if (overlayScreen != OverlayScreen.NONE) {
            overlayScreen = OverlayScreen.NONE
            return
        }
        if (showMediaDetail) {
            showMediaDetail = false
            return
        }
        currentTab = if (tabHistory.isNotEmpty()) tabHistory.removeAt(tabHistory.lastIndex) else AppTab.HOME
    }

    LaunchedEffect(currentTab, overlayScreen, autoThemeEnabled, autoThemeSpeedMs) {
        try {
            UserSettings.putString("main_tab", currentTab.name)
            UserSettings.putString("overlay_screen", overlayScreen.name)
            UserSettings.putBoolean("auto_theme_enabled", autoThemeEnabled)
            UserSettings.offlineMode = offlineMode
            UserSettings.putInt("auto_theme_speed_ms", autoThemeSpeedMs)
        } catch (_: Throwable) {}
    }

    LaunchedEffect(autoThemeEnabled, autoThemeSpeedMs) {
        if (!autoThemeEnabled) return@LaunchedEffect
        if (allThemes.isEmpty()) return@LaunchedEffect
        var i = allThemes.indexOfFirst { it.name.equals(selectedTheme, true) }
        if (i < 0) i = allThemes.indexOfFirst { it.name.equals(activePlayerPalette().name, true) }
        if (i < 0) i = 0
        while (autoThemeEnabled) {
            i = (i + 1) % allThemes.size
            val name = allThemes[i].name
            try {
                onThemeSelected(name)
            } catch (_: Throwable) {}
            delay(autoThemeSpeedMs.toLong())
        }
    }

    fun toggleFullscreenMode(fullscreen: Boolean) {
        isFullscreen = fullscreen
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (fullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    BackHandler(enabled = isFullscreen || showQuickDrawer || overlayScreen != OverlayScreen.NONE || showMediaDetail || currentTab != AppTab.HOME) {
        when {
            isFullscreen -> toggleFullscreenMode(false)
            showQuickDrawer -> showQuickDrawer = false
            overlayScreen != OverlayScreen.NONE -> overlayScreen = OverlayScreen.NONE
            showMediaDetail -> showMediaDetail = false
            else -> goBack()
        }
    }

    // Fasel HD explorer state (driven by the view model when available)
    val faselNetworks = vm?.networks?.collectAsState()?.value ?: emptyList()
    val faselGenres = vm?.genres?.collectAsState()?.value ?: emptyList()
    val moviesPage = vm?.moviesPage?.collectAsState()?.value
    val seriesPage = vm?.seriesPage?.collectAsState()?.value
    val animesPage = vm?.animesPage?.collectAsState()?.value
    val networkPage = vm?.networkPage?.collectAsState()?.value
    val genrePage = vm?.genrePage?.collectAsState()?.value
    val searchResults = vm?.searchResults?.collectAsState()?.value
    val hikayeMoviesPage = vm?.hikayeMoviesPage?.collectAsState()?.value
    val hikayeSeriesPage = vm?.hikayeSeriesPage?.collectAsState()?.value
    val hikayeSearchResults = vm?.hikayeSearchResults?.collectAsState()?.value
    val countryLiveItems = vm?.countryLive?.collectAsState()?.value ?: emptyList()
    val hideImages = vm?.hideImages?.collectAsState()?.value ?: false
    val gateways = vm?.gateways?.collectAsState()?.value ?: emptyList()
    val channelHealth = vm?.channelHealth?.collectAsState()?.value ?: emptyMap()
    val detailState = vm?.detailState?.collectAsState()?.value ?: MainViewModel.DetailState()

    // سجل الأفلام/المسلسلات تحت: لما الضغط على عنصر من السجل يفتح تفاصيل العمل (openMedia)
    // ما كانش فيه أي حاجة بتعرض شاشة التفاصيل، فكان بيبان إن الزر مش بيعمل حاجة.
    LaunchedEffect(detailState.mediaId, detailState.title) {
        if (detailState.mediaId > 0 && detailState.title.isNotBlank()) {
            lastLibraryTab = AppTab.LIBRARY
            UserSettings.putString("last_library_tab", AppTab.LIBRARY.name)
            UserSettings.putString("main_tab", AppTab.LIBRARY.name)
            showMediaDetail = true
            overlayScreen = OverlayScreen.NONE
            currentTab = AppTab.LIBRARY
        }
    }

    // تقليب القنوات/الأفلام/الأغاني من أزرار الريموت أو الكيبورد (KeyRouter ← MainActivity)
    androidx.compose.runtime.DisposableEffect(
        currentPlayingChannel, channels, films, series, cartoons, isFullscreen,
        controller.localQueue.value, controller.queueIndex.value, overlayScreen, currentTab
    ) {
        com.example.player.KeyRouter.dpadEnabled = isFullscreen || controller.currentChannel.value != null
        com.example.player.KeyRouter.handler = handler@{ dir ->
            try {
                val cur = controller.currentChannel.value
                // First: respect explicit local queue (songs)
                if (controller.localQueue.value.isNotEmpty()) {
                    val q = controller.localQueue.value
                    val idx = q.indexOfFirst { it.id == cur?.id }
                    val tg = when {
                        idx < 0 -> if (dir >= 0) 0 else q.lastIndex
                        dir >= 0 -> (idx + 1) % q.size
                        else -> if (idx == 0) q.lastIndex else idx - 1
                    }
                    controller.playUrl(q[tg].url, q[tg].name, q[tg].isLive)
                    controller.setCurrentContent(q[tg])
                    return@handler true
                }
                // Then: respect radio overlay
                if (overlayScreen == OverlayScreen.RADIO) {
                    val cats = RadioCatalog.load(context)
                    val curCat = UserSettings.getString("radio_expanded", "")
                    val category = cats.firstOrNull { it.type == curCat } ?: cats.firstOrNull()
                    val radio = category?.items.orEmpty()
                    if (radio.isEmpty()) return@handler false
                    val idx = radio.indexOfFirst { it.url == cur?.url }
                    val tg = when {
                        idx < 0 -> if (dir >= 0) 0 else radio.lastIndex
                        dir >= 0 -> (idx + 1) % radio.size
                        else -> if (idx == 0) radio.lastIndex else idx - 1
                    }
                    val it = radio[tg]
                    controller.playUrl(it.url, it.name, isLive = true)
                    controller.setCurrentContent(PlaylistItem(id="radio_${it.url.hashCode()}", channelNumber=tg, name=it.name, url=it.url, group="RADIO", isLive=true))
                    onHistoryUrl(it.name, it.url, "RADIO")
                    return@handler true
                }
                // Pick the matching filtered list based on the CURRENT tab/overlay
                val kindGroup = cur?.group.orEmpty()
                val isLive = cur?.isLive == true || currentTab == AppTab.CHANNELS
                val baseList = when {
                    isLive -> channels
                    kindGroup.contains("FILMS", true) || kindGroup.startsWith("FILM", true) -> films
                    kindGroup.contains("SERIES", true) -> series
                    kindGroup.contains("ANIME", true) || kindGroup.contains("CARTOON", true) -> cartoons
                    currentTab == AppTab.LIBRARY -> films + series + cartoons
                    else -> channels
                }
                // Narrow to the same category/group if available
                val list = if (kindGroup.isNotBlank() && baseList.any { it.group.equals(kindGroup, true) }) {
                    baseList.filter { it.group.equals(kindGroup, true) || it.group.startsWith(kindGroup.substringBefore('/').trim(), true) }
                } else baseList
                if (list.isEmpty()) return@handler false
                val idx = list.indexOfFirst { it.id == cur?.id }
                val tg = when {
                    idx < 0 -> if (dir >= 0) 0 else list.lastIndex
                    dir >= 0 -> (idx + 1) % list.size
                    else -> if (idx == 0) list.lastIndex else idx - 1
                }
                val item = list[tg]
                controller.playUrl(item.url, item.name, item.isLive)
                controller.setCurrentContent(item)
                onHistoryItem(item)
                true
            } catch (_: Throwable) {
                false
            }
        }
        onDispose {
            com.example.player.KeyRouter.handler = null
            com.example.player.KeyRouter.dpadEnabled = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        // Picture-in-Picture: ONLY the video surface — no bottom nav / tabs / chrome
        if (isInPipMode) {
            Box(modifier = Modifier.fillMaxSize().background(AmoledBlack)) {
                VideoPlayerView(
                    controller = controller,
                    isFullscreen = true,
                    isInPipMode = true,
                    onToggleFullscreen = { },
                    onPreviousChannel = { },
                    onNextChannel = { },
                    onOpenPlaylist = { },
                    suggestionItems = emptyList(),
                    onPlaySuggested = { },
                    allLiveChannels = channels.filter { it.isLive },
                    onSwitchChannelServer = { ch ->
                        onContentSelected(ch)
                        onHistoryItem(ch)
                    }
                )
            }
        } else if (isFullscreen) {
            Box(modifier = Modifier.fillMaxSize().background(AmoledBlack)) {
                VideoPlayerView(
                    controller = controller,
                    isFullscreen = true,
                    onToggleFullscreen = { toggleFullscreenMode(false) },
                    onPreviousChannel = {
                        // 1) سياق التشغيل الحالي (قناة/فيلم/جودة/أغنية)  2) قائمة الأغاني  3) قنوات التطبيق
                        if (!controller.previousInContext()) {
                            if (controller.localQueue.value.isNotEmpty()) {
                                controller.playPreviousInQueue()
                            } else {
                                val i = channels.indexOfFirst { it.id == currentPlayingChannel?.id }
                                if (i > 0) {
                                    onContentSelected(channels[i - 1])
                                    onHistoryItem(channels[i - 1])
                                }
                            }
                        }
                    },
                    onNextChannel = {
                        if (!controller.nextInContext()) {
                            if (controller.localQueue.value.isNotEmpty()) {
                                controller.playNextInQueue()
                            } else {
                                val i = channels.indexOfFirst { it.id == currentPlayingChannel?.id }
                                if (i != -1 && i < channels.size - 1) {
                                    onContentSelected(channels[i + 1])
                                    onHistoryItem(channels[i + 1])
                                }
                            }
                        }
                    },
                    onOpenPlaylist = { },
                    suggestionItems = run {
                        val cur = currentPlayingChannel
                        val all = (channels + films + series + cartoons).distinctBy { it.id }
                        if (cur == null) all
                        else {
                            val sameGroup = all.filter {
                                it.id != cur.id && it.group.equals(cur.group, true)
                            }
                            val sameLive = all.filter {
                                it.id != cur.id && it.isLive == cur.isLive && it.group != cur.group
                            }
                            val rest = all.filter { it.id != cur.id }
                            (sameGroup + sameLive + rest)
                                .distinctBy { it.id }
                                .sortedByDescending { it.logoUrl.isNotBlank() }
                        }
                    },
                    onPlaySuggested = { item ->
                        onContentSelected(item)
                        onHistoryItem(item)
                    },
                    onOpenSettings = { toggleFullscreenMode(false); currentTab = AppTab.SETTINGS },
                    onToggleOrientation = {
                        val act = context as? android.app.Activity
                        if (act != null) {
                            val current = act.requestedOrientation
                            act.requestedOrientation = when (current) {
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ->
                                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }
                        }
                    },
                    allLiveChannels = channels.filter { it.isLive },
                    onSwitchChannelServer = { ch ->
                        onContentSelected(ch)
                        onHistoryItem(ch)
                    }
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = AmoledBlack,
                bottomBar = {
                    BottomNavPill(currentTab = currentTab, onTab = { navigateTo(it) })
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    when (overlayScreen) {
                        OverlayScreen.RADIO -> RadioScreen(
                            onBack = { overlayScreen = OverlayScreen.NONE },
                            onPlayStation = { title, url ->
                                // تشغيل داخل الراديو بدون الخروج للمشغل الرئيسي
                                val radioItem = PlaylistItem(
                                    id = "radio_${url.hashCode()}", channelNumber = 0,
                                    name = title, url = url, group = "RADIO", isLive = true
                                )
                                controller.setCurrentContent(radioItem)
                                controller.playUrl(url, title = title, isLive = true)
                                onHistoryUrl(title, url, "RADIO")
                            },
                            controller = controller
                        )
                        OverlayScreen.VIDEOS -> LocalMediaScreen(
                            forAudio = false,
                            controller = controller,
                            onBack = { overlayScreen = OverlayScreen.NONE },
                            onHistoryUrl = onHistoryUrl
                        )
                        OverlayScreen.MUSIC -> LocalMediaScreen(
                            forAudio = true,
                            controller = controller,
                            onBack = { overlayScreen = OverlayScreen.NONE },
                            onHistoryUrl = onHistoryUrl
                        )
                        OverlayScreen.DOWNLOADS -> DownloadsScreen(
                            onOpenChannels = { overlayScreen = OverlayScreen.NONE },
                            onSearchFilms = { q -> vm?.searchFasel(q) },
                            searchResults = searchResults,
                            onOpenMediaForDownload = { m ->
                                vm?.openMedia(m, m.type)
                                showMediaDetail = true
                                overlayScreen = OverlayScreen.NONE
                                navigateTo(AppTab.LIBRARY)
                            }
                        )
                        OverlayScreen.NONE -> when (currentTab) {
                        AppTab.HOME -> HomeScreen(
                            controller = controller,
                            recentChannels = channels,
                            recentHistory = recentHistory,
                            onChannelSelected = { ch ->
                                // سياق التقليب: قنوات الرئيسية
                                controller.setContextQueue(channels, ch.id)
                                controller.playChannel(ch); onHistoryItem(ch)
                            },
                            onOpenMenu = { showQuickDrawer = true },
                            onNavigateToChannels = { navigateTo(AppTab.CHANNELS) },
                            onNavigateToSettings = { navigateTo(AppTab.SETTINGS) },
                            onNavigateToDownloads = { },
                            offlineMode = offlineMode,
                            onToggleOffline = {
                                offlineMode = !offlineMode
                                UserSettings.offlineMode = offlineMode
                                if (!offlineMode) {
                                    try { vm?.ensureLiveChannelsLoaded() } catch (_: Throwable) {}
                                }
                            },
                            autoThemeEnabled = autoThemeEnabled,
                            onToggleAutoTheme = { autoThemeEnabled = !autoThemeEnabled },
                            onOpenThemePicker = { showThemePicker = true },
                            onToggleFullscreen = { toggleFullscreenMode(true) },
                            onImportM3U = onImportM3U,
                            onHistoryUrl = onHistoryUrl,
                            onReplayHistory = { title, url, group ->
                                // One tap → play immediately (stay on Home player)
                                vm?.replayHistory(title, url, group)
                                    ?: controller.playUrl(
                                        url,
                                        title,
                                        group.contains("LIVE", ignoreCase = true) && !group.startsWith("FILM")
                                    )
                            },
                            onClearHistory = onClearHistory,
                            onToggleFavorite = onToggleFavorite,
                            onDeleteHistoryItem = { item -> vm?.deleteHistory(item) }
                        )
                        AppTab.CHANNELS -> ChannelsScreen(
                            channels = channels,
                            currentPlayingChannel = currentPlayingChannel,
                            controller = controller,
                            onExpandPlayer = {
                                lastLibraryTab = AppTab.CHANNELS
                                UserSettings.putString("last_library_tab", AppTab.CHANNELS.name)
                                currentTab = AppTab.HOME
                                UserSettings.putString("main_tab", AppTab.HOME.name)
                            },
                            channelHealth = channelHealth,
                            onCheckChannelStatus = { vm?.ensureChannelStatus(it) },
                            onProbeAllChannels = { list -> vm?.probeAllChannels(list) },
                            onBack = { goBack() },
                            onChannelSelected = { ch ->
                                // سياق التقليب: القنوات المعروضة حاليًا (قناة/سيرفر/مجموعة)
                                controller.setContextQueue(channels, ch.id)
                                // تشغيل في المكان + ميني بلاير — من غير خروج من صفحة القنوات
                                onContentSelected(ch)
                                onHistoryItem(ch)
                                lastLibraryTab = AppTab.CHANNELS
                                UserSettings.putString("last_library_tab", AppTab.CHANNELS.name)
                            },
                            onToggleFavorite = onToggleFavorite,
                            onAddCustomChannel = onAddCustomChannel,
                            onImportM3U = { text ->
                                viewModel?.importPlaylistSmart(text, com.example.ui.MainViewModel.ImportTarget.CHANNELS)
                                    ?: onImportM3U(text)
                            },
                            onImportRadio = { text ->
                                viewModel?.importPlaylistSmart(text, com.example.ui.MainViewModel.ImportTarget.RADIO)
                                    ?: onImportM3U(text)
                            },
                            onImportZipBytes = onImportZipBytes,
                            onImportSmart = { text, kind, pkg ->
                                val target = when (kind) {
                                    "RADIO" -> com.example.ui.MainViewModel.ImportTarget.RADIO
                                    "FILMS" -> com.example.ui.MainViewModel.ImportTarget.FILMS
                                    "SERIES" -> com.example.ui.MainViewModel.ImportTarget.SERIES
                                    "ANIME" -> com.example.ui.MainViewModel.ImportTarget.ANIME
                                    else -> com.example.ui.MainViewModel.ImportTarget.CHANNELS
                                }
                                viewModel?.importPlaylistSmart(text, target, pkg)
                                    ?: onImportM3U(text)
                            },
                            lastImportSummary = vm?.lastImport?.collectAsState()?.value,
                            onDeleteChannel = onDeleteChannel,
                            onDeleteChannels = { ids -> viewModel?.deleteChannelsByIds(ids) },
                            onDeleteAllCustom = { viewModel?.deleteAllCustomChannels() },
                            onNavigateToFilms = { navigateTo(AppTab.LIBRARY) },
                            onOpenRadio = { openOverlay(OverlayScreen.RADIO) },
                            onOpenDevice = { uri, title ->
                                controller.playUrl(uri.toString(), title, false)
                                onHistoryUrl(title, uri.toString(), "VIDEO")
                                currentTab = AppTab.HOME
                            },
                            onRefreshCatalog = onRefreshCatalog
                        )
                        AppTab.LIBRARY -> {
                            // Classic: detail REPLACES list (no page-on-page). Scroll restored via UserSettings.
                            // شاشة المكتبة الموحدة تضم الأفلام + المسلسلات + الأنمي في 3 sub-tabs حقيقية
                            if (vm != null && showMediaDetail) {
                                MediaDetailScreen(
                                    title = detailState.title,
                                    detail = detailState.detail,
                                    series = detailState.series,
                                    loading = detailState.loading,
                                    error = detailState.error,
                                    hideImages = hideImages,
                                    qualities = detailState.qualities,
                                    qualitiesLoading = detailState.qualitiesLoading,
                                    qualitiesLabel = detailState.qualitiesLabel,
                                    webEmbedFallback = detailState.webEmbedFallback,
                                    onBack = { showMediaDetail = false },
                                    onCloseQualities = { vm.closeQualities() },
                                    onResolveQualities = { vm.resolveQualities(it) },
                                    onPlayQuality = { q ->
                                        vm.playQuality(q)
                                        lastLibraryTab = AppTab.LIBRARY
                                        UserSettings.putString("last_library_tab", AppTab.LIBRARY.name)
                                    },
                                    onWebEmbed = {
                                        vm.playWebEmbedFallback()
                                        lastLibraryTab = AppTab.LIBRARY
                                        UserSettings.putString("last_library_tab", AppTab.LIBRARY.name)
                                    },
                                    onOpenRelated = { m -> vm.openRelated(m) }
                                )
                            } else {
                                FilmsScreen(
                                    onBack = { goBack() },
                                    networks = faselNetworks,
                                    genres = faselGenres,
                                    moviesPage = moviesPage,
                                    seriesPage = seriesPage,
                                    animesPage = animesPage,
                                    seriesGenrePage = vm?.seriesGenrePage?.collectAsState()?.value,
                                    animesGenrePage = vm?.animesGenrePage?.collectAsState()?.value,
                                    networkPage = networkPage,
                                    genrePage = genrePage,
                                    searchResults = searchResults,
                                    hikayeMoviesPage = hikayeMoviesPage,
                                    hikayeSeriesPage = hikayeSeriesPage,
                                    hikayeSearchResults = hikayeSearchResults,
                                    currentNetwork = null,
                                    channels = channels,
                                    // Unified library input: Firebase series must reach the series tab too.
                                    customFilms = (films + series + channels.filter { !it.isLive && !it.isCustom })
                                        .filter { !it.id.startsWith("faselhd_") }.distinctBy { it.id },
                                    countryItems = countryLiveItems,
                                    gateways = gateways,
                                    hideImages = hideImages,
                                    onHideImagesChange = { vm?.setHideImages(it) },
                                    onDeletePlaylistItem = { item ->
                                    viewModel?.deleteChannel(item)
                                },
                                onImportVod = { text, sec, pkg ->
                                    val target = when (sec) {
                                        "SERIES" -> com.example.ui.MainViewModel.ImportTarget.SERIES
                                        "ANIME" -> com.example.ui.MainViewModel.ImportTarget.ANIME
                                        else -> com.example.ui.MainViewModel.ImportTarget.FILMS
                                    }
                                    viewModel?.importPlaylistSmart(text, target, pkg)
                                },
                                onEnsurePortal = { vm?.ensurePortalImported() },
                                    onLoadMovies = { vm?.loadMovies(it) },
                                    onLoadSeries = { vm?.loadSeries(it) },
                                    onLoadAnimes = { vm?.loadAnimes(it) },
                                    onLoadNetwork = { id, pg -> vm?.loadNetwork(id, pg) },
                                    onLoadGenre = { id, pg -> vm?.loadGenre(id, pg) },
                                    onLoadFaselKindGenre = { k, id, pg -> vm?.loadFaselGenrePage(k, id, pg) },
                                    onLoadCountry = { id -> vm?.loadCountry(id) },
                                    onSearch = { vm?.searchFasel(it) },
                                    onLoadHikayeMovies = { pg, mode -> vm?.loadHikayeMovies(pg, mode) },
                                    onLoadHikayeSeries = { pg, mode -> vm?.loadHikayeSeries(pg, mode) },
                                    onSearchHikaye = { vm?.searchHikaye(it) },
                                    onOpenMedia = { m, kind ->
                                        vm?.openMedia(m, kind)
                                        showMediaDetail = true
                                    },
                                    onOpenHikayeMedia = { m ->
                                        vm?.openHikayeMedia(m)
                                        showMediaDetail = true
                                    },
                                    onOpenPlaylistItem = { item ->
                                        val u = item.url.trim().lowercase()
                                        val isSeriesGroup = item.group.startsWith("SERIES:") || item.group.startsWith("CARTOONS:")
                                        when {
                                            isSeriesGroup && u.contains("alooytv") && u.contains("/watch/") -> {
                                                vm?.openAlooySeries(item)
                                                showMediaDetail = true
                                            }
                                            isSeriesGroup && u.contains("alooytv") -> {
                                                // مجلد تصنيف — يجيب الأعمال الحقيقية ببوسترات
                                                vm?.openAlooyGenre(item)
                                            }
                                            u.startsWith("golive://") -> {
                                                vm?.openGoLiveItem(item)
                                                showMediaDetail = true
                                            }
                                            // ستريم حقيقي فقط — ممنوع فتح صفحات ويب فاضية في المشغّل
                                            u.contains(".m3u8") || u.contains(".mpd") || u.contains(".mp4") ||
                                                u.contains(".mkv") || u.contains(".ts") ||
                                                u.startsWith("rtmp") || u.startsWith("rtsp") -> {
                                                onContentSelected(item); onHistoryItem(item)
                                                lastLibraryTab = AppTab.LIBRARY
                                                if (tabHistory.lastOrNull() != AppTab.LIBRARY) tabHistory.add(AppTab.LIBRARY)
                                                currentTab = AppTab.HOME
                                            }
                                            // غير كده: عنصر مش صالح للتشغيل (مجلد/ويب بدون ميديا) — نتجاهله
                                            else -> { /* skip web-only shells like خليكي عربي */ }
                                        }
                                    }
                                )
                            }
                        }

                        AppTab.DEVICE -> DeviceHubScreen(
                            controller = controller,
                            onBack = { goBack() },
                            onHistoryUrl = onHistoryUrl
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            selectedTheme = selectedTheme,
                            onThemeSelected = onThemeSelected,
                            onCustomColor = { argb ->
                                onCustomColor(argb)
                            },
                            onResetChannels = onResetChannels,
                            onClearCache = onClearCache,
                            onRefreshCatalog = onRefreshCatalog,
                            onClearChannelCache = onClearChannelCache,
                            onNavigateToChannels = { goBack() },
                            onClearHistory = onClearHistory,
                            onSubtitleUrlSaved = onSubtitleUrlSaved,
                            onSubtitleFileSelected = onSubtitleFileSelected,
                            onImportM3U = onImportM3U,
                            onImportFromUrl = onImportFromUrl,
                            onImportZipBytes = onImportZipBytes,
                            onImportPortalCode = onImportPortalCode,
                            gateways = gateways,
                            onSetGateway = { id, en -> vm?.setGatewayEnabled(id, en) },
                            onSetAllGateways = { en -> vm?.setAllGateways(en) },
                            hideImages = hideImages,
                            onHideImagesChange = { vm?.setHideImages(it) },
                            onOpenMenu = { showQuickDrawer = true },
                            controller = controller
                        )
                    }

                    }

                    if (showQuickDrawer) {
                        QuickBarsDrawer(
                            autoThemeEnabled = autoThemeEnabled,
                            autoThemeSpeedMs = autoThemeSpeedMs,
                            onDismiss = { showQuickDrawer = false },
                            onToggleAutoTheme = { autoThemeEnabled = !autoThemeEnabled },
                    onThemeSpeedChange = {
                        autoThemeSpeedMs = it.coerceIn(100, 60000)
                        UserSettings.putInt("auto_theme_speed_ms", autoThemeSpeedMs)
                    },
                            onOpenColors = { showQuickDrawer = false; showThemePicker = true },
                            onOpenRadio = {
                                UserSettings.putString("radio_expanded", "modern")
                                openOverlay(OverlayScreen.RADIO)
                            },
                            onOpenVideos = { openOverlay(OverlayScreen.VIDEOS) },
                            onOpenMusic = { openOverlay(OverlayScreen.MUSIC) },
                            onOpenFavorites = {
                                UserSettings.putString("channels_group", "FAVORITES")
                                UserSettings.putString("channels_source", "ALL")
                                UserSettings.putString("channels_package", "")
                                navigateTo(AppTab.CHANNELS)
                            },
                            onOpenDevice = {
                                devicePicker.launch(arrayOf("audio/*", "video/*", "application/x-mpegURL", "application/vnd.apple.mpegurl", "*/*"))
                            },
                            onOpenDownloads = { openOverlay(OverlayScreen.DOWNLOADS) }
                        )
                    }

                    if (showThemePicker) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showThemePicker = false },
                            title = { Text("ألوان الثيم التلقائي", color = TextPrimary) },
                            text = {
                                Column(
                                    modifier = Modifier.height(430.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("الألوان — اختر لونًا واحدًا مباشرة من القائمة، أو اكتب كود HEX.", color = TextMuted, fontSize = 12.sp)
                                    Text("كل لون مستقل ويمكن تغييره بدون إيقاف باقي المزايا", color = activePlayerPalette().accentGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    availablePlayerThemes().chunked(3).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            row.forEach { theme ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(theme.accent)
                                                        .border(1.dp, theme.accentGlow, RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            onThemeSelected(theme.name)
                                                            showThemePicker = false
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(theme.name.take(8), color = Color.White, fontSize = 9.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                    var hex by remember { mutableStateOf("") }
                                    androidx.compose.material3.OutlinedTextField(
                                        value = hex,
                                        onValueChange = { hex = it },
                                        placeholder = { Text("#RRGGBB", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            val clean = hex.trim().removePrefix("#")
                                            if (clean.length == 6) {
                                                val argb = ("FF$clean").toLongOrNull(16)
                                                if (argb != null) {
                                                    com.example.ui.theme.applyCustomAccent(argb)
                                                    onCustomColor(argb)
                                                    showThemePicker = false
                                                }
                                            }
                                        }
                                    ) { Text("تطبيق اللون المخصص") }
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = { showThemePicker = false }) {
                                    Text("إغلاق", color = activePlayerPalette().accentGlow)
                                }
                            },
                            containerColor = DarkSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating glass pill bottom nav — selected tab now tracks the active theme:
 * gradient fill + accent glow border + highlighted label, so switching themes
 * visibly recolors the bottom bar (the reported "الأزرار مش بتغير لونها" bug).
 */
@Composable
private fun BottomNavPill(currentTab: AppTab, onTab: (AppTab) -> Unit) {
    val p = activePlayerPalette()
    // Flat/Glow = SVG vector يتبع الثيم | Bold = نفس أيقونة الـ3D الأصلية لكن بتلوين ديناميكي.
    // التلوين يحافظ على الإضاءة والظلال الأصلية عشان شكل الـ3D ما يضيعش.
    val use3d = GlobalIconStyle.equals("Bold", ignoreCase = true)

    fun dynamic3dColorFilter(accent: Color): ColorFilter {
        // تحويل الأيقونة إلى خريطة إضاءة ثم إسقاطها على لون الثيم.
        // المناطق السوداء/الظلال تظل داكنة، والهايلايت يأخذ لون الـAccent.
        val r = accent.red.coerceIn(0f, 1f)
        val g = accent.green.coerceIn(0f, 1f)
        val b = accent.blue.coerceIn(0f, 1f)
        val matrix = ColorMatrix(floatArrayOf(
            0.2126f * r, 0.7152f * r, 0.0722f * r, 0f, 0f,
            0.2126f * g, 0.7152f * g, 0.0722f * g, 0f, 0f,
            0.2126f * b, 0.7152f * b, 0.0722f * b, 0f, 0f,
            0f,          0f,          0f,          1f, 0f
        ))
        return ColorFilter.colorMatrix(matrix)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .navigationBarsPadding()
            .clip(RoundedCornerShape(22.dp))
            .background(glassGradient(forDark = true))
            .border(1.dp, p.borderStrong.copy(alpha = .70f), RoundedCornerShape(22.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val tint = if (isSelected) p.accentGlow else Color.White.copy(alpha = 0.62f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected)
                                Brush.linearGradient(
                                    listOf(p.accentContainer.copy(alpha = .92f), p.accentDark.copy(alpha = .48f))
                                )
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) p.accentGlow.copy(alpha = .90f) else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onTab(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = if (use3d) tab.icon3dRes else tab.iconRes),
                            contentDescription = tab.label,
                            modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                            contentScale = ContentScale.Fit,
                            // الـSVG يتبع لون الثيم، والـ3D يستخدم ColorMatrix يحافظ على العمق
                            // مع تحويل اللون الأصلي للـAccent الحالي.
                            colorFilter = if (use3d) dynamic3dColorFilter(p.accentGlow) else ColorFilter.tint(tint)
                        )
                        Text(
                            tab.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) p.accentGlow else p.muted,
                            letterSpacing = 0.15.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun QuickBarsDrawer(
    autoThemeEnabled: Boolean,
    autoThemeSpeedMs: Int,
    onDismiss: () -> Unit,
    onToggleAutoTheme: () -> Unit,
    onThemeSpeedChange: (Int) -> Unit,
    onOpenRadio: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenColors: () -> Unit = {},
) {
    val p = activePlayerPalette()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                .background(DarkCardBg.copy(alpha = 0.98f))
                .border(1.dp, CrimsonBorder.copy(alpha = 0.65f), RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp)
                // مسافة سفلية عشان ما يتداخلش مع شريط التنقل السفلي
                .padding(bottom = 72.dp)
                .clickable(onClick = { }),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("القائمة", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("إعدادات سريعة", color = TextMuted, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(p.accentContainer.copy(alpha = 0.55f))
                        .border(1.dp, p.accentGlow.copy(alpha = 0.55f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = p.accentGlow, modifier = Modifier.size(18.dp))
                }
            }

            Text("وصول سريع", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                QuickMenuCard("راديو", p.accentGlow, p.secondary, Icons.Default.Radio, onOpenRadio)
                QuickMenuCard("فيديو", p.secondaryGlow, p.accent, Icons.Default.SmartDisplay, onOpenVideos)
                QuickMenuCard("موسيقى", p.accentGlow, p.secondary, Icons.Default.MusicNote, onOpenMusic)
                QuickMenuCard("المفضلة", NeonRedGlow, p.accent, Icons.Default.FavoriteBorder, onOpenFavorites)
                QuickMenuCard("جهازي", p.secondaryGlow, p.accent, Icons.Default.FolderOpen, onOpenDevice)
                QuickMenuCard("تحميل", p.accentGlow, p.secondary, Icons.Default.Download, onOpenDownloads)
                QuickMenuCard("الألوان", p.accentGlow, p.secondaryGlow, Icons.Default.Palette, onOpenColors)
            }
            Text("اختر لون الثيم مباشرة", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.theme.availablePlayerThemes().forEach { theme ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(theme.accent)
                            .border(2.dp, theme.accentGlow, CircleShape)
                            .clickable {
                                try {
                                    ctx.getSharedPreferences("youseif_visual", 0)
                                        .edit().putString("theme", theme.name).apply()
                                    applyPlayerTheme(theme.name)
                                } catch (_: Throwable) {}
                            }
                    )
                }
            }
            // ثيم تلقائي: يعمل باستمرار، والسرعة محفوظة بين مرات التشغيل.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.accentContainer.copy(alpha = 0.22f))
                    .border(1.dp, p.accentGlow.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ثيم تلقائي", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (autoThemeEnabled) "يغير الثيم باستمرار" else "متوقف — فعّله للتغيير المستمر",
                        color = TextMuted, fontSize = 10.sp
                    )
                }
                Switch(checked = autoThemeEnabled, onCheckedChange = { onToggleAutoTheme() })
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface.copy(alpha = 0.88f))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سرعة تغيير الثيم", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(if (autoThemeSpeedMs < 1000) "${autoThemeSpeedMs} ملث" else "${(autoThemeSpeedMs / 1000f).let { String.format("%.1f", it) }} ث", color = p.accentGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = autoThemeSpeedMs.toFloat().coerceIn(500f, 60000f),
                    onValueChange = { onThemeSpeedChange(it.toInt().coerceIn(500, 60000)) },
                    valueRange = 500f..60000f,
                    steps = 0
                )
            }
        }
    }
}

@Composable
private fun QuickMenuCard(
    title: String,
    accent: Color,
    secondary: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface.copy(alpha = 0.92f))
            .border(1.dp, CrimsonBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("‹", color = TextMuted, fontSize = 18.sp)
    }
}

/** جهازي — صوت / فيديو (التبويبات جوّه الشاشة زي المكتبة) */
@Composable
private fun DeviceHubScreen(
    controller: YouseifPlayerController,
    onBack: () -> Unit,
    onHistoryUrl: (String, String, String) -> Unit
) {
    var mode by remember { mutableStateOf("AUDIO") } // AUDIO | VIDEO
    LocalMediaScreen(
        forAudio = mode == "AUDIO",
        controller = controller,
        onBack = onBack,
        onHistoryUrl = onHistoryUrl,
        onToggleMode = { audio -> mode = if (audio) "AUDIO" else "VIDEO" },
        showModeTabs = true
    )
}
