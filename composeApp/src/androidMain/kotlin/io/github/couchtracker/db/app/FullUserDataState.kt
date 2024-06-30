package io.github.couchtracker.db.app

import io.github.couchtracker.db.user.FullUserData
import io.github.couchtracker.db.user.UserDbResult

sealed interface FullUserDataState {

    sealed interface NotLoaded : FullUserDataState

    data object Idle : NotLoaded

    data object Loading : NotLoaded

    data class Error(val error: UserDbResult.AnyError) : FullUserDataState

    data class Loaded(val data: FullUserData) : FullUserDataState
}
