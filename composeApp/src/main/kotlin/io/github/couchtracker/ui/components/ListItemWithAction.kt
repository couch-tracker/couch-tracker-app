package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.actions.Action
import io.github.couchtracker.ui.actions.ActionButton

private val LIST_ITEM_VERTICAL_PADDING = 10.dp
private val LIST_ITEM_HORIZONTAL_PADDING = 12.dp

@Composable
fun ListItemWithAction(
    action: Action,
    onClick: () -> Unit,
    leadingContentHeight: Dp,
    position: ItemPosition,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    content: @Composable () -> Unit,
) {
    ListItem(
        modifier = modifier,
        onClick = onClick,
        leadingContent = leadingContent?.let {
            {
                Box(
                    modifier = Modifier
                        .padding(vertical = LIST_ITEM_VERTICAL_PADDING)
                        .padding(start = LIST_ITEM_HORIZONTAL_PADDING),
                ) {
                    leadingContent()
                }
            }
        },
        content = {
            Row(
                Modifier
                    .height(intrinsicSize = IntrinsicSize.Max)
                    // Needs to be at least as tall as the poster. I cannot just "fill the height" unfortunately.
                    .heightIn(min = leadingContentHeight + LIST_ITEM_VERTICAL_PADDING * 2),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = LIST_ITEM_VERTICAL_PADDING)
                        .padding(end = LIST_ITEM_HORIZONTAL_PADDING),
                ) {
                    content()
                }
                VerticalDivider()
                ActionButton(
                    action = action,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxSize(),
                )
            }
        },
        contentPadding = PaddingValues(),
        shapes = ListItemShapes(position),
        colors = colors,
    )
}
