package io.github.couchtracker.ui.screens.seasons

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.ComposableCache
import io.github.couchtracker.utils.allErrors
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectAsLoadableInScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

class SeasonsScreenViewModel(
    application: Application,
    showId: TmdbShowId,
    var pendingPulseAnimation: ExternalEpisodeId?,
) : AndroidViewModel(
    application = application,
) {
    private val retryContext = tmdbFlowRetryContext()
    private val baseViewModel = SeasonsScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        showId = showId,
        retryContext = retryContext,
    )
    private val childModels = ComposableCache<SeasonViewModel>()

    val showDetails by baseViewModel.showDetails.collectAsLoadable("showDetails")
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable("colorScheme")

    val allErrors by derivedStateOf {
        listOf(showDetails, colorScheme).allErrors() +
            childModels.elements.flatMap { it.allErrors }
    }

    class SeasonViewModel(
        application: Application,
        val scope: CoroutineScope,
        seasonId: TmdbSeasonId,
        retryContext: TmdbFlowRetryContext,
        showOriginalLanguage: () -> Bcp47Language?,
    ) : AndroidViewModel(application) {

        private val baseViewModel = SeasonsScreenViewModelHelper.SeasonViewModelHelper(
            application = application,
            seasonId = seasonId,
            retryContext = retryContext,
            showOriginalLanguage = showOriginalLanguage,
        )

        val details by baseViewModel.details.collectAsLoadableInScope(scope, "details")
        val allErrors by derivedStateOf {
            listOf(details).allErrors()
        }
    }

    @Composable
    fun viewModelForSeason(season: TmdbSeasonId, showOriginalLanguage: () -> Bcp47Language?): SeasonViewModel {
        val model = remember(season) {
            SeasonViewModel(
                application = application,
                scope = CoroutineScope(EmptyCoroutineContext),
                seasonId = season,
                retryContext = retryContext,
                showOriginalLanguage = showOriginalLanguage,
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
