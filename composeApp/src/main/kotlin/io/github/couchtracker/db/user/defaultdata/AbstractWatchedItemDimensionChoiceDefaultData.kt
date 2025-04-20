package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.user.UserData
import io.github.couchtracker.db.user.model.icon.DbDefaultIcon
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
        val name: DbDefaultText,
        val icon: DbDefaultIcon,
    )

    override fun insert(db: UserData) {
        val dimensionId = db.watchedItemDimensionQueries.insert(
            name = dimensionName,
            appliesTo = appliesTo,
            type = WatchedItemDimensionType.Choice.SINGLE,
            manualSortIndex = null,
        ).executeAsOne()

        for (choice in choices) {
            db.watchedItemDimensionChoiceQueries.insert(
                name = choice.name.toDbText(),
                icon = choice.icon.toDbIcon(),
                dimension = dimensionId,
            ).executeAsOne()
        }
    }

    override fun upgradeTo(db: UserData, version: Int) {
        // no-op
    }
}
