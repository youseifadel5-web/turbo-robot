package com.example.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardDeep
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.activePlayerPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val bytes: Long,
    val total: Long
) {
    val progressPct: Int
        get() = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0
    val statusLabel: String
        get() = when (status) {
            DownloadManager.STATUS_PENDING -> "قيد الانتظار"
            DownloadManager.STATUS_RUNNING -> "تحميل $progressPct%"
            DownloadManager.STATUS_PAUSED -> "متوقف $progressPct%"
            DownloadManager.STATUS_SUCCESSFUL -> "اكتمل"
            DownloadManager.STATUS_FAILED -> "فشل"
            else -> "—"
        }
}

private data class QualityOption(val label: String, val detail: String, val sizeMb: Int, val sizeLabel: String)

private val DOWNLOAD_QUALITIES = listOf(
    QualityOption("4K UHD", "2160p · ~18.0 Mbps", 4200, "~4200 MB"),
    QualityOption("Full HD", "1080p · ~6.0 Mbps", 1400, "~1400 MB"),
    QualityOption("HD", "720p · ~3.0 Mbps", 700, "~700 MB"),
    QualityOption("SD", "480p · ~1.4 Mbps", 320, "~320 MB"),
    QualityOption("Mobile", "360p · ~0.8 Mbps", 180, "~180 MB"),
)

private data class DownloadSnapshot(
    val active: Int,
    val completed: Int,
    val failed: Int,
    val items: List<DownloadItem> = emptyList()
)

@Composable
fun DownloadsScreen(
    onOpenChannels: () -> Unit,
    onSearchFilms: (String) -> Unit = {},
    searchResults: List<com.example.data.FaselMedia>? = null,
    onOpenMediaForDownload: (com.example.data.FaselMedia) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    val p = activePlayerPalette()
    var urlInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var snapshot by remember { mutableStateOf(DownloadSnapshot(0, 0, 0)) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var probedBytes by remember { mutableStateOf<Long?>(null) }
    var probing by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableIntStateOf(1) }

    fun queryDownloads(): DownloadSnapshot {
        var active = 0; var completed = 0; var failed = 0
        val items = mutableListOf<DownloadItem>()
        val cursor = manager.query(DownloadManager.Query())
        cursor.use {
            val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val idCol = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleCol = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val bytesCol = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            if (statusCol >= 0) while (it.moveToNext()) {
                val st = it.getInt(statusCol)
                when (st) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED -> active++
                    DownloadManager.STATUS_SUCCESSFUL -> completed++
                    DownloadManager.STATUS_FAILED -> failed++
                }
                items += DownloadItem(
                    id = if (idCol >= 0) it.getLong(idCol) else -1L,
                    title = if (titleCol >= 0) (it.getString(titleCol) ?: "تحميل") else "تحميل",
                    status = st,
                    bytes = if (bytesCol >= 0) it.getLong(bytesCol) else 0L,
                    total = if (totalCol >= 0) it.getLong(totalCol) else -1L
                )
            }
        }
        return DownloadSnapshot(active, completed, failed, items)
    }

    LaunchedEffect(refreshTick) { snapshot = queryDownloads() }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = queryDownloads()
            delay(1200)
        }
    }

    // Probe real size only for direct http file URLs
    LaunchedEffect(urlInput) {
        probedBytes = null
        val raw = urlInput.trim()
        if (raw.length < 12 || !raw.startsWith("http")) return@LaunchedEffect
        val lower = raw.lowercase()
        if (lower.contains(".m3u8") || lower.contains(".mpd")) return@LaunchedEffect
        probing = true
        val size = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(raw).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 4000
                    readTimeout = 4000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 YouseifPlayer")
                }
                conn.connect()
                val len = conn.getHeaderField("Content-Length")?.toLongOrNull()
                    ?: conn.contentLengthLong.takeIf { it > 0 }
                conn.disconnect()
                len
            }.getOrNull()
        }
        probedBytes = size
        probing = false
    }

    fun isPlaylistUrl(raw: String): Boolean {
        val l = raw.lowercase()
        return l.contains(".m3u8") || l.contains(".mpd") || l.endsWith(".m3u")
    }

    fun formatSize(bytes: Long?): String {
        if (bytes == null || bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000) String.format("%.1f GB", mb / 1024.0)
        else String.format("%.0f MB", mb)
    }

    fun startDirectDownload() {
        val raw = urlInput.trim()
        if (raw.isBlank()) {
            statusMessage = "اكتب اسم فيلم أو الصق رابط MP4 مباشر"
            return
        }
        // Film name → search Fasel for real download servers
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            onSearchFilms(raw)
            statusMessage = "جاري البحث… اختر فيلم ثم من صفحة التفاصيل اضغط تحميل على سيرفر MP4"
            return
        }
        if (isPlaylistUrl(raw)) {
            statusMessage = "رابط m3u8 مش ملف واحد — من صفحة الفيلم اختار سيرفر تحميل MP4"
            return
        }
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            statusMessage = "رابط غير صالح"
            return
        }
        val baseName = (uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "youseif_${System.currentTimeMillis()}")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(100)
        val name = if (baseName.contains('.')) baseName else "$baseName.mp4"
        try {
            val request = DownloadManager.Request(uri)
                .setTitle(name)
                .setDescription("Youseif Player · تحميل حقيقي")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            manager.enqueue(request)
            val sizeHint = formatSize(probedBytes).ifBlank { "" }
            statusMessage = "بدأ التحميل: $name" + if (sizeHint.isNotBlank()) " · $sizeHint" else ""
            urlInput = ""
            refreshTick++
        } catch (e: Exception) {
            statusMessage = "فشل: ${e.message ?: "خطأ"}"
        }
    }

    fun clearByStatus(target: Int) {
        val ids = mutableListOf<Long>()
        manager.query(DownloadManager.Query().setFilterByStatus(target)).use { cursor ->
            val idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            if (idCol >= 0) while (cursor.moveToNext()) ids += cursor.getLong(idCol)
        }
        if (ids.isNotEmpty()) manager.remove(*ids.toLongArray())
        refreshTick++
    }

    Column(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        PlayerTopBar(onMenu = onOpenChannels)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DOWNLOADS", color = p.accentGlow, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(p.accentContainer.copy(alpha = .8f))
                        .border(1.dp, p.accentGlow.copy(alpha = .7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                ) {
                    Text("${snapshot.items.size}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(
                "تحميل مباشر بواجهة مرتبة · الحجم الحقيقي عند توفره",
                color = TextMuted, fontSize = 11.sp
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                singleLine = true,
                placeholder = {
                    Text("اسم فيلم أو رابط mp4/mkv…", color = TextMuted, fontSize = 12.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface.copy(alpha = .9f),
                    unfocusedContainerColor = DarkSurface.copy(alpha = .9f),
                    focusedBorderColor = p.accentGlow,
                    unfocusedBorderColor = CrimsonBorder.copy(alpha = .6f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = p.accentGlow
                ),
                shape = RoundedCornerShape(16.dp)
            )

            if (probing) {
                Text("جاري قياس حجم الملف…", color = TextMuted, fontSize = 11.sp)
            } else if (probedBytes != null && probedBytes!! > 0) {
                Text("الحجم الحقيقي: ${formatSize(probedBytes)}", color = p.accentGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // ---- Quality chooser cards (مرتبة) ----
            Text("اختر جودة التحميل", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(
                "الحجم التقريبي لا يغير جودة الرابط الأصلي",
                color = TextMuted, fontSize = 10.sp
            )
            DOWNLOAD_QUALITIES.forEachIndexed { idx, q ->
                val sel = idx == selectedQuality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) p.accentContainer.copy(alpha = .55f) else DarkCardDeep.copy(alpha = .75f))
                        .border(
                            2.dp,
                            if (sel) p.accentGlow else CrimsonBorder.copy(alpha = .55f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedQuality = idx }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(q.label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(q.detail, color = TextMuted, fontSize = 11.sp)
                    }
                    Text(q.sizeLabel, color = if (sel) p.accentGlow else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Primary action
            val chosen = DOWNLOAD_QUALITIES[selectedQuality.coerceIn(0, DOWNLOAD_QUALITIES.lastIndex)]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(p.accent.copy(alpha = .9f), p.secondary.copy(alpha = .55f))))
                    .border(1.dp, p.accentGlow.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .clickable { startDirectDownload() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        "بدء التحميل · ${chosen.label} · ${chosen.sizeLabel}",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black
                    )
                }
            }

            if (statusMessage.isNotBlank()) {
                Text(statusMessage, color = TextSecondary, fontSize = 12.sp)
            }

            // Search results from Fasel
            val results = searchResults.orEmpty()
            if (results.isNotEmpty()) {
                Text("نتائج فاصل — اضغط فيلم ثم تحميل من سيرفر MP4", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                results.take(15).forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCardDeep)
                            .border(1.dp, CrimsonBorder.copy(alpha = .45f), RoundedCornerShape(14.dp))
                            .clickable { onOpenMediaForDownload(m) }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Movie, null, tint = p.accentGlow, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.title.take(50), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(m.type.ifBlank { "movie" }, color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.Download, null, tint = NeonRedGlow, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نشط ${snapshot.active} · مكتمل ${snapshot.completed}",
                    color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                if (snapshot.failed > 0) {
                    TextButton(onClick = { clearByStatus(DownloadManager.STATUS_FAILED) }) {
                        Text("مسح الفاشل (${snapshot.failed})", color = NeonRedGlow, fontSize = 11.sp)
                    }
                }
            }

            val activeItems = snapshot.items.filter {
                it.status == DownloadManager.STATUS_PENDING ||
                    it.status == DownloadManager.STATUS_RUNNING ||
                    it.status == DownloadManager.STATUS_PAUSED
            }
            val doneItems = snapshot.items.filter { it.status == DownloadManager.STATUS_SUCCESSFUL }.take(8)

            if (activeItems.isEmpty() && doneItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCardDeep.copy(alpha = 0.6f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("مفيش تحميلات حالياً", color = TextMuted, fontSize = 13.sp)
                }
            }

            activeItems.forEach { item ->
                DownloadRow(
                    item = item,
                    accent = p.accentGlow,
                    onCancel = {
                        if (item.id >= 0) manager.remove(item.id)
                        refreshTick++
                    }
                )
            }
            if (doneItems.isNotEmpty()) {
                Text("مكتملة", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                doneItems.forEach { item ->
                    DownloadRow(item = item, accent = p.accentGlow, onCancel = null)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    accent: androidx.compose.ui.graphics.Color,
    onCancel: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardDeep)
            .border(1.dp, CrimsonBorder.copy(alpha = .4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title.take(48), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.statusLabel, color = TextMuted, fontSize = 10.sp)
            }
            if (onCancel != null) {
                Icon(
                    Icons.Default.Close, null, tint = NeonRedGlow,
                    modifier = Modifier.size(20.dp).clickable { onCancel() }
                )
            }
        }
        if (item.total > 0 && (
                item.status == DownloadManager.STATUS_RUNNING ||
                    item.status == DownloadManager.STATUS_PAUSED ||
                    item.status == DownloadManager.STATUS_PENDING
                )
        ) {
            LinearProgressIndicator(
                progress = { item.progressPct / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = DarkSurface
            )
        }
    }
}
