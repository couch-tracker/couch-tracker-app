package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.utils.ActionState
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.str

@Composable
fun CouchTrackerErrorDialog(actionState: ActionState<*, CouchTrackerError, *, *>) {
    val state = actionState.current as? Loadable ?: return
    val error = state.resultErrorOrNull()

    if (error != null) {
        CouchTrackerErrorDialog(
            error = error,
            onDismissRequest = { actionState.reset() },
        )
    }
}

@Composable
fun CouchTrackerErrorDialog(
    error: CouchTrackerError,
    onDismissRequest: () -> Unit,
) {
    val detailedMessage = error.title.string()
    val exception = if (error is ProfileDbError.WithException) error.cause else null
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
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                content = { Text(android.R.string.ok.str()) },
            )
        },
    )
}

@Composable
fun CouchTrackerErrorComposable(
    error: CouchTrackerError,
    message: String,
    modifier: Modifier = Modifier,
    afterTitle: @Composable () -> Unit = {},
    icon: ImageVector = Icons.Filled.Error,
) {
    MessageComposable(
        modifier = modifier,
        icon = icon,
        message = message,
    ) {
        Text(error.title.string(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        afterTitle()

        val exception = if (error is ProfileDbError.WithException) error.cause else null
        if (exception != null) {
            Spacer(Modifier.size(8.dp))
            ExceptionStackTrace(exception, Modifier.heightIn(max = 200.dp))
        }
    }
}
