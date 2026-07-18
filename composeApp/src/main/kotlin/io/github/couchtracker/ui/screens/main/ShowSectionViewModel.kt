package io.github.couchtracker.ui.screens.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import app.moviebase.tmdb.model.TmdbEpisode
import app.moviebase.tmdb.model.TmdbSeasonDetail
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
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
import io.github.couchtracker.utils.combineResults
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
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
    )

    @Suppress("EqualsOrHashCode")
    data class BookmarkedShowData(
        val baseShowData: BaseTmdbShow,
        val watchSessions: List<WatchedEpisodeSessionWrapper>,
        val portraitModel: ShowPortraitModel,
        // TODO: I should probably keep less data in RAM than literally everything
        val seasons: ApiLoadable<List<TmdbSeasonDetail>>,
    ) {
        // Computing the hashcode of this class is expensive.
        // Caching it, so it's computed on creation on a background thread
        private val cachedHashCode = super.hashCode()
        override fun hashCode() = cachedHashCode
    }

    data class MaybeBookmarkedShowData(
        val showId: ExternalShowId,
        val watchSessions: List<WatchedEpisodeSessionWrapper>,
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
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.none { it.isActive } }
        }
    }.collectAsLoadable("shows-watchlist")

    val following: Loadable<List<MaybeBookmarkedShowData>> by bookmarks.mapLatest { bookmarks ->
        bookmarks.map { bookmarks ->
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.any { it.isActive } }
        }
    }.collectAsLoadable("shows-following")

    val upNext: CouchTrackerLoadable<List<UpNextEntry>> by bookmarks.flatMapLatest { bookmarks ->
        when (bookmarks) {
            Loadable.Loading -> flowOf(Loadable.Loading)
            is Loadable.Loaded -> {
                val allFlows = bookmarks.value.flatMap { bookmarkedShowData ->
                    bookmarkedShowData.upNextEntries()
                }
                combine(allFlows) { it.toList().mergeUpNextEntries() }
            }
        }
    }.collectAsLoadable("shows-up-next")

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
            flatMapLatest { bookmarkedData ->
                if (bookmarkedData.bookmarkedShows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        flows = bookmarkedData.bookmarkedShows.map { showId ->
                            flowDetailForShow(showId, bookmarkedData.watchSessions[showId].orEmpty(), languages)
                        },
                    ) { it.toList() }
                }
            }
        }
    }

    private fun flowDetailForShow(
        showId: ExternalShowId,
        watchSessions: List<WatchedEpisodeSessionWrapper>,
        languages: TmdbLanguages,
    ): Flow<MaybeBookmarkedShowData> {
        val tmdbShowId: TmdbShowId = when (showId) {
            is TmdbExternalShowId -> showId.id
            is UnknownExternalShowId -> return flowOf(
                MaybeBookmarkedShowData(showId, watchSessions, Result.Error(UnsupportedItemError(showId))),
            )
        }
        return tmdbShowId.details(languages.apiLanguage).transform { result ->
            val details = when (result) {
                is Result.Error -> {
                    emit(MaybeBookmarkedShowData(showId, watchSessions, result))
                    return@transform
                }
                is Result.Value -> result.value
            }
            val bookmarkedShow = Result.Value(
                value = BookmarkedShowData(
                    baseShowData = details.toBaseShow(languages.apiLanguage),
                    portraitModel = details.toShowPortraitModels(application, languages.apiLanguage),
                    seasons = Loadable.Loading,
                    watchSessions = watchSessions,
                ),
            )
            emit(MaybeBookmarkedShowData(showId, watchSessions, bookmarkedShow))
            emitAll(
                combine(
                    details.seasons.map {
                        val seasonId = TmdbSeasonId(tmdbShowId, it.seasonNumber)
                        seasonId.details(languages.apiLanguage)
                    },
                ) { seasons ->
                    MaybeBookmarkedShowData(
                        showId = showId,
                        watchSessions = watchSessions,
                        data = bookmarkedShow.map { data ->
                            data.copy(
                                seasons = Loadable.Loaded(seasons.asList().combineResults()),
                            )
                        },
                    )
                },
            )
        }
    }

    private fun MaybeBookmarkedShowData.upNextEntries(): List<Flow<CouchTrackerLoadable<UpNextEntry?>>> {
        return if (watchSessions.isEmpty()) {
            listOf(upNextEntry(showId, null, this.data))
        } else {
            // Has watch sessions => one entry for each watch session
            watchSessions.filter { it.isActive }.map { watchSession ->
                upNextEntry(showId, watchSession, this.data)
            }
        }
    }

    private fun List<CouchTrackerLoadable<UpNextEntry?>>.mergeUpNextEntries(): CouchTrackerLoadable<List<UpNextEntry>> {
        return if (isEmpty()) {
            Loadable.value(emptyList())
        } else if (any { it is Loadable.Loading }) {
            Loadable.Loading
        } else {
            val loaded = mapNotNull { it.resultValueOrNull() }
            if (loaded.isEmpty()) {
                // Note: I'm just taking the first error; in principle they could be merged
                Loadable.error(firstNotNullOf { it.resultErrorOrNull() })
            } else {
                Loadable.value(loaded)
            }
        }
    }

    private fun upNextEntry(
        showId: ExternalShowId,
        watchSession: WatchedEpisodeSessionWrapper?,
        showData: CouchTrackerResult<BookmarkedShowData>,
    ): Flow<CouchTrackerLoadable<UpNextEntry?>> {
        val (showData, seasons) = when (showData) {
            is Result.Error -> return flowOf(Loadable.Loaded(showData))
            is Result.Value -> {
                showData.value to when (showData.value.seasons) {
                    Loadable.Loading -> return flowOf(Loadable.Loading)
                    is Loadable.Loaded -> when (showData.value.seasons.value) {
                        is Result.Error -> return flowOf(Loadable.Loaded(showData.value.seasons.value))
                        is Result.Value -> showData.value.seasons.value.value
                    }
                }
            }
        }

        return findNextEpisodeToWatch(showData.baseShowData.key.id, watchSession, seasons).map {
            Loadable.value(
                if (it == null) {
                    null
                } else {
                    UpNextEntry(
                        showId = showId,
                        watchSession = watchSession,
                        model = UpNextListItemModel.withShowData(
                            context = application,
                            showPreloadData = showData.baseShowData,
                            watchSession = watchSession,
                            episode = it,
                        ),
                    )
                },
            )
        }
    }

    private fun findNextEpisodeToWatch(
        showId: TmdbShowId,
        watchSession: WatchedEpisodeSessionWrapper?,
        seasons: List<TmdbSeasonDetail>,
    ): Flow<TmdbEpisode?> {
        return if (watchSession == null) {
            // No watch sessions => one entry with the pilot
            flowOf(seasons.firstOrNull { it.seasonNumber > 0 }?.episodes?.firstOrNull())
        } else {
            // By design, I read the full profile data again here.
            // While it could potentially create minor inconsistencies,
            // it avoids the situation where each minor profile change causes for the whole up-next section to reload
            KoinPlatform.getKoin().get<Flow<ProfilesInfo>>()
                .map { profilesInfo ->
                    val fullData = profilesInfo.currentFullData.resultValueOrNull()
                    val watchedEpisodes: Set<ExternalEpisodeId> = fullData?.watchedEpisodesBySession[watchSession]
                        .orEmpty()
                        .mapTo(mutableSetOf()) { it.itemId }
                    for (season in seasons) {
                        if (season.seasonNumber > 0) {
                            val seasonId = TmdbSeasonId(showId, season.seasonNumber)
                            for (episode in season.episodes.orEmpty()) {
                                val episodeId = TmdbEpisodeId(seasonId, episode.episodeNumber).toExternalId()
                                if (episodeId !in watchedEpisodes) {
                                    return@map episode
                                }
                            }
                        }
                    }
                    null
                }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
