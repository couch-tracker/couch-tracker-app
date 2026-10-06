package io.github.couchtracker.ui.screens.show

import android.app.Application
import androidx.compose.material3.ColorScheme
import app.moviebase.tmdb.model.TmdbGenre
import app.moviebase.tmdb.model.TmdbSeasonDetail
import app.moviebase.tmdb.model.TmdbShowCreatedBy
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.R
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbFlowRetryContext
import io.github.couchtracker.tmdb.TmdbRating
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.aggregateCredits
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.extractColorScheme
import io.github.couchtracker.tmdb.images
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.toImageModelWithPlaceholder
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.components.CastPortraitModel
import io.github.couchtracker.ui.components.CrewCompactListItemModel
import io.github.couchtracker.ui.components.SeasonListItemModel
import io.github.couchtracker.ui.components.WatchedItemListItemModel
import io.github.couchtracker.ui.components.toCastPortraitModel
import io.github.couchtracker.ui.components.toCrewCompactListItemModel
import io.github.couchtracker.ui.toImageModel
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.error.aggregateResults
import io.github.couchtracker.utils.flatMapResult
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.settings.getCurrent
import io.github.couchtracker.utils.transpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import org.koin.mp.KoinPlatform

/**
 * A utility class to create your own model for a show screen.
 */
class ShowScreenViewModelHelper(
    val application: Application,
    scope: CoroutineScope,
    val showId: TmdbShowId,
    val retryContext: TmdbFlowRetryContext,
) {

    data class BaseDetails(
        val name: String?,
        val overview: String?,
        val year: Int?,
        val backdrop: ImageModel?,
        val originalLanguage: Bcp47Language?,
    )

    data class FullDetails(
        val baseDetails: BaseDetails,
        val createdBy: List<TmdbShowCreatedBy>,
        val createdByString: String?,
        val genres: List<TmdbGenre>,
        val rating: TmdbRating?,
        val tagline: String?,
        val seasons: List<Pair<TmdbSeasonId, SeasonListItemModel>>,
    )

    data class SeasonsDetails(
        val seasons: Map<ExternalSeasonId, ApiLoadable<EpisodesDetails>>,
    ) {
        private val aggregatedEpisodeDetails: CouchTrackerLoadable<List<EpisodesDetails>> = seasons.values.toList().aggregateResults()

        fun getEpisode(externalEpisodeId: ExternalEpisodeId): ApiLoadable<EpisodeDetails?> {
            val episodeDetails = seasons.values.firstNotNullOfOrNull {
                it.resultValueOrNull()?.episodes.orEmpty()[externalEpisodeId]
            }
            return if (episodeDetails != null) {
                Loadable.value(episodeDetails)
            } else {
                aggregatedEpisodeDetails.mapResult { null }
            }
        }
    }

    data class EpisodesDetails(
        val episodes: Map<ExternalEpisodeId, EpisodeDetails>,
    ) {

        companion object {
            fun fromTmdbSeasonDetail(showId: TmdbShowId, season: TmdbSeasonDetail) = EpisodesDetails(
                episodes = season.episodes.orEmpty().associate { tmdbEpisode ->
                    val id =
                        TmdbEpisodeId(showId.season(tmdbEpisode.seasonNumber), tmdbEpisode.episodeNumber).toExternalId()

                    id to EpisodeDetails(
                        name = tmdbEpisode.name,
                        seasonNumber = tmdbEpisode.seasonNumber,
                        episodeNumber = tmdbEpisode.episodeNumber,
                        backdrop = tmdbEpisode.backdropImage?.toImageModel(),
                    )
                },
            )
        }
    }

    data class EpisodeDetails(
        val name: String?,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val backdrop: ImageModel?,
    )

    data class Credits(
        val cast: List<CastPortraitModel>,
        val crew: List<CrewCompactListItemModel>,
    )

    private val baseAndFullDetails: SharedFlow<Pair<ApiLoadable<BaseDetails>, ApiLoadable<FullDetails>>> =
        retryContext.withCache(
            cacheFn = { KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().getShow(showId, it.apiLanguage)?.toBaseDetails() },
        ) { languages ->
            showId.details(languages.apiLanguage).map {
                it.map { details ->
                    details.toDetails()
                }
            }.flowOn(Dispatchers.Default)
        }.shareIn(scope, SharingStarted.Lazily, 1)

    val baseDetails: Flow<ApiLoadable<BaseDetails>> = baseAndFullDetails.map { it.first }

    val fullDetails: Flow<ApiLoadable<FullDetails>> = baseAndFullDetails.map { it.second }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val episodesDetails: Flow<ApiLoadable<SeasonsDetails>> = fullDetails.flatMapLatest { fullDetails ->
        fullDetails.mapResult { fullDetails ->
            val seasonFlows = fullDetails.seasons.map { (seasonId) ->
                retryContext { languages ->
                    seasonId.details(language = languages.apiLanguage)
                }.map { loadableSeason ->
                    seasonId.toExternalId() to loadableSeason.mapResult { season ->
                        EpisodesDetails.fromTmdbSeasonDetail(showId, season)
                    }
                }
            }
            combine(seasonFlows) { seasonResults ->
                SeasonsDetails(seasons = seasonResults.toMap())
            }
        }.transpose()
    }.flowOn(Dispatchers.Default).shareIn(scope, SharingStarted.Lazily, 1)


    @OptIn(ExperimentalCoroutinesApi::class)
    val colorScheme: Flow<ApiLoadable<ColorScheme?>> = baseAndFullDetails
        .map { (base, _) -> base.mapResult { it.backdrop } }
        .distinctUntilChanged()
        .mapLatest { backdrop ->
            backdrop.mapResult { backdrop ->
                backdrop?.extractColorScheme(application)
            }
        }

    val images: Flow<ApiLoadable<List<ImageModel>>> = retryContext { languages ->
        showId.images(languages.toTmdbLanguagesFilter()).map {
            it.map { images ->
                images.toImageModel(includeLogos = false)
            }
        }
    }

    val credits: Flow<ApiLoadable<Credits>> = retryContext { languages ->
        showId.aggregateCredits(languages.apiLanguage).map {
            it.map { credits ->
                Credits(
                    cast = credits.cast.toCastPortraitModel(application),
                    crew = credits.crew.toCrewCompactListItemModel(application),
                )
            }
        }
    }

    val history: Flow<CouchTrackerLoadable<List<WatchedItemListItemModel.EpisodeInShow>>> =
        combine(
            episodesDetails,
            KoinPlatform.getKoin().get<Flow<ProfilesInfo>>(),
            AppSettings.getCurrent { StyleAndBehavior.EpisodeNumberFormatting },
        ) { episodeDetails, fullProfileData, episodeFormatting ->
            fullProfileData.currentFullData.mapResult { currentFullData ->
                currentFullData.watchedEpisodesForShow(showId.toExternalId()).map { watchedEpisode ->
                    WatchedItemListItemModel.EpisodeInShow.withShowData(
                        context = application,
                        watchedItem = watchedEpisode,
                        episodeFormatting = episodeFormatting,
                        episode = episodeDetails.flatMapResult { it.getEpisode(watchedEpisode.itemId) },
                    )
                }
            }
        }

    private suspend fun TmdbShowDetail.toDetails(): Pair<BaseDetails, FullDetails> {
        val base = BaseDetails(
            name = name,
            overview = overview,
            year = firstAirDate?.year,
            backdrop = backdropImage?.toImageModelWithPlaceholder(),
            originalLanguage = language(),
        )
        val createdBy = createdBy.orEmpty()
        val full = FullDetails(
            baseDetails = base,
            tagline = tagline,
            rating = rating(),
            genres = genres,
            createdBy = createdBy,
            createdByString = if (createdBy.isEmpty()) {
                null
            } else {
                application.getString(R.string.show_by_creator, formatAndList(createdBy.mapNotNull { it.name }))
            },
            seasons = seasons.map { season ->
                val id = TmdbSeasonId(showId, season.seasonNumber)
                id to SeasonListItemModel.fromTmdbSeason(application, season)
            },
        )
        return base to full
    }

    private fun BaseTmdbShow.toBaseDetails(): BaseDetails = BaseDetails(
        name = name,
        overview = overview,
        year = firstAirDate?.year,
        backdrop = backdrop?.toImageModelWithPlaceholder(),
        originalLanguage = originalLanguage,
    )
}
