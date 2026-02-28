package io.github.couchtracker.db.profile.model.watchedItem

import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemChoice
import io.github.couchtracker.db.profile.WatchedItemDimension
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import io.github.couchtracker.db.profile.WatchedItemDimensionEnableIf
import io.github.couchtracker.db.profile.WatchedItemFreeText
import io.github.couchtracker.db.profile.WatchedItemLanguage

/**
 * Wraps a [WatchedItemDimension], providing:
 * - Sealed hierarchy for better handling of different types of dimensions
 * - Enriched data with related DB entities (e.g. list of choices for the dimensions of type Choice)
 * - Business logic for common things (e.g. whether a dimension should be visible)
 * - Natural sorting based on [manualSortIndex]
 */
sealed interface WatchedItemDimensionWrapper : Comparable<WatchedItemDimensionWrapper> {

    val dimension: WatchedItemDimension
    val enableIfs: List<WatchedItemDimensionEnableIf>

    val id get() = dimension.id
    val name get() = dimension.name
    val appliesTo get() = dimension.appliesTo
    val isImportant get() = dimension.isImportant
    val manualSortIndex get() = dimension.manualSortIndex

    val type: WatchedItemDimensionType

    /**
     * Returns a [WatchedItemDimensionSelection] with an "empty" value, used for new watched items.
     */
    fun emptySelection(): WatchedItemDimensionSelection<*>

    data class Choice(
        override val dimension: WatchedItemDimension,
        override val enableIfs: List<WatchedItemDimensionEnableIf>,
        override val type: WatchedItemDimensionType.Choice,
        val choices: List<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionWrapper {

        val choiceIds = this.choices.map { it.id }.toSet()

        /**
         * Returns a [WatchedItemDimensionSelection.Choice] with the given [choices] as value.
         *
         * @throws IllegalStateException if the one of the given [choices] doesn't exist in this dimension's choices.
         */
        fun selection(choices: List<WatchedItemChoice>): WatchedItemDimensionSelection.Choice {
            val allChoices = this.choices.associateBy { it.id }
            return WatchedItemDimensionSelection.Choice(
                dimension = this,
                value = choices.map { allChoices[it.choice] ?: error("Invalid choice ${it.choice} for dimension ${dimension.id}") }.toSet(),
            )
        }

        override fun emptySelection() = WatchedItemDimensionSelection.Choice(dimension = this, value = emptySet())
    }

    data class Language(
        override val dimension: WatchedItemDimension,
        override val enableIfs: List<WatchedItemDimensionEnableIf>,
        override val type: WatchedItemDimensionType.Language,
    ) : WatchedItemDimensionWrapper {

        override fun emptySelection() = WatchedItemDimensionSelection.Language(dimension = this, value = null)

        /**
         * Returns a [WatchedItemDimensionSelection.Language] with the language represented by [language] as value.
         * If [language] is null, an empty selection is returned.
         */
        fun selection(language: WatchedItemLanguage?): WatchedItemDimensionSelection.Language {
            if (language == null) {
                return emptySelection()
            }
            require(language.dimension == dimension.id) { "Language of another dimension" }
            return WatchedItemDimensionSelection.Language(
                dimension = this,
                value = language.language,
            )
        }
    }

    data class FreeText(
        override val dimension: WatchedItemDimension,
        override val enableIfs: List<WatchedItemDimensionEnableIf>,
        override val type: WatchedItemDimensionType.FreeText,
    ) : WatchedItemDimensionWrapper {

        /**
         * Returns a [WatchedItemDimensionSelection.FreeText] with the text represented by [text] as value.
         * If [text] is null, an empty selection is returned.
         */
        fun selection(text: WatchedItemFreeText?): WatchedItemDimensionSelection.FreeText {
            if (text == null) {
                return emptySelection()
            }
            require(text.dimension == dimension.id) { "Free text of another dimension" }
            return WatchedItemDimensionSelection.FreeText(
                dimension = this,
                value = text.text,
            )
        }

        override fun emptySelection() = WatchedItemDimensionSelection.FreeText(dimension = this, value = "")
    }

    override fun compareTo(other: WatchedItemDimensionWrapper) = manualSortIndex.compareTo(other.manualSortIndex)

    /**
     * Returns whether this dimension is enabled, based on the current [dimensionSelections].
     */
    fun isEnabled(dimensionSelections: Collection<WatchedItemDimensionSelection<*>>): Boolean {
        if (enableIfs.isEmpty()) {
            // If there's no enable ifs it means the dimension is always visible
            return true
        }

        // Collect IDs of the choices that enable this dimension
        val enablingChoiceIds = when (this) {
            // If I'm a choice, out of precaution, filter out my own choices
            is Choice -> enableIfs.filterNot { it.choice in choiceIds }
            else -> enableIfs
        }.map { it.choice }.toSet()

        // Detect if there's any of these choices currently selected
        return dimensionSelections
            .filterIsInstance<WatchedItemDimensionSelection.Choice>()
            .any { choiceSelection ->
                choiceSelection.value.any { it.id in enablingChoiceIds }
            }
    }

    companion object {

        private fun of(
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

                is WatchedItemDimensionType.Language -> Language(dimension, filteredEnableIfs, dimension.type)
                is WatchedItemDimensionType.FreeText -> FreeText(dimension, filteredEnableIfs, dimension.type)
            }
        }

        /**
         * Loads all the necessary information and wraps each [WatchedItemDimension] in [WatchedItemDimensionWrapper].
         */
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
