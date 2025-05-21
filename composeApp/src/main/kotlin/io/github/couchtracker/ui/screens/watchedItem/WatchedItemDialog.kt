@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.WatchedItemDimensionChoice
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionWrapper
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
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
    val profileData = LocalFullProfileDataContext.current

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
                for (dimension in profileData.watchedItemDimensions) {
                    when (dimension) {
                        is WatchedItemDimensionWrapper.Choice -> choiceSection(dimension)
                        is WatchedItemDimensionWrapper.FreeText -> freeTextSection(dimension)
                    }
                }
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

fun WatchedItemDialogScope.section(title: Text, key: Any, content: @Composable () -> Unit) {
    item(key) {
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

private fun WatchedItemDialogScope.choiceSection(dimension: WatchedItemDimensionWrapper.Choice) {
    section(dimension.name.text, key = dimension.id) {
        var selected by remember { mutableStateOf<WatchedItemDimensionChoice?>(null) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(dimension.choices, key = { it.id }) { choice ->
                FilterChip(
                    selected = choice == selected,
                    onClick = {
                        selected = if (choice == selected) {
                            null
                        } else {
                            choice
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    leadingIcon = { Icon(choice.icon.icon.painter(), contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(choice.name.text.string()) },
                )
            }
        }
    }
}

private fun WatchedItemDialogScope.freeTextSection(dimension: WatchedItemDimensionWrapper.FreeText) {
    section(dimension.name.text, key = dimension.id) {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            minLines = 2,
        )
    }
}
