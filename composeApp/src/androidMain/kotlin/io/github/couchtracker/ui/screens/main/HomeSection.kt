package io.github.couchtracker.ui.screens.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.asListState
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.user.db
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.components.MoviePortrait
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.UserPane
import io.github.couchtracker.ui.screens.navigateToMovie
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@Composable
fun HomeSection(innerPadding: PaddingValues) {
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
    ) { scaffoldInnerPadding ->
        Column(Modifier.fillMaxSize().padding(scaffoldInnerPadding)) {
            UserSection()
            HorizontalDivider()
            MoviesSection()
        }
    }
}

@Composable
private fun UserSection() {
    val appContext = LocalContext.current.applicationContext
    val appDb = koinInject<AppData>()

    val coroutineScope = rememberCoroutineScope()
    val selectAllUsers = remember { appDb.userQueries.selectAll() }
    val users by selectAllUsers.asListState()
    var currentUserId by remember { mutableStateOf<Long?>(null) }
    val openDbWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val document = uri?.let { DocumentFile.fromSingleUri(appContext, uri) }
        if (document != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            appDb.userQueries.insert(
                name = "Opened user (${document.name})",
                externalFileUri = document.uri,
            )
        }
    }

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
                name = "Created user (${Clock.System.now()}",
                externalFileUri = null,
            )
        },
    ) {
        Text(text = "Add user")
    }
    Button(onClick = { openDbWorkflow.launch(arrayOf("*/*")) }) {
        Text(text = "Open user")
    }
    Button(
        onClick = {
            for (user in users.orEmpty()) {
                coroutineScope.launch { user.db().unlink() }
                appDb.userQueries.delete(user.id)
            }
        },
    ) {
        Text(text = "Remove all")
    }
}

sealed interface MoviesSectionState {
    data object Loading : MoviesSectionState
    data class Error(val message: String) : MoviesSectionState
    data class Loaded(val movies: List<MoviePortraitModel>) : MoviesSectionState
}

private val exampleMovies = listOf(
    671,
    526_896,
    10_625,
    166_424,
    9607,
    438_631,
    693_134,
    940_721,
    929_590,
    577_922,
    346_698,
    786_892,
    150_540,
    748_783,
    297_762,
    284_053,
    508_883,
    787_699,
    579_974,
).map { TmdbMovie(TmdbMovieId(it), TmdbLanguage.ENGLISH) }

@Composable
private fun MoviesSection() {
    var state by remember { mutableStateOf<MoviesSectionState>(MoviesSectionState.Loading) }
    val tmdbCache = koinInject<TmdbCache>()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val movieMaxW = with(LocalDensity.current) {
        (MoviePortraitModel.SUGGESTED_WIDTH * 2).roundToPx()
    }
    LaunchedEffect(Unit) {
        state = try {
            coroutineScope {
                val details = exampleMovies.map { movie ->
                    async {
                        MoviePortraitModel.fromTmdbMovie(
                            context = context,
                            tmdbCache = tmdbCache,
                            movie = movie,
                            width = movieMaxW,
                        )
                    }
                }.awaitAll()
                MoviesSectionState.Loaded(details)
            }
        } catch (e: TmdbException) {
            MoviesSectionState.Error(e.message ?: "Error")
        }
    }
    when (val m = state) {
        is MoviesSectionState.Error -> Text("Error: ${m.message}")
        MoviesSectionState.Loading -> Text("Downloading movies...")
        is MoviesSectionState.Loaded -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = MoviePortraitModel.SUGGESTED_WIDTH),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(m.movies) { movie ->
                    MoviePortrait(
                        Modifier.fillMaxWidth(),
                        movie,
                    ) {
                        navController.navigateToMovie(movie.movie)
                    }
                }
            }
        }
    }
}
