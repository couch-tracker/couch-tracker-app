package io.github.couchtracker.db.app

import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ProfileDb
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest

class FullProfileDataFlows(coroutineScope: CoroutineScope, initialDb: ProfileDb) {

    val dbFlow = MutableStateFlow(initialDb to Any())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fullProfileDataStateFlow: SharedFlow<Loadable<FullProfileData, ProfileDbError>> = dbFlow
        .transformLatest { (db) ->
            emit(Loadable.Loading)
            val result = db.read { profileData ->
                FullProfileData.Companion.load(db = profileData)
            }
            emit(result)
        }
        .runningReduce { last, value ->
            if (value is Loadable.Loading && last is Result.Value) {
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
            started = SharingStarted.Companion.WhileSubscribed(stopTimeoutMillis = 1000, replayExpirationMillis = 0),
        )
}
