package io.github.couchtracker.ui.screens.main.show

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.UpNextListItem
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.ui.screens.main.ShowSectionViewModel.UpNextEntry
import io.github.couchtracker.utils.error.CouchTrackerLoadable
import io.github.couchtracker.utils.str

@Composable
fun UpNextTab(
    entries: CouchTrackerLoadable<List<UpNextEntry>>,
    onRetry: () -> Unit,
) {
    LoadableScreen(
        entries,
        onError = { apiError ->
            DefaultErrorScreen(
                error = apiError,
                retry = onRetry,
            )
        },
    ) { entries ->
        if (entries.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = R.string.tab_shows_up_next_empty.str(),
                details = R.string.tab_shows_up_next_empty_description.str(),
            )
        } else {
            // TODO: after marking an episode as watched, this should scroll to it
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = OverviewScreenComponents.LIST_BOTTOM_SPACE),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsWithPosition(
                    items = entries,
                    key = { _, upNextEntry -> upNextEntry.itemKey },
                ) { position, upNextEntry ->
                    UpNextListItem(upNextEntry.model, position, modifier = Modifier.animateItem())
                }
            }
        }
    }
}
