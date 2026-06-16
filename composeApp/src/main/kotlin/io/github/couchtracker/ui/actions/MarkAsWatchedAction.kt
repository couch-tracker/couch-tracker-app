package io.github.couchtracker.ui.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import io.github.couchtracker.R
import io.github.couchtracker.ui.screens.watchedItem.LocalWatchedItemSheetScaffoldState
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.utils.str

@Composable
fun MarkAsWatchedAction(watchedItemSheetModel: () -> WatchedItemSheetMode.New): Action {
    val state = LocalWatchedItemSheetScaffoldState.current
    return Action(
        name = R.string.mark_movie_as_watched.str(),
        icon = Icons.Filled.Check,
        onClick = {
            state.open(watchedItemSheetModel())
        },
    )
}
