package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** Central visual-token record. Single source of truth for every UI surface. */
data class PlayerPalette(
    val name: String,
    val background: Color,
    val backgroundGlowTop: Color,
    val backgroundGlowBottom: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val cardDeep: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentGlow: Color,
    val accentDark: Color,
    val accentContainer: Color,
    val secondary: Color,
    val secondaryGlow: Color,
    val text: Color,
    val secondaryText: Color,
    val muted: Color,
    val navSelectedFill: Color,
    val navSelectedBorder: Color,
    val glassTop: Color,
    val glassMid: Color,
    val glassBottom: Color,
)

val LiveGreen = Color(0xFF00E676)
val LiveRed = Color(0xFFFF1744)
val SignalGreen = Color(0xFF00E676)
val SignalYellow = Color(0xFFFFD600)
val SignalOrange = Color(0xFFFF9100)
val TechGold = Color(0xFFFFC107)
val ErrorRed = Color(0xFFFF4D4D)
val VideoSurfaceBlack = Color(0xFF000000)

// Legacy aliases still referenced across UI
val AmoledBlack get() = activePalette.background
val DarkSurface get() = activePalette.surface
val DarkSurfaceVariant get() = activePalette.surfaceVariant
val DarkCardBg get() = activePalette.card
val DarkCardDeep get() = activePalette.cardDeep
val CrimsonBorder get() = activePalette.border
val NeonRed get() = activePalette.accent
val NeonRedGlow get() = activePalette.accentGlow
val NeonRedContainer get() = activePalette.accentContainer
val TextPrimary get() = activePalette.text
val TextSecondary get() = activePalette.secondaryText
val TextMuted get() = activePalette.muted
val TechCyan get() = activePalette.secondary
val TechCyanGlow get() = activePalette.secondaryGlow

private fun hsvColor(h: Float, s: Float, v: Float, a: Float = 1f): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (rp + m).coerceIn(0f, 1f),
        green = (gp + m).coerceIn(0f, 1f),
        blue = (bp + m).coerceIn(0f, 1f),
        alpha = a
    )
}

private fun neonPalette(name: String, hue: Float, secondaryHueOffset: Float = 160f): PlayerPalette {
    val h = hue
    val h2 = (hue + secondaryHueOffset) % 360f
    return PlayerPalette(
        name = name,
        background = hsvColor(h, 0.35f, 0.04f),
        backgroundGlowTop = hsvColor(h, 0.55f, 0.18f),
        backgroundGlowBottom = hsvColor(h, 0.40f, 0.10f),
        surface = hsvColor(h, 0.30f, 0.08f),
        surfaceVariant = hsvColor(h, 0.32f, 0.12f),
        card = hsvColor(h, 0.28f, 0.11f),
        cardDeep = hsvColor(h, 0.35f, 0.06f),
        border = hsvColor(h, 0.55f, 0.32f),
        borderStrong = hsvColor(h, 0.70f, 0.55f),
        accent = hsvColor(h, 0.85f, 1.0f),
        accentGlow = hsvColor(h, 0.70f, 1.0f),
        accentDark = hsvColor(h, 0.80f, 0.40f),
        accentContainer = hsvColor(h, 0.55f, 0.22f),
        secondary = hsvColor(h2, 0.75f, 0.90f),
        secondaryGlow = hsvColor(h2, 0.55f, 0.96f),
        text = Color.White,
        secondaryText = hsvColor(h, 0.08f, 0.86f),
        muted = hsvColor(h, 0.12f, 0.52f),
        navSelectedFill = hsvColor(h, 0.50f, 0.16f),
        navSelectedBorder = hsvColor(h, 0.75f, 0.55f),
        glassTop = hsvColor(h, 0.28f, 0.26f),
        glassMid = hsvColor(h, 0.32f, 0.12f),
        glassBottom = hsvColor(h, 0.38f, 0.07f),
    )
}

private val palettes = listOf(
    // Matches Stream Info / Connection neon panel (purple → cyan)
    neonPalette("CYBER NEON", 275f, 185f),
    neonPalette("AURORA FLOW", 285f, 175f),
    neonPalette("RED NEON", 350f, 20f),
    neonPalette("GREEN NEON", 140f, 190f),
    neonPalette("CYBER CYAN", 190f, 300f),
    neonPalette("NEON PURPLE", 270f, 190f),
    neonPalette("EMERALD GLASS", 155f, 200f),
    neonPalette("ICE GLASS", 205f, 30f),
    neonPalette("GOLD GLASS", 42f, 200f),
    // Extra neon presets
    neonPalette("BLUE PLASMA", 220f, 310f),
    neonPalette("MAGENTA FIRE", 320f, 40f),
    neonPalette("LIME PULSE", 95f, 200f),
    neonPalette("ORANGE FLARE", 28f, 200f),
    neonPalette("PINK SUGAR", 330f, 190f),
    neonPalette("TEAL WAVE", 175f, 300f),
    neonPalette("VIOLET STORM", 260f, 40f),
    neonPalette("WHITE NEON", 210f, 40f),
    // ---- extra presets (v24.8) ----
    neonPalette("ELECTRIC INDIGO", 245f, 60f),
    neonPalette("NEON TANGERINE", 20f, 195f),
    neonPalette("MINT GLOW", 165f, 285f),
    neonPalette("ROSE QUARTZ", 342f, 205f),
    neonPalette("SKY AURORA", 198f, 120f),
    neonPalette("GOLDEN HOUR", 38f, 250f),
    neonPalette("CRIMSON STEEL", 356f, 205f),
    neonPalette("AQUA NEON", 178f, 305f),
    neonPalette("LAVENDER DREAM", 268f, 320f),
    neonPalette("SUNSET GRADIENT", 14f, 300f),
    neonPalette("COBALT DEEP", 225f, 70f),
    neonPalette("ULTRAVIOLET", 288f, 145f),
    neonPalette("LASER BLUE", 205f, 330f),
    neonPalette("FIRE OPAL", 8f, 52f),
    neonPalette("CYBER LIME", 108f, 285f),
    neonPalette("DEEP OCEAN", 190f, 250f),
    neonPalette("PLASMA ROSE", 310f, 175f),
    neonPalette("ROYAL AMBER", 48f, 270f),
    neonPalette("MATRIX GREEN", 125f, 205f),
    neonPalette("COSMIC VIOLET", 275f, 35f),
    neonPalette("TROPICAL AQUA", 165f, 35f),
    neonPalette("GALAXY BLUE", 235f, 315f),
    neonPalette("HOT CORAL", 12f, 345f),
    neonPalette("ARCTIC MINT", 175f, 220f),
)

private var activePalette: PlayerPalette by mutableStateOf(
    palettes.firstOrNull { it.name.equals("GOLDEN HOUR", true) } ?: palettes.first()
)
var ActiveThemeName: String by mutableStateOf(
    palettes.firstOrNull { it.name.equals("GOLDEN HOUR", true) }?.name ?: palettes.first().name
)
    private set

/** Bumps on every palette change so Compose always recomposes consumers. */
var PaletteGeneration: Int by mutableStateOf(0)
    private set

/** Global visual prefs driven from Colors sheet */
var GlobalIconStyle: String by mutableStateOf("Bold")
var GlobalFontScale: Float by mutableStateOf(1f)
var GlobalGlowIntensity: Float by mutableStateOf(0.7f)

fun applyPlayerTheme(name: String) {
    val key = name.trim().uppercase()
    if (key == "CUSTOM") return
    val found = palettes.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: palettes.firstOrNull { it.name.replace(" ", "_").equals(name.replace(" ", "_"), ignoreCase = true) }
    if (found != null) {
        activePalette = found
        ActiveThemeName = found.name
        PaletteGeneration += 1
    }
}

/**
 * Build a FULL palette from any ARGB color so the entire UI (background, cards,
 * borders, text, nav, glass) actually follows the chosen color — not just the accent.
 */
fun applyCustomAccent(argb: Long, name: String = "CUSTOM") {
    val c = Color((argb and 0xFFFFFFFF).toInt())
    val r = c.red
    val g = c.green
    val b = c.blue
    val maxc = max(r, max(g, b))
    val minc = min(r, min(g, b))
    val delta = maxc - minc
    var h = 0f
    if (delta > 0.0001f) {
        h = when (maxc) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        if (h < 0f) h += 360f
    }
    val exactAccent = c.copy(alpha = 1f)
    // Secondary complementary-ish hue for cyan/teal highlights
    val h2 = (h + 160f) % 360f

    // Derive readable supporting tones from the exact color
    fun tone(sat: Float, value: Float, alpha: Float = 1f) = hsvColor(h, sat, value, alpha)
    fun tone2(sat: Float, value: Float, alpha: Float = 1f) = hsvColor(h2, sat, value, alpha)

    val glow = if (maxc < 0.92f) {
        Color(
            red = ((r + 1f) * 0.5f).coerceIn(0f, 1f),
            green = ((g + 1f) * 0.5f).coerceIn(0f, 1f),
            blue = ((b + 1f) * 0.5f).coerceIn(0f, 1f),
            alpha = 1f
        )
    } else exactAccent

    activePalette = PlayerPalette(
        name = name,
        background = tone(0.40f, 0.035f),
        backgroundGlowTop = Color(red = r * 0.22f, green = g * 0.22f, blue = b * 0.22f, alpha = 1f),
        backgroundGlowBottom = tone(0.45f, 0.09f),
        surface = tone(0.32f, 0.07f),
        surfaceVariant = tone(0.34f, 0.11f),
        card = tone(0.30f, 0.10f),
        cardDeep = tone(0.38f, 0.055f),
        border = Color(red = (r * 0.55f).coerceIn(0f, 1f), green = (g * 0.55f).coerceIn(0f, 1f), blue = (b * 0.55f).coerceIn(0f, 1f), alpha = 1f),
        borderStrong = Color(red = (r * 0.75f).coerceIn(0f, 1f), green = (g * 0.75f).coerceIn(0f, 1f), blue = (b * 0.75f).coerceIn(0f, 1f), alpha = 1f),
        accent = exactAccent,
        accentGlow = glow,
        accentDark = Color(red = r * 0.45f, green = g * 0.45f, blue = b * 0.45f, alpha = 1f),
        accentContainer = Color(red = r * 0.22f, green = g * 0.22f, blue = b * 0.22f, alpha = 1f),
        secondary = tone2(0.72f, 0.88f),
        secondaryGlow = tone2(0.55f, 0.95f),
        text = Color.White,
        secondaryText = tone(0.06f, 0.88f),
        muted = tone(0.10f, 0.55f),
        navSelectedFill = Color(red = r * 0.18f, green = g * 0.18f, blue = b * 0.18f, alpha = 1f),
        navSelectedBorder = glow,
        glassTop = tone(0.30f, 0.24f),
        glassMid = tone(0.34f, 0.11f),
        glassBottom = tone(0.40f, 0.06f),
    )
    ActiveThemeName = name
    PaletteGeneration += 1
}

/**
 * 3D / full-range custom color: build the complete palette from an exact
 * Hue + Saturation + Brightness triple (live color studio picker).
 * Any color in the whole HSV space is supported, not only presets.
 */
fun applyCustomColor3D(hue: Float, saturation: Float, brightness: Float, name: String = "CUSTOM 3D") {
    val exact = hsvColor(hue, saturation.coerceIn(0f, 1f), brightness.coerceIn(0f, 1f))
    val argb = (0xFF000000L or (exact.value.toLong() and 0xFFFFFFL))
    applyCustomAccent(argb, name)
}

fun availablePlayerThemes(): List<PlayerPalette> = palettes

fun activePlayerPalette(): PlayerPalette = activePalette

val DarkCardBorder get() = activePalette.borderStrong
val NeonRedDark get() = activePalette.accentDark
val NavSelectedBorder get() = activePalette.navSelectedBorder

fun palette(): PlayerPalette = activePalette

private var glassCacheKey: Triple<String, Boolean, Int>? = null
private var glassCache: Brush? = null

fun glassGradient(forDark: Boolean = true): Brush {
    val key = Triple(activePalette.name, forDark, (GlobalGlowIntensity * 100f).toInt())
    glassCache?.let { if (glassCacheKey == key) return it }
    val b = Brush.linearGradient(
        listOf(
            activePalette.glassTop.copy(alpha = if (forDark) 0.40f * GlobalGlowIntensity else 0.28f),
            activePalette.glassMid.copy(alpha = if (forDark) 0.92f else 0.78f),
            activePalette.glassBottom.copy(alpha = 0.38f),
        )
    )
    glassCacheKey = key
    glassCache = b
    return b
}

private var ambientCacheKey: Pair<String, Int>? = null
private var ambientCache: Brush? = null

fun ambientBackgroundBrush(): Brush {
    val key = activePalette.name to (GlobalGlowIntensity * 100f).toInt()
    ambientCache?.let { if (ambientCacheKey == key) return it }
    val b = Brush.verticalGradient(
        listOf(
            activePalette.background,
            activePalette.backgroundGlowTop.copy(alpha = 0.55f + 0.35f * GlobalGlowIntensity),
            activePalette.backgroundGlowBottom.copy(alpha = 0.70f + 0.25f * GlobalGlowIntensity),
            activePalette.background,
        )
    )
    ambientCacheKey = key
    ambientCache = b
    return b
}
