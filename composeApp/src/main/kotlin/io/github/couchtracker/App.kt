package io.github.couchtracker

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.couchtracker.settings.AppSettingsContext
import io.github.couchtracker.ui.AnimationDefaults
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.composable
import io.github.couchtracker.ui.screens.main.MainScreen
import io.github.couchtracker.ui.screens.main.SearchScreen
import io.github.couchtracker.ui.screens.movie.MovieScreen
import io.github.couchtracker.ui.screens.settings.settings
import io.github.couchtracker.ui.screens.show.ShowScreen
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemsScreen
import io.github.couchtracker.utils.loadAll
import io.github.couchtracker.utils.rememberComputationResult
import org.koin.compose.getKoin
import kotlin.math.roundToInt

val LocalNavController = staticCompositionLocalOf<NavController> { error("no default nav controller") }

private const val ANIMATION_SLIDE = 0.25f
private val FADE_ANIMATION_SPEC = tween<Float>(AnimationDefaults.ANIMATION_DURATION_MS)

@Composable
fun App() {
    val navController = rememberNavController()
    val koin = getKoin()
    val koinLoadState = rememberComputationResult { koin.loadAll() }
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalNavController provides navController) {
            Surface(color = MaterialTheme.colorScheme.background) {
                LoadableScreen(koinLoadState) {
                    AppSettingsContext {
                        ProfilesContext {
                            NavHost(
                                navController = navController,
                                startDestination = MainScreen,
                                enterTransition = {
                                    val slideSpec = tween<IntOffset>(
                                        durationMillis = AnimationDefaults.ANIMATION_DURATION_MS,
                                        easing = LinearOutSlowInEasing,
                                    )
                                    slideInVertically(slideSpec) { (it * ANIMATION_SLIDE).roundToInt() } +
                                        fadeIn(FADE_ANIMATION_SPEC)
                                },
                                exitTransition = { fadeOut(FADE_ANIMATION_SPEC) },
                                popEnterTransition = { fadeIn(FADE_ANIMATION_SPEC) },
                                popExitTransition = {
                                    val slideSpec = tween<IntOffset>(
                                        durationMillis = AnimationDefaults.ANIMATION_DURATION_MS,
                                        easing = FastOutLinearInEasing,
                                    )
                                    slideOutVertically(slideSpec) { (it * ANIMATION_SLIDE).roundToInt() } +
                                        fadeOut(FADE_ANIMATION_SPEC)
                                },
                            ) {
                                composable<MainScreen>()
                                composable<SearchScreen>()
                                composable<MovieScreen>()
                                composable<ShowScreen>()
                                composable<WatchedItemsScreen>()
                                settings()
                            }
                        }
                    }
                }
            }
        }
    }
}
