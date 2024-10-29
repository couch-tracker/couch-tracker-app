package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.user.UserData
import io.github.couchtracker.db.user.WatchedItemDimensionChoice
import io.github.couchtracker.db.user.model.icon.DbDefaultIcon
import io.github.couchtracker.db.user.model.icon.DbIcon
import io.github.couchtracker.db.user.model.text.DbDefaultText
import io.github.couchtracker.db.user.model.text.DbText
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemType

abstract class AbstractWatchedItemDimensionChoiceDefaultData protected constructor(
    private val dimensionName: DbText,
    private val appliesTo: Set<WatchedItemType> = WatchedItemType.entries.toSet(),
    private val choices: List<DefaultChoice>,
) : DefaultData<UserData> {

    protected data class DefaultChoice(
        val name: DbText,
        val icon: DbIcon,
    ) {
        constructor(text: DbDefaultText, icon: DbDefaultIcon) : this(text.toDbText(), icon.toDbIcon())
        constructor(text: DbText, icon: DbDefaultIcon) : this(text, icon.toDbIcon())
    }

    override fun insert(db: UserData) {
        val dimensionId = db.watchedItemDimensionQueries.insert(
            name = dimensionName,
            appliesTo = appliesTo,
            type = WatchedItemDimensionType.Choice.SINGLE,
            manualSortIndex = null,
        ).executeAsOne()

        for (choice in choices) {
            db.watchedItemDimensionChoiceQueries.insert(
                name = choice.name,
                icon = choice.icon,
                dimension = dimensionId,
            ).executeAsOne()
        }

        for (choice in enableIf(db)) {
            db.watchedItemDimensionQueries.insertEnableIf(
                dimension = dimensionId,
                choice = choice.id,
            )
        }
    }

    protected open fun enableIf(db: UserData): List<WatchedItemDimensionChoice> = emptyList()

    override fun upgradeTo(db: UserData, version: Int) {
        // no-op
    }
}
