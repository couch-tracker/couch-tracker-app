package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.ProfileDbError

typealias ProfileDbActionState<T> = ActionState<T, ProfileDbError, ProfileData, T>

@Composable
fun <T> rememberProfileDbActionState(
    onSuccess: (T) -> Unit = {},
): ProfileDbActionState<T> {
    val currentProfile = LocalProfileManagerContext.current.current

    return rememberActionState(
        decorator = { block ->
            currentProfile.write(block = block)
        },
        onSuccess = onSuccess,
    )
}
