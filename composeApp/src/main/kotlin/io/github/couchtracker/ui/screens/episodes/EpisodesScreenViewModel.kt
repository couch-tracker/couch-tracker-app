package io.github.couchtracker.ui.screens.episodes

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.ui.screens.episodes.EpisodesScreenViewModelHelper.EpisodeViewModelHelper
import io.github.couchtracker.utils.api.ApiLoadable

class EpisodesScreenViewModel(
    application: Application,
    val seasonId: TmdbSeasonId,
) : AndroidViewModel(
    application = application,
) {
    private val baseViewModel = EpisodesScreenViewModelHelper(
        application = application,
        scope = viewModelScope,
        seasonId = seasonId,
    )

    val seasonDetails by baseViewModel.seasonDetails()
    val showBaseDetails by baseViewModel.showViewModel.baseDetails()
    val colorScheme by baseViewModel.showViewModel.colorScheme()
    val seasonSubtitle get() = baseViewModel.subtitle(seasonDetails, showBaseDetails)

    val allLoadables: List<ApiLoadable<*>> get() = baseViewModel.flowCollector.currentValues

    class EpisodeViewModel(
        helper: EpisodeViewModelHelper,
    ) {
        val details by helper.details()
        val images by helper.images()
    }

    @Composable
    fun viewModelForEpisode(episode: TmdbEpisodeId): EpisodeViewModel {
        val helper = baseViewModel.viewModelForEpisode(episode)
        return EpisodeViewModel(helper)
    }

    fun retryAll() {
        baseViewModel.retryAll()
    }
}
