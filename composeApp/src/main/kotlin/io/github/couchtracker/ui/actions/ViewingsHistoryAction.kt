package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.WatchableExternalId
import io.github.couchtracker.ui.screens.watchedItem.navigateToWatchedItems

@Composable
fun ViewingsHistoryAction(id: WatchableExternalId): Action {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    val watchCount = when (id) {
        is ExternalMovieId -> {
            fullProfileData.watchedItemsByMovie[id].orEmpty().size
        }
        is ExternalEpisodeId -> {
            fullProfileData.watchedItemsByEpisode[id].orEmpty().size
        }
    }

    return Action(
        name = stringResource(R.string.viewings_history),
        icon = Icons.Default.History,
        badgeLabel = if (watchCount > 0) watchCount.toString() else null,
        onClick = {
            navController.navigateToWatchedItems(id)
        },
    )
}
