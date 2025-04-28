package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.MultipleDefaultData
import io.github.couchtracker.db.profile.ProfileData

object WatchedItemDimensionDefaultData : MultipleDefaultData<ProfileData>(
    // IMPORTANT: order is significant, the dimensions will be created in this order
    WatchedItemDimensionPlaceDefaultData,
    WatchedItemDimensionSourceDefaultData,
)
