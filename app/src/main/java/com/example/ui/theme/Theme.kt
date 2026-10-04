package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    // Force recomposition whenever custom/preset palette changes
    val _gen = PaletteGeneration
    val p = palette()
    val scheme = darkColorScheme(
        primary = p.accent,
        onPrimary = p.text,
        primaryContainer = p.accentContainer,
        onPrimaryContainer = p.accentGlow,
        secondary = p.secondary,
        onSecondary = p.background,
        tertiary = SignalYellow,
        onTertiary = p.background,
        background = p.background,
        onBackground = p.text,
        surface = p.surface,
        onSurface = p.text,
        surfaceVariant = p.surfaceVariant,
        onSurfaceVariant = p.secondaryText,
        outline = p.border,
        outlineVariant = p.borderStrong
    )
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = p.background.toArgb()
                window.navigationBarColor = p.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
