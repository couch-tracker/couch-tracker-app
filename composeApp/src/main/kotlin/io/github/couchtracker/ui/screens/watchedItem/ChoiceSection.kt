package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection

@Composable
fun WatchedItemSheetScope.ChoiceSection(
    selection: WatchedItemDimensionSelection.Choice,
    onSelectionChange: (WatchedItemDimensionSelection.Choice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimension = selection.dimension
    Section(dimension.name.text, modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(dimension.choices, key = { it.id }) { choice ->
                FilterChip(
                    selected = choice in selection.value,
                    onClick = { onSelectionChange(selection.toggled(choice)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    leadingIcon = {
                        if (choice.icon != null) {
                            Icon(
                                choice.icon.icon.painter(),
                                contentDescription = null,
                                modifier = Modifier.Companion.height(16.dp),
                            )
                        }
                    },
                    label = { Text(choice.name.text.string()) },
                )
            }
        }
    }
}
