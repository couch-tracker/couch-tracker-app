package io.github.couchtracker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Contains the app's [ColorScheme]s
 */
object ColorSchemes {

    val Base = darkColorScheme(background = Color(red = 10, green = 8, blue = 12))

    /**
     * [ColorScheme] for things int the app that are common between the media types (e.g. Home or People)
     */
    val CommonColor = Color.hsv(240f, 1f, 0.5f)
    val Common = CommonColor.generateColorScheme()

    /**
     * [ColorScheme] for parts of the app that are related to TV shows
     */
    val ShowColor = Color.hsv(0f, 1f, 0.5f)
    val Show = ShowColor.generateColorScheme()

    /**
     * [ColorScheme] for parts of the app that are related to Movies
     */
    val MovieColor = Color.hsv(180f, 1f, 0.5f)
    val Movie = MovieColor.generateColorScheme()
}
