package io.github.couchtracker.ui.screens.seasons

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.utils.ComposableCache
import io.github.couchtracker.utils.allErrors
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectAsLoadableInScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SeasonsScreenViewModel(
    application: Application,
    showId: TmdbShowId,
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

    val showDetails by baseViewModel.showDetails.collectAsLoadable()
    val colorScheme by baseViewModel.colorScheme.collectAsLoadable()

    val allErrors by derivedStateOf {
        listOf(showDetails, colorScheme).allErrors() +
            childModels.elements.flatMap { it.allErrors }
    }

    class SeasonViewModel(
        application: Application,
        scope: CoroutineScope,
        seasonId: TmdbSeasonId,
        retryContext: TmdbFlowRetryContext,
    ) {
        private val baseViewModel = SeasonsScreenViewModelHelper.SeasonViewModelHelper(
            application = application,
            seasonId = seasonId,
            retryContext = retryContext,
        )
        val details by baseViewModel.details.collectAsLoadableInScope(scope)
        val allErrors by derivedStateOf {
            listOf(details).allErrors()
        }
    }

    @Composable
    fun viewModelForSeason(season: TmdbSeasonId): SeasonViewModel {
        return SeasonViewModel(
            application = application,
            scope = rememberCoroutineScope(),
            seasonId = season,
            retryContext = retryContext,
        ).also { childModels.put(it) }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
