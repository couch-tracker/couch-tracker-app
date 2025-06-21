package io.github.couchtracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.couchtracker.ProfileDataContext
import kotlinx.serialization.Serializable

@Serializable
abstract class Screen {

    open fun profileDataContext(): Boolean = true

    @Composable
    protected abstract fun content()

    @Composable
    fun ScreenContent() {
        if (profileDataContext()) {
            ProfileDataContext { content() }
        } else {
            content()
        }
    }
}

// This fails compilation due to a bug: https://youtrack.jetbrains.com/issue/KT-77127
inline fun <reified T : Screen> NavGraphBuilder.composable() {
    composable<T> { backStackEntry ->
        backStackEntry.toRoute<T>().ScreenContent()
    }
}
