package io.github.couchtracker.ui.screens.show

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedEpisodeSessionWrapper
import io.github.couchtracker.intl.datetime.localizedFull
import io.github.couchtracker.ui.components.ProfileDbActionDialog
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str

@Composable
fun DeleteWatchedEpisodeSessionConfirmDialog(
    watchedEpisodeSession: WatchedEpisodeSessionWrapper?,
    onDismissRequest: () -> Unit,
    onDeleted: () -> Unit = {},
) {
    val fullProfileData = LocalFullProfileDataContext.current
    val info = watchedEpisodeSession?.let { fullProfileData.getWatchedEpisodeSessionInfo(it) }
    ProfileDbActionDialog(
        state = info,
        execute = { db, info ->
            for (watchedEpisode in info.watchedEpisodes) {
                db.watchedItemQueries.delete(watchedEpisode.id)
            }
            db.watchedEpisodeSessionQueries.delete(info.watchedEpisodeSession.id)
        },
        onDismissRequest = onDismissRequest,
        onSuccess = { onDeleted() },
        confirmText = { Text(R.string.delete_action.str()) },
        confirmButtonColors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text(R.string.delete_watch_session_dialog_title.str()) },
        text = { info ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(R.string.delete_watch_session_dialog_description.str())
                Text(
                    R.plurals.delete_watch_session_dialog_viewings_detail_count.pluralStr(
                        info.watchedEpisodes.size,
                        info.watchedEpisodes.size,
                    ),
                )
                ViewingDateSection(
                    title = R.string.delete_watch_session_dialog_viewings_detail_first_viewing.str(),
                    watchAt = info.firstWatchedEpisodeAt,
                )
                ViewingDateSection(
                    title = R.string.delete_watch_session_dialog_viewings_detail_last_viewing.str(),
                    watchAt = info.lastWatchedEpisodeAt,
                )
            }
        },
    )
}

@Composable
private fun ViewingDateSection(title: String, watchAt: PartialDateTime?) {
    if (watchAt != null) {
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(watchAt.localizedFull().localize())
        }
    }
}
