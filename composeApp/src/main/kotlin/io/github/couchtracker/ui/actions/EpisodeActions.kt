package io.github.couchtracker.ui.actions

import androidx.compose.runtime.Composable
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode

@Composable
fun episodeActions(
    episodeId: ExternalEpisodeId,
    showId: ExternalShowId?,
    watchedItemSheetModel: (WatchedItemSheetMode.New.Episode.WatchedSession) -> WatchedItemSheetMode.New.Episode,
): Actions {
    return Actions(
        mainAction = showId?.let { markEpisodeAsWatchedAction(it, watchedItemSheetModel) },
        otherActions = listOf(
            ViewingsListAction(episodeId),
        ),
    )
}
