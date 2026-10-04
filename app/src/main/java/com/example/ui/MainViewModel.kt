package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FaselGateways
import com.example.data.FaselGenre
import com.example.data.FaselHdApi
import com.example.data.FaselMedia
import com.example.data.FaselNetwork
import com.example.data.FaselPage
import com.example.data.FaselQuality
import com.example.data.FaselDetail
import com.example.data.FaselSeries
import com.example.data.FaselSeason
import com.example.data.FaselEpisode
import com.example.data.FaselVideo
import com.example.data.HikayeTvApi
import com.example.data.HostConfig
import com.example.data.SecretVault
import com.example.data.HostResolver
import com.example.data.M3UParser
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.data.GatewayEntry
import com.example.data.GoLiveApi
import com.example.data.AlooyTvApi
import com.example.player.YouseifPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class ChannelHealth { IDLE, CHECKING, ONLINE, OFFLINE }

    private val db = try {
        AppDatabase.getInstance(application)
    } catch (e: Throwable) {
        Log.e("MainViewModel", "DB open failed, rebuilding", e)
        application.deleteDatabase("youseif_player.db")
        AppDatabase.getInstance(application)
    }
    private val channelDao = db.channelDao()
    private val historyDao = db.historyDao()

    val playerController = YouseifPlayerController(application, viewModelScope)

    val channels: StateFlow<List<PlaylistItem>> = channelDao.getAllChannels()
        .catch { e -> Log.e("MainViewModel", "channels flow", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory = historyDao.getRecentHistory()
        .catch { e -> Log.e("MainViewModel", "history flow", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- VOD lists: MERGED per category (fixes the mutual-hiding bug).
    // Fasel HD items use "faselhd_*" ids, portal items use "vod_*" / custom ids,
    // so both sources co-exist instead of replacing each other.
    private val _films = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val films: StateFlow<List<PlaylistItem>> = _films.asStateFlow()
    private val _series = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val series: StateFlow<List<PlaylistItem>> = _series.asStateFlow()
    private val _cartoons = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val cartoons: StateFlow<List<PlaylistItem>> = _cartoons.asStateFlow()

    // ---- Fasel HD explorer state ----
    private val _networks = MutableStateFlow<List<FaselNetwork>>(emptyList())
    val networks: StateFlow<List<FaselNetwork>> = _networks.asStateFlow()
    private val _genres = MutableStateFlow<List<FaselGenre>>(emptyList())
    val genres: StateFlow<List<FaselGenre>> = _genres.asStateFlow()
    private val _countryLive = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val countryLive: StateFlow<List<PlaylistItem>> = _countryLive.asStateFlow()

    private val _moviesPage = MutableStateFlow<FaselPage?>(null)
    val moviesPage: StateFlow<FaselPage?> = _moviesPage.asStateFlow()
    private val _seriesPage = MutableStateFlow<FaselPage?>(null)
    val seriesPage: StateFlow<FaselPage?> = _seriesPage.asStateFlow()
    private val _animesPage = MutableStateFlow<FaselPage?>(null)
    val animesPage: StateFlow<FaselPage?> = _animesPage.asStateFlow()
    private val _seriesGenrePage = MutableStateFlow<FaselPage?>(null)
    val seriesGenrePage: StateFlow<FaselPage?> = _seriesGenrePage.asStateFlow()
    private val _animesGenrePage = MutableStateFlow<FaselPage?>(null)
    val animesGenrePage: StateFlow<FaselPage?> = _animesGenrePage.asStateFlow()
    private val _networkPage = MutableStateFlow<FaselPage?>(null)
    val networkPage: StateFlow<FaselPage?> = _networkPage.asStateFlow()
    private val _genrePage = MutableStateFlow<FaselPage?>(null)
    val genrePage: StateFlow<FaselPage?> = _genrePage.asStateFlow()
    private val _searchResults = MutableStateFlow<List<FaselMedia>?>(null)
    val searchResults: StateFlow<List<FaselMedia>?> = _searchResults.asStateFlow()

    // ---- حكاية TV section state (service: HikayeTvApi — ported from the owner's bot source) ----
    private val _hikayeMoviesPage = MutableStateFlow<HikayeTvApi.HPage?>(null)
    val hikayeMoviesPage: StateFlow<HikayeTvApi.HPage?> = _hikayeMoviesPage.asStateFlow()
    private val _hikayeSeriesPage = MutableStateFlow<HikayeTvApi.HPage?>(null)
    val hikayeSeriesPage: StateFlow<HikayeTvApi.HPage?> = _hikayeSeriesPage.asStateFlow()
    private val _hikayeSearchResults = MutableStateFlow<List<HikayeTvApi.HMedia>?>(null)
    val hikayeSearchResults: StateFlow<List<HikayeTvApi.HMedia>?> = _hikayeSearchResults.asStateFlow()

    private val _hideImages = MutableStateFlow(false)
    val hideImages: StateFlow<Boolean> = _hideImages.asStateFlow()

    private val _gateways = MutableStateFlow(FaselGateways.defaults())
    val gateways: StateFlow<List<GatewayEntry>> = _gateways.asStateFlow()

    private val _channelHealth = MutableStateFlow<Map<String, ChannelHealth>>(emptyMap())
    val channelHealth: StateFlow<Map<String, ChannelHealth>> = _channelHealth.asStateFlow()
    private val channelProbeTimes = ConcurrentHashMap<String, Long>()
    private val channelProbeClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var hostsCache: List<HostConfig>? = null
    private var hikayeMovieMode = "latest"
    private var hikayeSeriesMode = "latest"
    private var portalImportStarted = false
    private var goLiveLoaded = false

    // ---- Detail state ----
    data class DetailState(
        val mediaId: Int = 0,
        val title: String = "",
        val mediaKind: String = "movie",
        val detail: com.example.data.FaselDetail? = null,
        val series: com.example.data.FaselSeries? = null,
        val loading: Boolean = false,
        val error: String = "",
        val qualities: List<FaselQuality> = emptyList(),
        val qualitiesLoading: Boolean = false,
        val qualitiesLabel: String = "",
        val webEmbedFallback: String = ""
    )

    private val _detailState = MutableStateFlow(DetailState())
    val detailState: StateFlow<DetailState> = _detailState.asStateFlow()
    private var lastResolvedVideo: FaselVideo? = null
    private var lastResolvedTitle: String = ""
    /** True while viewing a حكاية TV item → quality resolve uses HikayeTvApi.resolve. */
    private var hikayeMode = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try { UserSettings.init(getApplication()) } catch (_: Throwable) {}
            try {
                _hideImages.value = UserSettings.hideImages
                _gateways.value = FaselGateways.defaults().map {
                    it.copy(enabled = UserSettings.isSourceEnabled(it.id, it.enabled))
                }
            } catch (_: Throwable) {}

            // Keep local DB across launches — do NOT wipe channels every start.
            // Only download catalogs when empty or cache older than TTL (saves data).
            // لو الفاضي بس — مش كل مرة TTL عشان ما نسحبش قنوات قبل ما المستخدم يفتحها
            val emptyBuiltIn = try { channelDao.builtInCount() == 0 } catch (_: Throwable) { true }
            if (emptyBuiltIn) {
                Log.i("MainViewModel", "channels DB empty — initial catalog pull")
                refreshLiveCatalogInternal(force = true)
                liveChannelsLoadStarted = true
            } else {
                Log.i("MainViewModel", "using cached channels; network catalog waits until Channels tab")
            }

            // VOD explorers (Fasel/Hikaye pages) — lightweight, independent of live list
            try { loadFaselBaseMetadataOnly() } catch (e: Throwable) {
                Log.w("MainViewModel", "fasel metadata failed", e)
            }
            try { loadGoLiveCatalog() } catch (e: Throwable) {
                Log.w("MainViewModel", "GoLive catalog failed", e)
            }

            // Periodic soft refresh (catalog only, not stream URLs)
            while (true) {
                delay(UserSettings.CATALOG_TTL_MS)
                if (UserSettings.offlineMode) continue
                if (!liveChannelsLoadStarted) continue // ما نحدّثش قنوات لو المستخدم ما فتحهاش
                try { refreshLiveCatalogInternal(force = true) } catch (e: Throwable) {
                    Log.w("MainViewModel", "periodic catalog refresh failed", e)
                }
            }
        }
    }



    /**
     * Upsert channels while preserving the user's favorite flags.
     */
    private fun isDramaLiveChannel(item: PlaylistItem): Boolean {
        val v = "${item.group} ${item.name} ${item.id} ${item.url}".lowercase()
        return v.contains("dramalive") || v.contains("drama live") || v.contains("دراما لايف") ||
            v.contains("drama_live") || v.contains("drama-live") || (v.contains("دراما") && v.contains("لايف"))
    }

    private suspend fun upsertChannelsPreserveFavorites(incoming: List<PlaylistItem>) {
        val incoming = incoming.filterNot { isDramaLiveChannel(it) }
        val cleanIncoming = incoming
        if (cleanIncoming.isEmpty()) return
        val favIds = try { channelDao.getFavoriteIds().toHashSet() } catch (_: Throwable) { emptySet<String>() }
        val merged = if (favIds.isEmpty()) cleanIncoming else cleanIncoming.map {
            if (it.id in favIds) it.copy(isFavorite = true) else it
        }
        channelDao.insertChannels(merged)
    }

    /**
     * Public: user tapped "تحديث القنوات" — re-download category lists only.
     */
    @Volatile private var liveChannelsLoadStarted = false

    /** يتحمّل كتالوج القنوات لما المستخدم يفتح تبويب القنوات فقط */
    fun ensureLiveChannelsLoaded() {
        if (UserSettings.offlineMode) return
        if (liveChannelsLoadStarted) return
        liveChannelsLoadStarted = true
        refreshLiveCatalog(force = false)
    }

    fun refreshLiveCatalog(force: Boolean = true) {
        if (UserSettings.offlineMode) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                refreshLiveCatalogInternal(force = force)
            } catch (e: Throwable) {
                Log.e("MainViewModel", "refreshLiveCatalog failed", e)
            }
        }
    }

    private suspend fun refreshLiveCatalogInternal(force: Boolean) {
        // 1) Firebase first
        try {
            val code = SecretVault.PORTAL_CODE
            if (code.isNotBlank() && UserSettings.isSourceEnabled("googlefire", true)) {
                val imported = com.example.data.FirebaseCatalogImporter.import(code)
                // Persist the VOD (films/series/cartoons) as well: it used to live only
                // in RAM, so after a restart the whole "اليوم" library was empty.
                val persisted = imported.channels + imported.films + imported.series + imported.cartoons
                if (persisted.isNotEmpty()) {
                    try { channelDao.clearBuiltInVod() } catch (_: Throwable) {}
                    upsertChannelsPreserveFavorites(persisted)
                }
                withContext(Dispatchers.Main) {
                    _films.value = (imported.films + _films.value).distinctBy { it.id }
                    _series.value = (imported.series + _series.value).distinctBy { it.id }
                    _cartoons.value = (imported.cartoons + _cartoons.value).distinctBy { it.id }
                }
            }
        } catch (e: Throwable) { Log.w("MainViewModel", "firebase catalog failed", e) }

        // 2) Fasel HD — قنوات اللايف والكتالوج بالتوازي عشان شبكة قنوات HD تظهر فوراً
        //    (قبل كده كان loadHomeCatalog الثقيل بيمنع قنوات اللايف من الظهور إلا بعده)
        try {
            if (UserSettings.isSourceEnabled("faselhd", true)) {
                coroutineScope {
                    val mvDeferred = async {
                        runCatching { FaselHdApi.livetvMostWatched() }.getOrDefault(emptyList())
                    }
                    val homeDeferred = async {
                        runCatching { FaselHdApi.loadHomeCatalog() }.getOrNull()
                    }
                    // المسار السريع: الأكثر مشاهدة (قنوات اللايف) يوصل الأول
                    val mv = mvDeferred.await()
                    if (mv.isNotEmpty()) upsertChannelsPreserveFavorites(mv)
                    val catalog = homeDeferred.await()
                    if (catalog != null) {
                        if (catalog.channels.isNotEmpty()) upsertChannelsPreserveFavorites(catalog.channels)
                        withContext(Dispatchers.Main) {
                            _films.value = (catalog.films + _films.value).distinctBy { it.id }
                            _series.value = (catalog.series + _series.value).distinctBy { it.id }
                            _cartoons.value = (catalog.cartoons + _cartoons.value).distinctBy { it.id }
                        }
                    }
                    // mostwatched is only a featured subset (often ~1,800 rows).
                    // The complete live catalog is distributed by country/category;
                    // merge those lists so the Channels tab exposes the full API set.
                    val countryIds = runCatching { FaselHdApi.countries().map { it.id.toIntOrNull() ?: 0 }.filter { it > 0 } }
                        .getOrDefault(emptyList())
                    for (countryId in countryIds.distinct()) {
                        val countryRows = runCatching { FaselHdApi.countryLive(countryId) }.getOrDefault(emptyList())
                        if (countryRows.isNotEmpty()) upsertChannelsPreserveFavorites(countryRows)
                    }
                }
            }
        } catch (e: Throwable) { Log.w("MainViewModel", "fasel live catalog failed", e) }

        UserSettings.markCatalogRefreshed()
        Log.i("MainViewModel", "catalog marked fresh; builtIn=${try { channelDao.builtInCount() } catch (_: Throwable) { -1 }}")
    }

    /** Networks/genres/hosts only — no heavy live channel pull if already cached. */
    private suspend fun loadFaselBaseMetadataOnly() = coroutineScope {
        try {
            val netsDeferred = async { FaselHdApi.networks() }
            val countriesDeferred = async { FaselHdApi.countries() }
            val genresDeferred = async { FaselHdApi.genres() }
            val hostsDeferred = async { FaselHdApi.hostsConfig() }
            val nets = netsDeferred.await()
            val ctries = countriesDeferred.await()
            val gens = genresDeferred.await()
            hostsCache = hostsDeferred.await()
            withContext(Dispatchers.Main) {
                _networks.value = nets + ctries
                _genres.value = gens
            }
        } catch (e: Throwable) {
            Log.e("MainViewModel", "fasel metadata failed", e)
        }
        // الصفحات تتحمّل لما المستخدم يفتح المكتبة / يضغط صفحة — مش عند الإقلاع
    }

    private suspend fun loadFaselBase() = coroutineScope {
        try {
            val netsDeferred = async { FaselHdApi.networks() }
            val countriesDeferred = async { FaselHdApi.countries() }
            val genresDeferred = async { FaselHdApi.genres() }
            val hostsDeferred = async { FaselHdApi.hostsConfig() }

            val nets = netsDeferred.await()
            val ctries = countriesDeferred.await()
            val gens = genresDeferred.await()
            hostsCache = hostsDeferred.await()

            withContext(Dispatchers.Main) {
                _networks.value = nets + ctries
                _genres.value = gens
            }
        } catch (e: Throwable) {
            Log.e("MainViewModel", "fasel base failed", e)
        }

        try {
            val catalogDeferred = async { FaselHdApi.loadHomeCatalog() }
            val liveDeferred = async { FaselHdApi.livetvMostWatched() }

            val catalog = catalogDeferred.await()
            withContext(Dispatchers.Main) {
                _films.value = (catalog.films + _films.value).distinctBy { it.id }
                _series.value = (catalog.series + _series.value).distinctBy { it.id }
                _cartoons.value = (catalog.cartoons + _cartoons.value).distinctBy { it.id }
            }
            if (catalog.channels.isNotEmpty()) channelDao.insertChannels(catalog.channels)
            Log.i("MainViewModel", "home catalog: ${catalog.films.size} films, ${catalog.series.size} series, ${catalog.cartoons.size} anime, ${catalog.channels.size} live")

            val mv = liveDeferred.await()
            if (mv.isNotEmpty()) channelDao.insertChannels(mv)
            Log.i("MainViewModel", "livetv mostwatched: ${mv.size} channels")
        } catch (e: Throwable) {
            Log.e("MainViewModel", "startup preload failed", e)
        }

        // Lazy-first startup: preload the default visible pages only.
        loadMovies(1)
        loadHikayeMovies(1, hikayeMovieMode)
    }

    // ------------------------------------------------------ حكاية TV paging
    fun loadHikayeMovies(page: Int, mode: String = hikayeMovieMode) {
        if (UserSettings.offlineMode) return
        val normalizedMode = mode.ifBlank { "latest" }
        val effectivePage = if (normalizedMode != hikayeMovieMode) 1 else page
        hikayeMovieMode = normalizedMode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = HikayeTvApi.movies(effectivePage, mode = hikayeMovieMode)
                _hikayeMoviesPage.value = mergeHikayePage(_hikayeMoviesPage.value, incoming, effectivePage)
            } catch (_: Throwable) {
                _hikayeMoviesPage.value = null
            }
        }
    }

    fun loadHikayeSeries(page: Int, mode: String = hikayeSeriesMode) {
        if (UserSettings.offlineMode) return
        val normalizedMode = mode.ifBlank { "latest" }
        val effectivePage = if (normalizedMode != hikayeSeriesMode) 1 else page
        hikayeSeriesMode = normalizedMode
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = HikayeTvApi.series(effectivePage, mode = hikayeSeriesMode)
                _hikayeSeriesPage.value = mergeHikayePage(_hikayeSeriesPage.value, incoming, effectivePage)
            } catch (_: Throwable) {
                _hikayeSeriesPage.value = null
            }
        }
    }

    /** Scoped search: قسم حكاية TV يبحث في الـ API بتاعه فقط. */
    fun searchHikaye(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _hikayeSearchResults.value = if (query.isBlank()) null else HikayeTvApi.search(query)
        }
    }

    /** صفحة بصفحة — نستبدل المحتوى مش نكدّس كل الصفحات (أخف على الذاكرة والبطارية) */
    private fun mergeFaselPage(current: FaselPage?, incoming: FaselPage, requestedPage: Int): FaselPage {
        // دائماً الصفحة المطلوبة فقط
        return incoming.copy(page = requestedPage.coerceAtLeast(1))
    }

    private fun mergeHikayePage(current: HikayeTvApi.HPage?, incoming: HikayeTvApi.HPage, requestedPage: Int): HikayeTvApi.HPage {
        return incoming.copy(page = requestedPage.coerceAtLeast(1))
    }

    // ---------------------------------------------------------------- paging
    fun loadMovies(page: Int) {
        if (UserSettings.offlineMode) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.moviesPage(page)
                _moviesPage.value = mergeFaselPage(_moviesPage.value, incoming, page)
            } catch (e: Throwable) {
                _moviesPage.value = null
            }
        }
    }

    fun loadSeries(page: Int) {
        if (UserSettings.offlineMode) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.seriesPage(page)
                _seriesPage.value = mergeFaselPage(_seriesPage.value, incoming, page)
            } catch (e: Throwable) {
                _seriesPage.value = null
            }
        }
    }

    /** صفحات أنمي حقيقية من السورس (genres/animes/all). */
    fun loadAnimes(page: Int) {
        if (UserSettings.offlineMode) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.animesPage(page)
                _animesPage.value = mergeFaselPage(_animesPage.value, incoming, page)
            } catch (e: Throwable) {
                _animesPage.value = null
            }
        }
    }

    /** تصنيفات فاصل الحقيقية: endpoints منفصلة لكل نوع —
     *  kind="SERIES" → genres/series/show · kind="ANIME" → genres/animes/show.
     *  (كنا بنستخدم endpoint الأفلام لكل التصنيفات → نتايج غلط). */
    fun loadFaselGenrePage(kind: String, genreId: Int, page: Int) {
        if (genreId <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.kindGenrePage(kind, genreId, page)
                if (kind.equals("ANIME", true)) {
                    _animesGenrePage.value = mergeFaselPage(_animesGenrePage.value, incoming, page)
                } else {
                    _seriesGenrePage.value = mergeFaselPage(_seriesGenrePage.value, incoming, page)
                }
            } catch (e: Throwable) {
                if (kind.equals("ANIME", true)) _animesGenrePage.value = null else _seriesGenrePage.value = null
            }
        }
    }

    fun loadNetwork(id: String, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.networkPage(id, page)
                _networkPage.value = mergeFaselPage(_networkPage.value, incoming, page)
            } catch (e: Throwable) {
                _networkPage.value = null
            }
        }
    }

    fun loadGenre(id: Int, page: Int) {
        if (id <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incoming = FaselHdApi.genrePage(id, page)
                _genrePage.value = mergeFaselPage(_genrePage.value, incoming, page)
            } catch (e: Throwable) {
                _genrePage.value = null
            }
        }
    }

    /** Real live channels of a country category (verified live). */
    fun loadCountry(categoryId: Int) {
        if (categoryId <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            try { _countryLive.value = FaselHdApi.countryLive(categoryId) } catch (_: Throwable) { _countryLive.value = emptyList() }
        }
    }

    fun searchFasel(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = if (query.isBlank()) null else FaselHdApi.search(query)
        }
    }

    // ---------------------------------------------------------------- detail
    fun openMedia(m: FaselMedia, kindHint: String = "") {
        hikayeMode = false
        val resolvedKind = when {
            kindHint.equals("anime", true) || m.type.contains("anime", true) -> "anime"
            kindHint.equals("serie", true) || m.type.contains("serie", true) -> "serie"
            else -> "movie"
        }
        _detailState.value = DetailState(mediaId = m.id, title = m.title, mediaKind = resolvedKind, loading = true)
        lastResolvedTitle = m.title
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The folder the user browsed from is the truth: مسلسلات → serie، أنمي → anime.
                // This fixes fake classification (series/anime opening as movies).
                val isSerie = kindHint.equals("serie", true) || m.type.contains("serie")
                val isAnime = kindHint.equals("anime", true) || m.type.contains("anime")
                if (isSerie || isAnime) {
                    val s = if (isAnime) FaselHdApi.animeDetail(m.id) else FaselHdApi.series(m.id)
                    if (s == null) {
                        // fallback: some series/anime live in the movies tree
                        val d = FaselHdApi.detail(m.id)
                        _detailState.value = _detailState.value.copy(
                            detail = d, loading = false,
                            error = if (d == null) "لا توجد بيانات لهذا العمل" else ""
                        )
                        return@launch
                    }
                    _detailState.value = _detailState.value.copy(series = s, loading = false, error = "")
                } else {
                    var d = FaselHdApi.detail(m.id)
                    if (d == null) { // fallback: might be a serie without type
                        val s = FaselHdApi.series(m.id)
                        if (s != null) {
                            _detailState.value = _detailState.value.copy(series = s, loading = false)
                            return@launch
                        }
                    }
                    _detailState.value = _detailState.value.copy(detail = d, loading = false, error = if (d == null) "لا توجد بيانات لهذا العمل" else "")
                }
            } catch (e: Throwable) {
                _detailState.value = _detailState.value.copy(loading = false, error = e.message ?: "خطأ في التحميل")
            }
        }
    }

    fun openRelated(m: FaselMedia) {
        // pass the REAL type from the related card (fixes مسلسل/أنمي opening as movie)
        openMedia(m, m.type)
    }

    // ------------------------------------------------------ حكاية TV detail
    /** Open a حكاية TV movie/series — converted to Fasel objects so the existing
     *  MediaDetailScreen (qualities sheet, seasons, episodes) renders it unchanged. */
    fun openHikayeMedia(m: HikayeTvApi.HMedia) {
        hikayeMode = true
        val hid = m.id.toIntOrNull() ?: 0
        _detailState.value = DetailState(
            mediaId = hid,
            title = m.displayTitle,
            mediaKind = if (m.kind.equals("series", true)) "hikaye_series" else "hikaye_movie",
            loading = true
        )
        lastResolvedTitle = m.displayTitle
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (m.kind.equals("series", true)) {
                    val s = HikayeTvApi.seriesDetail(m.id)
                    _detailState.value = _detailState.value.copy(
                        series = s?.let { hikayeToFaselSeries(it) }, loading = false,
                        error = if (s == null) "لا توجد بيانات لهذا العمل" else ""
                    )
                } else {
                    val d = HikayeTvApi.movieDetail(m.id)
                    _detailState.value = _detailState.value.copy(
                        detail = d?.let { hikayeToFaselDetail(it) }, loading = false,
                        error = if (d == null) "لا توجد بيانات لهذا العمل" else ""
                    )
                }
            } catch (e: Throwable) {
                _detailState.value = _detailState.value.copy(loading = false, error = e.message ?: "خطأ في التحميل")
            }
        }
    }

    private fun hikayeVideo(url: String, label: String) = FaselVideo(
        server = label.ifBlank { "سيرفر مشاهدة" },
        link = url,
        hd = true,
        hls = url.contains(".m3u8", true)
    )

    private fun hikayeToFaselDetail(d: Pair<HikayeTvApi.HMedia, List<HikayeTvApi.HSource>>): FaselDetail {
        val m = d.first
        return FaselDetail(
            id = m.id.toIntOrNull() ?: 0,
            title = m.displayTitle,
            overview = m.overview,
            poster = m.poster,
            backdrop = m.backdrop,
            vote = m.rating,
            release = if (m.year > 0) m.year.toString() else "",
            videos = d.second.map { hikayeVideo(it.url, it.label) }
        )
    }

    private fun hikayeToFaselSeries(s: HikayeTvApi.HSeriesDetail): FaselSeries =
        FaselSeries(
            id = s.id.toIntOrNull() ?: 0,
            title = s.title,
            overview = s.overview,
            poster = s.poster,
            seasons = s.seasons.map { season ->
                FaselSeason(
                    id = season.number,
                    name = season.name,
                    episodes = season.episodes.map { e ->
                        FaselEpisode(
                            id = e.id.toIntOrNull() ?: 0,
                            number = e.number,
                            name = e.title.ifBlank { "الحلقة ${e.number}" },
                            videos = e.sources.map { hikayeVideo(it.url, it.label) }
                        )
                    }
                )
            }
        )

    fun resolveQualities(v: FaselVideo) {
        lastResolvedVideo = v
        lastResolvedTitle = _detailState.value.title.ifBlank { lastResolvedTitle }
        _detailState.value = _detailState.value.copy(qualities = emptyList(), qualitiesLoading = true, qualitiesLabel = v.server)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // حكاية TV path: links are direct (or need the bot's /stream/resolve)
                if (hikayeMode) {
                    val resolved = HikayeTvApi.resolve(v.link)
                    val qs = ArrayList<FaselQuality>()
                    qs += FaselQuality("تشغيل", resolved)
                    if (resolved != v.link) qs += FaselQuality("الرابط الأصلي", v.link)
                    _detailState.value = _detailState.value.copy(
                        qualities = qs, qualitiesLoading = false,
                        qualitiesLabel = v.server, webEmbedFallback = ""
                    )
                    publishQualitiesToPlayer(qs)
                    // تشغيل تلقائي لأول جودة متاحة
                    val auto = qs.firstOrNull()?.url
                    if (!auto.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            try {
                                playerController.playUrl(auto, title = lastResolvedTitle, isLive = false)
                            } catch (_: Throwable) {}
                        }
                    }
                    return@launch
                }
                // Fast path: server-aware parallel scrapers (Shahed / UpDown / Wish / VIP / Egy)
                var direct = tryScripttestFasel(v.link, v.userAgent, v.referer, v.server)
                // Server Egy often returns one 1080p master.m3u8 — expand to 720/480/360
                if (direct.size == 1 && direct.first().url.contains(".m3u8", true)) {
                    val expanded = expandMasterQualities(direct.first().url, v.userAgent, v.referer)
                    if (expanded.size > 1) direct = expanded
                }
                if (direct.isNotEmpty()) {
                    _detailState.value = _detailState.value.copy(
                        qualities = direct,
                        qualitiesLoading = false,
                        qualitiesLabel = v.server.ifBlank { "VIP" },
                        webEmbedFallback = ""
                    )
                    publishQualitiesToPlayer(direct)
                    val auto = direct.firstOrNull()?.url
                    if (!auto.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            try {
                                playerController.playUrl(
                                    auto,
                                    title = lastResolvedTitle,
                                    isLive = false,
                                    userAgent = v.userAgent,
                                    referer = v.referer ?: faselReferer(v.link) ?: hostReferer(v.link)
                                )
                            } catch (_: Throwable) {}
                        }
                    }
                    return@launch
                }

                // Secondary path — isolated so HostResolver/R8 issues never kill the fast path
                var qs: List<FaselQuality> = emptyList()
                var label = ""
                var resSource = ""
                try {
                    val hosts = try {
                        hostsCache ?: FaselHdApi.hostsConfig().also { hostsCache = it }
                    } catch (_: Throwable) {
                        emptyList()
                    }
                    val res = HostResolver.resolve(v, hosts, getApplication<Application>().applicationContext)
                    resSource = res.source
                    qs = res.qualities.map { FaselQuality(it.label, it.url) }.ifEmpty {
                        res.directUrl?.let { listOf(FaselQuality("Original", it)) }
                            ?: listOf(FaselQuality("Original", v.link))
                    }
                    label = when {
                        qs.size == 1 && qs.first().label == "Original" -> "المصدر الأصلي — جرب فتح صفحة المشاهدة"
                        else -> v.server.ifBlank { res.source }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MainViewModel", "HostResolver path failed", e)
                    qs = listOf(FaselQuality("Original", v.link))
                    label = "المصدر الأصلي — جرب فتح صفحة المشاهدة"
                    resSource = "original"
                }
                // Always keep the embed page as fallback when we only got Original
                // so the user can still watch (StreamWish / UpDown / etc.).
                val onlyOriginal = qs.size == 1 && qs.first().label.equals("Original", ignoreCase = true)
                val embedFallback = if (onlyOriginal || resSource == "original") v.link else ""
                _detailState.value = _detailState.value.copy(
                    qualities = qs,
                    qualitiesLoading = false,
                    qualitiesLabel = label,
                    webEmbedFallback = embedFallback
                )
                // Auto-play real streams only — skip auto-play for bare Original embeds
                // (they usually need WebView / correct referer and fail in ExoPlayer).
                val auto = qs.firstOrNull()?.url
                val isRealStream = auto != null && !onlyOriginal &&
                    (auto.contains(".m3u8", true) || auto.contains(".mp4", true) ||
                        auto.contains(".mpd", true) || auto.contains("/hls", true) ||
                        resSource.startsWith("webview") || resSource.contains("script") ||
                        resSource.contains("worker") || resSource.contains("api-source") ||
                        resSource.contains("playlist") || resSource.contains("direct"))
                if (!auto.isNullOrBlank() && isRealStream) {
                    withContext(Dispatchers.Main) {
                        try {
                            playerController.playUrl(
                                auto,
                                title = lastResolvedTitle,
                                isLive = false,
                                userAgent = v.userAgent,
                                referer = v.referer ?: hostReferer(v.link)
                            )
                        } catch (_: Throwable) {}
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "resolveQualities failed", e)
                _detailState.value = _detailState.value.copy(
                    qualities = listOf(FaselQuality("Original", v.link)),
                    qualitiesLoading = false,
                    qualitiesLabel = "فشل: ${e.javaClass.simpleName}: ${e.message?.take(40) ?: ""}",
                    webEmbedFallback = v.link
                )
            }
        }
    }

    /**
     * Fast server-aware scrapers — parallel, short timeout, priority by server name.
     * Avoids the old sequential 13×5×2 = 130 requests that hung on "جارٍ فتح الجودات".
     */
    private fun tryScripttestFasel(
        link: String,
        ua: String?,
        ref: String?,
        server: String? = null
    ): List<FaselQuality> {
        if (link.isBlank()) return emptyList()
        return try {
            val encoded = java.net.URLEncoder.encode(link.trim(), "UTF-8")
            val s = (server ?: "").lowercase()
            val l = link.lowercase()
            // EasyPlex BaseVed path for UpDown / StreamWish / Server Egy / Shahed — skip VIP EGYBEST
            val isVip = s.contains("vip") || l.contains("plusfas") || Regex("[?&]p=\\d+").containsMatchIn(l)
            if (!isVip && (s.contains("updown") || s.contains("wish") ||
                    (s.contains("egy") && !s.contains("vip")) || s.contains("cima") ||
                    s.contains("shahed") || s.contains("شاهد") ||
                    l.contains("updown") || l.contains("streamwish") ||
                    l.contains("shahed") || l.contains("vidtube") || l.contains("fdewsdc"))) {
                val ved = tryBaseVedPagePost(link, ua, ref, server)
                if (ved.isNotEmpty()) return ved
            }
            val bases = when {
                isVip -> listOf(
                    "https://mawdhou3.com/scripttestfasel.php?api=",
                    "https://autumn-dust-1a31.flechlivraison.workers.dev/?url=",
                    "https://mawdhou3.com/test.php?api=",
                    "https://mawdhou3.com/scrapefinal/faselpost.php?api="
                )
                s.contains("shahed") || s.contains("شاهد") || l.contains("shahed") ||
                    l.contains("vidtube") || l.contains("fdewsdc") -> listOf(
                    "https://mawdhou3.com/scrapefinal/faselpost.php?api=",
                    "https://mawdhou3.com/scrapefinal/vidtubepost.php?api=",
                    "https://mawdhou3.com/test.php?api=",
                    "https://shahed4uapp.com/akwam/test.php?api="
                )
                s.contains("updown") || l.contains("updown") -> listOf(
                    "https://mawdhou3.com/scrapefinal/updown.php?api=",
                    "https://mawdhou3.com/test.php?api="
                )
                s.contains("wish") || l.contains("streamwish") || l.contains("stmruby") -> listOf(
                    "https://mawdhou3.com/scrapefinal/earnvids.php?api=",
                    "https://mawdhou3.com/test.php?api="
                )
                s.contains("server egy") || s.contains("cima") ||
                    (s.contains("egy") && !s.contains("vip")) -> listOf(
                    "https://mawdhou3.com/scrapeamine/scriptEgybest.php?api=",
                    "https://mawdhou3.com/test.php?api=",
                    "https://flech.tn/recuperepost.php?api=",
                    "https://mawdhou3.com/scrapefinal/faselpost.php?api="
                )
                else -> listOf(
                    "https://mawdhou3.com/scripttestfasel.php?api=",
                    "https://mawdhou3.com/test.php?api=",
                    "https://mawdhou3.com/scrapefinal/earnvids.php?api=",
                    "https://mawdhou3.com/scrapefinal/updown.php?api=",
                    "https://mawdhou3.com/scrapefinal/faselpost.php?api=",
                    "https://mawdhou3.com/scrapeamine/scriptEgybest.php?api=",
                    "https://flech.tn/recuperepost.php?api="
                )
            }
            val userAgent = ua?.ifBlank { null }
                ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
            val referer = ref?.ifBlank { null } ?: when {
                isVip -> "https://faselhd.center/"
                s.contains("shahed") -> "https://shaaheid4u.net/"
                s.contains("egy") -> "https://flech.tn/"
                s.contains("updown") || s.contains("wish") -> "https://topcinema.media/"
                else -> "https://faselhd.center/"
            }
            // Parallel race with short timeouts
            val results = java.util.concurrent.ConcurrentLinkedQueue<List<FaselQuality>>()
            val pool = java.util.concurrent.Executors.newFixedThreadPool(bases.size.coerceAtMost(6))
            try {
                val futures = bases.map { base ->
                    pool.submit<List<FaselQuality>> {
                        try {
                            val body = httpGetPlain(base + encoded, userAgent, referer)
                                ?: httpGetPlain(base + link.trim(), userAgent, referer)
                                ?: httpPostPlain(base, link.trim(), userAgent, referer)
                            if (body.isNullOrBlank()) return@submit emptyList()
                            parseQualityPairs(body)
                        } catch (_: Throwable) {
                            emptyList()
                        }
                    }
                }
                val deadline = System.currentTimeMillis() + 8_000L
                for (f in futures) {
                    val left = (deadline - System.currentTimeMillis()).coerceAtLeast(100)
                    try {
                        val r = f.get(left, java.util.concurrent.TimeUnit.MILLISECONDS)
                        if (r.isNotEmpty()) {
                            futures.forEach { it.cancel(true) }
                            return r
                        }
                    } catch (_: Throwable) { }
                }
            } finally {
                pool.shutdownNow()
            }
            emptyList()
        } catch (e: Throwable) {
            android.util.Log.w("MainViewModel", "tryScripttestFasel: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
    }

    /** Parse {file:"url",label:"1080p"} without touching HostResolver. */
    private fun parseQualityPairs(text: String): List<FaselQuality> {
        val out = ArrayList<FaselQuality>()
        val re = Regex(
            """\{\s*"?file"?\s*:\s*"(.*?)"\s*,\s*"?label"?\s*:\s*"(.*?)"""",
            RegexOption.IGNORE_CASE
        )
        for (m in re.findAll(text)) {
            val u = m.groupValues.getOrNull(1)?.trim().orEmpty()
            val l = m.groupValues.getOrNull(2)?.trim().orEmpty()
            if (u.startsWith("http")) out += FaselQuality(if (l.isBlank()) "جودة" else l, u)
        }
        if (out.isEmpty()) {
            val re2 = Regex("""file\s*:\s*"(.*?)"\s*,\s*label\s*:\s*"(.*?)"""", RegexOption.IGNORE_CASE)
            for (m in re2.findAll(text)) {
                val u = m.groupValues.getOrNull(1)?.trim().orEmpty()
                val l = m.groupValues.getOrNull(2)?.trim().orEmpty()
                if (u.startsWith("http")) out += FaselQuality(if (l.isBlank()) "جودة" else l, u)
            }
        }
        return out.distinctBy { it.url }
    }

    /**
     * EasyPlex BaseVedEasyPlex flow from private Fasel APK:
     * GET embed HTML → POST content=&url= → {Quality, filtered_content}
     */
    private fun tryBaseVedPagePost(
        link: String,
        ua: String?,
        ref: String?,
        server: String?
    ): List<FaselQuality> {
        val s = (server ?: "").lowercase()
        val endpoints = when {
            s.contains("updown") || link.contains("updown", true) -> listOf(
                "https://mawdhou3.com/scrapefinal/updown.php",
                "https://mawdhou3.com/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            s.contains("wish") || link.contains("streamwish", true) -> listOf(
                "https://mawdhou3.com/scrapefinal/earnvids.php",
                "https://mawdhou3.com/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            s.contains("shahed") || link.contains("shahed", true) || link.contains("vidtube", true) -> listOf(
                "https://mawdhou3.com/scrapefinal/faselpost.php",
                "https://mawdhou3.com/scrapefinal/vidtubepost.php",
                "https://flech.tn/recuperepost.php",
                "https://mawdhou3.com/test.php"
            )
            else -> listOf(
                "https://mawdhou3.com/scrapeamine/scriptEgybest.php",
                "https://mawdhou3.com/test.php",
                "https://flech.tn/recuperepost.php",
                "https://mawdhou3.com/scrapefinal/faselpost.php"
            )
        }
        val userAgent = ua?.ifBlank { null }
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        val referers = listOfNotNull(
            ref?.ifBlank { null },
            if (s.contains("egy")) "https://flech.tn/" else null,
            "https://topcinema.media/",
            "https://faselhd.center/",
            "https://shaaheid4u.net/"
        ).distinct()
        var html: String? = null
        var usedRef = referers.firstOrNull() ?: "https://flech.tn/"
        for (r in referers) {
            val page = httpGetPlain(link, userAgent, r)
            if (!page.isNullOrBlank() && page.length >= 80 &&
                !page.contains("Just a moment", true)
            ) {
                html = page
                usedRef = r
                break
            }
        }
        val urlEnc = java.net.URLEncoder.encode(link, "UTF-8")
        for (ep in endpoints) {
            try {
                val endpoint = ep.substringBefore("?").trimEnd('/')
                // Prefer full BaseVed when we have HTML; always try url-only as backup
                val bodies = buildList {
                    if (!html.isNullOrBlank()) {
                        add(
                            "content=" + java.net.URLEncoder.encode(html, "UTF-8") +
                                "&url=$urlEnc"
                        )
                    }
                    add("url=$urlEnc")
                }
                for (body in bodies) {
                    val conn = java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 7_000
                    conn.readTimeout = 14_000
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("User-Agent", userAgent)
                    conn.setRequestProperty("Referer", usedRef)
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    if (conn.responseCode !in 200..299) continue
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseBaseVedJson(resp)
                    if (parsed.isNotEmpty()) return parsed
                    val pairs = parseQualityPairs(resp)
                    if (pairs.isNotEmpty()) return pairs
                }
            } catch (_: Throwable) { }
        }
        return emptyList()
    }

    private fun parseBaseVedJson(text: String): List<FaselQuality> {
        val out = ArrayList<FaselQuality>()
        try {
            val root = org.json.JSONObject(text.trim())
            val status = root.optString("status")
            if (status.isNotBlank() && !status.equals("success", true)) return emptyList()
            val quals = root.optJSONArray("Quality")
            val urls = root.optJSONArray("filtered_content") ?: return emptyList()
            for (i in 0 until urls.length()) {
                val u = urls.optString(i, "").trim()
                if (!u.startsWith("http")) continue
                val lab = quals?.optString(i, "")?.ifBlank { null }
                    ?: when {
                        "1080" in u -> "1080p"
                        "720" in u -> "720p"
                        "480" in u -> "480p"
                        "360" in u -> "360p"
                        else -> "جودة"
                    }
                out += FaselQuality(lab, u)
            }
        } catch (_: Throwable) { }
        return out.distinctBy { it.url }
    }

    /** Expand master.m3u8 into 1080p/720p/480p/360p (Server Egy single-quality fix). */
    private fun expandMasterQualities(masterUrl: String, ua: String?, ref: String?): List<FaselQuality> {
        return try {
            val body = httpGetPlain(
                masterUrl,
                ua ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/137.0.0.0 Safari/537.36",
                ref ?: "https://faselhd.center/"
            ) ?: return emptyList()
            if (!body.contains("#EXT-X-STREAM-INF")) return emptyList()
            val out = ArrayList<FaselQuality>()
            val lines = body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    val res = Regex("RESOLUTION=(\\d+)x(\\d+)").find(line)
                    val w = res?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                    val label = when {
                        w >= 1600 || line.contains("1080") -> "1080p"
                        w >= 1100 || line.contains("720") -> "720p"
                        w >= 700 || line.contains("480") -> "480p"
                        w >= 400 || line.contains("360") -> "360p"
                        else -> res?.groupValues?.getOrNull(0)?.removePrefix("RESOLUTION=") ?: "جودة"
                    }
                    if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) {
                        val uri = lines[i + 1]
                        val abs = try {
                            java.net.URI(masterUrl).resolve(uri).toString()
                        } catch (_: Throwable) {
                            uri
                        }
                        if (abs.startsWith("http")) out += FaselQuality(label, abs)
                        i++
                    }
                }
                i++
            }
            out.distinctBy { it.url }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun httpGetPlain(url: String, ua: String, ref: String): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            // Fail-fast: was 15s/20s and made quality sheet hang for a minute+
            conn.connectTimeout = 4_000
            conn.readTimeout = 6_000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", ua)
            conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Accept-Language", "ar,en;q=0.9")
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    /** POST for UpDown / StreamWish scrapers that reject GET (Méthode non autorisée). */
    private fun httpPostPlain(base: String, link: String, ua: String, ref: String): String? {
        return try {
            val encoded = java.net.URLEncoder.encode(link, "UTF-8")
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
            conn.setRequestProperty("User-Agent", ua)
            conn.setRequestProperty("Referer", ref)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "*/*")
            conn.outputStream.use { it.write("api=$encoded&url=$encoded&link=$encoded".toByteArray()) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    fun closeQualities() {
        _detailState.value = _detailState.value.copy(qualities = emptyList(), qualitiesLoading = false, webEmbedFallback = "")
    }

    /** Last resort: open the protected page itself in the in-app WebView. */
    fun playWebEmbedFallback() {
        val url = _detailState.value.webEmbedFallback
        if (url.isBlank()) return
        closeQualities()
        val title = lastResolvedTitle.ifBlank { "مشاهدة" }
        val poster = _detailState.value.detail?.poster ?: _detailState.value.series?.poster ?: ""
        val mediaId = _detailState.value.mediaId
        recordHistory(title, url, if (mediaId > 0) "FILM:$mediaId" else "FILM", poster)
        playerController.playWebEmbed(url, title)
    }

    /** Faseln short-link sources need their own referer to stream/download properly. */
    private fun faselReferer(link: String?): String? =
        if (link != null && (link.contains("fasel-hd") || link.contains("plusfas") || link.contains("/?p=")))
            "https://www.fasel-hd.com/" else null

    /** Derive a sensible Referer/Origin from the embed page host for third-party CDNs. */
    private fun hostReferer(link: String?): String? {
        if (link.isNullOrBlank()) return null
        return try {
            val u = java.net.URI(link.trim())
            val scheme = u.scheme ?: "https"
            val host = u.host ?: return null
            "$scheme://$host/"
        } catch (_: Throwable) {
            null
        }
    }

    fun playQuality(q: FaselQuality) {
        val v = lastResolvedVideo
        val liveHint = false
        val title = lastResolvedTitle.ifBlank { "مشاهدة" }
        val poster = _detailState.value.detail?.poster
            ?: _detailState.value.series?.poster
            ?: ""
        val mediaId = _detailState.value.mediaId
        val groupTag = if (mediaId > 0) {
            if (hikayeMode) "HIKAYE:${_detailState.value.mediaKind}:$mediaId"
            else "FILM:${_detailState.value.mediaKind}:$mediaId"
        } else if (hikayeMode) "HIKAYE" else "FILM"
        // If user taps Original and we have an embed page, prefer in-app WebView
        // so StreamWish / UpDown / etc. actually play instead of a dead Exo attempt.
        if (q.label.equals("Original", ignoreCase = true)) {
            val embed = v?.link?.takeIf { it.isNotBlank() } ?: _detailState.value.webEmbedFallback
            if (!embed.isNullOrBlank() && (embed.contains("/e/") || embed.contains("embed") ||
                    embed.contains("streamwish") || embed.contains("updown") ||
                    embed.contains("dood") || embed.contains("filemoon") ||
                    embed.contains("uqload") || embed.contains("vidoba") ||
                    !q.url.contains(".m3u8", true) && !q.url.contains(".mp4", true))) {
                closeQualities()
                playerController.setCurrentContent(
                    PlaylistItem(
                        id = "embed_${mediaId}_${embed.hashCode()}",
                        channelNumber = 0,
                        name = title,
                        url = embed,
                        group = groupTag,
                        logoUrl = poster,
                        isLive = false
                    )
                )
                recordHistory(title, embed, groupTag, poster)
                playerController.playWebEmbed(embed, title)
                return
            }
        }
        // نغذّي المشغّل بالجودات قبل ما نقفل لوحة الجودات (closeQualities بيفضّي القائمة)
        publishQualitiesToPlayer(_detailState.value.qualities)
        closeQualities()
        // Keep the active media type so hardware and on-screen next/previous
        // stay inside films, series, or anime instead of falling back to live channels.
        playerController.setCurrentContent(
            PlaylistItem(
                id = "quality_${mediaId}_${q.url.hashCode()}",
                channelNumber = 0,
                name = title,
                url = q.url,
                group = groupTag,
                logoUrl = poster,
                isLive = false
            )
        )
        recordHistory(title, q.url, groupTag, poster)
        playerController.playUrl(
            url = q.url,
            title = title,
            isLive = liveHint,
            userAgent = v?.userAgent,
            referer = v?.referer ?: faselReferer(v?.link) ?: hostReferer(v?.link) ?: hostReferer(q.url)
        )
    }

    /**
     * يغذّي المشغّل بقائمة الجودات/السيرفرات اللي جت من الموقع:
     * - تظهر مباشرة في قائمة الجودة (HD) داخل المشغّل من غير ما تخرج للمكتبة.
     * - والتقليب (سابق/تالي) داخل المشغّل/من الإشعار يمشي على نفس الجودات.
     */
    private fun publishQualitiesToPlayer(list: List<FaselQuality>) {
        try {
            if (list.isEmpty()) return
            val v = lastResolvedVideo
            val title = lastResolvedTitle.ifBlank { "مشاهدة" }
            val groupTag = currentGroupTag()
            val ua = v?.userAgent
            val ref = v?.referer ?: faselReferer(v?.link) ?: hostReferer(v?.link)
            playerController.setQualities(
                list.map { q ->
                    com.example.player.QualityOption(
                        label = q.label,
                        url = q.url,
                        server = v?.server.orEmpty(),
                        userAgent = ua,
                        referer = ref
                    )
                }
            )
            playerController.setContextQueue(
                list.map { q ->
                    PlaylistItem(
                        id = "quality_${_detailState.value.mediaId}_${q.url.hashCode()}",
                        channelNumber = 0,
                        name = "$title · ${q.label}",
                        url = q.url,
                        group = groupTag,
                        isLive = false
                    )
                },
                null
            )
        } catch (_: Throwable) {}
    }

    private fun currentGroupTag(): String {
        val mediaId = _detailState.value.mediaId
        return if (mediaId > 0) {
            if (hikayeMode) "HIKAYE:${_detailState.value.mediaKind}:$mediaId"
            else "FILM:${_detailState.value.mediaKind}:$mediaId"
        } else if (hikayeMode) "HIKAYE" else "FILM"
    }

    /**
     * Replay from home "Recently watched".
     * Old behavior users want: one tap → starts playing immediately.
     * If stored stream URL is dead, FILM:id can still re-open detail later.
     */
    fun replayHistory(title: String, url: String, group: String) {
        val safeTitle = title.ifBlank { "مشاهدة" }
        // Film/series history stores the selected quality or an embed URL, both
        // of which may expire. Re-open the current API detail first so servers,
        // subtitles and fresh links are fetched again instead of retrying a dead
        // signed CDN URL from the old history row.
        val filmMatch = Regex("""FILM:(?:(anime|serie|movie):)?(\d+)""", RegexOption.IGNORE_CASE).find(group)
        val hikayeMatch = Regex("""HIKAYE:(?:(series|movie):)?(\d+)""", RegexOption.IGNORE_CASE).find(group)
        val mid = (filmMatch?.groupValues?.getOrNull(2) ?: hikayeMatch?.groupValues?.getOrNull(2))?.toIntOrNull() ?: 0
        if (mid > 0) {
            if (hikayeMatch != null) {
                val hk = hikayeMatch.groupValues.getOrNull(1).orEmpty().ifBlank { "movie" }
                openHikayeMedia(HikayeTvApi.HMedia(id = mid.toString(), title = safeTitle, kind = hk))
                return
            }
            val fk = filmMatch?.groupValues?.getOrNull(1).orEmpty().ifBlank { "movie" }
            openMedia(
                com.example.data.FaselMedia(id = mid, title = safeTitle, type = fk, poster = ""),
                fk
            )
            return
        }
        // Direct streams expire. Prefer the current catalog row (same title/group)
        // so songs and other refreshed channels receive their latest URL.
        val current = channels.value.firstOrNull { it.name.equals(safeTitle, true) &&
            (group.isBlank() || it.group.equals(group, true)) }
            ?: channels.value.firstOrNull { it.name.equals(safeTitle, true) }
        if (current != null) {
            recordHistory(current)
            if (current.url.startsWith("faselhd://")) playContentItem(current)
            else playerController.playChannel(current)
            return
        }
        // Local audio/video history uses content:// (and sometimes android.resource://).
        // These are valid Media3 DataSource URIs but were previously rejected here,
        // which made a tap on a saved song appear to do nothing.
        val replayable = url.isNotBlank() && (
            url.startsWith("http", ignoreCase = true) ||
                url.startsWith("rtmp", ignoreCase = true) ||
                url.startsWith("file", ignoreCase = true) ||
                url.startsWith("content:", ignoreCase = true) ||
                url.startsWith("android.resource:", ignoreCase = true)
            )
        if (replayable) {
            val isLive = group.contains("LIVE", ignoreCase = true) &&
                !group.startsWith("FILM") && !group.contains("AUDIO", ignoreCase = true) &&
                !url.startsWith("content:", ignoreCase = true) &&
                !url.startsWith("file:", ignoreCase = true)
            // Re-record as newest
            recordHistory(safeTitle, url, group)
            playerController.playUrl(
                url = url,
                title = safeTitle,
                isLive = isLive,
                userAgent = if (url.startsWith("http", ignoreCase = true))
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"
                else null
            )
            return
        }
        // No playable URL — open media detail so user can pick a server
    }

    // (مسارات التحميل العامة اتشالت — المطلوب: مفيش تحميل خالص، جودات بس)

    /** Fasel download-only / progressive MP4 links from current detail. */
    @Suppress("unused")
    private fun findDirectDownloadLink(): Pair<String, String>? {
        val fromDetail = _detailState.value.detail?.videos.orEmpty()
        val fromSeries = _detailState.value.series?.seasons
            ?.flatMap { s -> s.episodes.flatMap { e -> e.videos } }
            .orEmpty()
        val videos = fromDetail + fromSeries
        val pick = videos.firstOrNull { v ->
            v.downloadOnly && !v.hls && v.link.startsWith("http") && !v.link.contains(".m3u8", true)
        } ?: videos.firstOrNull { v ->
            !v.hls && v.link.startsWith("http") && (
                v.link.contains(".mp4", true) || v.link.contains(".mkv", true) ||
                    v.server.contains("download", true) || v.server.contains("تحميل", true)
            )
        }
        return pick?.let { it.link to it.server }
    }

    /** Real DownloadManager enqueue — mp4/mkv/webm only (not HLS playlists). */
    @Suppress("unused")
    private fun startRealDownload(url: String, title: String, tag: String) {
        try {
            val u = url.trim()
            if (!u.startsWith("http")) {
                toast("رابط غير صالح للتحميل")
                return
            }
            val lower = u.lowercase()
            if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains("/hls")) {
                toast("جودة HLS (m3u8) مش تتحمل كملف واحد — اختار سيرفر تحميل MP4")
                return
            }
            val ext = when {
                lower.contains(".mkv") -> ".mkv"
                lower.contains(".webm") -> ".webm"
                lower.contains(".mp4") -> ".mp4"
                lower.contains(".ts") -> ".ts"
                else -> ".mp4"
            }
            val fileName = (sanitize(title).ifBlank { "youseif_${System.currentTimeMillis()}" } +
                (if (tag.isNotBlank()) "_${sanitize(tag)}" else "") + ext).take(120)
            val dm = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(u))
                .setTitle((if (title.isBlank()) "تحميل" else title) + if (tag.isNotBlank()) " [$tag]" else "")
                .setDescription("Youseif Player · تحميل حقيقي")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val ua = lastResolvedVideo?.userAgent
                ?: "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"
            req.addRequestHeader("User-Agent", ua)
            (lastResolvedVideo?.referer ?: faselReferer(lastResolvedVideo?.link) ?: hostReferer(u))
                ?.let { req.addRequestHeader("Referer", it) }
            dm.enqueue(req)
            toast("بدأ التحميل: $fileName")
        } catch (e: Throwable) {
            Log.e("MainViewModel", "download failed", e)
            toast("فشل التحميل: ${e.message ?: "خطأ"}")
        }
    }

    private fun toast(msg: String) {
        try {
            android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {}
    }

    private fun sanitize(s: String): String =
        s.map { if (it.isLetterOrDigit() || it == ' ') it else '_' }.joinToString("").trim().take(60)

    // ----------------------------------------------------------------- images
    fun setHideImages(v: Boolean) {
        _hideImages.value = v
        try { UserSettings.hideImages = v } catch (_: Throwable) {}
    }

    // ---------------------------------------------------------------- gateways
    fun setGatewayEnabled(id: String, enabled: Boolean) {
        _gateways.value = _gateways.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        try { UserSettings.setSourceEnabled(id, enabled) } catch (_: Throwable) {}
    }

    fun setAllGateways(enabled: Boolean) {
        _gateways.value = _gateways.value.map { it.copy(enabled = enabled) }
        try { UserSettings.setAllSources(enabled, _gateways.value.map { it.id }) } catch (_: Throwable) {}
    }

    // ---------------------------------------------------------------- history
    fun recordHistory(item: PlaylistItem) {
        recordHistory(item.name, item.url, item.group, item.logoUrl)
    }

    fun recordHistory(title: String, url: String, group: String = "LIVE", logoUrl: String = "") {
        val safeTitle = title.trim().take(240)
        val safeUrl = url.trim().take(4096)
        if (safeUrl.isBlank() && safeTitle.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // One entry per channel/stream: remove previous play of same URL/title, then insert as newest
                historyDao.deleteByUrlOrTitle(safeUrl, safeTitle)
                historyDao.insertHistory(
                    com.example.data.HistoryItem(
                        title = safeTitle.ifBlank { "بدون عنوان" },
                        url = safeUrl,
                        group = group.trim().take(80).ifBlank { "LIVE" },
                        logoUrl = logoUrl.trim().take(4096),
                        playedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Throwable) { Log.e("MainViewModel", "recordHistory", e) }
        }
    }

    fun toggleFavorite(id: String, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try { channelDao.setFavorite(id, isFavorite) } catch (e: Throwable) {}
        }
    }

    fun addCustomChannel(channel: PlaylistItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try { channelDao.insertChannel(channel) } catch (e: Throwable) {}
        }
    }

    // ----------------------------------------------------------------- import
    fun importFromUrl(url: String) {
        importPlaylistSmart(url, ImportTarget.CHANNELS)
    }

    fun importFromZipBytes(bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
                val all = mutableListOf<PlaylistItem>()
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".m3u") || name.endsWith(".m3u8") || name.endsWith(".txt"))) {
                        val text = zis.bufferedReader().readText()
                        all.addAll(M3UParser.parse(text))
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zis.close()
                if (all.isNotEmpty()) {
                    val tagged = all.map { it.copy(isCustom = true) }
                    channelDao.insertChannels(tagged)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication(), "تم استيراد ${tagged.size} قناة من ZIP", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) { Log.e("MainViewModel", "importZip", e) }
        }
    }

    fun importM3U(m3uContent: String) {
        importPlaylistSmart(m3uContent, ImportTarget.CHANNELS)
    }

    enum class ImportTarget { CHANNELS, RADIO, FILMS, SERIES, ANIME }

    /** ملخّص حقيقي لآخر عملية استرداد — بيتعرض للمستخدم بعدد فعلي. */
    data class ImportSummary(val kind: String, val label: String, val count: Int, val packageName: String)

    private val _lastImport = MutableStateFlow<ImportSummary?>(null)
    val lastImport: StateFlow<ImportSummary?> = _lastImport.asStateFlow()

    fun clearLastImport() { _lastImport.value = null }

    /** حذف صريح لعنصر من سجل المشاهدة. */
    fun deleteHistory(item: com.example.data.HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try { historyDao.deleteByUrlOrTitle(item.url, item.title) } catch (_: Throwable) {}
        }
    }

    /**
     * استيراد ذكي:
     * - لو رابط (http/github) يتحمّل النص ويفكّه
     * - لو نص M3U يتحلّل مباشرة
     * - **مش** بيشغّل أول قناة — بس يضيف للقائمة
     * - الهدف (قنوات / راديو / أفلام…) عشان المحتوى ما يختلطش
     */
    @Volatile private var importInProgress = false

    fun importPlaylistSmart(input: String, target: ImportTarget = ImportTarget.CHANNELS, packageName: String = "") {
        val raw = input.trim()
        if (raw.isBlank()) return
        // FIX: رفض ملفات الأرشيف (.rar/.zip/.gz) — استخراج الـ m3u يدويًا قبل الاستيراد
        val lowerRaw = raw.lowercase()
        if (lowerRaw.endsWith(".rar") || lowerRaw.endsWith(".zip") || lowerRaw.endsWith(".7z") ||
            lowerRaw.endsWith(".gz") || lowerRaw.endsWith(".tar") || lowerRaw.endsWith(".tar.gz")
        ) {
            viewModelScope.launch(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(),
                    "⛔ الرابط ده أرشيف (rar/zip). استخرج الـ m3u منه الأول.", android.widget.Toast.LENGTH_LONG).show()
            }
            Log.w("MainViewModel", "Refused archive: ${raw.take(60)}")
            return
        }
        if (importInProgress) {
            viewModelScope.launch(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    getApplication(),
                    "⏳ فيه استرداد شغّال دلوقتي — استنى يخلص الأول",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            importInProgress = true
            try {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "⏳ جاري قراءة القائمة…",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                val text = resolveImportText(raw)
                if (text.isBlank()) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication(), "ما قدرتش أقرأ المصدر", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val parsed = M3UParser.parse(text)
                if (parsed.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication(), "مفيش قنوات/روابط صالحة", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                when (target) {
                    ImportTarget.CHANNELS -> {
                        val pkg = packageName.trim().ifBlank { "مستورد" }
                        val tagged = parsed.map {
                            val g = it.group.trim().ifBlank { pkg }
                            it.copy(
                                isCustom = true,
                                isLive = true,
                                group = if (g.startsWith("LIVE:", true)) g else "LIVE:$pkg"
                            )
                        }
                        insertChannelsChunked(tagged)
                        _lastImport.value = ImportSummary("CHANNELS", "قناة", tagged.size, pkg)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "تم استرداد ${tagged.size} قناة ← $pkg", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    ImportTarget.RADIO -> {
                        // راديو مخصص منفصل عن القنوات
                        val lines = parsed.map { "${it.name}|||${it.url}" }
                        val existing = UserSettings.getString("custom_radio_stations", "")
                        val old = if (existing.isBlank()) emptyList() else existing.split("\n")
                        val merged = (old + lines).distinct()
                        UserSettings.putString("custom_radio_stations", merged.joinToString("\n"))
                        _lastImport.value = ImportSummary("RADIO", "محطة راديو", parsed.size, "راديو")
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "تم استرداد ${parsed.size} محطة راديو", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    ImportTarget.FILMS, ImportTarget.SERIES, ImportTarget.ANIME -> {
                        val kind = when (target) {
                            ImportTarget.FILMS -> "FILMS"
                            ImportTarget.SERIES -> "SERIES"
                            else -> "ANIME"
                        }
                        val pkg = packageName.trim().ifBlank { "CUSTOM" }
                        val prefix = "$kind:$pkg"
                        val tagged = parsed.map {
                            it.copy(
                                isCustom = true,
                                isLive = false,
                                group = prefix
                            )
                        }
                        insertChannelsChunked(tagged)
                        _lastImport.value = ImportSummary(kind, if (kind == "FILMS") "فيلم" else if (kind == "SERIES") "مسلسل" else "أنمي", tagged.size, pkg)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "تم استرداد ${tagged.size} → $prefix", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "importPlaylistSmart", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "فشل الاستيراد: ${e.message?.take(40)}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                importInProgress = false
            }
        }
    }

    private fun resolveImportText(raw: String): String {
        val looksUrl = raw.startsWith("http://", true) || raw.startsWith("https://", true)
        if (!looksUrl) return raw
        var url = raw.trim().substringBefore(" ").substringBefore("\n").trim()
        // GitHub blob → raw
        if (url.contains("github.com/") && url.contains("/blob/")) {
            url = url.replace("github.com/", "raw.githubusercontent.com/").replace("/blob/", "/")
        }
        // gist
        if (url.contains("gist.github.com/") && !url.contains("/raw")) {
            // leave; may redirect
        }
        val conn = java.net.URL(url).openConnection()
        conn.connectTimeout = 25_000
        // Large lists (iptv-org ~10k+) need a longer read window.
        conn.readTimeout = 120_000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) YouseifPlayer/25")
        conn.setRequestProperty("Accept", "*/*")
        if (conn is java.net.HttpURLConnection) {
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
        }
        return conn.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private var portalImported = false

    fun ensurePortalImported() {
        if (portalImported || portalImportStarted) return
        val code = try { SecretVault.PORTAL_CODE } catch (_: Throwable) { "" }
        if (code.isBlank()) return
        importPortalCode(code)
    }

    /** Portal/Firebase catalog. Merges instead of replacing → no more hiding.
     *  Import runs ONCE per app session — re-tapping never re-downloads or re-fires
     *  playback (the old hang-on-import bug). */
    fun importPortalCode(code: String) {
        if (portalImported) {
            android.widget.Toast.makeText(
                getApplication(), "فولدرات فاير بيز مستوردة بالفعل", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (portalImportStarted) return
        portalImportStarted = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = com.example.data.FirebaseCatalogImporter.import(code)
                if (imported.channels.isNotEmpty()) channelDao.insertChannels(imported.channels)
                portalImported = imported.channels.isNotEmpty() || imported.films.isNotEmpty() || imported.series.isNotEmpty() || imported.cartoons.isNotEmpty()
                withContext(Dispatchers.Main) {
                    _films.value = (imported.films + _films.value).distinctBy { it.id }
                    _series.value = (imported.series + _series.value).distinctBy { it.id }
                    _cartoons.value = (imported.cartoons + _cartoons.value).distinctBy { it.id }
                }
            } catch (e: Throwable) {
                portalImportStarted = false
                Log.e("MainViewModel", "importPortal", e)
            }
        }
    }

    fun ensureChannelStatus(channel: PlaylistItem) {
        if (UserSettings.offlineMode) return
        val now = System.currentTimeMillis()
        val current = _channelHealth.value[channel.id]
        val last = channelProbeTimes[channel.id] ?: 0L
        if (current == ChannelHealth.CHECKING) return
        if (current != null && now - last < 5 * 60_000L) return // 5 دقايق — توفير بطارية
        _channelHealth.value = _channelHealth.value + (channel.id to ChannelHealth.CHECKING)
        viewModelScope.launch(Dispatchers.IO) {
            val ok = probeChannel(channel)
            channelProbeTimes[channel.id] = System.currentTimeMillis()
            _channelHealth.value = _channelHealth.value + (channel.id to if (ok) ChannelHealth.ONLINE else ChannelHealth.OFFLINE)
        }
    }

    /**
     * فحص جماعي خفيف للبطارية:
     * - مفيش إعادة فحص ONLINE إلا بعد 15 دقيقة
     * - OFFLINE/IDLE بعد 3 دقايق بس
     * - توازي أقل (4) وحد أقصى 40 قناة في الطلب
     */
    fun probeAllChannels(channels: List<PlaylistItem>, maxParallel: Int = 4) {
        if (UserSettings.offlineMode) return
        if (channels.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val queue = channels.filter { ch ->
                val cur = _channelHealth.value[ch.id]
                if (cur == ChannelHealth.CHECKING) return@filter false
                val last = channelProbeTimes[ch.id] ?: 0L
                val minGap = when (cur) {
                    ChannelHealth.ONLINE -> 15 * 60_000L   // 15 دقيقة
                    ChannelHealth.OFFLINE -> 3 * 60_000L   // 3 دقايق
                    else -> 0L                            // IDLE: أول مرة
                }
                now - last >= minGap
            }.take(40)
            if (queue.isEmpty()) return@launch
            val checking = queue.associate { it.id to ChannelHealth.CHECKING }
            _channelHealth.value = _channelHealth.value + checking
            queue.chunked(maxParallel.coerceIn(1, 4)).forEach { batch ->
                coroutineScope {
                    val jobs = batch.map { ch ->
                        launch {
                            val ok = try { probeChannel(ch) } catch (_: Throwable) { false }
                            channelProbeTimes[ch.id] = System.currentTimeMillis()
                            _channelHealth.value = _channelHealth.value + (ch.id to if (ok) ChannelHealth.ONLINE else ChannelHealth.OFFLINE)
                        }
                    }
                    jobs.forEach { it.join() }
                }
            }
        }
    }

    private fun probeChannel(channel: PlaylistItem): Boolean {
        val url = channel.url.trim()
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return false
        val lower = url.lowercase()

        fun requestBuilder(methodGet: Boolean, withRange: Boolean): Request.Builder {
            val b = Request.Builder().url(url)
            if (!methodGet) b.head()
            channel.httpUserAgent?.takeIf { it.isNotBlank() }?.let { b.header("User-Agent", it) }
                ?: b.header("User-Agent", "Mozilla/5.0 YouseifPlayer/24")
            channel.httpReferrer?.takeIf { it.isNotBlank() }?.let { b.header("Referer", it) }
            b.header("Accept", "*/*")
            if (withRange) b.header("Range", "bytes=0-1023")
            return b
        }

        fun looksLikeMedia(bodyStart: String?, contentType: String?): Boolean {
            val ct = (contentType ?: "").lowercase()
            if (ct.contains("text/html") || ct.contains("application/json") && bodyStart?.contains("#EXT") != true) return false
            if (ct.contains("mpegurl") || ct.contains("m3u8") || ct.contains("mp2t") ||
                ct.contains("video/") || ct.contains("audio/") || ct.contains("application/octet") ||
                ct.contains("dash") || ct.contains("mp4") || ct.contains("matroska")) return true
            val b = bodyStart ?: return ct.isNotBlank() && !ct.contains("text/")
            val s = b.trimStart()
            if (s.startsWith("#EXTM3U") || s.startsWith("#EXTINF") || s.startsWith("#EXT-X")) return true
            if (s.startsWith("<?xml") && s.contains("MPD", ignoreCase = true)) return true
            // binary-ish media often has nulls / non-printables early
            if (s.length >= 4 && !s.take(32).all { it.code < 128 && (it.isLetterOrDigit() || it in " \\t\\n#./:-_?=,&") }) return true
            return false
        }

        // Prefer small GET for stream detection (HEAD often lies on CDNs)
        try {
            channelProbeClient.newCall(requestBuilder(true, true).build()).execute().use { r ->
                if (r.code !in 200..399) return@use
                val ct = r.header("Content-Type")
                val peek = try { r.body?.source()?.let { src ->
                    src.request(2048)
                    src.buffer.clone().readUtf8(minOf(2048L, src.buffer.size))
                } } catch (_: Throwable) { null }
                if (looksLikeMedia(peek, ct)) return true
                // m3u8 path without proper content-type
                if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains("/live/") || lower.contains("playlist")) {
                    if (peek?.contains("#EXT") == true || peek?.contains("MPD") == true) return true
                }
            }
        } catch (_: Throwable) {}

        // Fallback HEAD + content-type only (strict)
        return try {
            channelProbeClient.newCall(requestBuilder(false, false).build()).execute().use { r ->
                if (r.code !in 200..399) return false
                looksLikeMedia(null, r.header("Content-Type"))
            }
        } catch (_: Throwable) {
            false
        }
    }

    /** GoLive lists omit episode links; details at /content/series/{id} contain them. */
    private suspend fun loadGoLiveCatalog() {
        if (goLiveLoaded) return
        val (movies, series) = GoLiveApi.catalog()
        val movieItems = movies.mapIndexed { i, m -> PlaylistItem("golive_movie_${m.id}", i + 1, m.title, "golive://movie/${m.id}", "اليوم", m.poster, "AR", false) }
        val seriesItems = series.mapIndexed { i, m -> PlaylistItem("golive_series_${m.id}", i + 1, m.title, "golive://series/${m.id}", "اليوم • مسلسلات", m.poster, "AR", false) }
        withContext(Dispatchers.Main) {
            _films.value = (movieItems + _films.value).distinctBy { it.id }
            _series.value = (seriesItems + _series.value).distinctBy { it.id }
            goLiveLoaded = true
        }
    }

    fun openGoLiveItem(item: PlaylistItem) {
        val parts = item.url.removePrefix("golive://").split("/", limit = 2)
        if (parts.size != 2) return
        val kind = parts[0]; val id = parts[1]
        hikayeMode = false
        _detailState.value = DetailState(title = item.name, mediaKind = "golive_$kind", loading = true)
        lastResolvedTitle = item.name
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (kind == "series") {
                    val d = GoLiveApi.seriesDetail(id)
                    val s = d?.let { FaselSeries(0, it.media.title, it.media.overview, it.media.poster, seasons = it.episodes.groupBy { e -> e.season }.toSortedMap().map { (n, eps) -> FaselSeason(n, "الموسم $n", eps.sortedBy { e -> e.number }.map { e -> FaselEpisode(0, e.number, e.title, videos = e.sources.map { v -> FaselVideo(v.label, v.url, hd = true, hls = v.url.contains(".m3u8", true)) }) }) }) }
                    _detailState.value = _detailState.value.copy(series = s, loading = false, error = if (s == null) "لا توجد حلقات لهذا العمل" else "")
                } else {
                    val d = GoLiveApi.movieDetail(id)
                    val detail = d?.let { FaselDetail(0, it.media.title, it.media.overview, it.media.poster, release = it.media.year.toString(), videos = it.sources.map { v -> FaselVideo(v.label, v.url, hd = true, hls = v.url.contains(".m3u8", true)) }) }
                    _detailState.value = _detailState.value.copy(detail = detail, loading = false, error = if (detail == null) "لا توجد روابط لهذا الفيلم" else "")
                }
            } catch (e: Throwable) { _detailState.value = _detailState.value.copy(loading = false, error = e.message ?: "خطأ في تحميل التفاصيل") }
        }
    }

    fun openAlooySeries(item: PlaylistItem) {
        hikayeMode = false
        _detailState.value = DetailState(title = item.name, mediaKind = "alooy_series", loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val series = AlooyTvApi.loadSeries(item.url, item.name)
                _detailState.value = _detailState.value.copy(series = series, loading = false,
                    error = if (series == null || series.seasons.all { it.episodes.isEmpty() }) "لا توجد حلقات لهذا العمل" else "")
            } catch (e: Throwable) {
                _detailState.value = _detailState.value.copy(loading = false, error = e.message ?: "تعذر تحميل الحلقات")
            }
        }
    }

    /** Opens an البصري genre folder and turns its cards into real series/cartoon
     *  entries (name + poster + page link) so the library is never blank. */
    fun openAlooyGenre(item: PlaylistItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cards = AlooyTvApi.loadGenre(item.url)
                val base = item.group.substringAfter(':', item.name).trim().ifBlank { item.name }
                val isCartoon = item.group.startsWith("CARTOONS:")
                val mapped = cards.map { c ->
                    PlaylistItem(
                        id = "alooy_" + c.pageUrl.substringAfterLast('/').substringBefore(".html").ifBlank { "x${c.title.hashCode()}" },
                        channelNumber = 0,
                        name = c.title,
                        url = c.pageUrl,
                        group = (if (isCartoon) "CARTOONS:" else "SERIES:") + base,
                        logoUrl = c.poster,
                        language = "AR",
                        isLive = false,
                        isCustom = false
                    )
                }
                if (mapped.isEmpty()) { toast("مفيش نتائج من ${item.name}"); return@launch }
                withContext(Dispatchers.Main) {
                    if (isCartoon) _cartoons.value = (mapped + _cartoons.value).distinctBy { it.id }
                    else _series.value = (mapped + _series.value).distinctBy { it.id }
                    toast("تم جلب ${mapped.size} عمل من ${item.name}")
                }
            } catch (e: Throwable) {
                toast("تعذر جلب القائمة: ${e.message ?: "خطأ"}")
            }
        }
    }

    // ----------------------------------------------------------------- playback
    fun playContentItem(item: PlaylistItem) {
        playerController.setCurrentContent(item)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val marker = item.url.removePrefix("faselhd://")
                if (marker == item.url) {
                    withContext(Dispatchers.Main) { playerController.playChannel(item) }
                    return@launch
                }
                val parts = marker.split("/")
                if (parts.size < 2) { withContext(Dispatchers.Main) { playerController.playChannel(item) }; return@launch }
                val type = parts[0]
                val id = parts[1].toIntOrNull() ?: run {
                    withContext(Dispatchers.Main) { playerController.playChannel(item) }; return@launch
                }
                val video = when (type) {
                    "series" -> FaselHdApi.series(id)?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.videos?.firstOrNull()
                    "anime" -> FaselHdApi.animeDetail(id)?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.videos?.firstOrNull()
                    else -> FaselHdApi.detail(id)?.videos?.firstOrNull()
                } ?: run {
                    withContext(Dispatchers.Main) { playerController.playChannel(item) }; return@launch
                }
                val hosts = hostsCache ?: FaselHdApi.hostsConfig().also { hostsCache = it }
                val res = HostResolver.resolve(video, hosts, getApplication<Application>().applicationContext)
                val target = res.qualities.firstOrNull { !it.url.contains(".m3u8") }?.url
                    ?: res.qualities.firstOrNull()?.url
                    ?: res.directUrl
                if (target.isNullOrBlank()) {
                    // Protected page & no direct stream → open the page in the in-app WebView
                    withContext(Dispatchers.Main) { playerController.playWebEmbed(video.link, item.name) }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    playerController.playUrl(target, item.name, false, video.userAgent, video.referer ?: faselReferer(video.link))
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "playContentItem failed for ${item.name}", e)
                withContext(Dispatchers.Main) { playerController.playChannel(item) }
            }
        }
    }

    fun deleteChannel(channel: PlaylistItem) {
        // حماية: ما نمسحش مصادر مدمجة (فاصل / فاير باس / اليوم / حكاية)
        if (!channel.isCustom && !channel.group.startsWith("LIVE:", ignoreCase = true)) return
        viewModelScope.launch(Dispatchers.IO) {
            try { channelDao.deleteChannel(channel) } catch (e: Throwable) {}
        }
    }

    /** حذف دفعة واحدة على IO — يمنع ANR وتلعثم الواجهة.
     *  يحذف المستورد فقط (isCustom أو LIVE:) — فاصل / فاير باس / اليوم / حكاية ما يتأثروا أبداً. */
    fun deleteChannelsByIds(ids: Collection<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (id in ids.distinct()) {
                    try {
                        val ch = channelDao.getChannelById(id) ?: continue
                        // حماية المصادر المدمجة
                        if (!ch.isCustom && !ch.group.startsWith("LIVE:", ignoreCase = true)) continue
                        channelDao.deleteChannel(ch)
                    } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "deleteChannelsByIds", e)
            }
        }
    }

    fun deleteAllCustomChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                channelDao.clearCustomChannels()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "deleteAllCustom", e)
            }
        }
    }

    fun resetDefaultChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            try { channelDao.clearBuiltInChannels() } catch (e: Throwable) {}
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyDao.clearHistory()
                _searchResults.value = null
                UserSettings.catalogRefreshAt = 0L
                Log.i("MainViewModel", "soft cache cleared (history + catalog TTL reset)")
            } catch (e: Throwable) {}
        }
    }

    /** Wipe built-in channel rows and force a full catalog re-download. */
    fun clearChannelCacheAndRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favIds = try { channelDao.getFavoriteIds() } catch (_: Throwable) { emptyList() }
                channelDao.clearBuiltInChannels()
                UserSettings.catalogRefreshAt = 0L
                refreshLiveCatalogInternal(force = true)
                // Re-apply favorites after reload
                for (id in favIds) {
                    try { channelDao.setFavorite(id, true) } catch (_: Throwable) {}
                }
            } catch (e: Throwable) {
                Log.e("MainViewModel", "clearChannelCacheAndRefresh failed", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT release the player here. Releasing on ViewModel clear was causing
        // sudden app death when switching screens / config changes / brief process reclaim.
        // PlaybackService owns background lifetime; player stays until process dies or explicit stop.
    }

    /**
     * FIX #3 (التقل الجامد في الاسترداد): insert على دفعات + delay بين الدفعات،
     * يمنع ANR لما الـ m3u فيه عشرات الآلاف (زي iptv-org). Stable IDs من الـ URL
     * تخلي REPLACE يشتغل بدل ما يضاعف الصفوف.
     */
    private suspend fun insertChannelsChunked(items: List<PlaylistItem>, batchSize: Int = 800) {
        if (items.isEmpty()) return
        // Deduplicate by id inside the batch so Room REPLACE doesn't thrash.
        val unique = items.distinctBy { it.id }
        val size = unique.size
        // Adaptive batch: fewer Flow emissions for huge lists.
        val step = when {
            size > 20_000 -> 1_500
            size > 8_000 -> 1_000
            else -> batchSize
        }
        var imported = 0
        for (i in unique.indices step step) {
            val end = minOf(i + step, size)
            channelDao.insertChannels(unique.subList(i, end))
            imported += (end - i)
            // Yield so UI / Flow collectors can breathe; longer yield on big lists.
            kotlinx.coroutines.delay(if (size > 10_000) 24L else 10L)
            if (imported > 0 && imported % 4_000 == 0) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "⏳ جاري الاسترداد… $imported / $size",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
