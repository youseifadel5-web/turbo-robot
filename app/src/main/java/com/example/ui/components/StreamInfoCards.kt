package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.platform.LocalContext
import com.example.player.StreamDiagnostics
import com.example.player.YouseifPlayerController
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette

/**
 * Clean dashboard matching the stable reference:
 * STREAM INFO (Type / Duration / Resolution / Status)
 * CONNECTION (Youseif RGB + NETWORK / PING / SPEED / BUFFER)
 * Real values only — compact fonts, no overflow.
 */
@Composable
fun DiagnosticDashboardCards(
    diagnostics: StreamDiagnostics,
    modifier: Modifier = Modifier,
    controller: YouseifPlayerController? = null
) {
    val p = activePlayerPalette()
    val ctx = LocalContext.current
    val deviceOnline = remember(diagnostics.isPlaying, diagnostics.isLoading) {
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return@remember false
            val caps = cm.getNetworkCapabilities(n) ?: return@remember false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) { true }
    }
    // بث شغّال / بيحمّل — مش معناه نت الجهاز
    val streamActive = diagnostics.isPlaying || diagnostics.isLoading
    val online = deviceOnline
    val isLive = diagnostics.isLive
    val streamLabel = when {
        diagnostics.isPlaying -> "PLAYING"
        diagnostics.isLoading -> "LOADING"
        diagnostics.errorMessage != null -> "ERROR"
        else -> "READY"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── STREAM INFO ──────────────────────────────────────────────
        GlassCard(modifier = Modifier.weight(1f).height(168.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "STREAM INFO",
                    color = p.accentGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (online) p.accentGlow else TextMuted)
                )
            }
            Spacer(Modifier.height(8.dp))
            InfoLine(
                "Type",
                when {
                    isLive -> "Live stream"
                    diagnostics.totalDurationMs > 0L -> "VOD / File"
                    online -> "Stream"
                    else -> "—"
                }
            )
            InfoLine(
                "Duration",
                when {
                    isLive -> "LIVE"
                    diagnostics.totalDurationMs > 0L -> formatDuration(diagnostics.totalDurationMs)
                    else -> "—"
                }
            )
            InfoLine(
                "Resolution",
                when {
                    diagnostics.resolution.isNotBlank() && diagnostics.resolution != "—" ->
                        diagnostics.resolution
                    online && diagnostics.videoCodec == "—" && diagnostics.audioCodec != "—" ->
                        "Audio / no video"
                    else -> "—"
                }
            )
            InfoLine(
                "Status",
                when {
                    diagnostics.errorMessage != null -> "Error"
                    diagnostics.isPlaying -> "Playing"
                    diagnostics.isLoading -> "Loading"
                    else -> "Idle"
                }
            )
        }

        // ── CONNECTION ───────────────────────────────────────────────
        GlassCard(modifier = Modifier.weight(1f).height(168.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CONNECTION",
                    color = p.accentGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    streamLabel,
                    color = if (streamActive) p.accentGlow else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            YouseifRgbName(
                isActive = streamActive || deviceOnline,
                speedKbps = diagnostics.downloadSpeedKbps,
                modifier = Modifier.fillMaxWidth().height(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTile(
                    "NETWORK",
                    if (deviceOnline) "Online" else "Offline",
                    Modifier.weight(1f),
                    valueColor = if (deviceOnline) p.accentGlow else TextMuted
                )
                MiniTile(
                    "PING",
                    if (diagnostics.latencyMs > 0) "${diagnostics.latencyMs} ms" else "—",
                    Modifier.weight(1f),
                    valueColor = if (diagnostics.latencyMs > 0) p.accentGlow else TextMuted
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTile(
                    "SPEED",
                    when {
                        diagnostics.downloadSpeedKbps > 0 ->
                            String.format("%.1f Mbps", diagnostics.downloadSpeedKbps / 1000f)
                        streamActive -> "…"
                        deviceOnline -> "Idle"
                        else -> "—"
                    },
                    Modifier.weight(1f),
                    valueColor = if (diagnostics.downloadSpeedKbps > 0) p.accentGlow else TextSecondary
                )
                MiniTile(
                    "BUFFER",
                    when {
                        diagnostics.bufferMs > 0 -> String.format("%.1fs", diagnostics.bufferMs / 1000f)
                        diagnostics.bufferPercentage > 0 -> "${diagnostics.bufferPercentage}%"
                        else -> "—"
                    },
                    Modifier.weight(1f),
                    valueColor = if (diagnostics.bufferMs > 0) p.secondaryGlow else TextSecondary
                )
            }
        }
    }
}

/** RGB wave across "Youseif" while stream is active; dim when idle. */
@Composable
fun YouseifRgbName(
    isActive: Boolean,
    speedKbps: Long,
    modifier: Modifier = Modifier
) {
    val name = "Youseif"
    val transition = rememberInfiniteTransition(label = "youseif_rgb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = name.length.toFloat() + 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 1400 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val strength = when {
        !isActive -> 0.2f
        speedKbps <= 0 -> 0.4f
        else -> (speedKbps / 8000f).coerceIn(0.4f, 1f)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        name.forEachIndexed { index, ch ->
            val dist = kotlin.math.abs(phase - index)
            val lit = isActive && dist < 1.7f
            val alpha = when {
                !isActive -> 0.38f
                lit -> (1f * strength * pulse).coerceIn(0.55f, 1f)
                else -> (0.4f + strength * 0.25f).coerceIn(0.35f, 0.75f)
            }
            val color = if (isActive) {
                val hue = ((index * 42f) + phase * 28f) % 360f
                val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.88f, 1f))
                Color(rgb).copy(alpha = alpha)
            } else {
                TextPrimary.copy(alpha = alpha)
            }
            Text(
                text = ch.toString(),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val p = activePlayerPalette()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.card.copy(alpha = 0.95f),
                        DarkCardBg.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, p.border.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.Top) { content() }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
        Text(
            value,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun MiniTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    val p = activePlayerPalette()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.surface.copy(alpha = 0.9f))
            .border(1.dp, p.border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
        Text(
            value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "—"
    val s = ms / 1000
    val m = s / 60
    val h = m / 60
    return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
    else "%d:%02d".format(m, s % 60)
}

@Composable
fun ConnectionWaveformChart(
    speedKbps: Long,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    // kept for API compatibility — unused in clean layout
    Spacer(modifier.height(0.dp))
}

@Composable
fun YouseifProLink(
    isActive: Boolean,
    speedKbps: Long,
    modifier: Modifier = Modifier
) {
    YouseifRgbName(isActive = isActive, speedKbps = speedKbps, modifier = modifier)
}
