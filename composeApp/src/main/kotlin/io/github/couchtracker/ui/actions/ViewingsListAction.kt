package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.WatchableExternalId
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems

@Composable
fun ViewingsListAction(id: WatchableExternalId): Action {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    val watchCount = fullProfileData.watchedItems.count { it.itemId == id }

    return Action(
        name = stringResource(R.string.viewings_list),
        icon = Icons.Default.Checklist,
        badgeLabel = if (watchCount > 0) watchCount.toString() else null,
        onClick = {
            navController.navigateToWatchedItems(id)
        },
    )
}
