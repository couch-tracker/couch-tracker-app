package io.github.couchtracker.ui.screens.main

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.moviebase.tmdb.model.TmdbSeasonDetail
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.tmdb.TmdbLanguages
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.ui.components.ShowPortraitModel
import io.github.couchtracker.ui.components.toShowPortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.combineResults
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class ShowSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val retryContext = tmdbFlowRetryContext()

    val exploreState = ShowExploreTabState(application, viewModelScope)

    private data class PartitionedBookmarkedShows(
        val watchlist: Set<ExternalShowId>,
        val following: Set<ExternalShowId>,
    )

    @Suppress("EqualsOrHashCode")
    data class BookmarkedShowData(
        val showId: ExternalShowId,
        val portraitModel: ShowPortraitModel,
        val seasons: ApiLoadable<List<TmdbSeasonDetail>>,
    ) {
        // Computing the hascode of this class is expensive.
        // Caching it, so it's computed on creation on a background thread
        private val cachedHashCode = super.hashCode()
        override fun hashCode() = cachedHashCode
    }

    private val bookmarks: Flow<PartitionedBookmarkedShows> = KoinPlatform.getKoin().get<Flow<ProfilesInfo>>()
        .mapNotNull { profilesInfo ->
            val fullData = profilesInfo.currentFullData.resultValueOrNull()
            if (fullData != null) {
                val bookmarkedShows = fullData.bookmarkedShows.keys.injectBrokenItems()
                val (following, watchlist) = bookmarkedShows.partition { showId ->
                    fullData.watchedEpisodeSessions[showId].orEmpty().any { it.isActive }
                }
                PartitionedBookmarkedShows(
                    watchlist = watchlist.toSet(),
                    following = following.toSet(),
                )
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    val watchlist: Loadable<List<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>>> by flowDetailForShows(
        bookmarks.map { it.watchlist },
    ).collectAsLoadable()

    val following: Loadable<List<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>>> by flowDetailForShows(
        bookmarks.map { it.following },
    ).collectAsLoadable()

    val allErrors: List<CouchTrackerError> by derivedStateOf {
        watchlist.allErrors() + following.allErrors()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun flowDetailForShows(
        shows: Flow<Collection<ExternalShowId>>,
    ): Flow<Loadable<List<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>>>> {
        return retryContext { languages ->
            shows.flatMapLatest { shows ->
                if (shows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        flows = shows.map { showId ->
                            flowDetailForShow(showId, languages)
                        },
                    ) { it.toList() }
                }
            }
        }
    }

    private fun flowDetailForShow(
        showId: ExternalShowId,
        languages: TmdbLanguages,
    ): Flow<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>> {
        val tmdbShowId: TmdbShowId = when (showId) {
            is TmdbExternalShowId -> showId.id
            is UnknownExternalShowId -> return flowOf(
                showId to Result.Error(UnsupportedItemError(showId)),
            )
        }
        return tmdbShowId.details(languages.apiLanguage).transform { result ->
            val details = when (result) {
                is Result.Error -> {
                    emit(showId to result)
                    return@transform
                }
                is Result.Value -> result.value
            }
            val bookmarkedShow = Result.Value(
                value = BookmarkedShowData(
                    showId = showId,
                    portraitModel = details.toShowPortraitModels(application, languages.apiLanguage),
                    seasons = Loadable.Loading,
                ),
            )
            emit(showId to bookmarkedShow)
            emitAll(
                combine(
                    details.seasons.map {
                        val seasonId = TmdbSeasonId(tmdbShowId, it.seasonNumber)
                        seasonId.details(languages.apiLanguage)
                    },
                ) { seasons ->
                    showId to bookmarkedShow.map { data ->
                        data.copy(
                            seasons = Loadable.Loaded(seasons.asList().combineResults()),
                        )
                    }
                },
            )
        }
    }

    private fun Loadable<List<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>>>.allErrors(): List<CouchTrackerError> {
        return when (this) {
            Loadable.Loading -> emptyList()
            is Loadable.Loaded -> value.mapNotNull { (_, bookmarkedShow) ->
                when (bookmarkedShow) {
                    is Result.Error -> bookmarkedShow.error
                    is Result.Value -> bookmarkedShow.value.seasons.resultErrorOrNull()
                }
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
