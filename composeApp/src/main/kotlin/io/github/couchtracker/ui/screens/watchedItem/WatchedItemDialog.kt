@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toText
import kotlinx.coroutines.launch
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchedItemDialog(
    watchedItemType: WatchedItemType,
    approximateVideoRuntime: Duration,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val dateTimeSectionState = rememberDateTimeSectionState()

    DateTimeSectionDialog(dateTimeSectionState)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        LazyColumn {
            WatchedItemDialogScope(this).apply {
                item {
                    Text(
                        R.string.mark_movie_as_watched.str(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                dateTimeSection(dateTimeSectionState, watchedItemType, approximateVideoRuntime)
                fakeSection("Resolution", listOf("4K", "1080p", "720p"))
                fakeSection("Place", listOf("Plane", "Car", "Camionetta"))
                item {
                    TextButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismissRequest()
                                }
                            }
                        },
                    ) {
                        Text("Hide bottom sheet")
                    }
                }
            }
        }
    }
}

@JvmInline
value class WatchedItemDialogScope(val lazyListScope: LazyListScope) : LazyListScope by lazyListScope

fun WatchedItemDialogScope.section(title: Text, content: @Composable () -> Unit) {
    item {
        Column {
            Text(
                title.string(),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun WatchedItemDialogScope.fakeSection(title: String, items: List<String>) {
    section(title.toText()) {
        var selected by remember { mutableStateOf<String?>(null) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                FilterChip(
                    selected = item == selected,
                    onClick = {
                        selected = if (item == selected) {
                            null
                        } else {
                            item
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    label = { Text(item) },
                )
            }
        }
    }
}
