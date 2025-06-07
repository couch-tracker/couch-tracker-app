package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection

@Composable
fun WatchedItemSheetScope.FreeTextSection(
    enabled: Boolean,
    selection: WatchedItemDimensionSelection.FreeText,
    onSelectionChange: (WatchedItemDimensionSelection.FreeText) -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(selection.dimension.name.text, modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.Companion
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            enabled = enabled,
            value = selection.value,
            onValueChange = { onSelectionChange(selection.copy(value = it)) },
            minLines = 2,
        )
    }
}
