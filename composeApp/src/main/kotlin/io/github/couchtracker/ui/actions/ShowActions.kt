package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.ui.screens.show.navigateToEpisodeWatchSessions
import io.github.couchtracker.utils.str

@Composable
fun ShowActions(showId: ExternalShowId): List<Action> {
    val navController = LocalNavController.current
    return listOf(
        Action(R.string.action_lists.str(), Icons.AutoMirrored.Default.List) { /* TODO */ },
        Action(R.string.action_watch_sessions.str(), Icons.Outlined.Layers) {
            navController.navigateToEpisodeWatchSessions(showId)
        },
        BookmarkAction(showId),
    )
}
