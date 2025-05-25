package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.model.text.DbDefaultText
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType

object WatchedItemDimensionNotesDefaultData : DefaultData<ProfileData> {

    override fun insert(db: ProfileData) {
        db.watchedItemDimensionQueries.insert(
            name = DbDefaultText.NOTES.toDbText(),
            appliesTo = WatchedItemType.entries.toSet(),
            type = WatchedItemDimensionType.FreeText,
            isImportant = false,
            manualSortIndex = null,
        ).executeAsOne()
    }

    override fun upgradeTo(db: ProfileData, version: Int) {
        // no-op
    }
}
