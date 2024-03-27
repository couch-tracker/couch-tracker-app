package io.github.couchtracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.couchtracker.LocalTmdbCache
import io.github.couchtracker.db.user.movie.ExternalMovieId
import io.github.couchtracker.db.user.movie.TmdbExternalMovieId
import io.github.couchtracker.db.user.movie.UnknownExternalMovieId
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie

private const val ARGUMENTS_MOVIE_ID = "movieId"
private const val ARGUMENTS_LANGUAGE = "language"

fun NavController.navigateToMovie(movie: TmdbMovie) {
    navigate("movie/${movie.id.toExternalId().serialize()}?language=${movie.language}")
}

fun NavGraphBuilder.movieScreen() {
    composable(
        "movie/{$ARGUMENTS_MOVIE_ID}?language={$ARGUMENTS_LANGUAGE}",
        arguments = listOf(
            navArgument(ARGUMENTS_MOVIE_ID) {
                type = NavType.StringType
            },
            navArgument(ARGUMENTS_LANGUAGE) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        // TODO: handle parse errors
        val args = requireNotNull(backStackEntry.arguments)
        val movieIdStr = requireNotNull(args.getString(ARGUMENTS_MOVIE_ID))
        when (val movieId = ExternalMovieId.parse(movieIdStr)) {
            is TmdbExternalMovieId -> {
                val languageStr = requireNotNull(args.getString(ARGUMENTS_LANGUAGE))
                val language = TmdbLanguage.parse(languageStr)
                MovieScreen(TmdbMovie(movieId.id, language))
            }

            is UnknownExternalMovieId -> TODO()
        }
    }
}

sealed interface MoviesScreenState {
    data object Loading : MoviesScreenState
    data class Error(val message: String) : MoviesScreenState
    data class Loaded(val title: String, val overview: String) : MoviesScreenState
}

@Composable
fun MovieScreen(movie: TmdbMovie) {
    val tmdbCache = LocalTmdbCache.current
    var screenState by remember { mutableStateOf<MoviesScreenState>(MoviesScreenState.Loading) }

    LaunchedEffect(movie) {
        screenState = try {
            val details = movie.details(tmdbCache)
            MoviesScreenState.Loaded(details.title, details.overview)
        } catch (e: TmdbException) {
            MoviesScreenState.Error(e.message ?: "Error")
        }
    }

    when (val state = screenState) {
        is MoviesScreenState.Error -> Text("Error: ${state.message}")
        MoviesScreenState.Loading -> Text("Downloading movie...")
        is MoviesScreenState.Loaded -> {
            Column {
                Text(state.title, style = MaterialTheme.typography.displayLarge)
                Text(state.overview, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
