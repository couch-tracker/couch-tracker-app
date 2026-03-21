package io.github.couchtracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.couchtracker.ProfileDataContext
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffold
import kotlinx.serialization.Serializable

val LocalBackgroundColor = compositionLocalOf { ColorSchemes.Base.background }

@Serializable
abstract class Screen {

    open fun profileDataContext(): Boolean = true

    @Composable
    protected abstract fun Content()

    @Composable
    fun ScreenContent() {
        if (profileDataContext()) {
            ProfileDataContext { Content() }
        } else {
            Content()
        }
    }

    @Composable
    protected fun ScreenContainer(
        colorScheme: ColorScheme = ColorSchemes.Base,
        animateBgChange: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        MaterialTheme(colorScheme = colorScheme) {
            val background = if (animateBgChange) {
                animateColorAsState(MaterialTheme.colorScheme.background).value
            } else {
                MaterialTheme.colorScheme.background
            }
            CompositionLocalProvider(LocalBackgroundColor provides background) {
                Surface(color = LocalBackgroundColor.current) {
                    if (profileDataContext()) {
                        WatchedItemSheetScaffold(
                            containerColor = { Color.Transparent },
                            content = {
                                content()
                            },
                        )
                    } else {
                        content()
                    }
                }
            }
        }
    }
}

// This fails compilation due to a bug: https://youtrack.jetbrains.com/issue/KT-77127
inline fun <reified T : Screen> NavGraphBuilder.composable() {
    composable<T> { backStackEntry ->
        backStackEntry.toRoute<T>().ScreenContent()
    }
}
