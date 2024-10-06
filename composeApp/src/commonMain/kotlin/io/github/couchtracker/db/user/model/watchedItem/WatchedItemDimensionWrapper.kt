package io.github.couchtracker.db.user.model.watchedItem

import io.github.couchtracker.db.user.WatchedItemChoice
import io.github.couchtracker.db.user.WatchedItemDimension
import io.github.couchtracker.db.user.WatchedItemDimensionChoice
import io.github.couchtracker.db.user.WatchedItemFreeText

sealed interface WatchedItemDimensionWrapper {

    val entity: WatchedItemDimension
    val type: WatchedItemDimensionType

    data class Choice(
        override val entity: WatchedItemDimension,
        override val type: WatchedItemDimensionType.Choice,
        val choices: List<WatchedItemDimensionChoice>,
    ) : WatchedItemDimensionWrapper {

        private val choiceIds = choices.map { it.id }.toSet()

        init {
            require(choices.isNotEmpty())
        }

        fun toSelection(allChoices: List<WatchedItemChoice>): WatchedItemDimensionSelection.Choice {
            return WatchedItemDimensionSelection.Choice(
                dimension = this,
                selection = allChoices.filter { it.choice in choiceIds },
            )
        }
    }

    data class FreeText(
        override val entity: WatchedItemDimension,
        override val type: WatchedItemDimensionType.FreeText,
    ) : WatchedItemDimensionWrapper {

        fun toSelection(allFreeTexts: List<WatchedItemFreeText>): WatchedItemDimensionSelection.FreeText {
            return WatchedItemDimensionSelection.FreeText(
                dimension = this,
                selection = allFreeTexts.singleOrNull { it.watchedItemDimension == entity.id },
            )
        }
    }

    companion object {
        fun wrapAll(
            dimensions: List<WatchedItemDimension>,
            choices: List<WatchedItemDimensionChoice>,
        ): List<WatchedItemDimensionWrapper> {
            return dimensions.mapNotNull { dimension ->
                when (dimension.type) {
                    is WatchedItemDimensionType.Choice -> {
                        val filteredChoices = choices.filter { it.dimension == it.id }
                        if (filteredChoices.isEmpty()) {
                            null
                        } else {
                            Choice(
                                entity = dimension,
                                type = dimension.type,
                                choices = filteredChoices,
                            )
                        }
                    }

                    is WatchedItemDimensionType.FreeText -> FreeText(entity = dimension, type = dimension.type)
                }
            }
        }
    }
}
