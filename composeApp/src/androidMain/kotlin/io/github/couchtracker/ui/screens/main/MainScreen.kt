package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun MainScreen() {
    var currentSection by remember { mutableStateOf(Section.HOME) }
    val insetTop = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)

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

private enum class Section(
    val displayName: String, // TODO translate
    val icon: ImageVector,
) {
    HOME("Home", Icons.Filled.Home),
    SHOWS("Shows", Icons.Filled.Tv),
    MOVIES("Movies", Icons.Filled.Movie),
}
