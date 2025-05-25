package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import io.github.couchtracker.db.profile.model.icon.DbDefaultIcon
import io.github.couchtracker.db.profile.model.icon.DbIcon
import io.github.couchtracker.db.profile.model.text.DbDefaultText
import io.github.couchtracker.db.profile.model.text.DbText
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType

abstract class AbstractWatchedItemDimensionChoiceDefaultData protected constructor(
    private val dimensionName: DbText,
    private val appliesTo: Set<WatchedItemType> = WatchedItemType.entries.toSet(),
    private val choices: List<DefaultChoice>,
    private val type: WatchedItemDimensionType.Choice = WatchedItemDimensionType.Choice.SINGLE,
    private val isImportant: Boolean = false,
) : DefaultData<ProfileData> {

    protected data class DefaultChoice(
        val name: DbText,
        val icon: DbIcon?,
    ) {
        constructor(text: DbDefaultText, icon: DbDefaultIcon?) : this(text.toDbText(), icon?.toDbIcon())
        constructor(text: DbText, icon: DbDefaultIcon) : this(text, icon.toDbIcon())
    }

    override fun insert(db: ProfileData) {
        val dimensionId = db.watchedItemDimensionQueries.insert(
            name = dimensionName,
            appliesTo = appliesTo,
            type = type,
            isImportant = isImportant,
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

    protected open fun enableIf(db: ProfileData): List<WatchedItemDimensionChoice> = emptyList()

    override fun upgradeTo(db: ProfileData, version: Int) {
        // no-op
    }
}
