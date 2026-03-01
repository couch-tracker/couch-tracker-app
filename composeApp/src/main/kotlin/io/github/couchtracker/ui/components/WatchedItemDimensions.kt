package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelection
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.toIcon

/**
 * Lists in a [FlowRow] all the given [WatchedItemDimensionSelection] using the icon if it exists, or the name.
 */
@Composable
fun WatchedItemDimensionSelections(
    selections: List<WatchedItemDimensionSelection<*>>,
    emptyPlaceholder: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infoList = remember(selections) { selections.getDimensionInfoList() }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (infoList.isEmpty()) {
            emptyPlaceholder()
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

            is WatchedItemDimensionSelection.FreeText -> when {
                selection.isEmpty() -> emptyList()
                else -> listOf(WatchedItemDimensionInfo.Icon(Icons.AutoMirrored.Outlined.Article.toIcon()))
            }
        }
    }
}
