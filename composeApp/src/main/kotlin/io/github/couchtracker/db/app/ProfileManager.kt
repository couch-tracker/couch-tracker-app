@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.couchtracker.db.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.couchtracker.db.profile.DatabaseTransaction
import io.github.couchtracker.db.profile.ExternalProfileDb
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ManagedProfileDb
import io.github.couchtracker.db.profile.ProfileDb
import io.github.couchtracker.db.profile.ProfileDbResult
import io.github.couchtracker.utils.requireMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ProfileManager(
    initialProfiles: List<Profile>,
    private val coroutineScope: CoroutineScope,
) {
    init {
        require(initialProfiles.isNotEmpty())
    }

    private inner class ProfilesInfo(
        val profiles: Map<Long, ProfileInfo>,
        val currentProfileId: Long,
    ) {
        val currentProfileInfo = profiles.getValue(currentProfileId)

        fun computeNew(
            newProfiles: List<Profile> = profiles.values.map { it.profile },
            newCurrentProfileId: Long = currentProfileId,
        ): ProfilesInfo {
            val profiles = newProfiles.associate { profile ->
                val oldProfileData = this.profiles[profile.id]
                val db = when (oldProfileData != null && oldProfileData.profile.canReuseProfileDbInstance(profile)) {
                    true -> oldProfileData.db
                    false -> profile.db()
                }
                val fullProfileDataFlows = oldProfileData?.fullProfileDataFlows ?: FullProfileDataFlows(initialDb = db)
                profile.id to ProfileInfo(profile, db, fullProfileDataFlows)
            }
            return ProfilesInfo(
                profiles = profiles,
                currentProfileId = newProfiles.findOrFirst(newCurrentProfileId).id,
            )
        }
    }

    inner class FullProfileDataFlows(initialDb: ProfileDb) {
        val dbFlow = MutableStateFlow(initialDb to Any())
        val fullProfileDataStateFlow = dbFlow
            .transformLatest { (db) ->
                emit(FullProfileDataState.Loading)
                val result = db.read { profileData ->
                    FullProfileData.load(db = profileData)
                }
                emit(
                    when (result) {
                        is ProfileDbResult.Completed.Success -> FullProfileDataState.Loaded(result.result)
                        is ProfileDbResult.AnyError -> FullProfileDataState.Error(result)
                    },
                )
            }
            .runningReduce { last, value ->
                if (value is FullProfileDataState.Loading && last is FullProfileDataState.Loaded) {
                    // If value is already loaded, and I'm loading a new value, I don't want to emit a loading
                    // So I'll keep the stale value instead, waiting for the loaded data or an error
                    last
                } else {
                    value
                }
            }
            .flowOn(Dispatchers.IO)
            .shareIn(
                scope = coroutineScope,
                replay = 1,
                started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            )
    }

    inner class ProfileInfo(
        val profile: Profile,
        val db: ProfileDb,
        val fullProfileDataFlows: FullProfileDataFlows,
    ) {

        val fullProfileDataState: SharedFlow<FullProfileDataState> = fullProfileDataFlows.fullProfileDataStateFlow

        private fun reloadFullProfileData() {
            fullProfileDataFlows.dbFlow.value = db to Any()
        }

        suspend fun <T> write(coroutineContext: CoroutineContext = Dispatchers.IO, block: DatabaseTransaction<T>): ProfileDbResult<T> {
            return coroutineScope.async(coroutineContext) {
                db.write(block).also {
                    if (it is ProfileDbResult.Completed.Success) {
                        reloadFullProfileData()
                    }
                }
            }.await()
        }

        suspend fun delete(appDb: AppData, coroutineContext: CoroutineContext = Dispatchers.IO) {
            coroutineScope.launch(coroutineContext) {
                db.unlink()
                appDb.profileQueries.delete(profile.id)
            }.join()
        }
    }

    private val profilesInfoState = mutableStateOf(
        ProfilesInfo(
            profiles = initialProfiles.associate {
                val db = it.db()
                it.id to ProfileInfo(profile = it, db = db, fullProfileDataFlows = FullProfileDataFlows(initialDb = db))
            },
            currentProfileId = initialProfiles.first().id,
        ),
    )
    private var profilesInfo by profilesInfoState

    val profiles: List<ProfileInfo> get() = profilesInfo.profiles.map { it.value }
    val current: ProfileInfo get() = profilesInfo.currentProfileInfo

    // TODO shouldn't be exposed?
    fun updateListOfProfiles(profiles: List<Profile>) {
        require(profiles.isNotEmpty())
        update(profilesInfo.computeNew(newProfiles = profiles))
    }

    fun changeLoggedProfile(profile: Profile) {
        update(profilesInfo.computeNew(newCurrentProfileId = profile.id))
    }

    private fun update(newProfilesInfo: ProfilesInfo) {
        requireMainThread()
        profilesInfo = newProfilesInfo
    }
}

private fun List<Profile>.findOrFirst(profileId: Long): Profile {
    return find { it.id == profileId } ?: first()
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
