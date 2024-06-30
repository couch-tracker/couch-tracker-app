@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.couchtracker.db.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.couchtracker.db.user.DatabaseTransaction
import io.github.couchtracker.db.user.ExternalUserDb
import io.github.couchtracker.db.user.FullUserData
import io.github.couchtracker.db.user.ManagedUserDb
import io.github.couchtracker.db.user.UserDb
import io.github.couchtracker.db.user.UserDbResult
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

class UserManager(
    initialUsers: List<User>,
    private val coroutineScope: CoroutineScope,
) {
    init {
        require(initialUsers.isNotEmpty())
    }

    private inner class UsersInfo(
        val users: Map<Long, UserInfo>,
        val currentUserId: Long,
    ) {
        val currentUserInfo = users.getValue(currentUserId)

        fun computeNew(
            newUsers: List<User> = users.values.map { it.user },
            newCurrentUserId: Long = currentUserId,
        ): UsersInfo {
            val users = newUsers.associate { user ->
                val oldUserData = this.users[user.id]
                val db = when (oldUserData != null && oldUserData.user.canReuseUserDbInstance(user)) {
                    true -> oldUserData.db
                    false -> user.db()
                }
                val fullUserDataFlows = oldUserData?.fullUserDataFlows ?: FullUserDataFlows(initialDb = db)
                user.id to UserInfo(user, db, fullUserDataFlows)
            }
            return UsersInfo(
                users = users,
                currentUserId = newUsers.findOrFirst(newCurrentUserId).id,
            )
        }
    }

    inner class FullUserDataFlows(initialDb: UserDb) {
        val dbFlow = MutableStateFlow(initialDb to Any())
        val fullUserDataStateFlow = dbFlow
            .transformLatest { (db) ->
                emit(FullUserDataState.Loading)
                val result = db.read { userData ->
                    FullUserData.load(db = userData)
                }
                emit(
                    when (result) {
                        is UserDbResult.Completed.Success -> FullUserDataState.Loaded(result.result)
                        is UserDbResult.AnyError -> FullUserDataState.Error(result)
                    },
                )
            }
            .runningReduce { last, value ->
                if (value is FullUserDataState.Loading && last is FullUserDataState.Loaded) {
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

    inner class UserInfo(
        val user: User,
        val db: UserDb,
        val fullUserDataFlows: FullUserDataFlows,
    ) {

        val fullUserDataState: SharedFlow<FullUserDataState> = fullUserDataFlows.fullUserDataStateFlow

        private fun reloadFullUserData() {
            fullUserDataFlows.dbFlow.value = db to Any()
        }

        suspend fun <T> write(coroutineContext: CoroutineContext = Dispatchers.IO, block: DatabaseTransaction<T>): UserDbResult<T> {
            return coroutineScope.async(coroutineContext) {
                db.write(block).also {
                    if (it is UserDbResult.Completed.Success) {
                        reloadFullUserData()
                    }
                }
            }.await()
        }

        suspend fun delete(appDb: AppData, coroutineContext: CoroutineContext = Dispatchers.IO) {
            coroutineScope.launch(coroutineContext) {
                db.unlink()
                appDb.userQueries.delete(user.id)
            }.join()
        }
    }

    private val usersInfoState = mutableStateOf(
        UsersInfo(
            users = initialUsers.associate {
                val db = it.db()
                it.id to UserInfo(user = it, db = db, fullUserDataFlows = FullUserDataFlows(initialDb = db))
            },
            currentUserId = initialUsers.first().id,
        ),
    )
    private var usersInfo by usersInfoState

    val users: List<UserInfo> get() = usersInfo.users.map { it.value }
    val current: UserInfo get() = usersInfo.currentUserInfo

    // TODO shouldn't be exposed?
    fun updateListOfUsers(users: List<User>) {
        require(users.isNotEmpty())
        update(usersInfo.computeNew(newUsers = users))
    }

    fun changeLoggedUser(user: User) {
        update(usersInfo.computeNew(newCurrentUserId = user.id))
    }

    private fun update(newUsersInfo: UsersInfo) {
        requireMainThread()
        usersInfo = newUsersInfo
    }
}

private fun List<User>.findOrFirst(userId: Long): User {
    return find { it.id == userId } ?: first()
}

private fun User.canReuseUserDbInstance(newUser: User): Boolean {
    return id == newUser.id && externalFileUri == newUser.externalFileUri
}

private fun User.db(): UserDb {
    val user = this
    return when (user.externalFileUri) {
        null -> ManagedUserDb(user.id)
        else -> ExternalUserDb.of(
            userId = user.id,
            externalFileUri = user.externalFileUri,
        )
    }
}
