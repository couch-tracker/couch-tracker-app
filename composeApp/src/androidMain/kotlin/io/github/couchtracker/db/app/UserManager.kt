package io.github.couchtracker.db.app

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.couchtracker.db.user.DatabaseTransaction
import io.github.couchtracker.db.user.ExternalUserDb
import io.github.couchtracker.db.user.FullUserData
import io.github.couchtracker.db.user.ManagedUserDb
import io.github.couchtracker.db.user.UserDb
import io.github.couchtracker.db.user.UserDbResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
                val fullUserData = oldUserData?.fullUserDataState ?: mutableStateOf<FullUserDataState>(FullUserDataState.Idle)
                user.id to UserInfo(user, db, fullUserData)
            }
            return UsersInfo(
                users = users,
                currentUserId = newUsers.findOrFirst(newCurrentUserId).id,
            )
        }
    }

    inner class UserInfo(
        val user: User,
        val db: UserDb,
        val fullUserDataState: MutableState<FullUserDataState>,
    ) {
        suspend fun reloadFullUserData(coroutineContext: CoroutineContext = Dispatchers.IO) {
            if (fullUserDataState.value !is FullUserDataState.Loaded) {
                fullUserDataState.value = FullUserDataState.Loading
            }
            val result = coroutineScope.async(coroutineContext) {
                db.read { userData ->
                    FullUserData.load(db = userData)
                }
            }.await()
            fullUserDataState.value = when (result) {
                is UserDbResult.Completed.Success -> FullUserDataState.Loaded(result.result)
                is UserDbResult.AnyError -> FullUserDataState.Error(result)
            }
        }

        suspend fun ensureFullUserData() {
            when (fullUserDataState.value) {
                is FullUserDataState.Error, FullUserDataState.Idle -> reloadFullUserData()
                is FullUserDataState.Loaded, FullUserDataState.Loading -> {}
            }
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
                it.id to UserInfo(user = it, db = it.db(), fullUserDataState = mutableStateOf(FullUserDataState.Idle))
            },
            currentUserId = initialUsers.first().id,
        ),
    )
    private var usersInfo by usersInfoState

    val users get() = usersInfo.users.map { it.value }
    val current get() = usersInfo.currentUserInfo

    init {
        // Ensure data starts loading
        update(usersInfo)
    }

    // TODO shouldn't be exposed?
    fun updateListOfUsers(users: List<User>) {
        require(users.isNotEmpty())
        update(usersInfo.computeNew(newUsers = users))
    }

    fun changeLoggedUser(user: User) {
        update(usersInfo.computeNew(newCurrentUserId = user.id))
    }

    private fun update(newUsersInfo: UsersInfo) {
        val changedUser = usersInfo.currentUserId != newUsersInfo.currentUserId
        if (changedUser) {
            usersInfo.currentUserInfo.fullUserDataState.value = FullUserDataState.Idle
        }
        usersInfo = newUsersInfo
        coroutineScope.launch {
            newUsersInfo.currentUserInfo.ensureFullUserData()
        }
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
