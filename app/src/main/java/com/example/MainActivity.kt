package com.example

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.UserSettings
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.applyPlayerTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var isInPipMode by androidx.compose.runtime.mutableStateOf(false)
    private var selectedTheme by mutableStateOf("GOLDEN HOUR")
    private var pendingImportKind by mutableStateOf<String?>(null)
    private var pendingImportPayload by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent silent process death from secondary threads (network, audiofx, exo)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                android.util.Log.e("YouseifCrash", "Uncaught on ${thread.name}: ${throwable.message}", throwable)
            } catch (_: Throwable) {}
            // Swallow non-fatal crashes from media/audio/network worker threads so the UI stays alive.
            val name = thread.name.orEmpty().lowercase()
            val msg = (throwable.message ?: "").lowercase()
            val isMediaWorker = name.contains("exo") || name.contains("audio") ||
                name.contains("okhttp") || name.contains("media") || name.contains("loader") ||
                msg.contains("mediacodec") || msg.contains("audiotrack") || msg.contains("loudness")
            if (isMediaWorker && thread != Looper.getMainLooper().thread) {
                android.util.Log.w("YouseifCrash", "Suppressed media-worker crash — process kept alive")
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, throwable)
        }
        try { UserSettings.init(applicationContext) } catch (_: Throwable) {}
        // درع السلامة — Release يغلق التطبيق عند التلاعب / الروت / الديباجر
        // Sentinel v25.4 wiring — silent for the original build
        try {
            com.example.security.Sentinel.activate(applicationContext)
        } catch (_: Throwable) {}
        try {
            com.example.security.Sentinel.callGuard(this)
        } catch (_: Throwable) {}
        try {
            val vPrefs = getSharedPreferences("y0s3if_vlt", MODE_PRIVATE)
            if (vPrefs.getBoolean("y0s3if_ln_lock", false)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("⚠️")
                    .setMessage(com.example.security.Sentinel.revealNote())
                    .setCancelable(false)
                    .setPositiveButton("إغلاق") { d, _ ->
                        d.dismiss()
                        com.example.security.Sentinel.shutdown()
                    }
                    .show()
            }
        } catch (_: Throwable) {}
        // تهيئة Google Cast مبكّرًا (آمن لو Play Services مش موجودة)
        try { com.example.player.CastHelper.init(applicationContext) } catch (_: Throwable) {}
        // مراقبة دورية ضد الـ inject أثناء التشغيل
        try {
            window.decorView.postDelayed(object : Runnable {
                override fun run() {
                    try {
                        com.example.security.Sentinel.callGuard(this@MainActivity)
                        window.decorView.postDelayed(this, 12_000L)
                    } catch (_: Throwable) {}
                }
            }, 8_000L)
        } catch (_: Throwable) {}
        try {
            selectedTheme = getSharedPreferences("youseif_visual", MODE_PRIVATE)
                .getString("theme", "GOLDEN HOUR") ?: "GOLDEN HOUR"
            if (selectedTheme == "CUSTOM") {
                val argb = getSharedPreferences("youseif_visual", MODE_PRIVATE).getLong("custom_argb", 0xFFC83DFFL)
                com.example.ui.theme.applyCustomAccent(argb, "CUSTOM")
            } else {
                applyPlayerTheme(selectedTheme)
            }
            // restore visual prefs
            val prefs = getSharedPreferences("youseif_visual", MODE_PRIVATE)
            com.example.ui.theme.GlobalGlowIntensity = prefs.getFloat("glow", 0.7f)
            com.example.ui.theme.GlobalFontScale = prefs.getFloat("font_scale", 1f)
            com.example.ui.theme.GlobalIconStyle = prefs.getString("icon_style", "Bold") ?: "Bold"
        } catch (_: Throwable) {
            selectedTheme = "GOLDEN HOUR"
            try { applyPlayerTheme("GOLDEN HOUR") } catch (_: Throwable) {}
        }
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            !getSharedPreferences("youseif_runtime", MODE_PRIVATE).getBoolean("notification_permission_requested", false)) {
            getSharedPreferences("youseif_runtime", MODE_PRIVATE).edit()
                .putBoolean("notification_permission_requested", true).apply()
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 4401)
        }
        // Defer intent handling until after first frame so Activity can open
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val channels by viewModel.channels.collectAsState()
                    val films by viewModel.films.collectAsState()
                    val cartoons by viewModel.cartoons.collectAsState()
                    val series by viewModel.series.collectAsState()
                    val recentHistory by viewModel.recentHistory.collectAsState()

                    val brightness by viewModel.playerController.brightness.collectAsState()
                    androidx.compose.runtime.LaunchedEffect(brightness) {
                        try {
                            val lp = window.attributes
                            lp.screenBrightness = brightness.coerceIn(0.05f, 1f)
                            window.attributes = lp
                        } catch (_: Throwable) {
                        }
                    }

                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        try { handleIncomingIntent(intent) } catch (_: Throwable) {}
                    }
                    MainScreen(
                        controller = viewModel.playerController,
                        isInPipMode = isInPipMode,
                        viewModel = viewModel,
                        channels = channels,
                        films = films,
                        cartoons = cartoons,
                        series = series,
                        recentHistory = recentHistory,
                        selectedTheme = selectedTheme,
                        onThemeSelected = { theme ->
                            if (selectedTheme != theme) {
                                selectedTheme = theme
                                if (theme != "CUSTOM") applyPlayerTheme(theme)
                                getSharedPreferences("youseif_visual", MODE_PRIVATE)
                                    .edit().putString("theme", theme).apply()
                            }
                        },
                        onCustomColor = { argb ->
                            com.example.ui.theme.applyCustomAccent(argb, "CUSTOM")
                            selectedTheme = "CUSTOM"
                            getSharedPreferences("youseif_visual", MODE_PRIVATE)
                                .edit()
                                .putString("theme", "CUSTOM")
                                .putLong("custom_argb", argb)
                                .apply()
                        },
                        onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                        onContentSelected = { item ->
                            val playable = (channels + films + series + cartoons).distinctBy { it.id }
                            viewModel.playerController.setContextQueue(playable, item.id)
                            viewModel.playContentItem(item)
                        },
                        onAddCustomChannel = { ch -> viewModel.addCustomChannel(ch) },
                        onImportM3U = { m3u -> viewModel.importM3U(m3u) },
                        onImportFromUrl = { url -> viewModel.importFromUrl(url) },
                        onImportZipBytes = { bytes -> viewModel.importFromZipBytes(bytes) },
                        onImportPortalCode = { code -> viewModel.importPortalCode(code) },
                        onDeleteChannel = { ch -> viewModel.deleteChannel(ch) },
                        onResetChannels = { viewModel.resetDefaultChannels() },
                        onClearCache = { viewModel.clearCache() },
                        onRefreshCatalog = { viewModel.refreshLiveCatalog(force = true) },
                        onClearChannelCache = { viewModel.clearChannelCacheAndRefresh() },
                        onClearHistory = { viewModel.clearCache() },
                        onHistoryItem = { item -> viewModel.recordHistory(item) },
                        onHistoryUrl = { title, url, group -> viewModel.recordHistory(title, url, group) },
                        onSubtitleUrlSaved = { url ->
                            getSharedPreferences("youseif_subtitles", MODE_PRIVATE).edit()
                                .putString("subtitle_url", url).apply()
                        },
                        onSubtitleFileSelected = { uri ->
                            getSharedPreferences("youseif_subtitles", MODE_PRIVATE).edit()
                                .putString("subtitle_uri", uri.toString()).apply()
                            try { viewModel.playerController.applyExternalSubtitle(uri.toString()) } catch (_: Throwable) {}
                        }
                    )
                    // نافذة نوع الاستيراد لأي قائمة خارجية (من تيليجرام/المتصفح…)
                    if (pendingImportKind != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { pendingImportKind = null; pendingImportPayload = null },
                            containerColor = com.example.ui.theme.DarkCardBg,
                            title = {
                                androidx.compose.material3.Text(
                                    "استرداد قائمة",
                                    color = com.example.ui.theme.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.Text(
                                        "اختر نوع المحتوى عشان يتصنّف في مكانه الصح:",
                                        color = com.example.ui.theme.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    listOf(
                                        "CHANNELS" to "قنوات",
                                        "FILMS" to "أفلام",
                                        "SERIES" to "مسلسلات",
                                        "ANIME" to "أنمي",
                                        "RADIO" to "راديو",
                                        "SUBTITLE" to "ترجمة (ملف/رابط)"
                                    ).forEach { (key, label) ->
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                val payload = pendingImportPayload.orEmpty()
                                                try {
                                                    if (key == "SUBTITLE") {
                                                        viewModel.playerController.applyExternalSubtitle(payload)
                                                    } else {
                                                        viewModel.importPlaylistSmart(
                                                            payload,
                                                            com.example.ui.MainViewModel.ImportTarget.valueOf(key),
                                                            ""
                                                        )
                                                    }
                                                } catch (_: Throwable) {}
                                                pendingImportKind = null
                                                pendingImportPayload = null
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = com.example.ui.theme.NeonRedContainer
                                            )
                                        ) {
                                            androidx.compose.material3.Text(label, color = com.example.ui.theme.TextPrimary)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { pendingImportKind = null; pendingImportPayload = null }
                                ) {
                                    androidx.compose.material3.Text("إلغاء", color = com.example.ui.theme.TextSecondary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * تقليب المحتوى من أزرار الريموت / الكيبورد:
     * CHANNEL UP/DOWN, MEDIA NEXT/PREVIOUS, PAGE UP/DOWN، وأزرار الاتجاهات
     * (أزرار الاتجاهات تعمل فقط داخل المشغل حتى لا تتعارض مع تنقل الواجهة).
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val k = event.keyCode
            val isDpad = k == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                k == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                k == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                k == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
            if (!isDpad || com.example.player.KeyRouter.dpadEnabled) {
                when (k) {
                    android.view.KeyEvent.KEYCODE_CHANNEL_UP,
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    android.view.KeyEvent.KEYCODE_PAGE_UP,
                    android.view.KeyEvent.KEYCODE_DPAD_UP,
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT ->
                        if (com.example.player.KeyRouter.dispatch(-1)) return true

                    android.view.KeyEvent.KEYCODE_CHANNEL_DOWN,
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT,
                    android.view.KeyEvent.KEYCODE_PAGE_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT ->
                        if (com.example.player.KeyRouter.dispatch(1)) return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPipMode = isInPictureInPictureMode
    }

        override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            val data = intent.dataString
            when {
                action == android.content.Intent.ACTION_VIEW && !data.isNullOrBlank() -> {
                    // ملف قائمة (.m3u) جاي من تطبيق تاني → نسأل نوعه بدل ما يشتغل كرابط
                    val low = data.lowercase()
                    val looksPlaylist = low.endsWith(".m3u") || low.contains(".m3u?") || low.contains("#extm3u")
                    if (looksPlaylist) {
                        val text = try {
                            if (low.startsWith("content://")) {
                                contentResolver.openInputStream(android.net.Uri.parse(data))
                                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                            } else data
                        } catch (_: Throwable) { "" }
                        if (text.isNotBlank()) {
                            pendingImportPayload = text
                            pendingImportKind = ""
                        }
                    } else {
                        viewModel.playerController.playUrl(data, title = data.substringAfterLast('/').ifBlank { "Opened file" })
                    }
                }
                action == android.content.Intent.ACTION_SEND -> {
                    val shared = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                    if (!shared.isNullOrBlank()) {
                        val url = shared.lineSequence().map { it.trim() }.firstOrNull {
                            it.startsWith("http://") || it.startsWith("https://") || it.startsWith("rtmp")
                        } ?: shared.trim()
                        viewModel.playerController.playUrl(url, title = "Shared")
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "handleIncomingIntent: ${e.message}")
        }
    }
}
