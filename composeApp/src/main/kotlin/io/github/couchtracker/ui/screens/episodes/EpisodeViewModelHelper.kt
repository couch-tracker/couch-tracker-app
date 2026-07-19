package io.github.couchtracker.ui.screens.episodes

import android.app.Application
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.images
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EpisodeViewModelHelper(
    val application: Application,
    val episodeId: TmdbEpisodeId,
    val retryContext: TmdbFlowRetryContext,
) {
    val details: Flow<ApiLoadable<EpisodesScreenViewModelHelper.EpisodeFullDetails>> = retryContext { languages ->
        episodeId.details(languages.apiLanguage).map { result ->
            result.map { tmdbEpisodeDetails ->
                EpisodesScreenViewModelHelper.EpisodeFullDetails(
                    overview = tmdbEpisodeDetails.overview,
                    crew = tmdbEpisodeDetails.crew.orEmpty().toCrewCompactListItemModel(application),
                    guestStars = tmdbEpisodeDetails.guestStars.orEmpty().toCastPortraitModel(),
                )
            }
        }
    }

    val images: Flow<ApiLoadable<List<ImageModel>>> = retryContext { languages ->
        episodeId.images(languages.toTmdbLanguagesFilter()).map { result ->
            result.map { images ->
                images.toImageModel(includeLogos = false)
            }
        }
    }
}
