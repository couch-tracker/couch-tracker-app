package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.utils.str
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen : Screen() {

    @Composable
    override fun content() = Content()
}

private val DEFAULT_SECTION = Section.SHOWS

@Composable
private fun Content() {
    val insetTop = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
    val navController = rememberNavController()
    val currentSection =
        navController.currentBackStackEntryAsState().value?.destination?.route?.let { Section.fromId(it) } ?: DEFAULT_SECTION

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
                    startDestination = DEFAULT_SECTION.id,
                ) {
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
                    composable(Section.PROFILE.id) {
                        MaterialTheme(colorScheme = Section.PROFILE.colorScheme) {
                            ProfileSection(innerPadding)
                        }
                    }
                    composable(Section.SEARCH.id) {
                        MaterialTheme(colorScheme = Section.SEARCH.colorScheme) {
                            WipMessageComposable(
                                gitHubIssueId = 181,
                            )
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
    SHOWS("shows", R.string.main_section_shows, Icons.Filled.Tv, ColorSchemes.Show),
    MOVIES("movies", R.string.main_section_movies, Icons.Filled.Movie, ColorSchemes.Movie),
    PROFILE("profile", R.string.main_section_profile, Icons.Filled.Person, ColorSchemes.Common),
    SEARCH("search", R.string.main_section_search, Icons.Filled.Search, ColorSchemes.Common),
    ;

    companion object {
        fun fromId(id: String) = entries.single { it.id == id }
    }
}
