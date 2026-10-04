package com.example.ui.radio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.example.player.YouseifPlayerController
import com.example.ui.theme.LiveGreen
import com.example.data.RadioCatalog
import com.example.data.UserSettings
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette

@Composable
fun RadioScreen(
    onBack: () -> Unit,
    onPlayStation: (String, String) -> Unit,
    controller: YouseifPlayerController? = null
) {
    val context = LocalContext.current
    val p = activePlayerPalette()
    val categories = remember { RadioCatalog.load(context) }
    var expanded by remember { mutableStateOf(UserSettings.getString("radio_expanded", categories.firstOrNull()?.type.orEmpty())) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var favoritesTick by remember { mutableStateOf(0) }
    val radioFavoritesGeneration = favoritesTick
    val currentChannelState = controller?.currentChannel?.collectAsState()
    val diagnosticsState = controller?.diagnostics?.collectAsState()
    val current = currentChannelState?.value
    val isPlaying = diagnosticsState?.value?.isPlaying == true

    LaunchedEffect(expanded) {
        try { UserSettings.putString("radio_expanded", expanded) } catch (_: Throwable) {}
    }
    BackHandler(onBack = onBack)

    val playingTitle = current?.name.orEmpty()

    Column(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        PlayerTopBar(onBack = onBack, onMenu = null, onRefresh = null, onInfo = null, onDownloads = null)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الإذاعة", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("تصنيفات مرتبة بنفس الترتيب المطلوب، مع تشغيل مباشر", color = TextMuted, fontSize = 12.sp)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (!favoritesOnly) p.accentContainer else DarkSurface)
                            .border(1.dp, if (!favoritesOnly) p.accentGlow else CrimsonBorder, RoundedCornerShape(14.dp))
                            .clickable { favoritesOnly = false }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("كل المحطات", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (favoritesOnly) p.accentContainer else DarkSurface)
                            .border(1.dp, if (favoritesOnly) p.accentGlow else CrimsonBorder, RoundedCornerShape(14.dp))
                            .clickable { favoritesOnly = true }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Favorite, null, tint = if (favoritesOnly) p.accentGlow else TextMuted, modifier = Modifier.size(14.dp))
                            Text("المفضلة", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            itemsIndexed(categories, key = { _, it -> it.type }) { _, category ->
                val visibleItems = category.items.filter { !favoritesOnly || UserSettings.isRadioFavorite(it.url) }
                if (favoritesOnly && visibleItems.isEmpty()) return@itemsIndexed
                val isExpanded = expanded == category.type
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isExpanded) NeonRedContainer.copy(alpha = .24f) else DarkCardBg.copy(alpha = .86f), RoundedCornerShape(22.dp))
                        .border(1.dp, if (isExpanded) p.accentGlow.copy(alpha = .72f) else CrimsonBorder.copy(alpha = .82f), RoundedCornerShape(22.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = if (isExpanded) "" else category.type },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = p.accentGlow, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${visibleItems.size} محطة", color = TextSecondary, fontSize = 11.sp)
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = p.accentGlow
                        )
                    }
                    if (isExpanded) {
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            visibleItems.forEach { item ->
                                val isFavorite = UserSettings.isRadioFavorite(item.url)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface.copy(alpha = .92f), RoundedCornerShape(16.dp))
                                        .border(1.dp, CrimsonBorder.copy(alpha = .68f), RoundedCornerShape(16.dp))
                                        .clickable { onPlayStation(item.name, item.url) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(p.accentContainer.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                            .border(1.dp, p.accentGlow.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Radio, contentDescription = null, tint = p.accentGlow, modifier = Modifier.size(22.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    IconButton(onClick = {
                                        UserSettings.toggleRadioFavorite(item.url)
                                        favoritesTick += 1
                                    }) {
                                        Icon(
                                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) LiveGreen else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(p.accentContainer.copy(alpha = 0.96f), p.accentDark.copy(alpha = 0.70f))
                                                )
                                            )
                                            .border(1.dp, p.accentGlow.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                                            .clickable { onPlayStation(item.name, item.url) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Play", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ميني بلاير من تحت — نفس أسلوب القنوات/الموسيقى
        if (playingTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCardBg.copy(alpha = 0.97f))
                    .border(
                        1.dp,
                        if (isPlaying) LiveGreen.copy(alpha = 0.55f) else CrimsonBorder.copy(alpha = 0.7f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(p.accentGlow.copy(alpha = 0.45f), p.accentContainer.copy(alpha = 0.6f))
                            )
                        )
                        .border(1.dp, p.accentGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Radio,
                        contentDescription = null,
                        tint = if (isPlaying) LiveGreen else p.accentGlow,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        playingTitle,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        if (isPlaying) "جاري التشغيل الآن · راديو مباشر" else "متوقف / يحمّل",
                        color = if (isPlaying) LiveGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                // زر تشغيل / إيقاف
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) LiveGreen.copy(alpha = 0.2f) else p.accentContainer.copy(alpha = 0.55f))
                        .border(1.dp, if (isPlaying) LiveGreen else p.accentGlow, CircleShape)
                        .clickable {
                            controller?.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) LiveGreen else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
