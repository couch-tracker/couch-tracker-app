package io.github.couchtracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.WatchableExternalId
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems
import io.github.couchtracker.utils.str

@Composable
fun WatchedItemsIconButton(id: WatchableExternalId) {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    val watchCount = fullProfileData.watchedItems.count { it.itemId == id }

    BadgedBox(
        badge = {
            if (watchCount > 0) {
                Badge { Text(watchCount.toString()) }
            }
        },
    ) {
        IconButton(onClick = { navController.navigateToWatchedItems(id) }) {
            Icon(Icons.Default.Checklist, contentDescription = R.string.viewing_history.str())
        }
    }
}
