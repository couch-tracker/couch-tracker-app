@file:Suppress("MagicNumber")

package io.github.couchtracker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import kotlin.math.pow
import android.graphics.Color as AndroidColor

private const val MAIN_COLOR_SATURATION = 0.1f
private const val MAIN_COLOR_VALUE = 1f
private const val OUTLINE_VALUE = 0.6f
private const val OUTLINE_VARIANT_MULTIPLIER = 0.8f
private const val OUTLINE_VARIANT_MAX = 0.8f
private const val CONTAINER_VALUE = 0.4f
private const val SURFACE_VALUE = 0.30f
private const val SURFACE_CONTAINER_VALUE_INCREMENT = 1.40f
private const val BACKGROUND_VALUE = 0.22f
private const val MAX_FOREGROUND_ELEMENT_SATURATION = 0.1f
private const val HUE_RANGE = 360f
private const val HUE_COMPLEMENTARY_DISTANCE = 20f

val DEFAULT_COLOR_SCHEME = darkColorScheme()
private const val DARK_VALUE_THRESHOLD = 0.5f

/**
 * Generates a [ColorScheme] based on the main color of this [Palette].
 */
fun Palette?.generateColorScheme(fallback: ColorScheme = DEFAULT_COLOR_SCHEME): ColorScheme {
    val mainSwatch = this?.vibrantSwatch ?: this?.dominantSwatch
    val rgb = mainSwatch?.rgb ?: return fallback
    return Color(rgb).generateColorScheme()
}

/**
 * Generates a [ColorScheme] based on this [Color].
 */
fun Color.generateColorScheme(): ColorScheme {
    val argb = this.toArgb()
    val hsv = floatArrayOf(0f, 0f, 0f)
    AndroidColor.colorToHSV(argb, hsv)

    val primaryHue = hsv[0]
    val primarySaturation = hsv[1]
    val secondaryHue = (primaryHue - HUE_COMPLEMENTARY_DISTANCE).mod(HUE_RANGE)
    val tertiaryHue = (primaryHue + HUE_COMPLEMENTARY_DISTANCE).mod(HUE_RANGE)

    val primaryBase = this
    val secondaryBase = Color.hsv(
        hue = secondaryHue,
        saturation = primarySaturation * 0.5f,
        value = 0.8f,
    )
    val tertiaryBase = Color.hsv(
        hue = tertiaryHue,
        saturation = primarySaturation * 0.5f,
        value = 0.8f,
    )

    val primary = mainColor(primaryHue)
    val secondary = mainColor(secondaryHue)
    val tertiary = mainColor(tertiaryHue)

    val primaryContainer = primaryBase.containerColor()
    val secondaryContainer = secondaryBase.containerColor()
    val tertiaryContainer = tertiaryBase.containerColor()

    val background = primaryBase.backgroundColor()
    val surface = primaryBase.surfaceColor()
    val surfaceVariant = secondaryBase.surfaceColor()

    return DEFAULT_COLOR_SCHEME.copy(
        primary = primary,
        onPrimary = primary.toForeground(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.toForeground(),
        inversePrimary = DEFAULT_COLOR_SCHEME.inversePrimary.withHue(primaryHue),
        secondary = secondary,
        onSecondary = secondary.toForeground(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.toForeground(),
        tertiary = tertiary,
        onTertiary = tertiary.toForeground(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.toForeground(),
        background = background,
        onBackground = background.toForeground(),
        surface = surface,
        onSurface = surface.toForeground(),
        surfaceContainer = primaryBase.surfaceColor(1),
        surfaceContainerHigh = primaryBase.surfaceColor(2),
        surfaceContainerHighest = primaryBase.surfaceColor(3),
        surfaceContainerLow = primaryBase.surfaceColor(-1),
        surfaceContainerLowest = primaryBase.surfaceColor(-2),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = surfaceVariant.toForeground(),
        outline = primaryBase.changeValue(OUTLINE_VALUE, OUTLINE_VALUE),
        outlineVariant = primaryBase.changeValue(OUTLINE_VARIANT_MULTIPLIER, 0f, OUTLINE_VARIANT_MAX),
    )
}

private fun isDark(value: Float): Boolean {
    return value < DARK_VALUE_THRESHOLD
}

private fun mainColor(hue: Float): Color {
    return Color.hsv(hue, MAIN_COLOR_SATURATION, MAIN_COLOR_VALUE)
}

private fun Color.containerColor(): Color {
    return changeValue(CONTAINER_VALUE, minValue = CONTAINER_VALUE / 2)
}

private fun Color.backgroundColor(): Color {
    return changeValue(BACKGROUND_VALUE, minValue = BACKGROUND_VALUE / 2)
}

private fun Color.surfaceColor(valueIncrement: Int = 0): Color {
    val value = (SURFACE_VALUE * SURFACE_CONTAINER_VALUE_INCREMENT.pow(valueIncrement)).coerceIn(0f, 1f)
    return changeValue(value, minValue = value / 2)
}

private fun Color.toForeground(): Color {
    return edit { h, s, v ->
        Color.hsv(
            hue = h,
            value = if (isDark(v)) 0.9f else 0.1f,
            saturation = s.coerceAtMost(MAX_FOREGROUND_ELEMENT_SATURATION),
        )
    }
}

private fun Color.changeValue(
    multiplier: Float,
    minValue: Float = 0.0f,
    maxValue: Float = 1.0f,
): Color {
    return edit { h, s, v ->
        Color.hsv(
            hue = h,
            saturation = s.coerceAtMost(1 - multiplier),
            value = (v * multiplier).coerceIn(minValue, maxValue),
        )
    }
}

private fun Color.withHue(hue: Float): Color {
    require(hue in 0f..HUE_RANGE)
    return edit { _, s, v ->
        Color.hsv(hue, s, v)
    }
}

private inline fun Color.edit(f: (h: Float, s: Float, v: Float) -> Color): Color {
    val hsv = floatArrayOf(0f, 0f, 0f)
    AndroidColor.colorToHSV(this.toArgb(), hsv)
    return f(hsv[0], hsv[1], hsv[2])
}
