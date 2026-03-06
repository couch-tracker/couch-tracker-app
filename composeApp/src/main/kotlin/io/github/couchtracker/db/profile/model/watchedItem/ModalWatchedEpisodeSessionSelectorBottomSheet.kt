package io.github.couchtracker.db.profile.model.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.intl.datetime.localizedFull
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.ui.screens.show.WatchedEpisodeSessionInfo
import io.github.couchtracker.ui.screens.show.getWatchedEpisodeSessionInfo
import io.github.couchtracker.ui.screens.show.navigateToEpisodeWatchSessions
import io.github.couchtracker.ui.screens.show.sorted
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModalWatchedEpisodeSessionSelectorBottomSheet(
    state: ModalWatchedEpisodeSessionSelectorBottomSheetState,
) {
    val navController = LocalNavController.current
    val fullProfileData = LocalFullProfileDataContext.current
    ModalBottomSheet(
        sheetState = state.sheetState,
        onDismissRequest = { state.close() },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Scegli una watch session", // TODO
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val sessions = state.sessions
                if (sessions != null) {
                    val watchedEpisodeSessions = sessions
                        .map { fullProfileData.getWatchedEpisodeSessionInfo(it) }
                        .sorted()
                    itemsWithPosition(watchedEpisodeSessions) { position, session ->
                        WatchedEpisodeSessionListItem(
                            session = session,
                            position = position,
                            onClick = {
                                state.onSelected?.invoke(session.watchedEpisodeSession)
                                state.close()
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            ) {
                OutlinedButton(
                    onClick = {
                        state.close()
                        navController.navigateToEpisodeWatchSessions(state.externalShowId)
                    },
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open watch sessions") // TODO
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchedEpisodeSessionListItem(
    session: WatchedEpisodeSessionInfo,
    position: ItemPosition,
    onClick: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                session.watchedEpisodeSession.name ?: R.string.unnamed_watch_session.str(),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Column {
                Text(R.plurals.x_watched_episodes_in_this_session.pluralStr(session.watchedEpisodes.size, session.watchedEpisodes.size))
                if (session.lastWatchedEpisodeAt != null) {
                    Text(R.string.last_episode_watched_on_x.str(session.lastWatchedEpisodeAt.localizedFull().localize()))
                }
            }
        },
        shapes = ListItemShapes(position),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
class ModalWatchedEpisodeSessionSelectorBottomSheetState(
    val coroutineScope: CoroutineScope,
    val sheetState: SheetState,
) {
    var sessions: List<WatchedEpisodeSessionWrapper>? by mutableStateOf(null)
        private set
    var onSelected: ((WatchedEpisodeSessionWrapper) -> Unit)? by mutableStateOf(null)
        private set

    val showBottomSheet get() = sessions != null && onSelected != null
    val externalShowId get() = requireNotNull(sessions).map { it.showId }.toSet().single()

    fun open(sessions: List<WatchedEpisodeSessionWrapper>, onSelected: (WatchedEpisodeSessionWrapper) -> Unit) {
        this.sessions = sessions
        this.onSelected = onSelected
        coroutineScope.launch {
            sheetState.show()
        }
    }

    fun close() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            this.sessions = null
            this.onSelected = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberModalWatchedEpisodeSessionSelectorBottomSheetState(): ModalWatchedEpisodeSessionSelectorBottomSheetState {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    return remember(coroutineScope, sheetState) {
        ModalWatchedEpisodeSessionSelectorBottomSheetState(
            coroutineScope = coroutineScope,
            sheetState = sheetState,
        )
    }
}
