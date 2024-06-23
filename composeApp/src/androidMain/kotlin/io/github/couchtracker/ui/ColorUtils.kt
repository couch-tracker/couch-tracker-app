package io.github.couchtracker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import android.graphics.Color as AndroidColor

private const val MAX_BACKGROUND_SATURATION = 0.8f
private const val MAX_BACKGROUND_VALUE = 0.2f

fun Palette?.backgroundColor(cs: ColorScheme): Color {
    val rgb = this?.darkVibrantSwatch?.rgb ?: return cs.background
    return backgroundColor(rgb)
}

fun Color.backgroundColor(): Color {
    return backgroundColor(this.toArgb())
}

private fun backgroundColor(rgb: Int): Color {
    val hsv = floatArrayOf(0f, 0f, 0f)
    AndroidColor.colorToHSV(rgb, hsv)
    return Color.hsv(
        hue = hsv[0],
        saturation = minOf(hsv[1], MAX_BACKGROUND_SATURATION),
        value = minOf(hsv[2], MAX_BACKGROUND_VALUE),
    )
}
