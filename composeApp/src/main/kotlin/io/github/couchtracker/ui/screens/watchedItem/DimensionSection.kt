package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.runtime.Composable
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection

@Composable
fun WatchedItemSelectionsScope.DimensionSection(
    enabled: Boolean,
    selection: WatchedItemDimensionSelection<*>,
    mediaLanguages: () -> List<Bcp47Language>,
    onSelectionChange: (WatchedItemDimensionSelection<*>) -> Unit,
) {
    when (selection) {
        is WatchedItemDimensionSelection.Choice -> ChoiceSection(
            enabled = enabled,
            selection = selection,
            onSelectionChange = onSelectionChange,
        )

        is WatchedItemDimensionSelection.Language -> LanguageSection(
            enabled = enabled,
            mediaLanguages = mediaLanguages(),
            selection = selection,
            onSelectionChange = onSelectionChange,
        )

        is WatchedItemDimensionSelection.FreeText -> FreeTextSection(
            enabled = enabled,
            selection = selection,
            onSelectionChange = onSelectionChange,
        )
    }
}
