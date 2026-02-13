package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ExternalId
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisode
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.WatchedItemChoice
import io.github.couchtracker.db.profile.WatchedMovie
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTimeGroup
import io.github.couchtracker.intl.datetime.localizedFull
import io.github.couchtracker.utils.str

/**
 * Wrapper around a [WatchedItem]. It's useful to also capture the values for all dimensions associated with it.
 */
sealed class WatchedItemWrapper {

    protected abstract val watchedItem: WatchedItem
    abstract val dimensions: List<WatchedItemDimensionSelection<*>>

    val id get() = watchedItem.id
    abstract val itemId: ExternalId
    val addedAt get() = watchedItem.addedAt
    val watchAt get() = watchedItem.watchAt

    fun type() = when (this) {
        is Episode -> WatchedItemType.EPISODE
        is Movie -> WatchedItemType.MOVIE
    }

    fun delete(db: ProfileData) {
        db.watchedItemQueries.delete(watchedItem.id)
    }

    data class Movie(
        override val watchedItem: WatchedItem,
        val watchedMovie: WatchedMovie,
        override val dimensions: List<WatchedItemDimensionSelection<*>>,
    ) : WatchedItemWrapper() {

        override val itemId get() = watchedMovie.movieId

        init {
            require(watchedItem.id == watchedMovie.id) { "WatchedItem and WatchedMovie IDs must match" }
        }
    }

    data class Episode(
        override val watchedItem: WatchedItem,
        val watchedEpisode: WatchedEpisode,
        override val dimensions: List<WatchedItemDimensionSelection<*>>,
    ) : WatchedItemWrapper() {

        override val itemId get() = watchedEpisode.episodeId

        init {
            require(watchedItem.id == watchedEpisode.id) { "WatchedItem and WatchedEpisode IDs must match" }
        }
    }

    companion object {

        /**
         * Loads all the necessary information and wraps each [WatchedItem] in [WatchedItemWrapper].
         *
         * @param dimensions list of all dimensions present in the database, see [WatchedItemDimensionWrapper.load]
         */
        fun load(
            db: ProfileData,
            dimensions: List<WatchedItemDimensionWrapper>,
        ): List<WatchedItemWrapper> {
            val choices = db.watchedItemChoiceQueries.selectAllWithDimensionId().executeAsList()
                .groupBy { it.watchedItem to it.watchedItemDimension }
                .mapValues { (_, list) ->
                    list.map { WatchedItemChoice(watchedItem = it.watchedItem, choice = it.choice) }
                }
            val freeTexts = db.watchedItemFreeTextQueries.selectAll().executeAsList()
                .associateBy { it.watchedItem to it.watchedItemDimension }
            val languages = db.watchedItemLanguageQueries.selectAll().executeAsList()
                .associateBy { it.watchedItem to it.watchedItemDimension }

            val watchedItems = db.watchedItemQueries.selectAll().executeAsList()
            val watchedMovies = db.watchedMovieQueries.selectAll().executeAsList().associateBy { it.id }
            val watchedEpisodes = db.watchedEpisodeQueries.selectAll().executeAsList().associateBy { it.id }

            return watchedItems.map { watchedItem ->
                val dimensions = dimensions.map { dimension ->
                    val key = watchedItem.id to dimension.id
                    when (dimension) {
                        is WatchedItemDimensionWrapper.Choice -> dimension.selection(choices[key].orEmpty())
                        is WatchedItemDimensionWrapper.Language -> dimension.selection(languages[key])
                        is WatchedItemDimensionWrapper.FreeText -> dimension.selection(freeTexts[key])
                    }
                }.sortedBy { it.dimension.manualSortIndex }

                val watchedMovie = watchedMovies[watchedItem.id]
                val watchedEpisode = watchedEpisodes[watchedItem.id]
                when {
                    watchedMovie != null -> Movie(
                        watchedItem = watchedItem,
                        watchedMovie = watchedMovie,
                        dimensions = dimensions,
                    )
                    watchedEpisode != null -> Episode(
                        watchedItem = watchedItem,
                        watchedEpisode = watchedEpisode,
                        dimensions = dimensions,
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
