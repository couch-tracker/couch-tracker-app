package io.github.couchtracker.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.components.DelayedActionIconLoadingIndicator

@Composable
fun RowScope.ActionsRow(actions: List<Action>) {
    for (action in actions) {
        action.companionComposable()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip { Text(action.name) }
            },
            state = rememberTooltipState(),
        ) {
            IconButton(onClick = action.onClick, enabled = action.state?.isLoading != true) {
                DelayedActionIconLoadingIndicator(action.icon, contentDescription = action.name, action = action.state)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ActionsHorizontalFloatingToolbar(actions: List<Action>) {
    HorizontalFloatingToolbar(
        expanded = false,
        content = {
            ActionsRow(actions)
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionsVerticalMenu(actions: List<Action>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for ((index, action) in actions.withIndex()) {
            action.companionComposable()
            ListItem(
                onClick = action.onClick,
                enabled = action.state?.isLoading != true,
                leadingContent = {
                    DelayedActionIconLoadingIndicator(action.icon, contentDescription = null, action = action.state)
                },
                content = {
                    Text(action.name)
                },
                shapes = ListItemShapes(ItemPosition(index, actions.size)),
            )
        }
    }
}
