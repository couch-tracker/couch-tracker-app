package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.intl.errorMessage
import io.github.couchtracker.utils.ProfileDbActionState
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.str

@Composable
fun ProfileDbErrorDialog(actionState: ProfileDbActionState<*>) {
    val state = actionState.current

    if (state is Result.Error) {
        val detailedMessage = state.error.errorMessage()
        val exception = if (state.error is ProfileDbError.WithException) state.error.exception else null
        AlertDialog(
            icon = { Icon(Icons.Default.Error, contentDescription = null) },
            title = { Text(R.string.save_failed.str()) },
            text = {
                Column {
                    Text(R.string.save_failed_message.str(detailedMessage))

                    if (exception != null) {
                        Spacer(Modifier.size(8.dp))
                        ExceptionStackTrace(exception, Modifier.weight(1f))
                    }
                }
            },
            onDismissRequest = { actionState.reset() },
            confirmButton = {
                TextButton(
                    onClick = { actionState.reset() },
                    content = { Text(android.R.string.ok.str()) },
                )
            },
        )
    }
}
