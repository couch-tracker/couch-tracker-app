package io.github.couchtracker.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
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
private fun RowScope.ActionsRow(actions: List<Action>) {
    for (action in actions) {
        action.companionComposable()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip { Text(action.name) }
            },
            state = rememberTooltipState(),
        ) {
            ActionBadge(action) {
                IconButton(onClick = action.onClick, enabled = action.state?.isLoading != true) {
                    DelayedActionIconLoadingIndicator(action.icon, contentDescription = action.name, action = action.state)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ActionsHorizontalFloatingToolbar(actions: Actions, expanded: Boolean) {
    val mainAction = actions.mainAction
    if (mainAction != null) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = {
                mainAction.companionComposable()
                // TODO: enabled
                ActionBadge(mainAction) {
                    FloatingToolbarDefaults.StandardFloatingActionButton(onClick = mainAction.onClick) {
                        DelayedActionIconLoadingIndicator(mainAction.icon, contentDescription = mainAction.name, action = mainAction.state)
                    }
                }
            },
            content = {
                ActionsRow(actions.otherActions)
            },
        )
    } else {
        HorizontalFloatingToolbar(
            expanded = expanded,
            content = {
                ActionsRow(actions.otherActions)
            },
        )
    }
}

@Composable
fun ActionsVerticalMenu(actions: Actions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionsVerticalMenu(listOfNotNull(actions.mainAction))
        ActionsVerticalMenu(actions.otherActions)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionsVerticalMenu(actions: List<Action>) {
    if (actions.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for ((index, action) in actions.withIndex()) {
                action.companionComposable()
                ListItem(
                    onClick = action.onClick,
                    enabled = action.state?.isLoading != true,
                    leadingContent = {
                        ActionBadge(action) {
                            DelayedActionIconLoadingIndicator(action.icon, contentDescription = null, action = action.state)
                        }
                    },
                    content = {
                        Text(action.name)
                    },
                    shapes = ListItemShapes(ItemPosition(index, actions.size)),
                )
            }
        }
    }
}

@Composable
private fun ActionBadge(action: Action, content: @Composable () -> Unit) {
    BadgedBox(
        badge = {
            if (action.badgeLabel != null) {
                Badge { Text(action.badgeLabel) }
            }
        },
    ) {
        content()
    }
}
