package io.github.couchtracker.db.user.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultDataHandler
import io.github.couchtracker.db.user.UserData
import io.github.couchtracker.db.user.UserDbMetadata

/**
 * [DefaultDataHandler] for the [UserData] database.
 */
object UserDefaultDataHandler : DefaultDataHandler<UserData>(UserDefaultData) {

    override val latestVersion = 1

    override fun UserData.setVersion(version: Int) {
        UserDbMetadata.DefaultDataVersion.setValue(this, version)
    }

    override fun UserData.getVersion(): Int? {
        return UserDbMetadata.DefaultDataVersion.getValue(this)
    }
}
