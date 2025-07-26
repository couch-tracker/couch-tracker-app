package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.ui.components.DelayedActionLoadingIndicator
import io.github.couchtracker.ui.components.ProfileDbErrorDialog
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.rememberProfileDbActionState
import io.github.couchtracker.utils.str

@Composable
fun DeleteWatchedItemConfirmDialog(
    watchedItem: WatchedItemWrapper?,
    onDismissRequest: () -> Unit,
    onDeleted: () -> Unit = {},
) {
    val deleteAction = rememberProfileDbActionState<Unit>(
        onSuccess = {
            onDismissRequest()
            onDeleted()
        },
        onError = { onDismissRequest() },
    )
    if (watchedItem != null) {
        AlertDialog(
            onDismissRequest = {
                // Cant dismiss dialog if it's loading
                if (deleteAction.current !is Loadable.Loading) {
                    onDismissRequest()
                }
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(R.string.delete_viewing_title.str()) },
            text = { Text(R.string.delete_viewing_description.str()) },
            confirmButton = {
                TextButton(
                    enabled = !deleteAction.isLoading,
                    onClick = { deleteAction.execute { db -> watchedItem.delete(db) } },
                ) {
                    DelayedActionLoadingIndicator(action = deleteAction, modifier = Modifier.padding(end = 8.dp))
                    Text(R.string.delete_action.str())
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest, enabled = !deleteAction.isLoading) {
                    Text(android.R.string.cancel.str())
                }
            },
        )
    }
    ProfileDbErrorDialog(deleteAction)
}
