package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultData
import io.github.couchtracker.db.common.defaultdata.MultipleDefaultData
import io.github.couchtracker.db.user.UserData

/**
 * [DefaultData] implementation for the [UserData] database.
 */
object UserDefaultData : MultipleDefaultData<UserData>()
