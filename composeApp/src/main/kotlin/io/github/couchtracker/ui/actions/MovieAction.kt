package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.utils.str

@Composable
fun MovieActions(movieId: ExternalMovieId, watchedItemSheetModel: () -> WatchedItemSheetMode.New): Actions {
    return Actions(
        mainAction = MarkAsWatchedAction(watchedItemSheetModel),
        otherActions = listOf(
            Action(R.string.action_lists.str(), Icons.AutoMirrored.Default.List) { /* TODO */ },
            ViewingsListAction(movieId),
            BookmarkAction(movieId),
        ),
    )
}
