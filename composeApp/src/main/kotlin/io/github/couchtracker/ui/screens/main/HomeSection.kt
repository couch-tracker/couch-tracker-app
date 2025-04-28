package io.github.couchtracker.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.TmdbException
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovie
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MovieGrid
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.ProfilePane
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.ui.screens.settings.Settings
import io.github.couchtracker.utils.Loadable
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun HomeSection(innerPadding: PaddingValues) {
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
    ) { scaffoldInnerPadding ->
        Column(Modifier.fillMaxSize().padding(scaffoldInnerPadding)) {
            ProfileSection()
            HorizontalDivider()
            MoviesSection()
        }
    }
}

@Composable
@Suppress("LongMethod") // TODO: remove this debug composable
private fun ProfileSection() {
    val profileManager = LocalProfileManagerContext.current
    val navController = LocalNavController.current

    Button(
        onClick = { navController.navigate(Settings) },
        content = { Text("Settings") },
    )
    Text(text = "Current profile: ${profileManager.current.profile.name}", fontSize = 30.sp)
    ProfilePane()
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
    val context = LocalPlatformContext.current
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
