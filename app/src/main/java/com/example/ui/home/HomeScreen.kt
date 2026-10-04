package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryItem
import com.example.data.PlaylistItem
import com.example.player.YouseifPlayerController
import com.example.ui.components.DiagnosticDashboardCards
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyan
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.activePlayerPalette
import com.example.ui.theme.glassGradient

/**
 * Home screen geometry mirrors DESIGN_REFERENCE/screens/01_HOME_PLAYER.jpg:
 *   TopBar (~112dp) → URL row (~82dp) + PLAY button (132×82dp)
 *   → player at 1.72:1 aspect (taller player), ~24dp radius → status row
 *   → STREAM INFO + CONNECTION two-column cards → real chart
 *   → RECENTLY WATCHED chips → bottom nav pill.
 */
@Composable
fun HomeScreen(
    controller: YouseifPlayerController,
    recentChannels: List<PlaylistItem>,
    recentHistory: List<HistoryItem> = emptyList(),
    onChannelSelected: (PlaylistItem) -> Unit,
    onOpenMenu: () -> Unit = {},
    onNavigateToChannels: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    offlineMode: Boolean = false,
    onToggleOffline: () -> Unit = {},
    autoThemeEnabled: Boolean = false,
    onToggleAutoTheme: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onImportM3U: (String) -> Unit,
    onHistoryUrl: (String, String, String) -> Unit = { _, _, _ -> },
    /** title, url, group — FILM:id reopens media, else plays URL */
    onReplayHistory: (String, String, String) -> Unit = { t, u, g -> onHistoryUrl(t, u, g) },
    onClearHistory: () -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteHistoryItem: (HistoryItem) -> Unit = {}
) {
    val clipboard = LocalClipboardManager.current
    val p = activePlayerPalette()
    val diagnostics by controller.diagnostics.collectAsState()
    val currentChannel by controller.currentChannel.collectAsState()
    var urlInput by remember { mutableStateOf("") }

    fun streamTitleFromUrl(raw: String): String {
        val clean = raw.trim().substringBefore('?').substringBefore('#')
        val last = clean.substringAfterLast('/').ifBlank { clean }
        val name = last
            .removeSuffix(".m3u8").removeSuffix(".m3u").removeSuffix(".mpd")
            .removeSuffix(".mp4").removeSuffix(".mkv").removeSuffix(".ts")
            .replace('%', ' ')
            .replace('+', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return when {
            name.isBlank() -> "Stream"
            name.length > 36 -> name.take(34) + "…"
            else -> name
        }
    }

    fun playInput() {
        val target = (if (urlInput.isNotBlank()) urlInput else clipboard.getText()?.text.orEmpty()).trim()
        if (target.isNotBlank()) {
            urlInput = target
            val title = streamTitleFromUrl(target)
            controller.playUrl(target, title, true)
            onHistoryUrl(title, target, "LIVE")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().visualScreenBackground(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 2.dp, bottom = 6.dp)
    ) {
        item {
            PlayerTopBar(
                onMenu = onOpenMenu,
                // لا تعيد تشغيل البث عند تغيير الثيم.
                onRefresh = null,
                onInfo = onNavigateToSettings,
                onDownloads = null,
                onOfflineToggle = onToggleOffline,
                offlineActive = offlineMode,
                autoThemeEnabled = autoThemeEnabled,
                onToggleAutoTheme = onToggleAutoTheme,
                onLongPressTheme = onToggleAutoTheme
            )
        }

        // URL + PLAY neon capsules (reference style)
        item {
            val p = activePlayerPalette()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFF14080C))
                        .border(1.5.dp, p.accentGlow.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                        .padding(start = 12.dp, end = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, null, tint = p.accentGlow, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (urlInput.isBlank()) {
                                        Text("Paste link or select a file...", color = TextMuted.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1)
                                    }
                                    inner()
                                }
                            }
                        )
                        IconButton(
                            onClick = { clipboard.getText()?.text?.trim()?.takeIf { it.isNotBlank() }?.let { urlInput = it } },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, "Paste", tint = p.accentGlow, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .width(98.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(p.surface.copy(alpha = 0.30f))
                        .border(1.5.dp, p.accentGlow.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                        .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = p.accent.copy(alpha = 0.22f), spotColor = p.accentGlow.copy(alpha = 0.35f))
                        .clickable { playInput() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PlayArrow, null, tint = p.accentGlow, modifier = Modifier.size(18.dp))
                        Text("PLAY", color = p.accentGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                }
            }
        }

        // Larger player card for a wider viewing area; fullscreen behavior remains unchanged.
        item {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                VideoPlayerView(
                    controller = controller,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.45f).clip(RoundedCornerShape(22.dp)),
                    isFullscreen = false,
                    onToggleFullscreen = onToggleFullscreen,
                    onPreviousChannel = {
                        // سياق التشغيل الحالي (فيلم/قناة/جودة) ثم أزرار الريموت ثم قائمة القنوات
                        if (!controller.previousInContext() && !com.example.player.KeyRouter.dispatch(-1)) {
                            val index = recentChannels.indexOfFirst { it.id == currentChannel?.id }
                            if (index > 0) onChannelSelected(recentChannels[index - 1])
                        }
                    },
                    onNextChannel = {
                        if (!controller.nextInContext() && !com.example.player.KeyRouter.dispatch(1)) {
                            val index = recentChannels.indexOfFirst { it.id == currentChannel?.id }
                            if (index >= 0 && index < recentChannels.lastIndex) onChannelSelected(recentChannels[index + 1])
                        }
                    },
                    onOpenPlaylist = onNavigateToChannels,
                    onOpenSettings = onNavigateToSettings,
                    allLiveChannels = recentChannels.filter { it.isLive },
                    onSwitchChannelServer = { ch -> onChannelSelected(ch) }
                )
            }
        }

        // Audio Only row — simple label + small switch (no big button)
        item {
            val audioOnly by controller.audioOnlyMode.collectAsState()
            val p = activePlayerPalette()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(p.accentContainer.copy(alpha = 0.7f))
                        .border(1.dp, p.accentGlow.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♪", color = p.accentGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text("Audio Only", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                // compact switch
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (audioOnly) p.accent else p.surface.copy(alpha = 0.7f))
                        .border(1.dp, if (audioOnly) p.accentGlow else p.border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            try { controller.setAudioOnlyMode(!audioOnly) } catch (_: Throwable) {}
                        }
                        .padding(3.dp),
                    contentAlignment = if (audioOnly) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (audioOnly) Color.White else TextMuted.copy(alpha = 0.7f))
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (diagnostics.isPlaying) LiveGreen else TextMuted))
                Text(
                    if (diagnostics.isPlaying) "PLAYING" else if (diagnostics.isLoading) "LOADING" else "READY",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        }

        item { DiagnosticDashboardCards(diagnostics = diagnostics, controller = controller) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RECENTLY WATCHED", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                TextButton(onClick = onClearHistory) {
                    Text("CLEAR", color = NeonRedGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentHistory.take(12).forEach { item ->
                    val label = item.title.ifBlank {
                        item.url.substringAfterLast('/').take(48).ifBlank { "Stream" }
                    }
                    // تشغيل فوري من السجل (كان يستدعي onHistoryUrl بالخطأ فلا يعمل)
                    val favMatch = recentChannels.firstOrNull { it.name.equals(label, true) }
                    HistoryChip(
                        title = label,
                        group = item.group,
                        url = item.url,
                        isFav = favMatch?.isFavorite == true,
                        onFav = { favMatch?.let { onToggleFavorite(it.id, !it.isFavorite) } },
                        onDelete = { onDeleteHistoryItem(item) },
                        onClick = { onReplayHistory(label, item.url, item.group) }
                    )
                }
                if (recentHistory.isEmpty()) {
                    Text("No recently watched items", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(
    title: String,
    group: String,
    url: String,
    isFav: Boolean,
    onFav: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val palette = activePlayerPalette()
    // حسب المحتوى: ▶️ فيديو | 📡 قنوات | 🎶 موسيقى
    val kind = when {
        group.contains("RADIO", true) || group.contains("LIVE", true) || group.contains("CHANNEL", true) -> "channel"
        group.contains("AUDIO", true) || group.contains("MUSIC", true) || group.contains("SONG", true) -> "music"
        group.contains("VIDEO", true) || url.startsWith("content://", true) ||
            url.contains(".mp4", true) || url.contains(".mkv", true) || url.contains(".webm", true) -> "video"
        group.startsWith("FILM", true) || group.contains("SERIES", true) || group.contains("FASel", true) -> "video"
        else -> "video"
    }
    val kindIcon = when (kind) {
        "music" -> Icons.Default.MusicNote   // 🎶
        "channel" -> Icons.Default.LiveTv    // 📡
        else -> Icons.Default.PlayArrow      // ▶️ فيديو
    }
    val kindColor = when (kind) {
        "music" -> palette.accentGlow
        "channel" -> LiveGreen
        else -> palette.accent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardBg.copy(alpha = .72f))
            .border(1.dp, CrimsonBorder.copy(alpha = .72f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(kindIcon, contentDescription = kind, tint = kindColor, modifier = Modifier.size(15.dp))
        Text(title, color = TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 170.dp))
        // قلب: إضافة/إزالة من المفضلة
        IconButton(onClick = onFav, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "مفضلة",
                tint = if (isFav) NeonRedGlow else TextMuted,
                modifier = Modifier.size(15.dp)
            )
        }
        // حذف صريح من سجل المشاهدة
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "حذف",
                tint = NeonRedGlow,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
