package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str

/**
 * Lists in a [FlowRow] all the given [WatchedItemDimensionSelection] using the icon if it exists, or the name.
 */
@Composable
fun WatchedItemDimensionSelections(selections: List<WatchedItemDimensionSelection<*>>) {
    val infoList = remember(selections) { selections.getDimensionInfoList() }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (infoList.isEmpty()) {
            Text(R.string.watched_item_no_additional_information.str(), fontStyle = FontStyle.Italic)
        } else {
            for (info in infoList) {
                when (info) {
                    is WatchedItemDimensionInfo.Icon -> Icon(
                        painter = info.icon.painter(),
                        contentDescription = null,
                        modifier = Modifier.height(16.dp),
                    )

                    is WatchedItemDimensionInfo.Text -> Text(info.text.string())
                }
            }
        }
    }
}

private sealed interface WatchedItemDimensionInfo {

    data class Icon(val icon: io.github.couchtracker.utils.Icon) : WatchedItemDimensionInfo

    data class Text(val text: io.github.couchtracker.utils.Text) : WatchedItemDimensionInfo
}

private fun List<WatchedItemDimensionSelection<*>>.getDimensionInfoList(): List<WatchedItemDimensionInfo> {
    return flatMap { selection ->
        when (selection) {
            is WatchedItemDimensionSelection.Choice -> selection.value.map { choice ->
                if (choice.icon != null) {
                    WatchedItemDimensionInfo.Icon(choice.icon.icon)
                } else {
                    WatchedItemDimensionInfo.Text(choice.name.text)
                }
            }

            is WatchedItemDimensionSelection.Language -> selection.value?.let { language ->
                listOf(WatchedItemDimensionInfo.Text(Text.Literal(language.toString())))
            }.orEmpty()

            is WatchedItemDimensionSelection.FreeText -> emptyList() // We can't display free text, no-op
        }
    }
}
