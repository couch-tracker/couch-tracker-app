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
import io.github.couchtracker.utils.biFork
import io.github.couchtracker.utils.collectWithPrevious
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

data class ProfilesInfo(
    val profiles: Map<Long, ProfileInfo>,
    val current: ProfileInfo,
    val currentFullData: Loadable<FullProfileData, ProfileDbError>,
)

/** The inputs that will trigger a reload of the full user data */
private data class FullDataInput(
    val profileId: Long,
    val latestRevision: Any,
)

data class ProfileInfo(
    val profile: Profile,
    val db: ProfileDb,
    /** The "revision" of the current user data. New values in the flow correspond to new data in the [db] */
    val fullDataRevisionFlow: MutableStateFlow<Any>,
) {

    private fun reloadFullProfileData() {
        fullDataRevisionFlow.value = Any()
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

@OptIn(ExperimentalCoroutinesApi::class)
fun profilesInfoFlow(
    profiles: Flow<List<Profile>>,
    currentProfileId: Flow<Long?>,
): Flow<ProfilesInfo> {
    val profilesMap: Flow<Map<Long, ProfileInfo>> = profiles.collectWithPrevious { old, profiles ->
        profiles.associate { profile ->
            val oldProfileData = old?.get(profile.id)
            val db = when (oldProfileData != null && oldProfileData.profile.canReuseProfileDbInstance(profile)) {
                true -> oldProfileData.db
                false -> profile.db()
            }
            val revisionFlow = oldProfileData?.fullDataRevisionFlow ?: MutableStateFlow(Any())
            profile.id to ProfileInfo(profile, db, revisionFlow)
        }
    }
    return profilesMap
        .combine(currentProfileId) { profilesMap, currentProfileId ->
            val current = profilesMap[currentProfileId] ?: profilesMap.values.first()
            current to profilesMap
        }
        .biFork(
            fork1 = { it },
            fork2 = { infoFlow ->
                infoFlow
                    .flatMapLatest { (current, _) ->
                        current.fullDataRevisionFlow.mapLatest { revision ->
                            current.db to FullDataInput(current.profile.id, revision)
                        }
                    }
                    .fullDataFlow()
            },
            merge = { (current, profilesMap), (fullDataProfileId, fullData) ->
                if (current.profile.id == fullDataProfileId) {
                    emit(
                        ProfilesInfo(
                            profiles = profilesMap,
                            current = current,
                            currentFullData = fullData,
                        ),
                    )
                }
            },
        )
}

/**
 * Reloads the full user data when the FullDataInput changes.
 * When loading the data, the latest ProfileDb is used. However, new ProfileDbs won't trigger a full data reload.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun Flow<Pair<ProfileDb, FullDataInput>>.fullDataFlow(): Flow<Pair<Long, Loadable<FullProfileData, ProfileDbError>>> {
    return this
        .distinctUntilChangedBy { (_, fullDataInput) -> fullDataInput }
        .transformLatest { (db, fullDataInput) ->
            emit(fullDataInput.profileId to Loadable.Loading)
            val result = db.read { profileData ->
                FullProfileData.Companion.load(db = profileData)
            }
            emit(fullDataInput.profileId to result)
        }
        .flowOn(Dispatchers.IO)
        .runningReduce { (lastProfileId, last), (profileId, value) ->
            if (value is Loadable.Loading && last is Result.Value && lastProfileId == profileId) {
                // If value is already loaded, and I'm loading a new value, I don't want to emit a loading
                // So I'll keep the stale value instead, waiting for the loaded data or an error
                profileId to last
            } else {
                profileId to value
            }
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
