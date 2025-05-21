package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemDimension
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice

sealed interface WatchedItemDimensionWrapper : Comparable<WatchedItemDimensionWrapper> {

    val dimension: WatchedItemDimension

    val id get() = dimension.id
    val name get() = dimension.name
    val appliesTo get() = dimension.appliesTo
    val manualSortIndex get() = dimension.manualSortIndex

    val type: WatchedItemDimensionType

    data class Choice(
        override val dimension: WatchedItemDimension,
        override val type: WatchedItemDimensionType,
        val choices: List<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionWrapper

    data class FreeText(
        override val dimension: WatchedItemDimension,
        override val type: WatchedItemDimensionType,
    ) : WatchedItemDimensionWrapper

    override fun compareTo(other: WatchedItemDimensionWrapper) = manualSortIndex.compareTo(other.manualSortIndex)

    companion object {
        fun of(
            dimension: WatchedItemDimension,
            choices: List<WatchedItemDimensionChoice>,
        ) = when (dimension.type) {
            is WatchedItemDimensionType.Choice -> Choice(
                dimension = dimension,
                type = dimension.type,
                choices = choices.filter { it.dimension == dimension.id }.sortedBy { it.manualSortIndex },
            )

            WatchedItemDimensionType.FreeText -> FreeText(dimension, dimension.type)
        }

        fun load(db: ProfileData): List<WatchedItemDimensionWrapper> {
            val choices = db.watchedItemDimensionChoiceQueries.selectAll().executeAsList()
            return db.watchedItemDimensionQueries.selectAll()
                .executeAsList()
                .map { of(dimension = it, choices = choices) }
                .sortedBy { it.manualSortIndex }
        }
    }
}
