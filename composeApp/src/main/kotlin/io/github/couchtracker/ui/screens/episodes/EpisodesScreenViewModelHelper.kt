package io.github.couchtracker.ui.screens.episodes

import android.app.Application
import android.content.Context
import dev.mmauro.datetimepolyglot.localizers.absolute.DateStyle
import dev.mmauro.datetimepolyglot.localizers.absolute.localize
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.intl.datetime.RUNTIME_LOCALIZER_OPTIONS
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.ui.seasonNumberToString
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

/**
 * A utility class to create your own model for an episodes screen.
 */
class EpisodesScreenViewModelHelper(
    val application: Application,
    scope: CoroutineScope,
    val seasonId: TmdbSeasonId,
    val retryContext: TmdbFlowRetryContext,
) {
    val showViewModel = ShowScreenViewModelHelper(
        application = application,
        scope = scope,
        showId = seasonId.showId,
        retryContext = retryContext,
    )

    data class SeasonDetails(
        val number: Int,
        val name: String?,
        val defaultName: String,
        val episodes: List<EpisodeBaseDetails>,
    ) {
        fun subtitle(context: Context, showName: String?): String? {
            return if (showName != null) {
                context.getString(R.string.show_dash_x, showName, defaultName)
            } else {
                defaultName
            }
        }
    }

    data class EpisodeBaseDetails(
        val externalId: ExternalEpisodeId,
        val tmdbEpisodeId: TmdbEpisodeId,
        val name: String?,
        val number: String,
        val firstAirDate: String?,
        val runtime: Duration?,
        val runtimeString: String?,
        val tmdbRating: TmdbRating?,
    )

    class EpisodeFullDetails(
        val overview: String?,
        val crew: List<CrewCompactListItemModel>,
        val guestStars: List<CastPortraitModel>,
    )

    val seasonDetails: Flow<ApiLoadable<SeasonDetails>> = retryContext { languages ->
        seasonId.details(languages.apiLanguage).map { result ->
            result.map { tmdbSeasonDetails ->
                SeasonDetails(
                    number = tmdbSeasonDetails.seasonNumber,
                    name = tmdbSeasonDetails.name,
                    defaultName = seasonNumberToString(application, tmdbSeasonDetails.seasonNumber),
                    episodes = tmdbSeasonDetails.episodes.orEmpty().map { episode ->
                        val id = TmdbEpisodeId(seasonId, episode.episodeNumber)
                        val runtime = episode.runtime()
                        EpisodeBaseDetails(
                            externalId = TmdbExternalEpisodeId(id),
                            tmdbEpisodeId = id,
                            name = episode.name,
                            number = application.getString(R.string.episode_x, episode.episodeNumber),
                            firstAirDate = episode.airDate?.localize(DateStyle.FULL),
                            runtime = runtime,
                            runtimeString = runtime?.localize(RUNTIME_LOCALIZER_OPTIONS),
                            tmdbRating = TmdbRating.ofOrNull(episode.voteAverage, episode.voteCount),
                        )
                    },
                )
            }
        }
    }

    fun subtitle(
        seasonDetails: ApiLoadable<SeasonDetails>,
        showBaseDetails: ApiLoadable<ShowScreenViewModelHelper.BaseDetails>,
    ): ApiLoadable<String?> {
        return seasonDetails.mapResult { season ->
            val show = showBaseDetails.resultValueOrNull()
            season.subtitle(application, show?.name)
        }
    }
}
