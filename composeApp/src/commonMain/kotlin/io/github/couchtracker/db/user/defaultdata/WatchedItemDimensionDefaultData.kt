package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.MultipleDefaultData
import io.github.couchtracker.db.user.UserData

object WatchedItemDimensionDefaultData : MultipleDefaultData<UserData>(
    // IMPORTANT: order is significant, the dimensions will be created in this order
    WatchedItemDimensionPlaceDefaultData,
)
