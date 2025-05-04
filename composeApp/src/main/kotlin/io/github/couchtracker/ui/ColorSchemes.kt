package io.github.couchtracker.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Contains the app's [ColorScheme]s
 */
object ColorSchemes {

    /**
     * [ColorScheme] for things int the app that are common between the media types (e.g. Home or People)
     */
    val Common = Color.hsv(240f, 1f, 0.5f).generateColorScheme()

    /**
     * [ColorScheme] for parts of the app that are related to TV shows
     */
    val Show = Color.hsv(0f, 1f, 0.5f).generateColorScheme()

    /**
     * [ColorScheme] for parts of the app that are related to Movies
     */
    val Movie = Color.hsv(180f, 1f, 0.5f).generateColorScheme()
}
