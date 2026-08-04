package io.github.couchtracker.ui.screens.main

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.Bcp47Language
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
import io.github.couchtracker.tmdb.language
import io.github.couchtracker.tmdb.runtime
import io.github.couchtracker.tmdb.tmdbFlowRetryContext
import io.github.couchtracker.tmdb.toBaseShow
import io.github.couchtracker.ui.components.ShowPortraitModel
import io.github.couchtracker.ui.components.UpNextListItemModel
import io.github.couchtracker.ui.components.toShowPortraitModels
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectAsLoadable
import io.github.couchtracker.utils.collectWithPrevious
import io.github.couchtracker.utils.error.ApiLoadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.error.UnsupportedItemError
import io.github.couchtracker.utils.error.aggregateError
import io.github.couchtracker.utils.error.aggregateErrorOrNull
import io.github.couchtracker.utils.error.aggregateResults
import io.github.couchtracker.utils.flatMap
import io.github.couchtracker.utils.injectBrokenItems
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.rememberingCombined
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.valueOrNull
import io.github.couchtracker.utils.withLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.LocalDate
import org.koin.mp.KoinPlatform
import kotlin.time.Duration
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ShowSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val retryContext = tmdbFlowRetryContext()

    val exploreState = ShowExploreTabState(application, viewModelScope)

    private data class BookmarkedShows(
        val bookmarkedShows: Set<ExternalShowId>,
        val watchSessions: Map<ExternalShowId, List<WatchedEpisodeSessionWrapper>>,
        val watchedEpisodesBySession: Map<WatchedEpisodeSessionWrapper, List<WatchedItemWrapper.Episode>>,
    )

    data class BookmarkedShow(
        val showId: ExternalShowId,
        val watchSessions: Map<WatchedEpisodeSessionWrapper, List<WatchedItemWrapper.Episode>>,
        val data: CouchTrackerResult<BookmarkedShowData>,
    )

    @Suppress("EqualsOrHashCode")
    data class BookmarkedShowData(
        val baseShowData: BaseTmdbShow,
        val portraitModel: ShowPortraitModel,
        val seasons: ApiLoadable<List<BookmarkedSeasonData>>,
        val originalLanguage: Bcp47Language?,
    ) {
        // Computing the hashcode of this class is expensive.
        // Caching it, so it's computed on creation on a background thread
        private val cachedHashCode = super.hashCode()
        override fun hashCode() = cachedHashCode
    }

    data class BookmarkedSeasonData(
        val id: TmdbSeasonId,
        val number: Int,
        val episodes: List<BookmarkedEpisodeData>,
    )

    data class BookmarkedEpisodeData(
        val number: Int,
        val name: String?,
        val airDate: LocalDate?,
        val runtime: Duration?,
    )

    data class UpNextEntry(
        // A unique key for this entry
        val itemKey: Any,
        val showId: ExternalShowId,
        val watchSession: WatchedEpisodeSessionWrapper?,
        val lastWatchedEpisode: Instant?,
        val model: UpNextListItemModel,
    )

    private val bookmarks: Flow<Loadable<List<BookmarkedShow>>> =
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

    val watchlist: Loadable<List<BookmarkedShow>> by bookmarks.mapLatest { bookmarks ->
        bookmarks.map { bookmarks ->
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.none { it.key.isActive } }
        }
    }.collectAsLoadable("shows-watchlist")

    val following: Loadable<List<BookmarkedShow>> by bookmarks.mapLatest { bookmarks ->
        bookmarks.map { bookmarks ->
            bookmarks.filter { bookmarkedShowData -> bookmarkedShowData.watchSessions.any { it.key.isActive } }
        }
    }.collectAsLoadable("shows-following")

    val upNext: CouchTrackerLoadable<List<UpNextEntry>> by bookmarks
        .collectWithPrevious { previous: Loadable<Map<BookmarkedShow, CouchTrackerLoadable<List<UpNextEntry>>>>?, bookmarks ->
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

    fun aggregateError(): Flow<CouchTrackerError?> {
        // Note: all errors in this model originate from `bookmarks`, so I don't need to check errors of individual fields
        return bookmarks.map { bookmarks ->
            when (bookmarks) {
                Loadable.Loading -> null
                is Loadable.Loaded -> bookmarks.value.mapNotNull { bookmarkedShowData ->
                    when (bookmarkedShowData.data) {
                        is Result.Error -> bookmarkedShowData.data.error
                        is Result.Value -> bookmarkedShowData.data.value.seasons.resultErrorOrNull()
                    }
                }.aggregateErrorOrNull()
            }
        }
    }

    private fun Flow<BookmarkedShows>.withShowData(): Flow<Loadable<List<BookmarkedShow>>> {
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
                        BookmarkedShow(
                            showId = showId,
                            watchSessions = watchSessions.associateWith { watchSession ->
                                bookmarkedData.watchedEpisodesBySession[watchSession].orEmpty()
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
        Log.d("ShowSectionViewModel", "Computing flowDetailForShow for showId: $showId")
        val tmdbShowId: TmdbShowId = when (showId) {
            is TmdbExternalShowId -> showId.id
            is UnknownExternalShowId -> return flowOf(
                Result.Error(UnsupportedItemError(showId)),
            )
        }
        return tmdbShowId.details(languages.apiLanguage)
            .rememberingCombined(
                keys = { showDetails -> showDetails.valueOrNull()?.seasons.orEmpty().map { it.seasonNumber }.toSet() },
                flowToRemember = { seasonNumber ->
                    val seasonId = TmdbSeasonId(tmdbShowId, seasonNumber)
                    seasonId.details(languages.apiLanguage).withLoading().map { seasonResult ->
                        seasonResult.mapResult { seasonDetails ->
                            BookmarkedSeasonData(
                                id = seasonId,
                                number = seasonNumber,
                                episodes = seasonDetails.episodes.orEmpty().map { episode ->
                                    BookmarkedEpisodeData(
                                        number = episode.episodeNumber,
                                        name = episode.name,
                                        airDate = episode.airDate,
                                        runtime = episode.runtime(),
                                    )
                                },
                            )
                        }
                    }
                },
            ) { showDetails, seasonsDataCache ->
                val details = when (showDetails) {
                    is Result.Error -> {
                        return@rememberingCombined showDetails
                    }
                    is Result.Value -> showDetails.value
                }
                val seasons = details.seasons.map { seasonsDataCache.getValue(it.seasonNumber) }.aggregateResults()
                Result.Value(
                    value = BookmarkedShowData(
                        baseShowData = details.toBaseShow(languages.apiLanguage),
                        portraitModel = details.toShowPortraitModels(application, languages.apiLanguage),
                        seasons = seasons,
                        originalLanguage = details.language(),
                    ),
                )
            }
    }

    private fun BookmarkedShow.upNextEntries(): CouchTrackerLoadable<List<UpNextEntry>> {
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
        Log.d("ShowSectionViewModel", "Computing upNextEntries for showId: $showId")
        val watchSessionsToDisplay = if (watchSessions.isEmpty()) {
            // No watch sessions => a single entry for a new watch session
            mapOf(null to emptyList())
        } else {
            // Has watch sessions => one entry for each active watch session
            watchSessions.filter { it.key.isActive }
        }
        return Loadable.value(
            watchSessionsToDisplay.mapNotNull { (watchSession, episodes) ->
                upNextEntry(
                    showId,
                    watchSession,
                    episodes,
                    showData,
                    seasons,
                    hasMultipleWatchSessions = watchSessionsToDisplay.size > 1,
                )
            },
        )
    }

    @Suppress("LongParameterList", "NestedBlockDepth")
    private fun upNextEntry(
        showId: ExternalShowId,
        watchSession: WatchedEpisodeSessionWrapper?,
        watchedEpisodes: List<WatchedItemWrapper.Episode>,
        showData: BookmarkedShowData,
        seasons: List<BookmarkedSeasonData>,
        hasMultipleWatchSessions: Boolean,
    ): UpNextEntry? {
        val watchedEpisodesIds = watchedEpisodes.mapTo(HashSet()) { it.itemId }
        for (season in seasons) {
            if (season.number > 0) {
                for (episode in season.episodes) {
                    val episodeId = TmdbEpisodeId(season.id, episode.number).toExternalId()
                    if (episodeId !in watchedEpisodesIds) {
                        // Note the item key is the same with 0 or 1 watch sessions.
                        // That's so creating the first watch session won't change the entry's key.
                        // Note: the type should be savable via Bundle
                        val itemKey = if (hasMultipleWatchSessions) {
                            showId.serialize() to watchSession?.id
                        } else {
                            showId.serialize()
                        }
                        return UpNextEntry(
                            itemKey = itemKey,
                            showId = showId,
                            watchSession = watchSession,
                            model = UpNextListItemModel.withShowData(
                                context = application,
                                watchSession = watchSession,
                                show = showData,
                                season = season,
                                episode = episode,
                            ),
                            lastWatchedEpisode = watchedEpisodes.maxOfOrNull { it.addedAt },
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
            val loaded = mapNotNull { it.resultValueOrNull() }
                .flatten()
                .sortedByDescending { it.lastWatchedEpisode }
            if (loaded.isEmpty()) {
                Loadable.error(mapNotNull { it.resultErrorOrNull() }.aggregateError())
            } else {
                Loadable.value(loaded)
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
