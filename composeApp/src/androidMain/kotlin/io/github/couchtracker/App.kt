package io.github.couchtracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.couchtracker.ui.screens.main.MainScreen
import io.github.couchtracker.ui.screens.movieScreen
import org.koin.compose.KoinContext

val LocalNavController = staticCompositionLocalOf<NavController> { error("no default nav controller") }

@Composable
fun App() {
    val navController = rememberNavController()
    KoinContext {
        MaterialTheme(colorScheme = darkColorScheme()) {
            CompositionLocalProvider(LocalNavController provides navController) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen()
                        }
                        movieScreen()
                    }
                }
            }
        }
    }
}
