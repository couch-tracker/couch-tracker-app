package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisode
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedMovie
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTimeGroup
import io.github.couchtracker.intl.datetime.localizedFull
import io.github.couchtracker.utils.str

/**
 * Wrapper around a [WatchedItem]. It's useful to also capture the values for all dimensions associated with it.
 */
sealed class WatchedItemWrapper {

    protected abstract val watchedItem: WatchedItem
    abstract val selections: WatchedItemDimensionSelectionsWrapper

    val id get() = watchedItem.id
    abstract val itemId: ExternalId
    val addedAt get() = watchedItem.addedAt
    val watchAt get() = watchedItem.watchAt
    val dimensions get() = selections.dimensions

    fun type() = when (this) {
        is Episode -> WatchedItemType.EPISODE
        is Movie -> WatchedItemType.MOVIE
    }

    fun delete(db: ProfileData) {
        db.transaction {
            db.watchedItemQueries.delete(watchedItem.id)
            db.watchedItemDimensionSelectionsQueries.delete(watchedItem.dimensionSelections)
        }
    }

    data class Movie(
        override val watchedItem: WatchedItem,
        val watchedMovie: WatchedMovie,
        override val selections: WatchedItemDimensionSelectionsWrapper,
    ) : WatchedItemWrapper() {

        override val itemId get() = watchedMovie.movieId

        init {
            require(watchedItem.id == watchedMovie.id) { "WatchedItem and WatchedMovie IDs must match" }
            require(selections.id == watchedItem.dimensionSelections) { "WatchedItem selections and given selections wrapper must match" }
        }
    }

    data class Episode(
        override val watchedItem: WatchedItem,
        val watchedEpisode: WatchedEpisode,
        override val selections: WatchedItemDimensionSelectionsWrapper,
        val session: WatchedEpisodeSessionWrapper,
    ) : WatchedItemWrapper() {

        override val itemId get() = watchedEpisode.episodeId

        init {
            require(watchedItem.id == watchedEpisode.id) { "WatchedItem and WatchedEpisode IDs must match" }
            require(selections.id == watchedItem.dimensionSelections) { "WatchedItem selections and given selections wrapper must match" }
            require(session.id == watchedEpisode.session) { "WatchedItem session and given session wrapper must match" }
        }
    }

    companion object {

        /**
         * Loads all the necessary information and wraps each [WatchedItem] in [WatchedItemWrapper].
         *
         * @param selections list of all dimensions selections present in the database, see [WatchedItemDimensionSelectionsWrapper.load]
         */
        fun load(
            db: ProfileData,
            selections: List<WatchedItemDimensionSelectionsWrapper>,
            watchedEpisodeSessions: List<WatchedEpisodeSessionWrapper>,
        ): List<WatchedItemWrapper> {
            val watchedItems = db.watchedItemQueries.selectAll().executeAsList()
            val watchedMovies = db.watchedMovieQueries.selectAll().executeAsList().associateBy { it.id }
            val watchedEpisodes = db.watchedEpisodeQueries.selectAll().executeAsList().associateBy { it.id }
            val watchedEpisodeSessions = watchedEpisodeSessions.associateBy { it.id }

            val selectionsById = selections.associateBy { it.id }

            fun findSelections(id: Long) = selectionsById[id] ?: error("Unable to find WatchedItemDimensionSelections with id $id")

            return watchedItems.map { watchedItem ->
                val watchedMovie = watchedMovies[watchedItem.id]
                val watchedEpisode = watchedEpisodes[watchedItem.id]
                val selections = findSelections(watchedItem.dimensionSelections)
                when {
                    watchedMovie != null -> Movie(
                        watchedItem = watchedItem,
                        watchedMovie = watchedMovie,
                        selections = selections,
                    )
                    watchedEpisode != null -> Episode(
                        watchedItem = watchedItem,
                        watchedEpisode = watchedEpisode,
                        selections = selections,
                        session = watchedEpisodeSessions[watchedEpisode.session]
                            ?: error("Unable to find watched episode session with id ${watchedEpisode.session}"),
                    )
                    else -> error("Unknown type of watched item $watchedItem")
                }
            }
        }
    }
}

fun Collection<WatchedItemWrapper>.sortDescending(): List<WatchedItemWrapper> {
    return PartialDateTime.sort(
        items = this,
        getPartialDateTime = { watchAt },
        additionalComparator = compareBy { it.addedAt },
    ).reversed()
}

fun Collection<WatchedItemWrapper>.sortAndGroupDescending(): Map<PartialDateTimeGroup, List<WatchedItemWrapper>> {
    val groups = PartialDateTime.sortAndGroup(
        items = this,
        getPartialDateTime = { watchAt },
        additionalComparator = compareBy { it.addedAt },
    )
    return buildMap {
        for ((group, items) in groups.entries.reversed()) {
            put(group, items.reversed())
        }
    }
}

@ReadOnlyComposable
@Composable
fun WatchedItemWrapper.localizedWatchAt(includeTimeZone: Boolean): String {
    val watchAt = watchAt
    if (watchAt == null) {
        return R.string.unknown_date.str()
    }
    val date = if (includeTimeZone) watchAt else watchAt.local

    return date.localizedFull().string()
}
