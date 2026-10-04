package com.example.ui.home

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PlaylistItem
import com.example.data.UserSettings
import com.example.player.YouseifPlayerController
import com.example.ui.components.PlayerTopBar
import com.example.ui.components.TopManageAction
import com.example.ui.components.visualScreenBackground
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.NeonRedContainer
import com.example.ui.theme.NeonRedGlow
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.activePlayerPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class LocalLibraryMode(val title: String, val subtitle: String) {
    VIDEO("Videos", "الكل + ألبومات + فنانون + أنواع + مجلدات الجهاز"),
    AUDIO("Music", "تشغيل صوتي مباشر مع فرز ذكي ومنع ظهور الصوت داخل الفيديو")
}

private enum class FolderKind(val label: String) {
    ALL("الكل"),
    ALBUM("الألبومات"),
    ARTIST("الفنانون"),
    GENRE("الأنواع"),
    DEVICE("مجلدات الجهاز")
}

private enum class MediaKind { AUDIO, VIDEO }

private data class LocalMediaItem(
    val id: Long,
    val title: String,
    val displayName: String,
    val uri: Uri,
    val mimeType: String,
    val album: String,
    val artist: String,
    val genre: String,
    val deviceFolder: String,
    val durationMs: Long,
    val kind: MediaKind
)

private object LocalMediaCache {
    var audio: List<LocalMediaItem>? = null
    var video: List<LocalMediaItem>? = null
    fun clearAudio() { audio = null }
    fun clearVideo() { video = null }
}

private data class FolderNode(val key: String, val label: String, val count: Int)

private object LocalMediaClassifier {
    private val audioExtensions = setOf("mp3", "m4a", "flac", "aac", "ogg", "opus", "wav", "wma")

    fun classify(context: Context, uri: Uri, mimeType: String?, displayName: String, collectionKind: MediaKind): MediaKind {
        val mime = mimeType.orEmpty().lowercase()
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (mime.startsWith("audio/") || ext in audioExtensions) return MediaKind.AUDIO
        val hasVideo = hasVideoTrack(context, uri)
        if (hasVideo == false) return MediaKind.AUDIO
        if (mime.startsWith("video/")) return MediaKind.VIDEO
        return if (collectionKind == MediaKind.VIDEO && hasVideo != false) MediaKind.VIDEO else collectionKind
    }

    fun probeGenre(context: Context, uri: Uri): String {
        return runCatching {
            MediaMetadataRetriever().useRetriever { mmr ->
                mmr.setDataSource(context, uri)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty()
            }
        }.getOrDefault("")
    }

    private fun hasVideoTrack(context: Context, uri: Uri): Boolean? {
        return runCatching {
            MediaMetadataRetriever().useRetriever { mmr ->
                mmr.setDataSource(context, uri)
                val hasFlag = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO).orEmpty().lowercase()
                if (hasFlag == "yes" || hasFlag == "true" || hasFlag == "1") return@useRetriever true
                val width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                width > 0 && height > 0
            }
        }.getOrNull()
    }
}

private inline fun <T> MediaMetadataRetriever.useRetriever(block: (MediaMetadataRetriever) -> T): T {
    return try {
        block(this)
    } finally {
        runCatching { release() }
    }
}

private fun hasMediaPermission(context: Context, mode: LocalLibraryMode): Boolean {
    val permission = when {
        Build.VERSION.SDK_INT >= 33 && mode == LocalLibraryMode.AUDIO -> Manifest.permission.READ_MEDIA_AUDIO
        Build.VERSION.SDK_INT >= 33 && mode == LocalLibraryMode.VIDEO -> Manifest.permission.READ_MEDIA_VIDEO
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun permissionArray(mode: LocalLibraryMode): Array<String> = when {
    Build.VERSION.SDK_INT >= 33 && mode == LocalLibraryMode.AUDIO -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    Build.VERSION.SDK_INT >= 33 && mode == LocalLibraryMode.VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun String.cleanMeta(fallback: String): String {
    val value = trim().takeIf { it.isNotBlank() && !it.equals("<unknown>", true) } ?: fallback
    return value.take(60)
}

private fun deviceFolderFrom(relativePath: String, displayName: String): String {
    val clean = relativePath.trim().trim('/').substringAfterLast('/').ifBlank {
        displayName.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "جهازي" }
    }
    return clean.ifBlank { "جهازي" }.replace('_', ' ').replace('-', ' ')
}

private fun buildFolderNodes(items: List<LocalMediaItem>, kind: FolderKind): List<FolderNode> {
    val groups = when (kind) {
        FolderKind.ALL -> listOf(FolderNode("ALL", "الكل", items.size))
        FolderKind.ALBUM -> items.groupingBy { it.album.cleanMeta("بدون ألبوم") }.eachCount()
            .toList().sortedByDescending { it.second }.map { FolderNode(it.first, it.first, it.second) }
        FolderKind.ARTIST -> items.groupingBy { it.artist.cleanMeta("بدون فنان") }.eachCount()
            .toList().sortedByDescending { it.second }.map { FolderNode(it.first, it.first, it.second) }
        FolderKind.GENRE -> items.groupingBy { it.genre.cleanMeta("بدون نوع") }.eachCount()
            .toList().sortedByDescending { it.second }.map { FolderNode(it.first, it.first, it.second) }
        FolderKind.DEVICE -> items.groupingBy { it.deviceFolder.cleanMeta("جهازي") }.eachCount()
            .toList().sortedByDescending { it.second }.map { FolderNode(it.first, it.first, it.second) }
    }
    return if (kind == FolderKind.ALL) groups else listOf(FolderNode("ALL", "الكل", items.size)) + groups
}

private fun matchFolder(item: LocalMediaItem, kind: FolderKind, selected: String): Boolean {
    if (selected == "ALL") return true
    return when (kind) {
        FolderKind.ALL -> true
        FolderKind.ALBUM -> item.album.cleanMeta("بدون ألبوم") == selected
        FolderKind.ARTIST -> item.artist.cleanMeta("بدون فنان") == selected
        FolderKind.GENRE -> item.genre.cleanMeta("بدون نوع") == selected
        FolderKind.DEVICE -> item.deviceFolder.cleanMeta("جهازي") == selected
    }
}

private suspend fun loadAudioItems(context: Context): List<LocalMediaItem> = withContext(Dispatchers.IO) {
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.RELATIVE_PATH,
        MediaStore.Audio.Media.DURATION
    )
    val result = mutableListOf<LocalMediaItem>()
    context.contentResolver.query(uri, projection, null, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(uri, id)
            val displayName = cursor.getString(displayCol).orEmpty()
            val mime = cursor.getString(mimeCol).orEmpty()
            val title = cursor.getString(titleCol).orEmpty().trim()
                .takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                ?.take(120)
                ?: displayName.substringBeforeLast('.').ifBlank { "ملف صوت" }
            val album = cursor.getString(albumCol).orEmpty()
            val artist = cursor.getString(artistCol).orEmpty()
            val deviceFolder = deviceFolderFrom(cursor.getString(pathCol).orEmpty(), displayName)
            val genre = ""
            val durationMs = cursor.getLong(durationCol)
            // تجاهل أصوات النظام / الرنات / ملفات بدون مدة أو أقصر من 8 ثواني
            if (!isValidAudioTrack(title, displayName, album, artist, deviceFolder, durationMs)) continue
            result += LocalMediaItem(
                id = id,
                title = title,
                displayName = displayName,
                uri = contentUri,
                mimeType = mime,
                album = album,
                artist = artist,
                genre = genre,
                deviceFolder = deviceFolder,
                durationMs = durationMs,
                kind = MediaKind.AUDIO
            )
        }
    }
    result
}

/** أغاني حقيقية فقط — بدون رنات/إشعارات/ملفات 2–3 ثواني */
private fun isValidAudioTrack(
    title: String, displayName: String, album: String, artist: String, folder: String, durationMs: Long
): Boolean {
    if (durationMs in 1L until 8_000L) return false // أقصر من 8 ثواني
    // duration=0 غالباً ميتا-داتا ناقصة أو ملف تالف — نستبعده لو الاسم يشبه نظام
    val blob = "$title $displayName $album $artist $folder".lowercase()
    val junk = listOf(
        "ringtone", "notification", "alarm", "ui_", "effect", "sound effect",
        "رنين", "إشعار", "اشعار", "تنبيه", "نغمة", "system", "touch", "lock",
        "unlock", "camera_click", "boot", "twinkle", "hangouts", "samsung",
        "pixel", "oppo", "xiaomi", "huawei", "realme", "infinix", "tecno",
        "recordings", "call_rec", "whatsapp audio", "recording"
    )
    if (junk.any { blob.contains(it) }) return false
    // مجلدات نظام شائعة
    val sysFolders = listOf("ringtones", "notifications", "alarms", "ui", "samsung", "miui")
    if (sysFolders.any { folder.lowercase().contains(it) }) return false
    return true
}

private suspend fun loadVideoItems(context: Context): List<LocalMediaItem> = withContext(Dispatchers.IO) {
    val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.RELATIVE_PATH,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DURATION
    )
    val result = mutableListOf<LocalMediaItem>()
    context.contentResolver.query(uri, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
        val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(uri, id)
            val displayName = cursor.getString(displayCol).orEmpty()
            val mime = cursor.getString(mimeCol).orEmpty()
            val title = cursor.getString(titleCol).orEmpty().trim()
                .takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                ?.take(120)
                ?: displayName.substringBeforeLast('.').ifBlank { "ملف فيديو" }
            val deviceFolder = deviceFolderFrom(cursor.getString(pathCol).orEmpty(), displayName)
            val bucket = cursor.getString(bucketCol).orEmpty().cleanMeta(deviceFolder)
            val genre = ""
            result += LocalMediaItem(
                id = id,
                title = title,
                displayName = displayName,
                uri = contentUri,
                mimeType = mime,
                album = bucket,
                artist = bucket,
                genre = genre,
                deviceFolder = deviceFolder,
                durationMs = cursor.getLong(durationCol),
                kind = MediaKind.VIDEO
            )
        }
    }
    result
}

private fun durationLabel(durationMs: Long): String {
    if (durationMs <= 0L) return "—"
    val totalSec = durationMs / 1000L
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}


private fun LocalMediaItem.toPlaylistItem(): PlaylistItem = PlaylistItem(
    id = "local_${kind}_$id",
    channelNumber = 0,
    name = title,
    url = uri.toString(),
    group = album.cleanMeta("بدون ألبوم"),
    logoUrl = "",
    language = "AR",
    isLive = false,
    isFavorite = false,
    isCustom = true
)

@Composable
fun LocalMediaScreen(
    forAudio: Boolean,
    controller: YouseifPlayerController,
    onBack: () -> Unit,
    onHistoryUrl: (String, String, String) -> Unit = { _, _, _ -> },
    onToggleMode: (Boolean) -> Unit = {},
    showModeTabs: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val palette = activePlayerPalette()
    val mode = if (forAudio) LocalLibraryMode.AUDIO else LocalLibraryMode.VIDEO
    val diagnostics by controller.diagnostics.collectAsState()
    val currentChannel by controller.currentChannel.collectAsState()
    val repeatMode by controller.repeatMode.collectAsState()
    var searchQuery by remember(mode) { mutableStateOf("") }
    var searchOpen by remember(mode) { mutableStateOf(false) }
    var hiddenIds by remember(mode) { mutableStateOf(setOf<Long>()) }
    var loading by remember(mode) { mutableStateOf(false) }
    var reloadKey by remember(mode) { mutableStateOf(0) }
    var items by remember(mode) { mutableStateOf(emptyList<LocalMediaItem>()) }
    var folderKind by remember(mode) { mutableStateOf(FolderKind.ALL) }
    var selectedNode by remember(mode) { mutableStateOf("ALL") }
    var hasPermission by remember(mode) { mutableStateOf(hasMediaPermission(context, mode)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.any { it }
        if (hasPermission) reloadKey += 1
    }

    LaunchedEffect(mode, hasPermission, reloadKey) {
        if (!hasPermission) return@LaunchedEffect
        loading = true
        if (reloadKey > 0) {
            if (forAudio) LocalMediaCache.clearAudio() else LocalMediaCache.clearVideo()
        }
        items = if (forAudio) {
            LocalMediaCache.audio ?: loadAudioItems(context).also { LocalMediaCache.audio = it }
        } else {
            LocalMediaCache.video ?: loadVideoItems(context).also { LocalMediaCache.video = it }
        }
        loading = false
    }

    val folderNodes = remember(items, folderKind) { buildFolderNodes(items, folderKind) }
    LaunchedEffect(folderKind, folderNodes) {
        if (folderNodes.none { it.key == selectedNode }) selectedNode = "ALL"
    }

    var showFavOnly by remember(mode) { mutableStateOf(false) }
    var favTick by remember { mutableStateOf(0) }
    val localFavs = remember(mode, favTick) {
        if (forAudio) UserSettings.localAudioFavorites() else UserSettings.localVideoFavorites()
    }
    val filtered = remember(items, folderKind, selectedNode, searchQuery, hiddenIds, showFavOnly, localFavs) {
        items.filter { item ->
            if (item.id in hiddenIds) return@filter false
            if (showFavOnly && item.id.toString() !in localFavs) return@filter false
            val searchOk = if (searchQuery.isBlank()) true else listOf(item.title, item.displayName, item.album, item.artist, item.genre, item.deviceFolder)
                .any { it.contains(searchQuery, true) }
            matchFolder(item, folderKind, selectedNode) && searchOk
        }
    }

    Box(modifier = Modifier.fillMaxSize().visualScreenBackground()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayerTopBar(
                onBack = {
                    if (showFavOnly) showFavOnly = false else onBack()
                },
                onMenu = null,
                onRefresh = { reloadKey += 1 },
                onInfo = null,
                onFavorites = { showFavOnly = !showFavOnly },
                favoritesActive = showFavOnly,
                showThemeButton = false,
                manageActions = listOf(
                    TopManageAction("إخفاء كل الظاهر", destructive = true) {
                        hiddenIds = hiddenIds + filtered.map { it.id }.toSet()
                    },
                    TopManageAction("إظهار المخفي") { hiddenIds = emptySet() },
                    TopManageAction(if (showFavOnly) "عرض الكل" else "المفضلة فقط") {
                        showFavOnly = !showFavOnly
                    }
                )
            )

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
                // صف واحد زي المكتبة: [صوت | فيديو] + بحث + تحديث
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showModeTabs) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface.copy(alpha = .9f))
                                .border(1.dp, CrimsonBorder.copy(alpha = .65f), RoundedCornerShape(14.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(true to "🔊 صوت", false to "🎬 فيديو").forEach { (audio, label) ->
                                val sel = forAudio == audio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (sel) NeonRedContainer else Color.Transparent)
                                        .clickable { onToggleMode(audio) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) TextPrimary else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = if (sel) FontWeight.Black else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (forAudio) "مكتبة الصوت" else "مكتبة الفيديو",
                                color = TextMuted, fontSize = 10.sp, maxLines = 1
                            )
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
                        Icon(
                            Icons.Default.Search, null,
                            tint = if (searchOpen) NeonRedGlow else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface.copy(alpha = .8f))
                            .border(1.dp, CrimsonBorder.copy(alpha = .85f), RoundedCornerShape(12.dp))
                            .clickable { reloadKey += 1 }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Refresh, null, tint = palette.accentGlow, modifier = Modifier.size(14.dp))
                            Text("تحديث", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (searchOpen) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 18.sp),
                        placeholder = {
                            Text(
                                if (forAudio) "ابحث عن أغنية أو ألبوم أو فنان" else "ابحث عن فيديو",
                                color = TextMuted.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = palette.accentGlow, modifier = Modifier.size(18.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1220),
                            unfocusedContainerColor = Color(0xFF1A1220),
                            focusedBorderColor = palette.accentGlow,
                            unfocusedBorderColor = CrimsonBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = palette.accentGlow,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(Modifier.height(6.dp))
                if (!hasPermission) {
                    PermissionBox(mode = mode, onGrant = { permissionLauncher.launch(permissionArray(mode)) })
                } else {
                    FolderHierarchyStrip(
                        items = items,
                        currentKind = folderKind,
                        selectedNode = selectedNode,
                        nodes = folderNodes,
                        onKindSelected = { folderKind = it; selectedNode = "ALL" },
                        onNodeSelected = { selectedNode = it }
                    )

                    Spacer(Modifier.height(6.dp))
                    if (loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("جاري قراءة ملفات الجهاز...", color = TextMuted, fontSize = 13.sp)
                        }
                    } else if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد عناصر مطابقة حالياً", color = TextMuted, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { "${it.kind}_${it.id}" }) { item ->
                                val current = currentChannel?.url == item.uri.toString()
                                MediaRow(
                                    item = item,
                                    current = current,
                                    onPrimary = {
                                        val queue = filtered.map { it.toPlaylistItem() }
                                        val idx = filtered.indexOf(item).coerceAtLeast(0)
                                        if (forAudio) {
                                            controller.setLocalQueue(queue, idx, autoPlay = true)
                                        } else {
                                            controller.playUrl(item.uri.toString(), item.title, false)
                                        }
                                        onHistoryUrl(item.title, item.uri.toString(), if (forAudio) "AUDIO" else "VIDEO")
                                    },
                                    onInlinePlay = if (item.kind == MediaKind.AUDIO) {
                                        {
                                            val queue = filtered.map { it.toPlaylistItem() }
                                            val idx = filtered.indexOf(item).coerceAtLeast(0)
                                            controller.setLocalQueue(queue, idx, autoPlay = true)
                                            onHistoryUrl(item.title, item.uri.toString(), if (forAudio) "AUDIO" else "VIDEO")
                                        }
                                    } else null,
                                    onDelete = if (forAudio) {
                                        {
                                            hiddenIds = hiddenIds + item.id
                                            val q = controller.localQueue.value.filter { it.id != "local_${item.kind}_${item.id}" }
                                            if (q.isNotEmpty()) controller.setLocalQueue(q, 0, autoPlay = false)
                                            else controller.clearLocalQueue()
                                        }
                                    } else null,
                                    isFavorite = item.id.toString() in localFavs,
                                    onToggleFavorite = {
                                        if (forAudio) UserSettings.toggleLocalAudioFavorite(item.id.toString())
                                        else UserSettings.toggleLocalVideoFavorite(item.id.toString())
                                        favTick++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ميني بلاير أسفل الشاشة بشكل أنظف
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            MiniPlayerStrip(
                title = currentChannel?.name ?: "لا يوجد تشغيل الآن",
                subtitle = when {
                    !forAudio -> if (diagnostics.isPlaying) "المشغل شغال" else "اضغط تشغيل"
                    diagnostics.isPlaying -> "شغال · ${when (repeatMode) {
                        YouseifPlayerController.RepeatMode.OFF -> "بدون تكرار"
                        YouseifPlayerController.RepeatMode.ONE -> "تكرار أغنية"
                        YouseifPlayerController.RepeatMode.ALL -> "تكرار القائمة"
                    }}"
                    else -> "اختر أغنية"
                },
                isPlaying = diagnostics.isPlaying,
                showMusicControls = forAudio,
                repeatMode = repeatMode,
                onPrev = { controller.playPreviousInQueue() },
                onNext = { controller.playNextInQueue() },
                onCycleRepeat = { controller.cycleRepeatMode() },
                onPlayPause = { controller.togglePlayPause() }
            )
        }
    }
}

@Composable
private fun MiniPlayerStrip(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    showMusicControls: Boolean = false,
    repeatMode: YouseifPlayerController.RepeatMode = YouseifPlayerController.RepeatMode.OFF,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onPlayPause: () -> Unit = {}
) {
    val palette = activePlayerPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xEE1A0F18),
                        Color(0xF50E0810)
                    )
                )
            )
            .border(1.dp, if (isPlaying) LiveGreen.copy(alpha = 0.65f) else palette.accentGlow.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accentContainer.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (showMusicControls) Icons.Default.MusicNote else Icons.Default.Movie,
                    null,
                    tint = palette.accentGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showMusicControls) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(DarkSurface.copy(alpha = .9f))
                            .clickable(onClick = onPrev),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape)
                            .background(palette.accent)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(DarkSurface.copy(alpha = .9f))
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        Modifier.size(30.dp).clip(CircleShape)
                            .background(if (repeatMode == YouseifPlayerController.RepeatMode.OFF) DarkSurface else palette.accentContainer)
                            .clickable(onClick = onCycleRepeat),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (repeatMode == YouseifPlayerController.RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            null,
                            tint = if (repeatMode == YouseifPlayerController.RepeatMode.OFF) TextMuted else palette.accentGlow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(palette.accent)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBox(mode: LocalLibraryMode, onGrant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBg.copy(alpha = 0.90f))
            .border(1.dp, CrimsonBorder.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مطلوب إذن للوصول إلى ${if (mode == LocalLibraryMode.AUDIO) "الصوتيات" else "الفيديوهات"}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("بعد السماح، التطبيق هيبني فولدر الكل + الألبومات + الفنانين + الأنواع + مجلدات الجهاز تلقائياً.", color = TextMuted, fontSize = 11.sp)
            Button(onClick = onGrant) { Text("سماح") }
        }
    }
}

@Composable
private fun FolderHierarchyStrip(
    items: List<LocalMediaItem>,
    currentKind: FolderKind,
    selectedNode: String,
    nodes: List<FolderNode>,
    onKindSelected: (FolderKind) -> Unit,
    onNodeSelected: (String) -> Unit
) {
    val palette = activePlayerPalette()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(FolderKind.values().toList()) { kind ->
            val count = when (kind) {
                FolderKind.ALL -> items.size
                FolderKind.ALBUM -> items.map { it.album.cleanMeta("بدون ألبوم") }.distinct().size
                FolderKind.ARTIST -> items.map { it.artist.cleanMeta("بدون فنان") }.distinct().size
                FolderKind.GENRE -> items.map { it.genre.cleanMeta("بدون نوع") }.distinct().size
                FolderKind.DEVICE -> items.map { it.deviceFolder.cleanMeta("جهازي") }.distinct().size
            }
            val selected = currentKind == kind
            ChipBox(label = kind.label, count = count, selected = selected, accent = palette.accentGlow, onClick = { onKindSelected(kind) }, icon = {
                Icon(
                    imageVector = when (kind) {
                        FolderKind.ALL -> Icons.Default.Folder
                        FolderKind.ALBUM -> Icons.Default.Album
                        FolderKind.ARTIST -> Icons.Default.Person
                        FolderKind.GENRE -> Icons.Default.Category
                        FolderKind.DEVICE -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = if (selected) palette.accentGlow else TextMuted,
                    modifier = Modifier.size(12.dp)
                )
            })
        }
    }
    // Hide duplicate "الكل" second row when already on ALL — frees space for the song list
    if (currentKind != FolderKind.ALL && nodes.isNotEmpty()) {
        Spacer(Modifier.height(5.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(nodes.filter { it.key != "ALL" || nodes.size == 1 }) { node ->
                ChipBox(label = node.label, count = node.count, selected = node.key == selectedNode, accent = TechCyanGlow, onClick = { onNodeSelected(node.key) })
            }
        }
    }
}

@Composable
private fun ChipBox(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) NeonRedContainer.copy(alpha = 0.92f) else DarkSurface.copy(alpha = 0.80f))
            .border(1.dp, if (selected) accent else CrimsonBorder.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            icon?.invoke()
            Text(label, color = if (selected) TextPrimary else TextPrimary.copy(alpha = 0.92f), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(count.toString(), color = if (selected) accent else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun MediaThumbnail(
    item: LocalMediaItem,
    modifier: Modifier = Modifier,
    fallbackTint: Color
) {
    val context = LocalContext.current
    var bitmap by remember(item.id, item.kind) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id, item.kind) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(128, 128), null)
                } else {
                    when (item.kind) {
                        MediaKind.VIDEO -> {
                            @Suppress("DEPRECATION")
                            MediaStore.Video.Thumbnails.getThumbnail(
                                context.contentResolver,
                                item.id,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                        MediaKind.AUDIO -> {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, item.uri)
                                val art = retriever.embeddedPicture
                                if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
                            } finally {
                                retriever.release()
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, CrimsonBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = if (item.kind == MediaKind.AUDIO) Icons.Default.MusicNote else Icons.Default.Movie,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun MediaRow(
    item: LocalMediaItem,
    current: Boolean,
    onPrimary: () -> Unit,
    onInlinePlay: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    val palette = activePlayerPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (current) NeonRedContainer.copy(alpha = 0.70f) else DarkCardBg.copy(alpha = 0.90f))
            .border(1.dp, if (current) palette.accentGlow.copy(alpha = 0.82f) else CrimsonBorder.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
            .clickable(onClick = onPrimary)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MediaThumbnail(
                item = item,
                modifier = Modifier.size(58.dp),
                fallbackTint = if (current) palette.accentGlow else TechCyanGlow
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = if (current && item.title.length > 18) Modifier.basicMarquee() else Modifier
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (item.kind == MediaKind.AUDIO) Icons.Default.MusicNote else Icons.Default.Movie,
                        contentDescription = null,
                        tint = palette.accentGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        listOf(item.album.cleanMeta("بدون ألبوم"), item.artist.cleanMeta("بدون فنان"), item.genre.cleanMeta("بدون نوع"), item.deviceFolder.cleanMeta("جهازي"))
                            .distinct().joinToString(" • "),
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("${item.mimeType.ifBlank { if (item.kind == MediaKind.AUDIO) "audio/*" else "video/*" }} • ${durationLabel(item.durationMs)}", color = TechCyanGlow, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = 0.9f))
                        .border(1.dp, CrimsonBorder.copy(alpha = 0.7f), CircleShape)
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "مفضلة",
                        tint = if (isFavorite) Color(0xFFFF4D6D) else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DarkSurface.copy(alpha = 0.9f))
                        .border(1.dp, CrimsonBorder.copy(alpha = 0.7f), CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF4D4D), modifier = Modifier.size(18.dp))
                }
            }
            if (onInlinePlay != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(palette.accentContainer.copy(alpha = 0.96f), palette.accentDark.copy(alpha = 0.70f))))
                        .border(1.dp, palette.accentGlow.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onInlinePlay)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
