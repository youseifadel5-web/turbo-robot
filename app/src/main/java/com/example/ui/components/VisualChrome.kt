package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LiveRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.activePlayerPalette
import com.example.ui.theme.applyPlayerTheme
import com.example.ui.theme.availablePlayerThemes
import com.example.ui.theme.glassGradient
import kotlinx.coroutines.delay

@Composable
fun ChromeSquare(
    onClick: (() -> Unit)?,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    radius: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable () -> Unit
) {
    val p = activePlayerPalette()
    IconButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier
            .size(size)
            .shadow(
                12.dp, RoundedCornerShape(radius),
                ambientColor = p.accent.copy(alpha = .28f),
                spotColor = p.accentGlow.copy(alpha = .5f)
            )
            .clip(RoundedCornerShape(radius))
            .background(glassGradient(forDark = true))
            .border(1.dp, p.borderStrong.copy(alpha = .95f), RoundedCornerShape(radius))
    ) { content() }
}

@Composable
fun ChromeMenuButton(onClick: (() -> Unit)?) {
    ChromeSquare(onClick = onClick) {
        val p = activePlayerPalette()
        // 3 خطوط ناعمة مدورة بدل أيقونة Menu الحادة
        Column(
            modifier = Modifier.size(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            listOf(16.dp, 12.dp, 16.dp).forEach { w ->
                Box(
                    modifier = Modifier
                        .size(width = w, height = 2.4.dp)
                        .background(p.accentGlow, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
fun ChromeBackButton(onClick: (() -> Unit)?) {
    ChromeSquare(onClick = onClick) {
        val p = activePlayerPalette()
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
            tint = p.accentGlow, modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ChromeIconButton(
    iconRes: Int,
    description: String,
    onClick: (() -> Unit)?,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    ChromeSquare(onClick = onClick) {
        Image(
            painterResource(iconRes), description,
            modifier = Modifier.size(iconSize),
            colorFilter = ColorFilter.tint(activePlayerPalette().accentGlow)
        )
    }
}

/**
 * زر الثيم:
 *  - ضغطة عادية → لون واحد (التالي في القائمة)
 *  - دوسة مطوّلة → تفعيل/إيقاف التغيير التلقائي
 * قائمة الألوان الكاملة من القائمة الجانبية فقط
 */
@Composable
fun ThemeRefreshButton(
    autoEnabled: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onSingleExtra: (() -> Unit)? = null,
    onThemeCycle: (() -> Unit)? = null
) {
    val p = activePlayerPalette()

    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(
                10.dp, RoundedCornerShape(14.dp),
                ambientColor = p.accent.copy(alpha = .22f),
                spotColor = p.accentGlow.copy(alpha = .4f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(if (autoEnabled) p.accentContainer.copy(alpha = .82f) else Color.Transparent)
            .border(
                if (autoEnabled) 1.5.dp else 1.dp,
                if (autoEnabled) p.accentGlow else p.border.copy(alpha = .42f),
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        try {
                            if (onThemeCycle != null) onThemeCycle()
                            else {
                                val themes = availablePlayerThemes()
                                if (themes.isNotEmpty()) {
                                    val i = themes.indexOfFirst { it.name == activePlayerPalette().name }.coerceAtLeast(0)
                                    applyPlayerTheme(themes[(i + 1) % themes.size].name)
                                }
                            }
                        } catch (_: Throwable) {}
                    },
                    onLongPress = {
                        try {
                            onLongPress?.invoke()
                        } catch (_: Throwable) {}
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(R.drawable.refresh),
            contentDescription = "تغيير الثيم",
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(p.accentGlow)
        )
    }
}

data class TopManageAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun PlayerTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    onDownloads: (() -> Unit)? = null,
    /** قلب المفضلة — يظهر مكان زر التحميل على أقصى اليمين */
    onFavorites: (() -> Unit)? = null,
    favoritesActive: Boolean = false,
    /** كوكب: قطع النت عن التطبيق (وضع أوفلاين / جهازي فقط) */
    onOfflineToggle: (() -> Unit)? = null,
    offlineActive: Boolean = false,
    extraAction: (@Composable () -> Unit)? = null,
    autoThemeEnabled: Boolean = false,
    onToggleAutoTheme: (() -> Unit)? = null,
    onLongPressTheme: (() -> Unit)? = null,
    onThemeCycle: (() -> Unit)? = null,
    /** زر الثيم: الرئيسية + الإعدادات فقط */
    showThemeButton: Boolean = true,
    /** قائمة ⋮ إدارة (حذف / تحديد / …) — أقصى اليمين جنب القلب */
    manageActions: List<TopManageAction>? = null,
    title: String = "youseif",
    subtitle: String = "player pro",
    slot1: (@Composable () -> Unit)? = null,
    slot2: (@Composable () -> Unit)? = null,
    slot3: (@Composable () -> Unit)? = null,
    showQuickSlots: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) ChromeBackButton(onBack) else ChromeMenuButton(onMenu)
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.height(48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title.ifBlank { "Youseif" },
                    color = activePlayerPalette().accentGlow,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle.ifBlank { "PLAYER PRO" },
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.0.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                extraAction?.invoke()
                if (showThemeButton) {
                    ThemeRefreshButton(
                        autoEnabled = autoThemeEnabled,
                        onToggle = onToggleAutoTheme,
                        onLongPress = onLongPressTheme,
                        onSingleExtra = onRefresh,
                        onThemeCycle = onThemeCycle
                    )
                }
                // أقصى اليمين: مفضلة القسم أو كوكب أوفلاين
                when {
                    onFavorites != null -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (favoritesActive) activePlayerPalette().accentContainer.copy(alpha = .85f)
                                    else Color.Transparent
                                )
                                .clickable(onClick = onFavorites),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (favoritesActive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (favoritesActive) LiveRed else activePlayerPalette().accentGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    onOfflineToggle != null -> {
                        val op = activePlayerPalette()
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (offlineActive) op.accentContainer.copy(alpha = .92f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (offlineActive) 1.5.dp else 1.dp,
                                    color = if (offlineActive) op.accentGlow else op.border.copy(alpha = .32f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(onClick = onOfflineToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = if (offlineActive) "تفعيل النت" else "قطع النت",
                                tint = if (offlineActive) op.accentGlow else TextMuted.copy(alpha = .5f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    onDownloads != null -> ChromeIconButton(R.drawable.nav_downloads_3d, "Downloads", onDownloads, 20.dp)
                    onInfo != null -> ChromeIconButton(R.drawable.nav_downloads_3d, "Info", onInfo, 20.dp)
                }
                // ⋮ إدارة — أقصى اليمين جنب القلب
                if (!manageActions.isNullOrEmpty()) {
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface.copy(alpha = 0.85f))
                                .border(1.dp, CrimsonBorder.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { menuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "إدارة",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier
                                .widthIn(min = 200.dp)
                                .border(1.dp, CrimsonBorder.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                            containerColor = DarkCardBg.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            manageActions.forEachIndexed { index, action ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            action.label,
                                            color = if (action.destructive) LiveRed else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (action.destructive) FontWeight.Bold else FontWeight.SemiBold,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        action.onClick()
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showQuickSlots && (slot1 != null || slot2 != null || slot3 != null)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) { slot1?.invoke() }
                Box(Modifier.weight(1f)) { slot2?.invoke() }
                Box(Modifier.weight(1f)) { slot3?.invoke() }
            }
        }
    }
}

@Composable
fun TopQuickSlot(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val p = activePlayerPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (active) Modifier.background(p.accentContainer.copy(alpha = 0.75f))
                else Modifier.background(glassGradient(forDark = true))
            )
            .border(
                1.dp,
                if (active) p.accentGlow else p.border.copy(alpha = 0.75f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) p.accentGlow else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
    }
}

fun Modifier.visualScreenBackground(): Modifier = this.background(
    com.example.ui.theme.ambientBackgroundBrush()
)

fun Modifier.glassCard(radius: Int = 22): Modifier = this
    .clip(RoundedCornerShape(radius.dp))
    .background(glassGradient(forDark = true))
    .border(1.dp, activePlayerPalette().border.copy(alpha = .78f), RoundedCornerShape(radius.dp))
