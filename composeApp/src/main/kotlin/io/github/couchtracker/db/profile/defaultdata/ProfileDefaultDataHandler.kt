package io.github.couchtracker.db.profile.defaultdata

import io.github.couchtracker.db.common.defaultdata.DefaultDataHandler
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.ProfileDbMetadata

/**
 * [DefaultDataHandler] for the [ProfileData] database.
 */
object ProfileDefaultDataHandler : DefaultDataHandler<ProfileData>(ProfileDefaultData) {

    override val latestVersion = 1

    override fun ProfileData.setVersion(version: Int) {
        ProfileDbMetadata.DefaultDataVersion.setValue(this, version)
    }

    override fun ProfileData.getVersion(): Int? {
        return ProfileDbMetadata.DefaultDataVersion.getValue(this)
    }
}
