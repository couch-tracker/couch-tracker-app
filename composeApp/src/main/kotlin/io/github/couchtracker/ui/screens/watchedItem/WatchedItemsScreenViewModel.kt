package io.github.couchtracker.ui.screens.watchedItem

import android.app.Application
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.settings.getAppSettings
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.ImageModel
import io.github.couchtracker.ui.actions.Action
import io.github.couchtracker.ui.actions.markEpisodeAsWatchedAction
import io.github.couchtracker.ui.actions.markMovieAsWatchedAction
import io.github.couchtracker.ui.screens.episodes.EpisodesScreenViewModelHelper
import io.github.couchtracker.ui.screens.movie.MovieScreenViewModelHelper
import io.github.couchtracker.ui.screens.show.ShowScreenViewModelHelper
import io.github.couchtracker.ui.showSeasonEpisodeNumberToString
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.time.Duration

sealed interface WatchedItemsScreenViewModel {

    data class Details(
        val subtitle: String,
        val runtime: Duration?,
        val originalLanguage: Bcp47Language?,
        val backdrop: ImageModel?,
    )

    val colorScheme: ColorScheme
    val externalId: ExternalId
    val details: Details
    val watchedItemType: WatchedItemType

    @Composable
    fun markAsWatchedAction(): Action?

    fun retryAll()

    sealed interface Movie : WatchedItemsScreenViewModel {

        override val watchedItemType get() = WatchedItemType.MOVIE

        class Tmdb(
            application: Application,
            movieId: TmdbMovieId,
        ) : AndroidViewModel(application = application), Movie {

            override val externalId = TmdbExternalMovieId(movieId)

            private val retryContext = tmdbFlowRetryContext()
            private val baseViewModel = MovieScreenViewModelHelper(
                application = application,
                scope = viewModelScope,
                movieId = movieId,
                retryContext = retryContext,
            )
            private val movieDetails by baseViewModel.fullDetails.collectAsLoadable("movieDetails")
            override val details by derivedStateOf {
                val movieDetails = movieDetails.resultValueOrNull()
                Details(
                    subtitle = movieDetails?.baseDetails?.title ?: externalId.serialize(),
                    runtime = movieDetails?.runtime,
                    originalLanguage = movieDetails?.originalLanguage,
                    backdrop = movieDetails?.baseDetails?.backdrop,
                )
            }
            val colorSchemeResult by baseViewModel.colorScheme.collectAsLoadable("colorScheme")
            override val colorScheme get() = colorSchemeResult.resultValueOrNull() ?: ColorSchemes.Movie

            @Composable
            override fun markAsWatchedAction(): Action {
                return markMovieAsWatchedAction(externalId) {
                    val movieDetails = movieDetails.resultValueOrNull()
                    WatchedItemSheetMode.New.Movie(
                        itemId = externalId,
                        mediaRuntime = movieDetails?.runtime,
                        mediaLanguages = listOfNotNull(movieDetails?.originalLanguage),
                    )
                }
            }

            override fun retryAll() {
                viewModelScope.launch { retryContext.retryAll() }
            }
        }

        class Unknown(
            application: Application,
            override val externalId: ExternalMovieId,
        ) : AndroidViewModel(application = application), Movie {

            override val colorScheme get() = ColorSchemes.Movie
            override val details = Details(
                subtitle = externalId.serialize(),
                runtime = null,
                originalLanguage = null,
                backdrop = null,
            )

            @Composable
            override fun markAsWatchedAction() = null

            override fun retryAll() = Unit
        }
    }

    sealed interface Episode : WatchedItemsScreenViewModel {

        override val watchedItemType get() = WatchedItemType.EPISODE

        class Tmdb(
            application: Application,
            override val externalId: TmdbExternalEpisodeId,
        ) : AndroidViewModel(application = application), Episode {

            private val retryContext = tmdbFlowRetryContext()
            private val showBaseModel = ShowScreenViewModelHelper(
                application = application,
                scope = viewModelScope,
                showId = externalId.id.showId,
                retryContext = retryContext,
            )

            private val seasonModel = EpisodesScreenViewModelHelper(
                application = application,
                scope = viewModelScope,
                seasonId = externalId.id.seasonId,
                retryContext = retryContext,
            )

            private val settings by KoinPlatform.getKoin()
                .getAppSettings()
                .map { Loadable.Loaded(it) }
                .collectAsLoadable("settings")

            private val showBaseDetails by showBaseModel.baseDetails.collectAsLoadable("showBaseDetails")
            private val seasonDetails by seasonModel.seasonDetails.collectAsLoadable("seasonDetails")

            val colorSchemeResult by showBaseModel.colorScheme.collectAsLoadable("colorScheme")
            override val colorScheme get() = colorSchemeResult.resultValueOrNull() ?: ColorSchemes.Show

            override val details by derivedStateOf {
                val showBaseDetails = showBaseDetails.resultValueOrNull()
                val seasonDetails = seasonDetails.resultValueOrNull()
                val episodeDetails = seasonDetails?.findEpisode()

                Details(
                    subtitle = settings.valueOrNull()?.let { settings ->
                        val formatting = settings.get { StyleAndBehavior.EpisodeNumberFormatting }.current
                        showSeasonEpisodeNumberToString(
                            context = application,
                            formatting = formatting,
                            showName = showBaseDetails?.name ?: externalId.id.showId.toExternalId().serialize(),
                            seasonNumber = externalId.id.seasonId.number,
                            episodeNumber = externalId.id.number,
                            episodeName = episodeDetails?.name,
                        )
                    }.orEmpty(),
                    runtime = episodeDetails?.runtime,
                    originalLanguage = showBaseDetails?.originalLanguage,
                    backdrop = showBaseDetails?.backdrop,
                )
            }

            @Composable
            override fun markAsWatchedAction(): Action {
                return markEpisodeAsWatchedAction(externalId.id.showId.toExternalId(), externalId) { watchedSession ->
                    val showDetails = showBaseDetails.resultValueOrNull()
                    val seasonDetails = seasonDetails.resultValueOrNull()
                    val episodeDetails = seasonDetails?.findEpisode()

                    WatchedItemSheetMode.New.Episode(
                        itemId = externalId,
                        watchedSession = watchedSession,
                        mediaRuntime = episodeDetails?.runtime,
                        mediaLanguages = listOfNotNull(showDetails?.originalLanguage),
                    )
                }
            }

            private fun EpisodesScreenViewModelHelper.SeasonDetails.findEpisode(): EpisodesScreenViewModelHelper.EpisodeBaseDetails? {
                return episodes.find { it.tmdbEpisodeId == externalId.id }
            }

            override fun retryAll() {
                viewModelScope.launch { retryContext.retryAll() }
            }
        }

        class Unknown(
            application: Application,
            override val externalId: ExternalEpisodeId,
        ) : AndroidViewModel(application = application), Episode {

            override val colorScheme get() = ColorSchemes.Show
            override val details = Details(
                subtitle = externalId.serialize(),
                runtime = null,
                originalLanguage = null,
                backdrop = null,
            )

            @Composable
            override fun markAsWatchedAction() = null

            override fun retryAll() = Unit
        }
    }
}
