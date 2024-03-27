package io.github.couchtracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.db.app.AppDb
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbCacheDb
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.components.UserPane
import io.github.couchtracker.ui.screens.movieScreen
import io.github.couchtracker.ui.screens.navigateToMovie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

val LocalTmdbCache = staticCompositionLocalOf<TmdbCache> {
    throw IllegalStateException("Not initialized yet")
}

@Composable
fun App() {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val tmdbCache = remember(appContext) { TmdbCacheDb.get(appContext) }

    CompositionLocalProvider(
        LocalTmdbCache provides tmdbCache,
    ) {
        MaterialTheme {
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    Main(openMovie = { movie -> navController.navigateToMovie(movie) })
                }
                movieScreen()
            }
        }
    }
}

@Composable
private fun Main(openMovie: (TmdbMovie) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        UserSection()
        Divider()
        MoviesSection(openMovie)
    }
}

@Composable
private fun UserSection() {
    val appContext = LocalContext.current.applicationContext
    val appDb = remember { AppDb.get(appContext) }

    val selectAllUsers = remember { appDb.userQueries.selectAll() }
    val users by selectAllUsers.asListState()
    var currentUserId by remember { mutableStateOf<Long?>(null) }

    Text(text = "Current user", fontSize = 30.sp)
    when (val user = users?.find { it.id == currentUserId }) {
        null -> Text(text = "No current user", fontStyle = FontStyle.Italic)
        else -> UserPane(user = user)
    }

    Text(text = "User list", fontSize = 30.sp)
    when (val userList = users) {
        null -> Text("Loading...")
        else -> for (user in userList) {
            Button(onClick = { currentUserId = user.id }) {
                Text(text = user.name)
            }
        }
    }
    Button(
        onClick = {
            appDb.userQueries.insert(
                name = "Create time: ${System.currentTimeMillis()}",
                externalFileUri = null,
            )
        },
    ) {
        Text(text = "Add user")
    }
    Button(
        onClick = {
            appDb.userQueries.deleteAll()
        },
    ) {
        Text(text = "Remove all")
    }
}

sealed interface MoviesSectionState {
    data object Loading : MoviesSectionState
    data class Error(val message: String) : MoviesSectionState
    data class Loaded(val movies: Map<TmdbMovie, TmdbMovieDetail>) : MoviesSectionState
}

private val exampleMovies = listOf(
    526_896,
    10_625,
    166_424,
    9607,
).map { TmdbMovie(TmdbMovieId(it), TmdbLanguage.ENGLISH) }

@Composable
private fun MoviesSection(openMovie: (TmdbMovie) -> Unit) {
    var state by remember { mutableStateOf<MoviesSectionState>(MoviesSectionState.Loading) }
    val tmdbCache = LocalTmdbCache.current
    LaunchedEffect(Unit) {
        state = try {
            coroutineScope {
                val details = exampleMovies.map { movie ->
                    async { movie to movie.details(tmdbCache) }
                }.awaitAll()
                MoviesSectionState.Loaded(details.toMap())
            }
        } catch (e: TmdbException) {
            MoviesSectionState.Error(e.message ?: "Error")
        }
    }
    when (val m = state) {
        is MoviesSectionState.Error -> Text("Error: ${m.message}")
        MoviesSectionState.Loading -> Text("Downloading movies...")
        is MoviesSectionState.Loaded -> {
            Column {
                m.movies.forEach { (movie, details) ->
                    Text(details.title, Modifier.clickable { openMovie(movie) })
                }
            }
        }
    }
}
