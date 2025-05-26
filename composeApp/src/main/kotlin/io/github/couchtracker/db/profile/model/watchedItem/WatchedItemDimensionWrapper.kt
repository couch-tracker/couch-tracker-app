package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemDimension
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import io.github.couchtracker.db.profile.WatchedItemDimensionEnableIf

sealed interface WatchedItemDimensionWrapper : Comparable<WatchedItemDimensionWrapper> {

    val dimension: WatchedItemDimension
    val enableIfs: List<WatchedItemDimensionEnableIf>

    val id get() = dimension.id
    val name get() = dimension.name
    val appliesTo get() = dimension.appliesTo
    val isImportant get() = dimension.isImportant
    val manualSortIndex get() = dimension.manualSortIndex

    val type: WatchedItemDimensionType

    fun emptySelection(): WatchedItemDimensionSelection<*>

    data class Choice(
        override val dimension: WatchedItemDimension,
        override val enableIfs: List<WatchedItemDimensionEnableIf>,
        override val type: WatchedItemDimensionType.Choice,
        val choices: List<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionWrapper {

        val choiceIds = this.choices.map { it.id }.toSet()

        override fun emptySelection() = WatchedItemDimensionSelection.Choice(dimension = this, value = emptySet())
    }

    data class FreeText(
        override val dimension: WatchedItemDimension,
        override val enableIfs: List<WatchedItemDimensionEnableIf>,
        override val type: WatchedItemDimensionType.FreeText,
    ) : WatchedItemDimensionWrapper {

        override fun emptySelection() = WatchedItemDimensionSelection.FreeText(dimension = this, value = "")
    }

    override fun compareTo(other: WatchedItemDimensionWrapper) = manualSortIndex.compareTo(other.manualSortIndex)

    fun isVisible(selection: WatchedItemSelections): Boolean {
        fun log(str: String) {
            if (this is FreeText) {
                println(str)
            }
        }
        log(enableIfs.toString())
        if (enableIfs.isEmpty()) {
            // If there's no enable ifs it means the dimension is always visible
            log("empty")
            return true
        }

        // Collect IDs of the choices that enable this dimension
        val choiceIds = when (this) {
            // If I'm a choice, out of precaution, filter out my own choices
            is Choice -> enableIfs.filterNot { it.choice in choiceIds }
            is FreeText -> enableIfs
        }.map { it.choice }.toSet()

        log("dimension=${dimension.id}, choiceIds = $choiceIds")

        // Detect if there's any of these choices currently selected
        return selection.dimensions
            .filterIsInstance<WatchedItemDimensionSelection.Choice>()
            .any { choiceSelection ->
                choiceSelection.value.any { it.id in choiceIds }
            }
    }

    companion object {

        fun of(
            dimension: WatchedItemDimension,
            choices: List<WatchedItemDimensionChoice>,
            enableIfs: List<WatchedItemDimensionEnableIf>,
        ): WatchedItemDimensionWrapper {
            val filteredEnableIfs = enableIfs.filter { it.dimension == dimension.id }
            return when (dimension.type) {
                is WatchedItemDimensionType.Choice -> {
                    Choice(
                        dimension = dimension,
                        type = dimension.type,
                        choices = choices.filter { it.dimension == dimension.id }.sortedBy { it.manualSortIndex },
                        enableIfs = filteredEnableIfs,
                    )
                }

                is WatchedItemDimensionType.FreeText -> FreeText(dimension, filteredEnableIfs, dimension.type)
            }
        }

        fun load(db: ProfileData): List<WatchedItemDimensionWrapper> {
            val choices = db.watchedItemDimensionChoiceQueries.selectAll().executeAsList()
            val enableIfs = db.watchedItemDimensionQueries.selectAllEnableIf().executeAsList()
            return db.watchedItemDimensionQueries.selectAll()
                .executeAsList()
                .map {
                    of(
                        dimension = it,
                        choices = choices,
                        enableIfs = enableIfs,
                    )
                }
                .sortedBy { it.manualSortIndex }
        }
    }
}
