package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.ActiveThemeName
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardDeep
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedDark
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette
import com.example.ui.theme.availablePlayerThemes
import com.example.ui.theme.applyCustomAccent
import com.example.ui.theme.glassGradient

/**
 * Settings screen mirrors DESIGN_REFERENCE/screens/03_SETTINGS.jpg exactly:
 *   SETTINGS + "App appearance and playback" → 7 card rows → bottom nav.
 */
private fun realLocalIp(): String {
    return try {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
            ?.hostAddress ?: "غير متاح"
    } catch (_: Throwable) { "غير متاح" }
}

@Composable
fun SettingsScreen(
    selectedTheme: String = ActiveThemeName,
    onThemeSelected: (String) -> Unit = {},
    onCustomColor: (Long) -> Unit = {},
    onResetChannels: () -> Unit,
    onClearCache: () -> Unit,
    onRefreshCatalog: () -> Unit = {},
    onClearChannelCache: () -> Unit = {},
    onNavigateToChannels: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSubtitleUrlSaved: (String) -> Unit = {},
    onSubtitleFileSelected: (Uri) -> Unit = {},
    onImportM3U: (String) -> Unit = {},
    onImportFromUrl: (String) -> Unit = {},
    onImportZipBytes: (ByteArray) -> Unit = {},
    onImportPortalCode: (String) -> Unit = {},
    gateways: List<com.example.data.GatewayEntry> = emptyList(),
    onSetGateway: (String, Boolean) -> Unit = { _, _ -> },
    onSetAllGateways: (Boolean) -> Unit = {},
    hideImages: Boolean = false,
    onHideImagesChange: (Boolean) -> Unit = {},
    /** Opens the same quick drawer as Home, so the header menu button is not a dead button. */
    onOpenMenu: (() -> Unit)? = null,
    controller: com.example.player.YouseifPlayerController? = null
) {
    var cleared by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPlayback by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    var networkOpen by remember { mutableStateOf(false) }
    var networkMbps by remember { mutableStateOf(1.2f) }
    var customHex by remember { mutableStateOf("#C83DFF") }
    var customHue by remember { mutableStateOf(285f) }
    var customSat by remember { mutableStateOf(0.88f) }
    var customBri by remember { mutableStateOf(1.0f) }
    var playlistUrl by remember { mutableStateOf("") }
    var pasteText by remember { mutableStateOf("") }
    var showPortal by remember { mutableStateOf(false) }
    var portalCode by remember { mutableStateOf("") }
    var showGateways by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // MUST be called unconditionally (Compose rule) — not inside if (showRestore)
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val name = (uri.lastPathSegment ?: uri.toString()).lowercase()
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@rememberLauncherForActivityResult
            if (name.endsWith(".zip") || "zip" in name) {
                onImportZipBytes(bytes)
            } else {
                onImportM3U(bytes.toString(Charsets.UTF_8))
            }
            showRestore = false
        } catch (_: Throwable) {
        }
    }

    val subtitleFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onSubtitleFileSelected(uri)
        }
    }

    Box(Modifier.fillMaxSize().visualScreenBackground()) {
        Column(Modifier.fillMaxSize()) {
        // FIX: the top bar is now full-bleed (only its own 18.dp inner padding), exactly
        // like Home / Channels / Films / Radio. Previously it sat INSIDE the LazyColumn's
        // 20.dp horizontal padding, so it received 18 + 20 = 38.dp per side: the menu and
        // theme buttons were indented further than the cards below and the whole header
        // looked shrunk / out of line with the rest of the player.
        PlayerTopBar(onMenu = onOpenMenu, onRefresh = null, onInfo = null)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("SETTINGS", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("App appearance and playback", color = TextMuted, fontSize = 12.sp)
                }
            }
            item { SettingsVisualRow(Icons.Default.Palette, "Colors & Appearance", "Presets · custom color · icons · font") { showTheme = true } }
            item { SettingsVisualRow(Icons.Default.Download, "استرداد قنوات / إضافة قائمة", "From URL or from file (.m3u / .m3u8 / .txt)") { showRestore = true } }
            item { SettingsVisualRow(Icons.Default.CloudDownload, "تحميل كتالوج البوابة", "اكتب كود البوابة لتحميل كل القنوات والتصنيفات") { showPortal = true } }
            item {
                val gwEnabled = gateways.any { it.enabled }
                SettingsVisualRow(
                    icon = Icons.Default.Tune,
                    title = "تشغيل / إطفاء المواقع (Gateway)",
                    subtitle = if (gwEnabled) "${gateways.count { it.enabled }} من ${gateways.size} مواقع مفعّلة" else "كل المواقع مطفأة",
                    active = false,
                    onClick = { showGateways = !showGateways }
                )
            }
            item {
                SettingsVisualRow(
                    icon = Icons.Default.Image,
                    title = "إيقاف تحميل الصور (توفير النت)",
                    subtitle = if (hideImages) "الصور مخفية — لا تحميل أي صور من أي موقع" else "الصور تعمل بشكل عادي",
                    active = hideImages,
                    onClick = { onHideImagesChange(!hideImages) }
                )
            }
            if (showGateways) {
                item {
                    val gp = activePlayerPalette()
                    Column(Modifier.fillMaxWidth()) {
                        Text("اختر المواقع التي تعمل معك (لك حرية الاختيار)", color = gp.accentGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                        if (gateways.isEmpty()) {
                            Text("لا توجد مواقع مسجلة حالياً", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        gateways.forEach { g ->
                            SettingsVisualRow(
                                icon = if (g.enabled) Icons.Default.Check else Icons.Default.Close,
                                title = g.name,
                                subtitle = if (g.enabled) "مفعّل — يظهر محتواه في الفولدرات" else "مطفأ — مخفي من الشاشات",
                                active = g.enabled,
                                onClick = { onSetGateway(g.id, !g.enabled) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        SettingsVisualRow(
                            icon = Icons.Default.Check,
                            title = "تفعيل الكل",
                            subtitle = "تشغيل كل المواقع جنب بعض (نظام فولدر)",
                            active = false,
                            onClick = { onSetAllGateways(true) }
                        )
                        Spacer(Modifier.height(6.dp))
                        SettingsVisualRow(
                            icon = Icons.Default.Close,
                            title = "إطفاء الكل",
                            subtitle = "إيقاف كل المواقع مرة واحدة",
                            active = false,
                            onClick = { onSetAllGateways(false) }
                        )
                    }
                }
            }
            item { SettingsVisualRow(Icons.Default.Tune, "Playback Settings", "Speed, autoplay and controls") { showPlayback = true } }
            item { SettingsVisualRow(Icons.Default.Info, "Subtitle Settings", "Captions and appearance") { showSubtitle = true } }
            item { SettingsVisualRow(Icons.Default.Refresh, "تحديث قائمة القنوات",
                "يحمّل التصنيفات من جديد فقط — روابط البث تتحدث عند التشغيل") {
                    onRefreshCatalog()
                }
            }
            item { SettingsVisualRow(Icons.Default.RestartAlt, "Restore Defaults", "Restore colors, icons and font") { onThemeSelected("AURORA FLOW"); onResetChannels(); cleared = false } }
            item { SettingsVisualRow(Icons.Default.DeleteOutline, "مسح الكاش / السجل",
                if (cleared) "تم المسح — القنوات محفوظة محليًا" else "يمسح السجل ويصفّر مدة الكاش (القنوات تفضل محفوظة)") {
                    onClearCache(); onClearHistory(); cleared = true
                }
            }
            item { SettingsVisualRow(Icons.Default.DeleteOutline, "مسح قنوات وإعادة التحميل",
                "يحذف القنوات المدمجة ويعيد تنزيلها (يوفر مساحة / يصلح قائمة فاسدة)") {
                    onClearChannelCache()
                }
            }
            item {
            val p = activePlayerPalette()
            SettingsVisualRow(
                icon = Icons.Default.CloudDownload,
                title = "Network & Data",
                subtitle = if (networkOpen) "اختياري — الفيديو يشتغل بدون Data Saver" else "الفيديو يشتغل بدون تفعيل Data Saver",
                active = false,
                onClick = { networkOpen = !networkOpen }
            )
            if (networkOpen) {
            Text("NETWORK & DATA CONTROLS", color = p.accentGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp))
            SettingsVisualRow(
                icon = Icons.Default.CloudDownload,
                title = "Data Saver",
                subtitle = "Limit video bitrate to save mobile data",
                active = controller?.dataSaverEnabled?.collectAsState()?.value == true,
                onClick = {
                    controller?.let { c -> c.setDataSaver(!c.dataSaverEnabled.value, maxMbps = 1.2f) }
                }
            )
            Spacer(Modifier.height(8.dp))
            SettingsVisualRow(
                icon = Icons.Default.Phone,
                title = "Audio Only",
                subtitle = "Disable video track — audio continues, less data",
                active = controller?.audioOnlyMode?.collectAsState()?.value == true,
                onClick = {
                    controller?.let { c -> c.setAudioOnlyMode(!c.audioOnlyMode.value) }
                }
            )
            Spacer(Modifier.height(8.dp))
            SettingsVisualRow(
                icon = Icons.Default.Speed,
                title = "Soft cap ~0.8 Mbps",
                subtitle = "Optional ceiling only — does not enable Data Saver",
                onClick = { networkMbps = 0.8f; controller?.setMaxNetworkMbps(networkMbps) }
            )
            Text("Soft bitrate ceiling: ${"%.2f".format(networkMbps)} Mbps (Data Saver stays independent)", color = p.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            Slider(
                value = networkMbps,
                onValueChange = { networkMbps = it },
                onValueChangeFinished = { controller?.setMaxNetworkMbps(networkMbps) },
                valueRange = 0.25f..8f,
                colors = SliderDefaults.colors(thumbColor = p.accentGlow, activeTrackColor = p.accent),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            SettingsVisualRow(
                icon = Icons.Default.Speed,
                title = "Unlimited quality",
                subtitle = "Clear soft cap + turn Data Saver / Audio Only OFF",
                onClick = { controller?.disableNetworkControls() }
            )
            Text("الفيديو يشتغل بجودة كاملة تلقائيًا. لو الشبكة ضعيفة التطبيق بيقلل الجودة لوحده بدون ما تفعّل Data Saver. Data Saver اختياري لتوفير الباقة.", color = p.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            val ctx = LocalContext.current
            val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val type = when {
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi‑Fi"
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile data"
                caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Offline / unknown"
            }
            SettingsVisualRow(Icons.Default.Public, "Connection", "$type · IP ${realLocalIp()}") { }
        }

        item { SettingsVisualRow(Icons.Default.Info, "About", "Youseif Player Pro") { about = true } }
        }
        }
    }

    // Colors & Appearance sheet — 8 presets + Apply

    if (showRestore) {
        var step by remember { mutableStateOf(0) } // 0=menu, 1=url, 2=paste
        VisualModalSheet(
            if (step == 0) "إضافة ملف / قائمة" else if (step == 1) "إضافة من رابط" else "لصق قائمة M3U",
            onClose = { showRestore = false; step = 0 }
        ) {
            when (step) {
                0 -> {
                    Text("اختر طريقة الاستيراد", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    RestoreOptionCard(
                        icon = Icons.Default.Public,
                        title = "إضافة من رابط (URL)",
                        subtitle = "M3U / M3U8 / Xtream أو رابط وسائط مباشر"
                    ) { step = 1 }
                    Spacer(Modifier.height(10.dp))
                    RestoreOptionCard(
                        icon = Icons.Default.FolderOpen,
                        title = "فتح ملف",
                        subtitle = "اختيار قائمة أو ملف وسائط من التخزين"
                    ) { filePicker.launch("*/*") }
                    Spacer(Modifier.height(10.dp))
                    RestoreOptionCard(
                        icon = Icons.Default.FolderOpen,
                        title = "فتح ملف مضغوط",
                        subtitle = "استخراج قائمة أو ملف فيديو مدعوم — ZIP"
                    ) { filePicker.launch("application/zip") }
                    Spacer(Modifier.height(10.dp))
                    RestoreOptionCard(
                        icon = Icons.Default.ContentPaste,
                        title = "لصق محتوى M3U",
                        subtitle = "الصق نص القائمة مباشرة"
                    ) { step = 2 }
                }
                1 -> {
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Playlist URL", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonRedGlow,
                            unfocusedBorderColor = CrimsonBorder
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(NeonRedContainer).border(1.dp, NeonRed, RoundedCornerShape(16.dp))
                            .clickable {
                                if (playlistUrl.isNotBlank()) {
                                    onImportFromUrl(playlistUrl.trim())
                                    showRestore = false
                                    step = 0
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("استيراد من الرابط", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        label = { Text("Paste M3U content", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonRedGlow,
                            unfocusedBorderColor = CrimsonBorder
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                            .background(NeonRedContainer).border(1.dp, NeonRed, RoundedCornerShape(16.dp))
                            .clickable {
                                if (pasteText.isNotBlank()) {
                                    onImportM3U(pasteText)
                                    showRestore = false
                                    step = 0
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("استيراد", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPortal) {
        AlertDialog(
            onDismissRequest = { showPortal = false },
            title = { Text("تحميل قنوات البوابة", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = portalCode,
                    onValueChange = { portalCode = it },
                    singleLine = true,
                    label = { Text("كود البوابة", color = TextMuted) },
                    placeholder = { Text("اكتب كود البوابة", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonRedGlow,
                        unfocusedBorderColor = CrimsonBorder
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (portalCode.trim().isNotBlank()) {
                        onImportPortalCode(portalCode.trim())
                        portalCode = ""
                        showPortal = false
                    }
                }) { Text("تحميل", color = NeonRedGlow) }
            },
            dismissButton = { TextButton(onClick = { showPortal = false }) { Text("إلغاء", color = TextMuted) } }
        )
    }

    if (showTheme) {
        val ctx = LocalContext.current
        VisualModalSheet("Colors & Appearance", onClose = { showTheme = false }) {
            Text("Theme / custom color applies to buttons AND player icons — icons follow the accent automatically",
                color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            val presets = availablePlayerThemes()
            // scrollable grid for all neon presets
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(360.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = preset.name == selectedTheme
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                            .background(DarkCardBg.copy(alpha = .75f))
                            .border(
                                1.dp, if (isSelected) NeonRedGlow else CrimsonBorder.copy(alpha = .85f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onThemeSelected(preset.name); showTheme = false }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            preset.accentGlow.copy(alpha = .95f),
                                            preset.accent,
                                            preset.accentDark.copy(alpha = .92f)
                                        )
                                    )
                                )
                                .border(1.dp, preset.accentGlow.copy(alpha = .9f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(20.dp).clip(CircleShape).background(preset.accent))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            preset.name.replace("NEON ", "Neon "),
                            color = TextPrimary, fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            Text("Custom color (hex)", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            // customHex state is at top of SettingsScreen
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customHex,
                    onValueChange = { customHex = it },
                    modifier = Modifier.weight(1f).height(52.dp),
                    singleLine = true,
                    placeholder = { Text("#RRGGBB", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonRedGlow,
                        unfocusedBorderColor = CrimsonBorder
                    )
                )
                Box(
                    Modifier.height(46.dp).clip(RoundedCornerShape(14.dp))
                        .background(NeonRedContainer).border(1.dp, NeonRed, RoundedCornerShape(14.dp))
                        .clickable {
                            val cleaned = customHex.trim().removePrefix("#").replace(" ", "")
                            val expanded = when (cleaned.length) {
                                3 -> cleaned.map { "$it$it" }.joinToString("") // RGB -> RRGGBB
                                4 -> cleaned.map { "$it$it" }.joinToString("") // ARGB short
                                else -> cleaned
                            }
                            val value = expanded.toLongOrNull(16)
                            if (value != null) {
                                val argb = when (expanded.length) {
                                    6 -> (0xFF000000L or value)
                                    8 -> value
                                    else -> (0xFF000000L or value)
                                }
                                onCustomColor(argb)
                                onThemeSelected("CUSTOM")
                                // keep sheet open so user sees the change immediately
                            }
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Apply", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("مختبر الألوان 3D — أي لون في الدنيا", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            // Live 3D swatch preview (full HSV space)
            Box(
                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.hsv(customHue, customSat, customBri),
                                Color.hsv((customHue + 160f) % 360f, customSat, customBri)
                            )
                        )
                    )
                    .border(2.dp, activePlayerPalette().accentGlow, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "H ${customHue.toInt()}° · S ${(customSat * 100).toInt()}% · V ${(customBri * 100).toInt()}%",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(10.dp))
            // Hue slider — full 360 spectrum
            Text("Hue (درجة اللون)", color = TextMuted, fontSize = 11.sp)
            Slider(
                value = customHue,
                onValueChange = { customHue = it },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = activePlayerPalette().accentGlow,
                    activeTrackColor = activePlayerPalette().accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // Saturation slider — 0..1 (3D depth)
            Text("Saturation (التشبع)", color = TextMuted, fontSize = 11.sp)
            Slider(
                value = customSat,
                onValueChange = { customSat = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = activePlayerPalette().accentGlow,
                    activeTrackColor = Color.hsv(customHue, customSat, customBri)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            // Brightness slider — 0..1 (3D depth)
            Text("Brightness (الإضاءة)", color = TextMuted, fontSize = 11.sp)
            Slider(
                value = customBri,
                onValueChange = { customBri = it },
                valueRange = 0.3f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = activePlayerPalette().accentGlow,
                    activeTrackColor = Color.hsv(customHue, customSat, customBri)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            // Quick 3D gradient swatches — tap to jump the picker
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(0f, 45f, 90f, 140f, 190f, 240f, 285f, 330f).forEach { hueItem ->
                    Box(
                        Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.hsv(hueItem, 0.35f, 1f),
                                        Color.hsv(hueItem, 1f, 0.85f)
                                    )
                                )
                            )
                            .border(
                                if (kotlin.math.abs(customHue - hueItem) < 4f) 2.dp else 1.dp,
                                activePlayerPalette().accentGlow, RoundedCornerShape(9.dp)
                            )
                            .clickable { customHue = hueItem }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color.hsv(customHue, customSat, customBri), Color.hsv((customHue + 160f) % 360f, customSat, customBri))))
                    .border(1.dp, activePlayerPalette().accentGlow, RoundedCornerShape(14.dp))
                    .clickable {
                        val argb = Color.hsv(customHue, customSat, customBri).toArgb().toLong() and 0xFFFFFFFFL
                        onCustomColor(argb)
                        onThemeSelected("CUSTOM")
                        showTheme = false
                    },
                contentAlignment = Alignment.Center
            ) { Text("طبّق اللون 3D", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            Text("اسحب Hue / Saturation / Brightness أو اختار مربع gradient أو اكتب كود HEX بالأعلى — كل لون يتحول لثيم Neon كامل.", color = TextMuted, fontSize = 10.sp)

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom color", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                    .background(activePlayerPalette().accent)
                    .border(1.dp, CrimsonBorder, RoundedCornerShape(6.dp)))
            }
            Spacer(Modifier.height(10.dp))
            var iconStyle by remember { mutableStateOf(com.example.ui.theme.GlobalIconStyle) }
            var fontLabel by remember {
                mutableStateOf(
                    when {
                        com.example.ui.theme.GlobalFontScale < 0.95f -> "Small"
                        com.example.ui.theme.GlobalFontScale > 1.1f -> "Large"
                        else -> "Normal"
                    }
                )
            }
            var glowLabel by remember {
                mutableStateOf(
                    when {
                        com.example.ui.theme.GlobalGlowIntensity < 0.45f -> "Low"
                        com.example.ui.theme.GlobalGlowIntensity > 0.85f -> "High"
                        else -> "Medium"
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth().clickable {
                    iconStyle = when (iconStyle) {
                        "Flat" -> "Glow"
                        "Glow" -> "Bold"
                        else -> "Flat"
                    }
                    com.example.ui.theme.GlobalIconStyle = iconStyle
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Icon style", color = TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("$iconStyle (follows accent)", color = activePlayerPalette().accentGlow, fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth().clickable {
                    fontLabel = when (fontLabel) {
                        "Small" -> "Normal"
                        "Normal" -> "Large"
                        else -> "Small"
                    }
                    com.example.ui.theme.GlobalFontScale = when (fontLabel) {
                        "Small" -> 0.9f
                        "Large" -> 1.15f
                        else -> 1f
                    }
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font size", color = TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(fontLabel, color = activePlayerPalette().accentGlow, fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth().clickable {
                    glowLabel = when (glowLabel) {
                        "Low" -> "Medium"
                        "Medium" -> "High"
                        else -> "Low"
                    }
                    com.example.ui.theme.GlobalGlowIntensity = when (glowLabel) {
                        "Low" -> 0.35f
                        "High" -> 1f
                        else -> 0.7f
                    }
                }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Glow intensity", color = TextPrimary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(glowLabel, color = activePlayerPalette().accentGlow, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(46.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(NeonRed, p.secondary.copy(alpha = .45f))))
                    .border(1.dp, NeonRedGlow.copy(alpha = .6f), RoundedCornerShape(22.dp))
                    .clickable {
                        ctx.getSharedPreferences("youseif_visual", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putFloat("glow", com.example.ui.theme.GlobalGlowIntensity)
                            .putFloat("font_scale", com.example.ui.theme.GlobalFontScale)
                            .putString("icon_style", com.example.ui.theme.GlobalIconStyle)
                            .apply()
                        showTheme = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Apply", color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
            }
        }
    }

    if (showPlayback) {
        val p = activePlayerPalette()
        var autoplay by remember { mutableStateOf(true) }
        var rememberPos by remember { mutableStateOf(true) }
        var speed by remember { mutableStateOf("1x") }
        var showSpeed by remember { mutableStateOf(false) }
        VisualModalSheet("Playback Settings", onClose = { showPlayback = false }) {
            ToggleRow("Autoplay next stream", autoplay) { autoplay = it }
            ToggleRow("Remember playback position", rememberPos) { rememberPos = it }
            // Real audio boost (LoudnessEnhancer)
            val playerCtrl = controller
            if (playerCtrl != null) {
                val boost by playerCtrl.audioBoost.collectAsState()
                val brightness by playerCtrl.brightness.collectAsState()
                val contrast by playerCtrl.contrast.collectAsState()
                val saturation by playerCtrl.saturation.collectAsState()
                Spacer(Modifier.height(8.dp))
                Text("Audio Boost (real)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${boost.percentage}%", color = NeonRedGlow, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.player.AudioBoostLevel.values().forEach { level ->
                        val sel = boost == level
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (sel) NeonRedContainer else DarkCardDeep)
                                .border(1.dp, if (sel) NeonRedGlow else CrimsonBorder, RoundedCornerShape(10.dp))
                                .clickable { playerCtrl.setAudioBoost(level) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("${level.percentage}%", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Brightness", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Slider(
                    value = brightness,
                    onValueChange = { playerCtrl.setBrightness(it) },
                    valueRange = 0.05f..1f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRedGlow)
                )
                Text("Contrast", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Slider(
                    value = contrast,
                    onValueChange = { playerCtrl.setContrast(it) },
                    valueRange = 0.4f..1.8f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRedGlow)
                )
                Text("Saturation", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.Slider(
                    value = saturation,
                    onValueChange = { playerCtrl.setSaturation(it) },
                    valueRange = 0f..2f,
                    colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRedGlow)
                )
            }
            ValueRow("Default speed", speed) { showSpeed = true }
            Button(onClick = { showPlayback = false },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonRed)) {
                Text("Save", color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            DropdownMenu(expanded = showSpeed, onDismissRequest = { showSpeed = false }) {
                listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x").forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { speed = s; showSpeed = false })
                }
            }
        }
    }

    if (about) {
        AlertDialog(
            onDismissRequest = { about = false },
            title = { Text("Youseif Player Pro", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Pro media player UI, playlists, history and local download manager.", color = TextSecondary) },
            confirmButton = { TextButton(onClick = { about = false }) { Text("OK", color = NeonRedGlow) } },
            containerColor = DarkCardBg
        )
    }

    if (showSubtitle) {
        val styleFlow = controller?.subtitleStyle?.collectAsState()
        val subStyle = styleFlow?.value ?: com.example.player.SubtitleStyleConfig()
        var subUrl by remember { mutableStateOf("") }
        val subColors = listOf(
            "أبيض" to 0xFFFFFFFFL,
            "أصفر" to 0xFFFFFF00L,
            "سماوي" to 0xFF00E5FFL,
            "أخضر" to 0xFF69F0AEL,
            "برتقالي" to 0xFFFFAB40L,
            "أحمر" to 0xFFFF5252L
        )
        val sizeOptions = listOf("صغير" to 14f, "وسط" to 18f, "كبير" to 22f, "أكبر" to 28f)
        // لوحة مضغوطة مش 92% من الشاشة
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable { showSubtitle = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(DarkCardBg)
                    .clickable(enabled = false, onClick = {})
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("إعدادات الترجمة", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showSubtitle = false }) {
                        Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                    }
                }

                Text("لون النص", color = TextMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subColors.forEach { (label, argb) ->
                        val isSel = subStyle.textColor == argb
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(argb.toInt()).copy(alpha = if (isSel) 1f else 0.4f))
                                .border(if (isSel) 2.dp else 1.dp, if (isSel) NeonRed else CrimsonBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    controller?.updateSubtitleStyle(subStyle.copy(textColor = argb))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                color = if (argb == 0xFFFFFFFFL || argb == 0xFFFFFF00L) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text("حجم الخط", color = TextMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sizeOptions.forEach { (label, size) ->
                        val isSel = subStyle.fontSizeSp == size
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonRed else DarkCardDeep.copy(alpha = .7f))
                                .border(1.dp, if (isSel) NeonRed else CrimsonBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    controller?.updateSubtitleStyle(subStyle.copy(fontSizeSp = size))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSel) Color.White else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("موضع الترجمة", color = TextMuted, fontSize = 12.sp)
                Slider(
                    value = subStyle.bottomPaddingFraction,
                    onValueChange = {
                        controller?.updateSubtitleStyle(subStyle.copy(bottomPaddingFraction = it.coerceIn(0.02f, 0.35f)))
                    },
                    valueRange = 0.02f..0.35f,
                    colors = SliderDefaults.colors(thumbColor = NeonRed, activeTrackColor = NeonRed)
                )

                Text("ملف / رابط ترجمة", color = TextMuted, fontSize = 12.sp)
                OutlinedTextField(
                    value = subUrl,
                    onValueChange = { subUrl = it },
                    placeholder = { Text("SRT / VTT URL", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRed, unfocusedBorderColor = CrimsonBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = NeonRed,
                        focusedContainerColor = DarkCardDeep.copy(alpha = .7f),
                        unfocusedContainerColor = DarkCardDeep.copy(alpha = .7f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp))
                            .background(NeonRedContainer).border(1.dp, NeonRed, RoundedCornerShape(14.dp))
                            .clickable { if (subUrl.isNotBlank()) onSubtitleUrlSaved(subUrl.trim()) },
                        contentAlignment = Alignment.Center
                    ) { Text("تحميل رابط", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp))
                            .background(NeonRedContainer).border(1.dp, NeonRed, RoundedCornerShape(14.dp))
                            .clickable { subtitleFileLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                        contentAlignment = Alignment.Center
                    ) { Text("فتح ملف", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Box(
                    Modifier.fillMaxWidth().height(44.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(NeonRed, TechCyan.copy(alpha = .3f))))
                        .clickable { showSubtitle = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("حفظ وإغلاق", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}


@Composable
private fun RestoreOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val p = activePlayerPalette()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.card.copy(alpha = 0.9f))
            .border(1.dp, p.border.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(p.accentContainer.copy(alpha = 0.6f))
                .border(1.dp, p.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = p.accentGlow, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsVisualRow(icon: ImageVector, title: String, subtitle: String, active: Boolean = false, onClick: () -> Unit) {
    val p = activePlayerPalette()
    val interaction = androidx.compose.foundation.interaction.MutableInteractionSource()
    val pressed = interaction.collectIsPressedAsState().value
    Row(
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(glassGradient(forDark = true))
            .then(
                if (pressed) Modifier.background(p.accent.copy(alpha = 0.18f))
                else Modifier
            )
            .border(
                1.dp,
                if (pressed) p.accentGlow.copy(alpha = 0.9f) else p.border.copy(alpha = 0.7f),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(p.accentContainer.copy(alpha = 0.55f))
                .border(1.dp, p.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = p.accentGlow, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (active) p.accent.copy(alpha = 0.22f) else Color.Transparent)
                .border(1.dp, if (active) p.accentGlow else p.border.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = if (active) "Enabled" else "Disabled",
                tint = if (active) p.accentGlow else p.muted,
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun VisualModalSheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .62f))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(DarkCardBg)
                .clickable(enabled = false, onClick = {})
                .padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = TextPrimary)
                }
            }
            content()
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontSize = 13.sp)
        Text(value, color = NeonRedGlow, fontSize = 12.sp)
    }
}

private val p get() = activePlayerPalette()
