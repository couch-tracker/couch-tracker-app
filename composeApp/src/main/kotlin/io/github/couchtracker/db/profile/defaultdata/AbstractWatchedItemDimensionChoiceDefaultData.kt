package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText
import io.github.couchtracker.db.profile.model.text.DbText
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType

abstract class AbstractWatchedItemDimensionChoiceDefaultData protected constructor(
    private val dimensionName: DbText,
    private val appliesTo: Set<WatchedItemType> = WatchedItemType.entries.toSet(),
    private val choices: List<DefaultChoice>,
    private val type: WatchedItemDimensionType.Choice = WatchedItemDimensionType.Choice.SINGLE,
) : DefaultData<ProfileData> {

    protected data class DefaultChoice(
        val name: DbDefaultText,
        val icon: DbDefaultIcon,
    )

    override fun insert(db: ProfileData) {
        val dimensionId = db.watchedItemDimensionQueries.insert(
            name = dimensionName,
            appliesTo = appliesTo,
            type = type,
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

    override fun upgradeTo(db: ProfileData, version: Int) {
        // no-op
    }
}
