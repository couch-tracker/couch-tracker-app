package io.github.couchtracker.db.app

import io.github.couchtracker.db.profile.DatabaseTransaction
import io.github.couchtracker.db.profile.ExternalProfileDb
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ManagedProfileDb
import io.github.couchtracker.db.profile.ProfileDb
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.collectWithPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlin.collections.get

class ProfilesInfo(
    val profiles: Map<Long, ProfileInfo>,
    val current: ProfileInfo,
)

class ProfileInfo(
    val profile: Profile,
    val db: ProfileDb,
    val fullProfileDataFlows: FullProfileDataFlows,
) {

    val fullProfileDataState: SharedFlow<Loadable<FullProfileData, ProfileDbError>> = fullProfileDataFlows.fullProfileDataStateFlow

    private fun reloadFullProfileData() {
        fullProfileDataFlows.dbFlow.value = db to Any()
    }

    suspend fun <T> write(block: DatabaseTransaction<T>): ProfileDbResult<T> {
        return db.write(block).also {
            if (it is Result.Value) {
                reloadFullProfileData()
            }
        }
    }

    suspend fun delete(appDb: AppData) {
        db.unlink()
        withContext(Dispatchers.IO) {
            appDb.profileQueries.delete(profile.id)
        }
    }
}

fun profilesInfoFlow(
    profiles: Flow<List<Profile>>,
    currentProfileId: Flow<Long?>,
    coroutineScope: CoroutineScope,
): Flow<ProfilesInfo> {
    val profilesMap: Flow<Map<Long, ProfileInfo>> = profiles.collectWithPrevious { oldProfilesMap, profiles ->
        require(profiles.isNotEmpty()) { "Empty profile list" }

        profiles.associate { profile ->
            val oldProfileData = oldProfilesMap?.get(profile.id)
            val db = when (oldProfileData != null && oldProfileData.profile.canReuseProfileDbInstance(profile)) {
                true -> oldProfileData.db
                false -> profile.db()
            }
            val fullProfileDataFlows = oldProfileData?.fullProfileDataFlows ?: FullProfileDataFlows(coroutineScope, initialDb = db)
            profile.id to ProfileInfo(profile, db, fullProfileDataFlows)
        }
    }

    return profilesMap.combine(currentProfileId) { profilesMap, currentProfileId ->
        ProfilesInfo(
            profiles = profilesMap,
            current = profilesMap[currentProfileId] ?: profilesMap.values.first(),
        )
    }
}

private fun Profile.canReuseProfileDbInstance(newProfile: Profile): Boolean {
    return id == newProfile.id && externalFileUri == newProfile.externalFileUri
}

private fun Profile.db(): ProfileDb {
    val profile = this
    return when (profile.externalFileUri) {
        null -> ManagedProfileDb(profile.id)
        else -> ExternalProfileDb.of(
            profileId = profile.id,
            externalFileUri = profile.externalFileUri,
        )
    }
}
