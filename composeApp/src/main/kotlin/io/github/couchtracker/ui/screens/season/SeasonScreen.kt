@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.season

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.plus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.season.ExternalSeasonId
import io.github.couchtracker.db.profile.season.TmdbExternalSeasonId
import io.github.couchtracker.db.profile.season.UnknownExternalSeasonId
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.EpisodeListItem
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.ResultScreen
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import kotlinx.serialization.Serializable

private const val LOG_TAG = "SeasonScreen"

@Serializable
data class SeasonScreen(val seasonId: String) : Screen() {
    @Composable
    override fun content() {
        val seasonId = when (val externalSeasonId = ExternalSeasonId.parse(this@SeasonScreen.seasonId)) {
            is TmdbExternalSeasonId -> {
                externalSeasonId.id
            }
            is UnknownExternalSeasonId -> TODO()
        }

        Content(
            viewModel {
                SeasonScreenViewModel(
                    application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                    seasonId = seasonId,
                )
            },
        )
    }
}

fun NavController.navigateToSeason(id: ExternalSeasonId) {
    navigate(SeasonScreen(id.serialize()))
}

@Composable
private fun Content(viewModel: SeasonScreenViewModel) {
    ResultScreen(
        error = viewModel.details.resultErrorOrNull(),
        onError = { exception ->
            Surface {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = { viewModel.retryAll() },
                )
            }
        },
    ) {
        SeasonScreenContent(
            viewModel = viewModel,
            reloadSeason = { viewModel.retryAll() },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SeasonScreenContent(
    viewModel: SeasonScreenViewModel,
    reloadSeason: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        loadable = { viewModel.allLoadables },
        onRetry = reloadSeason,
    )
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    logCompositions(LOG_TAG, "Recomposing SeasonScreenContent")
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = backgroundColor,
            topBar = {
                OverviewScreenComponents.Header(
                    title = viewModel.details.resultValueOrNull()?.name.orEmpty(),
                    subtitle = viewModel.subtitle.resultValueOrNull(),
                    backdrop = viewModel.showBaseDetails.resultValueOrNull()?.backdrop,
                    scrollBehavior = scrollBehavior,
                    backgroundColor = { backgroundColor },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { innerPadding ->
                OverviewScreenComponents.SeasonDetailsContent(
                    innerPadding = innerPadding,
                    viewModel = viewModel,
                )
            },
        )
    }
}

@Composable
private fun OverviewScreenComponents.SeasonDetailsContent(
    innerPadding: PaddingValues,
    viewModel: SeasonScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val episodes = viewModel.details.mapResult { it.episodes }
    LoadableScreen(
        episodes,
        onError = { exception ->
            DefaultErrorScreen(
                errorMessage = exception.title.string(),
                errorDetails = exception.details?.string(),
                retry = { viewModel.retryAll() },
            )
        },
    ) { episodes ->
        ContentList(
            innerPadding.plus(PaddingValues(vertical = 16.dp, horizontal = 8.dp)),
            modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(episodes.size) { index ->
                val (episodeId, episode) = episodes[index]
                EpisodeListItem(
                    episode,
                    onClick = { navController.navigateToEpisode(episodeId) },
                    isFirstInList = index == 0,
                    isLastInList = index == episodes.size - 1,
                )
            }
        }
    }
}
