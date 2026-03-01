package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.AnimationDefaults
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.generateColorScheme
import io.github.couchtracker.ui.screens.main.search.SEARCH_SCREEN_EVENT_BUS
import io.github.couchtracker.ui.screens.main.search.SearchScreenEvent
import io.github.couchtracker.ui.screens.main.search.SearchSection
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentSection = currentRoute?.let { Section.fromId(it) } ?: DEFAULT_SECTION
    val animationSpec = AnimationDefaults.NAV_HOST_FADE_ANIMATION_SPEC

    val color by animateColorAsState(
        targetValue = currentSection.color,
        animationSpec = tween(AnimationDefaults.ANIMATION_DURATION_MS),
    )
    MaterialTheme(colorScheme = color.generateColorScheme()) {
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
                                    if (section == Section.SEARCH && currentRoute == section.id) {
                                        // SearchSection is already open, sending an event to it
                                        SEARCH_SCREEN_EVENT_BUS.tryEmit(SearchScreenEvent.FocusSearch)
                                    }
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
                    enterTransition = { fadeIn(animationSpec) },
                    exitTransition = { fadeOut(animationSpec) },
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
                            SearchSection(innerPadding)
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
    val color: Color,
) {
    SHOWS("shows", R.string.main_section_shows, Icons.Filled.Tv, ColorSchemes.ShowColor),
    MOVIES("movies", R.string.main_section_movies, Icons.Filled.Movie, ColorSchemes.MovieColor),
    PROFILE("profile", R.string.main_section_profile, Icons.Filled.Person, ColorSchemes.CommonColor),
    SEARCH("search", R.string.main_section_search, Icons.Filled.Search, ColorSchemes.CommonColor),
    ;

    val colorScheme = color.generateColorScheme()

    companion object {
        fun fromId(id: String) = entries.single { it.id == id }
    }
}
