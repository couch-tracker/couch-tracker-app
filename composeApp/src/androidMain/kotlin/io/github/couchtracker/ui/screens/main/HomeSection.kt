package io.github.couchtracker.ui.screens.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import io.github.couchtracker.LocalUserManagerContext
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MovieGrid
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.UserPane
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.toCouchTrackerUri
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
@Suppress("LongMethod") // TODO: remove this debug composable
private fun UserSection() {
    val appContext = LocalContext.current.applicationContext
    val userManager = LocalUserManagerContext.current
    val appDb = koinInject<AppData>()

    val cs = rememberCoroutineScope()

    val openDbWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val document = uri?.let { DocumentFile.fromSingleUri(appContext, uri) }
        if (document != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            appDb.userQueries.insert(
                name = "Opened user (${document.name})",
                externalFileUri = document.uri.toCouchTrackerUri(),
            )
        }
    }

    Text(text = "Current user: ${userManager.current.user.name}", fontSize = 30.sp)
    UserPane()

    Text(text = "User list", fontSize = 30.sp)
    for (userInfo in userManager.users) {
        Row {
            Button(onClick = { userManager.changeLoggedUser(userInfo.user) }) {
                Text(text = userInfo.user.name)
            }
            Button(onClick = { cs.launch { userInfo.delete(appDb) } }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "delete",
                )
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
            cs.launch {
                for (userInfo in userManager.users) {
                    userInfo.delete(appDb)
                }
            }
        },
    ) {
        Text(text = "Remove all")
    }
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
    val cs = rememberCoroutineScope()
    var state by remember { mutableStateOf<Loadable<List<MoviePortraitModel>>>(Loadable.Loading) }
    val tmdbCache = koinInject<TmdbCache>()
    val context = LocalContext.current
    val movieMaxW = with(LocalDensity.current) {
        (MoviePortraitModel.SUGGESTED_WIDTH * 2).roundToPx()
    }

    suspend fun download() {
        state = Loadable.Loading
        state = try {
            Loadable.Loaded(exampleMovies.toMoviePortraitModels(context, tmdbCache, movieMaxW))
        } catch (e: TmdbException) {
            // TODO translate
            Loadable.Error(e.message ?: "Error")
        }
    }

    LaunchedEffect(Unit) {
        download()
    }
    LoadableScreen(
        state,
        onError = { message ->
            DefaultErrorScreen(message) {
                cs.launch { download() }
            }
        },
    ) { movies ->
        MovieGrid(movies)
    }
}
