package io.github.couchtracker.ui.screens.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.TmdbExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.model.sort.SortAndCategorizedLocalizedList
import io.github.couchtracker.model.sort.SortableItemModel
import io.github.couchtracker.model.sort.SortableProperty
import io.github.couchtracker.model.sort.Sorter
import io.github.couchtracker.model.sort.localized
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.TmdbLanguages
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.rating
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.ui.components.MoviePortraitModel
import io.github.couchtracker.ui.components.toMoviePortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.error.aggregateErrorOrNull
import io.github.couchtracker.utils.errorOrNull
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.rememberingCombined
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.settings.getCurrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.mp.KoinPlatform

class MovieSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val retryContext = tmdbFlowRetryContext()

    val exploreState = MovieExploreTabState(viewModelScope)

    data class BookmarkedMoviesModel(
        val sorter: Sorter,
        val movies: SortAndCategorizedLocalizedList<BookmarkedMovie>,
    )

    data class BookmarkedMovie(
        // A unique key for this entry
        val itemKey: Any,
        val movieId: ExternalMovieId,
        val data: CouchTrackerResult<BookmarkedMovieData>,
    )

    @Suppress("EqualsOrHashCode")
    data class BookmarkedMovieData(
        val portraitModel: MoviePortraitModel,
        val sortModel: SortableItemModel,
    ) {
        // Computing the hashcode of this class is expensive.
        // Caching it, so it's computed on creation on a background thread
        private val cachedHashCode = super.hashCode()
        override fun hashCode() = cachedHashCode
    }

    private val bookmarks: Flow<Loadable<List<BookmarkedMovie>>> = KoinPlatform.getKoin().get<Flow<ProfilesInfo>>()
        .mapNotNull { profilesInfo ->
            val fullData = profilesInfo.currentFullData.resultValueOrNull()
            if (fullData != null) {
                fullData.bookmarkedMovies.keys.injectBrokenItems().toSet()
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .withMovieData()
        .distinctUntilChanged()
        .shareIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 1)

    val watchlist: Loadable<BookmarkedMoviesModel> by bookmarks
        .groupedAndSorted(
            sorter = AppSettings.getCurrent { StyleAndBehavior.MoviesWatchlistSortOrder },
        )
        .collectAsLoadable("movies-watchlist")

    fun aggregateError(): Flow<CouchTrackerError?> {
        // Note: all errors in this model originate from `bookmarks`, so I don't need to check errors of individual fields
        return bookmarks.map { bookmarks ->
            when (bookmarks) {
                Loadable.Loading -> null
                is Loadable.Loaded -> {
                    bookmarks.value
                        .mapNotNull { bookmarkedMovie -> bookmarkedMovie.data.errorOrNull() }
                        .aggregateErrorOrNull()
                }
            }
        }
    }

    private fun Flow<Set<ExternalMovieId>>.withMovieData(): Flow<Loadable<List<BookmarkedMovie>>> {
        return retryContext { languages ->
            rememberingCombined(
                keys = { it },
                flowToRemember = { flowDetailForMovie(it, languages) },
            ) { bookmarks, movieDataCache ->
                if (bookmarks.isEmpty()) {
                    emptyList()
                } else {
                    bookmarks.map { movieId ->
                        BookmarkedMovie(
                            itemKey = movieId.serialize(),
                            movieId = movieId,
                            data = movieDataCache.getValue(movieId),
                        )
                    }
                }
            }
        }
    }

    private fun flowDetailForMovie(
        movieId: ExternalMovieId,
        languages: TmdbLanguages,
    ): Flow<CouchTrackerResult<BookmarkedMovieData>> {
        val tmdbMovieId: TmdbMovieId = when (movieId) {
            is TmdbExternalMovieId -> movieId.id
            is UnknownExternalMovieId -> return flowOf(
                Result.Error(UnsupportedItemError(movieId)),
            )
        }
        return tmdbMovieId.details(languages.apiLanguage).map { result ->
            result.map { result ->
                val rating = result.rating()
                val runtime = SortableProperty.ofLoaded(result.runtime())
                BookmarkedMovieData(
                    portraitModel = result.toMoviePortraitModels(languages.apiLanguage, rating),
                    sortModel = SortableItemModel(
                        identifier = movieId,
                        name = SortableProperty.ofLoaded(result.title),
                        tmdbRating = SortableProperty.ofLoaded(rating),
                        firstPublicRelease = SortableProperty.ofLoaded(
                            result.releaseDate?.let { PartialDateTime.Local.Date(it) },
                        ),
                        totalRuntime = runtime,
                        episodeRuntime = runtime,
                    ),
                )
            }
        }
    }

    private fun Flow<Loadable<List<BookmarkedMovie>>>.groupedAndSorted(
        sorter: Flow<Sorter>,
    ): Flow<Loadable<BookmarkedMoviesModel>> {
        return sorter.combine(this) { sorter, bookmarks ->
            bookmarks.map { bookmarks ->
                val sortedAndGrouped = sorter.sortAndGroup(bookmarks) { movie ->
                    when (movie.data) {
                        is Result.Value -> movie.data.value.sortModel
                        is Result.Error -> SortableItemModel.error(movie.movieId)
                    }
                }
                BookmarkedMoviesModel(sorter, sortedAndGrouped.localized(application))
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
