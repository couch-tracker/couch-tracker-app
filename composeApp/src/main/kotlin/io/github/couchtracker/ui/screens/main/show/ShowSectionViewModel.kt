package io.github.couchtracker.ui.screens.main.show

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.couchtracker.R
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.db.profile.externalids.TmdbExternalShowId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.settings.StyleAndBehaviorSettings
import io.github.couchtracker.settings.StyleAndBehaviorSettings.UpNextSortOrderOption.LAST_WATCHED_FIRST
import io.github.couchtracker.settings.StyleAndBehaviorSettings.UpNextSortOrderOption.NEWEST_FIRST
import io.github.couchtracker.settings.StyleAndBehaviorSettings.UpNextSortOrderOption.OLDEST_FIRST
import io.github.couchtracker.settings.StyleAndBehaviorSettings.UpNextSortOrderOption.SAME_AS_SHOWS
import io.github.couchtracker.settings.UpNextOptions
import io.github.couchtracker.settings.upNextOptions
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
import io.github.couchtracker.utils.settings.getCurrent
import io.github.couchtracker.utils.valueOrNull
import io.github.couchtracker.utils.withLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.mp.KoinPlatform
import kotlin.time.Clock
import kotlin.time.Duration

private val UP_NEXT_MARKED_WATCHED_RECENTLY_THRESHOLD = DatePeriod(days = 1)
private val UP_NEXT_WATCHED_RECENTLY_THRESHOLD = DatePeriod(days = 14)
private val UP_NEXT_AIRED_RECENTLY_THRESHOLD = DatePeriod(days = 14)
private val UP_NEXT_AIRING_SOON_THRESHOLD = DatePeriod(days = 7)

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
        val episodeId: ExternalEpisodeId,
        val previousViewings: List<WatchedItemWrapper.Episode>,
        val airDate: LocalDate?,
        val model: UpNextListItemModel,
    )

    data class UpNextModel(
        val options: UpNextOptions,
        val sections: Map<UpNextSection, List<UpNextEntry>>,
    )

    enum class UpNextSection(@StringRes val stringRes: Int) {
        SUGGESTED(R.string.up_next_section_suggested),
        AIRING_TODAY(R.string.up_next_section_airing_today),
        AIRED_RECENTLY(R.string.up_next_section_aired_recently),
        AIRING_SOON(R.string.up_next_section_airing_soon),
        AIRED(R.string.up_next_section_aired),
        UNKNOWN_AIR_DATE(R.string.up_next_section_unknown_air_date),
        NOT_AIRED(R.string.up_next_section_not_aired),
        OTHER(R.string.up_next_section_other),
    }

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

    val upNext: CouchTrackerLoadable<UpNextModel> by AppSettings.getCurrent { StyleAndBehavior.EpisodeNumberFormatting }
        .flatMapLatest { episodeFormatting ->
            bookmarks.collectWithPrevious { previous: Loadable<Map<BookmarkedShow, CouchTrackerLoadable<List<UpNextEntry>>>>?, bookmarks ->
                bookmarks.map { bookmarks ->
                    bookmarks.associateWith { bookmarkedShowData ->
                        val old = previous?.valueOrNull()?.get(bookmarkedShowData)
                        old ?: bookmarkedShowData.upNextEntries(episodeFormatting)
                    }
                }
            }
        }
        .combine(
            AppSettings.loadedSettingsFlow.settings.map { it.upNextOptions() }.distinctUntilChanged(),
        ) { bookmarksWithUpNext, upNextOptions ->
            bookmarksWithUpNext.flatMap { bookmarksWithUpNext ->
                bookmarksWithUpNext.values.mergeUpNextEntries(upNextOptions).mapResult { sections ->
                    UpNextModel(upNextOptions, sections)
                }
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

    private fun BookmarkedShow.upNextEntries(
        episodeFormatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
    ): CouchTrackerLoadable<List<UpNextEntry>> {
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
                    showId = showId,
                    watchSession = watchSession,
                    watchedEpisodes = episodes,
                    showData = showData,
                    seasons = seasons,
                    hasMultipleWatchSessions = watchSessionsToDisplay.size > 1,
                    episodeFormatting = episodeFormatting,
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
        episodeFormatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
    ): UpNextEntry? {
        val watchedEpisodesIds = watchedEpisodes.mapTo(HashSet()) { it.itemId }
        var previousEpisodeId: ExternalEpisodeId? = null
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
                        val previousViewings = if (previousEpisodeId == null) {
                            emptyList()
                        } else {
                            watchedEpisodes.filter { it.itemId == previousEpisodeId }.also {
                                check(it.isNotEmpty())
                            }
                        }
                        return UpNextEntry(
                            itemKey = itemKey,
                            showId = showId,
                            watchSession = watchSession,
                            episodeId = episodeId,
                            model = UpNextListItemModel.withShowData(
                                context = application,
                                episodeFormatting = episodeFormatting,
                                watchSession = watchSession,
                                show = showData,
                                season = season,
                                episode = episode,
                            ),
                            previousViewings = previousViewings,
                            airDate = episode.airDate,
                        )
                    }
                    previousEpisodeId = episodeId
                }
            }
        }
        return null
    }

    private fun Collection<CouchTrackerLoadable<List<UpNextEntry>>>.mergeUpNextEntries(
        options: UpNextOptions,
    ): CouchTrackerLoadable<Map<UpNextSection, List<UpNextEntry>>> {
        return if (isEmpty()) {
            Loadable.value(emptyMap())
        } else if (any { it is Loadable.Loading }) {
            Loadable.Loading
        } else {
            val loaded = mapNotNull { it.resultValueOrNull() }.flatten()
            if (loaded.isEmpty()) {
                Loadable.error(mapNotNull { it.resultErrorOrNull() }.aggregateError())
            } else {
                Loadable.value(loaded.groupedAndSorted(options))
            }
        }
    }

    private fun List<UpNextEntry>.groupedAndSorted(options: UpNextOptions): Map<UpNextSection, List<UpNextEntry>> {
        val upNextEntries = this
        // TODO: today/timezone should recompute
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val recentlyWatchedBound = (today - UP_NEXT_WATCHED_RECENTLY_THRESHOLD).atStartOfDayIn(timeZone)
        val recentlyMarkedAsWatchedBound = (today - UP_NEXT_MARKED_WATCHED_RECENTLY_THRESHOLD).atStartOfDayIn(timeZone)

        val groupedEntries = upNextEntries.groupBy { entry ->
            val isSuggested = when {
                !options.enableSmartSuggestions -> false
                // Not aired
                entry.airDate == null || entry.airDate > today -> false
                // Recently aired
                entry.airDate >= today - UP_NEXT_AIRED_RECENTLY_THRESHOLD -> true
                // Recently (marked as) watched
                else -> {
                    entry.previousViewings.any { previousViewing ->
                        val watchedRecently = when (val watchedAt = previousViewing.watchAt) {
                            null -> false
                            is PartialDateTime.Local -> watchedAt.toInstant(timeZone) >= recentlyWatchedBound
                            is PartialDateTime.Zoned -> watchedAt.toInstant() >= recentlyWatchedBound
                        }
                        val markedAsWatchedRecently = previousViewing.addedAt >= recentlyMarkedAsWatchedBound
                        watchedRecently || markedAsWatchedRecently
                    }
                }
            }
            if (isSuggested) {
                return@groupBy UpNextSection.SUGGESTED
            }
            when {
                !options.divideAired -> UpNextSection.OTHER
                entry.airDate == null -> UpNextSection.UNKNOWN_AIR_DATE
                entry.airDate < today - UP_NEXT_AIRED_RECENTLY_THRESHOLD -> UpNextSection.AIRED
                entry.airDate < today -> UpNextSection.AIRED_RECENTLY
                entry.airDate == today -> UpNextSection.AIRING_TODAY
                entry.airDate <= today + UP_NEXT_AIRING_SOON_THRESHOLD -> UpNextSection.AIRING_SOON
                else -> UpNextSection.NOT_AIRED
            }
        }
        return groupedEntries
            .mapValues { it.value.sorted(options) }
            .toSortedMap()
    }

    private fun List<UpNextEntry>.sorted(options: UpNextOptions): List<UpNextEntry> {
        val upNextEntries = this
        return when (options.sortOrder) {
            LAST_WATCHED_FIRST -> {
                val entriesWithTime = upNextEntries.flatMap { upNextEntry ->
                    if (upNextEntry.previousViewings.isEmpty()) {
                        listOf(upNextEntry to null)
                    } else {
                        upNextEntry.previousViewings.map { previousViewing ->
                            upNextEntry to previousViewing.watchAt
                        }
                    }
                }
                PartialDateTime
                    .sort(entriesWithTime, { second })
                    .reversed()
                    .mapTo(mutableSetOf()) { it.first }
                    .toList()
            }
            // TODO: implement same as show
            SAME_AS_SHOWS -> upNextEntries
            NEWEST_FIRST -> upNextEntries.sortedByDescending { it.airDate }
            OLDEST_FIRST -> upNextEntries.sortedBy { it.airDate }
        }
    }

    fun retryAll() {
        viewModelScope.launch { retryContext.retryAll() }
    }
}
