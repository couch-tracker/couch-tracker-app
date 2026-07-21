package io.github.couchtracker.ui.screens.episodes

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.ComposableCache
import io.github.couchtracker.utils.allErrors
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectAsLoadableInScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

class EpisodesScreenViewModel(
    application: Application,
    val seasonId: TmdbSeasonId,
) : AndroidViewModel(
    application = application,
) {
    private val retryContext = tmdbFlowRetryContext()
    private val baseViewModel = EpisodesScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        seasonId = seasonId,
        retryContext = retryContext,
    )
    private val childModels = ComposableCache<EpisodeViewModel>()

    val seasonDetails by baseViewModel.seasonDetails.collectAsLoadable("seasonDetails")
    val showBaseDetails by baseViewModel.showViewModel.baseDetails.collectAsLoadable("showBaseDetails", Dispatchers.Main)
    val colorScheme by baseViewModel.showViewModel.colorScheme.collectAsLoadable("colorScheme")
    val seasonSubtitle get() = baseViewModel.subtitle(seasonDetails, showBaseDetails)

    val allErrors by derivedStateOf {
        listOf(seasonDetails, showBaseDetails, colorScheme).allErrors() +
            childModels.elements.flatMap { it.allErrors }
    }

    class EpisodeViewModel(
        application: Application,
        val scope: CoroutineScope,
        episodeId: TmdbEpisodeId,
        retryContext: TmdbFlowRetryContext,
    ) {
        private val baseViewModel = EpisodeViewModelHelper(
            application = application,
            episodeId = episodeId,
            retryContext = retryContext,
        )
        val details by baseViewModel.details.collectAsLoadableInScope(scope, "details")
        val images by baseViewModel.images.collectAsLoadableInScope(scope, "images")
        val allErrors by derivedStateOf {
            listOf(details, images).allErrors()
        }
    }

    @Composable
    fun viewModelForEpisode(episode: TmdbEpisodeId): EpisodeViewModel {
        val model = remember(episode) {
            EpisodeViewModel(
                getApplication(),
                CoroutineScope(EmptyCoroutineContext),
                episode,
                retryContext,
            )
        }
        DisposableEffect(model.scope) {
            onDispose { model.scope.cancel() }
        }
        childModels.put(model)
        return model
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
