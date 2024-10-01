package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.user.UserData
import io.github.couchtracker.db.user.model.icon.DbDefaultIcon
import io.github.couchtracker.db.user.model.text.DbDefaultText
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemDimensionType
import io.github.couchtracker.db.user.model.watchedItem.WatchedItemType

object WatchedItemDimensionPlaceDefaultData : DefaultData<UserData> {

    private data class DefaultPlace(
        val name: DbDefaultText,
        val icon: DbDefaultIcon,
    )

    private val DEFAULT_PLACES = listOf(
        DefaultPlace(DbDefaultText.PLACE_HOME, DbDefaultIcon.HOUSE),
        DefaultPlace(DbDefaultText.PLACE_FRIENDS_HOUSE, DbDefaultIcon.FRIENDS),
        DefaultPlace(DbDefaultText.PLACE_CINEMA, DbDefaultIcon.CINEMA),
        DefaultPlace(DbDefaultText.PLACE_VACATION, DbDefaultIcon.VACATION),
        DefaultPlace(DbDefaultText.PLACE_CAR, DbDefaultIcon.CAR),
        DefaultPlace(DbDefaultText.PLACE_PLANE, DbDefaultIcon.PLANE),
        DefaultPlace(DbDefaultText.PLACE_TRAIN, DbDefaultIcon.TRAIN),
        DefaultPlace(DbDefaultText.PLACE_BUS, DbDefaultIcon.BUS),
        DefaultPlace(DbDefaultText.PLACE_WORK, DbDefaultIcon.OFFICE_BUILDING),
        DefaultPlace(DbDefaultText.PLACE_SCHOOL, DbDefaultIcon.SCHOOL),
    )

    override fun insert(db: UserData) {
        val placeId = db.watchedItemDimensionQueries.insert(
            name = DbDefaultText.PLACE.toDbText(),
            appliesTo = WatchedItemType.entries.toSet(),
            type = WatchedItemDimensionType.Choice.SINGLE,
            manualSortIndex = null,
        ).executeAsOne()

        for (place in DEFAULT_PLACES) {
            db.watchedItemDimensionChoiceQueries.insert(
                name = place.name.toDbText(),
                icon = place.icon.toDbIcon(),
                dimension = placeId,
            ).executeAsOne()
        }
    }

    override fun upgradeTo(db: UserData, version: Int) {
        // no-op
    }
}
