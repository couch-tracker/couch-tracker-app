package io.github.couchtracker.ui.screens.main

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbLanguages
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.tmdb.details
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.components.ShowPortraitModel
import io.github.couchtracker.ui.components.UpNextListItemModel
import io.github.couchtracker.ui.components.toShowPortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectWithPrevious
import io.github.couchtracker.utils.combineResults
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.flatMap
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.rememberingCombined
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalCoroutinesApi::class)
class ShowSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val retryContext = tmdbFlowRetryContext()

    val exploreState = ShowExploreTabState(application, viewModelScope)

    private data class BookmarkedShows(
        val bookmarkedShows: Set<ExternalShowId>,
        val watchSessions: Map<ExternalShowId, List<WatchedEpisodeSessionWrapper>>,
        val watchedEpisodesBySession: Map<WatchedEpisodeSessionWrapper, List<WatchedItemWrapper.Episode>>,
    )

    @Suppress("EqualsOrHashCode")
    data class BookmarkedShowData(
        val baseShowData: BaseTmdbShow,
        val portraitModel: ShowPortraitModel,
        val seasons: ApiLoadable<List<BookmarkedSeasonData>>,
    ) {
        // Computing the hashcode of this class is expensive.
        // Caching it, so it's computed on creation on a background thread
        private val cachedHashCode = super.hashCode()
        override fun hashCode() = cachedHashCode
    }

    data class BookmarkedSeasonData(
        val id: TmdbSeasonId,
        val seasonNumber: Int,
        val episodes: List<BookmarkedEpisodeData>,
    )

    data class BookmarkedEpisodeData(
        val episodeNumber: Int,
    )

    data class MaybeBookmarkedShowData(
        val showId: ExternalShowId,
        val watchSessions: Map<WatchedEpisodeSessionWrapper, Set<ExternalEpisodeId>>,
        val data: CouchTrackerResult<BookmarkedShowData>,
    )

    data class UpNextEntry(
        val showId: ExternalShowId,
        val watchSession: WatchedEpisodeSessionWrapper?,
        val model: UpNextListItemModel,
    )

    private val bookmarks: Flow<Loadable<List<MaybeBookmarkedShowData>>> =
        KoinPlatform.getKoin().get<Flow<ProfilesInfo>>()
            .mapNotNull { profilesInfo ->
                val fullData = profilesInfo.currentFullData.resultValueOrNull()
                if (fullData != null) {
                    BookmarkedShows(
                        bookmarkedShows = fullData.bookmarkedShows.keys.injectBrokenItems(),
                        watchSessions = fullData.watchedEpisodeSessions,
                        watchedEpisodesBySession = fullData.watchedEpisodesBySession,
                    )
                } else {
                    null
                }
            }
            .distinctUntilChanged()
            .withShowData()
            .distinctUntilChanged()
            .shareIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 1)

    val watchlist: Loadable<List<MaybeBookmarkedShowData>> by bookmarks.mapLatest { bookmarks ->
        bookmarks.map { bookmarks ->
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.none { it.key.isActive } }
        }
    }.collectAsLoadable("shows-watchlist")

    val following: Loadable<List<MaybeBookmarkedShowData>> by bookmarks.mapLatest { bookmarks ->
        bookmarks.map { bookmarks ->
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.any { it.key.isActive } }
        }
    }.collectAsLoadable("shows-following")

    val upNext: CouchTrackerLoadable<List<UpNextEntry>> by bookmarks
        .collectWithPrevious { previous: Loadable<Map<MaybeBookmarkedShowData, CouchTrackerLoadable<List<UpNextEntry>>>>?, bookmarks ->
            bookmarks.map { bookmarks ->
                bookmarks.associateWith { bookmarkedShowData ->
                    val old = previous?.valueOrNull()?.get(bookmarkedShowData)
                    old ?: bookmarkedShowData.upNextEntries()
                }
            }
        }
        .mapLatest { bookmarksWithUpNext ->
            bookmarksWithUpNext.flatMap { bookmarksWithUpNext ->
                bookmarksWithUpNext.values.mergeUpNextEntries()
            }
        }
        .collectAsLoadable("shows-up-next")

    fun allErrors(): Flow<List<CouchTrackerError>> {
        // Note: all errors in this model originate from `bookmarks`, so I don't need to check errors of individual fields
        return bookmarks.map { bookmarks ->
            when (bookmarks) {
                Loadable.Loading -> emptyList()
                is Loadable.Loaded -> bookmarks.value.mapNotNull { bookmarkedShowData ->
                    when (bookmarkedShowData.data) {
                        is Result.Error -> bookmarkedShowData.data.error
                        is Result.Value -> bookmarkedShowData.data.value.seasons.resultErrorOrNull()
                    }
                }
            }
        }
    }

    private fun Flow<BookmarkedShows>.withShowData(): Flow<Loadable<List<MaybeBookmarkedShowData>>> {
        return retryContext { languages ->
            rememberingCombined(
                keys = { it.bookmarkedShows },
                flowToRemember = { showId -> flowDetailForShow(showId, languages) },
            ) { bookmarkedData, showDataCache ->
                if (bookmarkedData.bookmarkedShows.isEmpty()) {
                    emptyList()
                } else {
                    bookmarkedData.bookmarkedShows.map { showId ->
                        val watchSessions = bookmarkedData.watchSessions[showId].orEmpty()
                        MaybeBookmarkedShowData(
                            showId = showId,
                            watchSessions = watchSessions.associateWith { watchSession ->
                                bookmarkedData.watchedEpisodesBySession[watchSession].orEmpty().mapTo(HashSet()) { it.itemId }
                            },
                            data = showDataCache.getValue(showId),
                        )
                    }
                }
            }
        }
    }

    private fun flowDetailForShow(
        showId: ExternalShowId,
        languages: TmdbLanguages,
    ): Flow<CouchTrackerResult<BookmarkedShowData>> {
        Log.d("flowDetailForShow", "showId: $showId")
        val tmdbShowId: TmdbShowId = when (showId) {
            is TmdbExternalShowId -> showId.id
            is UnknownExternalShowId -> return flowOf(
                Result.Error(UnsupportedItemError(showId)),
            )
        }
        return tmdbShowId.details(languages.apiLanguage).transform { result ->
            val details = when (result) {
                is Result.Error -> {
                    emit(result)
                    return@transform
                }
                is Result.Value -> result.value
            }
            val bookmarkedShow = Result.Value(
                value = BookmarkedShowData(
                    baseShowData = details.toBaseShow(languages.apiLanguage),
                    portraitModel = details.toShowPortraitModels(application, languages.apiLanguage),
                    seasons = Loadable.Loading,
                ),
            )
            emit(bookmarkedShow)
            emitAll(
                combine(
                    details.seasons.map {
                        val seasonId = TmdbSeasonId(tmdbShowId, it.seasonNumber)
                        seasonId.details(languages.apiLanguage)
                    },
                ) { seasons ->
                    val bookmarkedSeasonsData = seasons.asList().combineResults().map { season ->
                        season.map { season ->
                            val seasonId = TmdbSeasonId(tmdbShowId, season.seasonNumber)
                            BookmarkedSeasonData(
                                id = seasonId,
                                seasonNumber = season.seasonNumber,
                                episodes = season.episodes.orEmpty().map { episode ->
                                    BookmarkedEpisodeData(
                                        episodeNumber = episode.episodeNumber,
                                    )
                                },
                            )
                        }
                    }
                    Result.Value(
                        value = bookmarkedShow.value.copy(
                            seasons = Loadable.Loaded(bookmarkedSeasonsData),
                        ),
                    )
                },
            )
        }
    }

    private fun MaybeBookmarkedShowData.upNextEntries(): CouchTrackerLoadable<List<UpNextEntry>> {
        val (showData, seasons) = when (this.data) {
            is Result.Error -> return Loadable.Loaded(data)
            is Result.Value -> {
                data.value to when (data.value.seasons) {
                    Loadable.Loading -> return Loadable.Loading
                    is Loadable.Loaded -> when (data.value.seasons.value) {
                        is Result.Error -> return Loadable.Loaded(data.value.seasons.value)
                        is Result.Value -> data.value.seasons.value.value
                    }
                }
            }
        }
        Log.d("upNextEntries", "showId: $showId")
        return Loadable.value(
            if (watchSessions.isEmpty()) {
                listOfNotNull(upNextEntry(showId, null, emptySet(), showData, seasons))
            } else {
                // Has watch sessions => one entry for each watch session
                watchSessions.filter { it.key.isActive }.mapNotNull { watchSession ->
                    upNextEntry(showId, watchSession.key, watchSession.value, showData, seasons)
                }
            },
        )
    }

    private fun upNextEntry(
        showId: ExternalShowId,
        watchSession: WatchedEpisodeSessionWrapper?,
        watchedEpisodes: Set<ExternalEpisodeId>,
        showData: BookmarkedShowData,
        seasons: List<BookmarkedSeasonData>,
    ): UpNextEntry? {
        for (season in seasons) {
            if (season.seasonNumber > 0) {
                for (episode in season.episodes) {
                    val episodeId = TmdbEpisodeId(season.id, episode.episodeNumber).toExternalId()
                    if (episodeId !in watchedEpisodes) {
                        return UpNextEntry(
                            showId = showId,
                            watchSession = watchSession,
                            model = UpNextListItemModel.withShowData(
                                context = application,
                                showPreloadData = showData.baseShowData,
                                watchSession = watchSession,
                                seasonNumber = season.seasonNumber,
                                episodeNumber = episode.episodeNumber,
                            ),
                        )
                    }
                }
            }
        }
        return null
    }

    private fun Collection<CouchTrackerLoadable<List<UpNextEntry>>>.mergeUpNextEntries(): CouchTrackerLoadable<List<UpNextEntry>> {
        return if (isEmpty()) {
            Loadable.value(emptyList())
        } else if (any { it is Loadable.Loading }) {
            Loadable.Loading
        } else {
            val loaded = mapNotNull { it.resultValueOrNull() }.flatten()
            if (loaded.isEmpty()) {
                // Note: I'm just taking the first error; in principle they could be merged
                Loadable.error(firstNotNullOf { it.resultErrorOrNull() })
            } else {
                Loadable.value(loaded)
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
