package io.github.couchtracker.ui.screens.main

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.tmdb.TmdbLanguages
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.resultValueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class MovieSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val retryContext = tmdbFlowRetryContext()

    val exploreState = MovieExploreTabState(application, viewModelScope)

    val watchlist: Loadable<List<Pair<ExternalMovieId, CouchTrackerResult<MoviePortraitModel>>>> by flowDetailForMovies(
        movies = KoinPlatform.getKoin().get<Flow<ProfilesInfo>>()
            .mapNotNull { profilesInfo ->
                val fullData = profilesInfo.currentFullData.resultValueOrNull()
                if (fullData != null) {
                    fullData.bookmarkedMovies.keys.injectBrokenItems().toSet()
                } else {
                    null
                }
            }
            .distinctUntilChanged(),
    ).collectAsLoadable("movies-watchlist")

    val allErrors: List<CouchTrackerError> by derivedStateOf {
        watchlist.allErrors()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun flowDetailForMovies(
        movies: Flow<Collection<ExternalMovieId>>,
    ): Flow<Loadable<List<Pair<ExternalMovieId, CouchTrackerResult<MoviePortraitModel>>>>> {
        return retryContext { languages ->
            movies.flatMapLatest { movies ->
                if (movies.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        flows = movies.map { movieId ->
                            flowDetailForMovie(movieId, languages)
                        },
                    ) { it.toList() }
                }
            }
        }
    }

    private fun flowDetailForMovie(
        movieId: ExternalMovieId,
        languages: TmdbLanguages,
    ): Flow<Pair<ExternalMovieId, CouchTrackerResult<MoviePortraitModel>>> {
        val tmdbMovieId: TmdbMovieId = when (movieId) {
            is TmdbExternalMovieId -> movieId.id
            is UnknownExternalMovieId -> return flowOf(
                movieId to Result.Error(UnsupportedItemError(movieId)),
            )
        }
        return tmdbMovieId.details(languages.apiLanguage).map { result ->
            movieId to result.map {
                it.toMoviePortraitModels(application, languages.apiLanguage)
            }
        }
    }

    private fun Loadable<List<Pair<ExternalMovieId, CouchTrackerResult<MoviePortraitModel>>>>.allErrors(): List<CouchTrackerError> {
        return when (this) {
            Loadable.Loading -> emptyList()
            is Loadable.Loaded -> value.mapNotNull { (_, movie) ->
                when (movie) {
                    is Result.Error -> movie.error
                    is Result.Value -> null
                }
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
