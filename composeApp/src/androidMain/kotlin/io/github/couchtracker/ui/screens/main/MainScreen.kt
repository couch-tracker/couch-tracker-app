package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.couchtracker.ui.generateColorScheme

private val HOME_COLOR_SCHEME = Color.hsv(240f, 1f, 0.5f).generateColorScheme()
private val SHOW_COLOR_SCHEME = Color.hsv(0f, 1f, 0.5f).generateColorScheme()
private val MOVIE_COLOR_SCHEME = Color.hsv(180f, 1f, 0.5f).generateColorScheme()

@Composable
fun MainScreen() {
    var currentSection by remember { mutableStateOf(Section.HOME) }
    val insetTop = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
    MaterialTheme(colorScheme = currentSection.mainColor) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (section in Section.entries) {
                        NavigationBarItem(
                            icon = { Icon(section.icon, contentDescription = section.displayName) },
                            label = { Text(section.displayName) },
                            selected = currentSection == section,
                            onClick = { currentSection = section },
                        )
                    }
                }
            },
            content = { innerPadding ->
                when (currentSection) {
                    Section.HOME -> HomeSection(innerPadding)
                    Section.SHOWS -> ShowSection(innerPadding)
                    Section.MOVIES -> MoviesSection(innerPadding)
                }
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(insetTop),
        )
    }
}

private enum class Section(
    val displayName: String, // TODO translate
    val icon: ImageVector,
    val mainColor: ColorScheme,
) {
    HOME("Home", Icons.Filled.Home, HOME_COLOR_SCHEME),
    SHOWS("Shows", Icons.Filled.Tv, SHOW_COLOR_SCHEME),
    MOVIES("Movies", Icons.Filled.Movie, MOVIE_COLOR_SCHEME),
}
