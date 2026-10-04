package com.example.ui.films

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FaselDetail
import com.example.data.FaselEpisode
import com.example.data.FaselMedia
import com.example.data.FaselQuality
import com.example.data.FaselSeries
import com.example.data.FaselVideo
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

/**
 * Film / Series / Anime details — synopsis, cast, genres, servers, qualities,
 * download + subtitles, season/episode picker. Qualities are resolved by the
 * parent view-model (HostResolver chain) and rendered as a bottom chooser.
 */
@Composable
fun MediaDetailScreen(
    title: String,
    detail: FaselDetail? = null,
    series: FaselSeries? = null,
    loading: Boolean = true,
    error: String = "",
    hideImages: Boolean = false,
    qualities: List<FaselQuality> = emptyList(),
    qualitiesLoading: Boolean = false,
    qualitiesLabel: String = "",
    webEmbedFallback: String = "",
    onBack: () -> Unit,
    onCloseQualities: () -> Unit = onBack,
    onResolveQualities: (FaselVideo) -> Unit,
    onPlayQuality: (FaselQuality) -> Unit,
    onWebEmbed: () -> Unit = {},
    onOpenRelated: (FaselMedia) -> Unit = { _ -> }
) {
    val p = activePlayerPalette()
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize().visualScreenBackground()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = .9f))
                        .border(1.dp, CrimsonBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = NeonRedGlow, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("التفاصيل والمشاهدة", color = TextMuted, fontSize = 11.sp)
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("جارٍ تحميل التفاصيل...", color = TextSecondary, fontSize = 14.sp)
                }
                return@Box
            }
            if (error.isNotBlank() && detail == null && series == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تعذر تحميل التفاصيل", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(error, color = TextMuted, fontSize = 12.sp)
                    }
                }
                return@Box
            }

            val poster = detail?.poster ?: series?.poster ?: ""
            val overview = detail?.overview ?: series?.overview ?: ""
            val vote = detail?.vote ?: series?.vote ?: 0.0
            val genres = detail?.genres ?: series?.genres ?: emptyList()
            val videos = detail?.videos ?: emptyList()
            val seriesSeasons = series?.seasons ?: emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (poster.isNotBlank()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 14.dp).clip(RoundedCornerShape(18.dp)).background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hideImages) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(poster).crossfade(true).build(),
                                    contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(title.take(1).uppercase(), color = p.accentGlow, fontSize = 48.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                item {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (vote > 0) {
                            Icon(Icons.Default.Star, null, tint = TechCyanGlow, modifier = Modifier.size(14.dp))
                            Text("%.1f".format(vote), color = TechCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        val rel = detail?.release ?: ""
                        if (rel.isNotBlank()) {
                            Text(rel.take(4), color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (detail != null) {
                            Text("جودة: ${detail.subtitle.ifBlank { "متعددة" }}", color = NeonRedGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (genres.isNotEmpty()) {
                    item {
                        LazyRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(genres) { g -> TechTag(text = g, color = TechCyanGlow) }
                        }
                    }
                }

                if (overview.isNotBlank()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("القصة", color = TechCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(overview, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                if (detail != null && detail.cast.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("الأبطال", color = TechCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(detail.cast.take(16).joinToString(" • "), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // Movie servers
                if (videos.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("روابط المشاهدة والجودات", color = TechCyanGlow, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            videos.forEach { v ->
                                ServerRow(video = v, onResolve = { onResolveQualities(v) })
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // Series / Anime seasons + episodes
                if (seriesSeasons.isNotEmpty() && detail == null) {
                    item {
                        if (selectedSeasonIndex >= seriesSeasons.size) selectedSeasonIndex = 0
                        val season = seriesSeasons[selectedSeasonIndex]
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("الموسم والحلقات", color = TechCyanGlow, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(count = seriesSeasons.size) { idx ->
                                    val sel = idx == selectedSeasonIndex
                                    Box(
                                        Modifier.clip(RoundedCornerShape(16.dp))
                                            .background(if (sel) NeonRedContainer else DarkSurface)
                                            .border(1.dp, if (sel) NeonRed else CrimsonBorder.copy(alpha = .7f), RoundedCornerShape(16.dp))
                                            .clickable { selectedSeasonIndex = idx }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(seasonLabel(seriesSeasons[idx].name, idx), color = if (sel) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height((((season.episodes.size + 4) / 5) * 48 + 8).dp.coerceAtLeast(56.dp))
                            ) {
                                gridItems(season.episodes) { ep ->
                                    EpisodeButton(ep) {
                                        val v = ep.videos.firstOrNull() ?: return@EpisodeButton
                                        onResolveQualities(v)
                                    }
                                }
                            }
                        }
                    }
                }

                // (أزرار تحميل الترجمة اتشالت — المطلوب: مفيش تحميل خالص)

                if (detail != null && detail.related.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text("أفلام مشابهة", color = TechCyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(detail.related.take(20)) { m ->
                                    Column(
                                        Modifier.width(96.dp).clip(RoundedCornerShape(12.dp))
                                            .background(DarkCardBg)
                                            .border(1.dp, CrimsonBorder.copy(alpha = .8f), RoundedCornerShape(12.dp))
                                            .clickable { onOpenRelated(m) }
                                            .padding(4.dp)
                                    ) {
                                        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)).background(DarkSurface), contentAlignment = Alignment.Center) {
                                            if (!hideImages && m.poster.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current).data(m.poster).build(),
                                                    contentDescription = m.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                                )
                                            } else Text(m.title.take(1).uppercase(), color = p.accentGlow, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        }
                                        Text(m.title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Quality chooser bottom sheet ----
        if (qualities.isNotEmpty() || qualitiesLoading || webEmbedFallback.isNotBlank() || qualitiesLabel.isNotBlank()) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xF0110A12))
                    .border(1.dp, NeonRed.copy(alpha = .6f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 470.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(if (qualitiesLoading) "جارٍ فتح الجودات..." else "اختر الجودة",
                                color = NeonRedGlow, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            if (qualitiesLabel.isNotBlank() && qualities.isNotEmpty()) {
                                Text(qualitiesLabel, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(DarkSurface).clickable { onCloseQualities() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, "غلق", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (qualities.isEmpty() && !qualitiesLoading && qualitiesLabel.isNotBlank()) {
                        Text(qualitiesLabel, color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (webEmbedFallback.isNotBlank() && !qualitiesLoading) {
                        // No direct multi-quality stream (or only Original) — offer working WebView path.
                        // Covers StreamWish / UpDown / ServerEgy and Cloudflare-protected Fasel pages.
                        Box(
                            Modifier.fillMaxWidth().height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .5f))))
                                .clickable { onWebEmbed() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (qualities.any { it.label.equals("Original", ignoreCase = true) })
                                    "افتح صفحة المشاهدة (موصى به)"
                                else
                                    "المصدر محمي — افتح صفحة المشاهدة",
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // كل جودة في كارت مستقل: زر "مشاهدة" لوحده في سطر، وزر "تحميل" لوحده في سطر تاني
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        qualities.forEach { q ->
                            QualityActionCard(
                                q = q,
                                onWatch = { onPlayQuality(q) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun seasonLabel(name: String, idx: Int): String {
    val t = name.trim()
    return if (t.isBlank()) "موسم ${idx + 1}" else if (t.length > 14) t.take(13) + "…" else t
}

@Composable
private fun ServerRow(video: FaselVideo, onResolve: () -> Unit) {
    val p = activePlayerPalette()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DarkCardBg.copy(alpha = .85f))
            .border(1.dp, CrimsonBorder.copy(alpha = .85f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(NeonRedContainer.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, null, tint = NeonRedGlow, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(video.server.ifBlank { "سيرفر" }, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TechTag(text = if (video.hd) "HD" else "SD", color = NeonRedGlow)
                if (video.lang.isNotBlank()) TechTag(text = video.lang, color = TechCyanGlow)
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .5f))))
                .clickable { onResolve() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("الجودات", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun QualityActionCard(q: FaselQuality, onWatch: () -> Unit) {
    // المشاهدة والتحميل منفصلين تماماً: كل زر في سطر لوحده بعرض كامل، مفيش جنب-بجنب.
    val p = activePlayerPalette()
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardBg.copy(alpha = .92f))
            .border(1.dp, CrimsonBorder.copy(alpha = .8f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: quality label chip + label text
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(NeonRedContainer.copy(alpha = .6f))
                    .border(1.dp, NeonRed.copy(alpha = .75f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(q.label.ifBlank { "جودة" }, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Text("سيرفر المشاهدة", color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // WATCH button — full width, one line, alone
        Box(
            Modifier.fillMaxWidth().height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(p.accent, p.secondary.copy(alpha = .5f))))
                .clickable { onWatch() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.PlayArrow, "مشاهدة", tint = Color.White, modifier = Modifier.size(19.dp))
                Text("مشاهدة الآن", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
        // (زر التحميل اتشال — المطلوب: جودات بس، مفيش تحميل خالص)
    }
}

@Composable
private fun EpisodeButton(ep: FaselEpisode, onClick: () -> Unit) {
    val hasVideo = ep.videos.isNotEmpty()
    Box(
        Modifier.fillMaxWidth().height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasVideo) NeonRedContainer.copy(alpha = .5f) else DarkSurface)
            .border(1.dp, if (hasVideo) NeonRed.copy(alpha = .85f) else CrimsonBorder.copy(alpha = .5f), RoundedCornerShape(12.dp))
            .clickable(enabled = hasVideo) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${ep.number}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (ep.name.isNotBlank() && ep.name != ep.number.toString()) {
                Text(ep.name.take(10), color = TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
