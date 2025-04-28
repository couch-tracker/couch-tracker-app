package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.common.defaultdata.MultipleDefaultData
import io.github.couchtracker.db.profile.ProfileData

/**
 * [DefaultData] implementation for the [ProfileData] database.
 */
object ProfileDefaultData : MultipleDefaultData<ProfileData>(
    WatchedItemDimensionDefaultData,
)
