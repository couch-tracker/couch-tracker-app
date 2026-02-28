package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.rememberProfileDbActionState
import io.github.couchtracker.utils.str

@Composable
fun <S : Any, O> ProfileDbActionDialog(
    state: S?,
    execute: (db: ProfileData, state: S) -> O,
    onDismissRequest: () -> Unit,
    onSuccess: (O) -> Unit,
    confirmText: @Composable (state: S) -> Unit,
    icon: (@Composable (state: S) -> Unit)?,
    title: (@Composable (state: S) -> Unit)?,
    text: (@Composable (state: S) -> Unit)?,
    confirmButtonColors: ButtonColors = ButtonDefaults.textButtonColors(),
    dismissText: @Composable (state: S) -> Unit = { Text(android.R.string.cancel.str()) },
    onError: (ProfileDbError) -> Unit = { onDismissRequest() },
) {
    val action = rememberProfileDbActionState<O>(
        onSuccess = {
            onDismissRequest()
            onSuccess(it)
        },
        onError = onError,
    )
    if (state != null) {
        AlertDialog(
            onDismissRequest = {
                // Cant dismiss dialog if it's loading
                if (action.current !is Loadable.Loading) {
                    onDismissRequest()
                }
            },
            icon = icon?.let { { icon(state) } },
            title = title?.let { { title(state) } },
            text = text?.let { { text(state) } },
            confirmButton = {
                TextButton(
                    enabled = !action.isLoading,
                    onClick = {
                        action.execute { db ->
                            execute(db, state)
                        }
                    },
                    colors = confirmButtonColors,
                ) {
                    DelayedActionLoadingIndicator(action = action, modifier = Modifier.padding(end = 8.dp))
                    confirmText(state)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest, enabled = !action.isLoading) {
                    dismissText(state)
                }
            },
        )
    }
    ProfileDbErrorDialog(action)
}
