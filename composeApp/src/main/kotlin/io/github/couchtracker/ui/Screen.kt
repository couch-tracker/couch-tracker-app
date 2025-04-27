package io.github.couchtracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute

interface Screen {

    @Composable
    fun content()
}

// This fails compilation due to a bug: https://youtrack.jetbrains.com/issue/KT-77127
// inline fun <reified T : Screen> NavGraphBuilder.composable2() {
//     composable<T> { backStackEntry ->
//         backStackEntry.toRoute<T>().content()
//     }
// }

// Next best thing:
@Composable
inline fun <reified T : Screen> NavBackStackEntry.screenContent() {
    toRoute<T>().content()
}
