package io.github.couchtracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.db.app.AppDb
import io.github.couchtracker.db.app.User
import io.github.couchtracker.db.tmdbCache.TmdbCacheDb
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@Composable
fun App() {
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            UserSection()
            Divider()
            MoviesSection()
        }
    }
}

@Composable
private fun UserSection() {
    val appDb = AppDb.get(LocalContext.current)
    val users by appDb.userQueries.selectAll().asListState()
    var currentUser by remember { mutableStateOf<User?>(null) }

    Text(text = "Current user", fontSize = 30.sp)
    when (val u = currentUser) {
        null -> Text(text = "No current user", fontStyle = FontStyle.Italic)
        else -> Column {
            Text("ID: ${u.id}")
            Text("Name: ${u.name}")
            Text("External file URI: ${u.externalFileUri}")
        }
    }

    Text(text = "User list", fontSize = 30.sp)
    when (val userList = users) {
        null -> Text("Loading...")
        else -> for (user in userList) {
            Button(onClick = { currentUser = user }) {
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
    data class Loaded(val movies: List<TmdbMovieDetail>) : MoviesSectionState
}

private val exampleMovies = listOf(
    TmdbMovie(526_896, TmdbLanguage.English),
    TmdbMovie(10_625, TmdbLanguage.English),
    TmdbMovie(166_424, TmdbLanguage.English),
    TmdbMovie(9607, TmdbLanguage.English),
)

@Composable
private fun MoviesSection() {
    val appContext = LocalContext.current.applicationContext
    val tmdbCache = remember(appContext) { TmdbCacheDb.get(appContext) }
    var state by remember { mutableStateOf<MoviesSectionState>(MoviesSectionState.Loading) }
    LaunchedEffect(Unit) {
        state = try {
            val details = exampleMovies.map { movie ->
                async { movie.details(tmdbCache) }
            }.awaitAll()
            MoviesSectionState.Loaded(details)
        } catch (e: TmdbException) {
            MoviesSectionState.Error(e.message ?: "Error")
        }
    }
    when (val m = state) {
        is MoviesSectionState.Error -> Text("Error: ${m.message}")
        MoviesSectionState.Loading -> Text("Downloading movies...")
        is MoviesSectionState.Loaded -> {
            Column {
                m.movies.forEach { movie ->
                    Text(movie.title)
                }
            }
        }
    }
}
