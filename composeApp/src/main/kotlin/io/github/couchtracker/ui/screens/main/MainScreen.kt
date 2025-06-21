package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.utils.str
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen : Screen() {

    @Composable
    override fun content() = Content()
}

@Composable
private fun Content() {
    val insetTop = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
    val navController = rememberNavController()
    val currentSection = navController.currentBackStackEntryAsState().value?.destination?.route?.let { Section.fromId(it) } ?: Section.HOME

    // TODO: animate color changes
    MaterialTheme(colorScheme = currentSection.colorScheme) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (section in Section.entries) {
                        NavigationBarItem(
                            icon = { Icon(section.icon, contentDescription = section.displayName.str()) },
                            label = { Text(section.displayName.str()) },
                            selected = currentSection == section,
                            onClick = {
                                navController.navigate(section.id) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
            content = { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Section.HOME.id,
                ) {
                    composable(Section.HOME.id) {
                        MaterialTheme(colorScheme = Section.HOME.colorScheme) {
                            HomeSection(innerPadding)
                        }
                    }
                    composable(Section.SHOWS.id) {
                        MaterialTheme(colorScheme = Section.SHOWS.colorScheme) {
                            ShowSection(innerPadding)
                        }
                    }
                    composable(Section.MOVIES.id) {
                        MaterialTheme(colorScheme = Section.MOVIES.colorScheme) {
                            MoviesSection(innerPadding)
                        }
                    }
                }
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(insetTop),
        )
    }
}

private enum class Section(
    val id: String,
    @StringRes
    val displayName: Int,
    val icon: ImageVector,
    val colorScheme: ColorScheme,
) {
    HOME("home", R.string.main_section_home, Icons.Filled.Home, ColorSchemes.Common),
    SHOWS("shows", R.string.main_section_shows, Icons.Filled.Tv, ColorSchemes.Show),
    MOVIES("movies", R.string.main_section_movies, Icons.Filled.Movie, ColorSchemes.Movie),
    ;

    companion object {
        fun fromId(id: String) = entries.single { it.id == id }
    }
}
