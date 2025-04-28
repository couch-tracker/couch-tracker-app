package io.github.couchtracker.db.app

import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ProfileDbResult

sealed interface FullProfileDataState {

    sealed interface NotLoaded : FullProfileDataState

    data object Idle : NotLoaded

    data object Loading : NotLoaded

    data class Error(val error: ProfileDbResult.AnyError) : FullProfileDataState

    data class Loaded(val data: FullProfileData) : FullProfileDataState
}
