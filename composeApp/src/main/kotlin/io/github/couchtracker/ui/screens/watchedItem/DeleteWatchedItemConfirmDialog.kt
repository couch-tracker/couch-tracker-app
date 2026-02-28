package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.ui.components.ProfileDbActionDialog
import io.github.couchtracker.utils.str

@Composable
fun DeleteWatchedItemConfirmDialog(
    watchedItem: WatchedItemWrapper?,
    onDismissRequest: () -> Unit,
    onDeleted: () -> Unit = {},
) {
    ProfileDbActionDialog(
        state = watchedItem,
        execute = { db, watchedItem -> watchedItem.delete(db) },
        onDismissRequest = onDismissRequest,
        onSuccess = { onDeleted() },
        confirmText = { Text(R.string.delete_action.str()) },
        confirmButtonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(R.string.delete_viewing_title.str()) },
        text = { Text(R.string.delete_viewing_description.str()) },
    )
}
