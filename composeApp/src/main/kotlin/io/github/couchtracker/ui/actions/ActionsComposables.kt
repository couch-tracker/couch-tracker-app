package io.github.couchtracker.ui.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.components.DelayedActionIconLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
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
fun ActionsHorizontalFloatingToolbar(actions: Actions, expanded: Boolean) {
    val mainAction = actions.mainAction
    if (mainAction != null) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = {
                mainAction.companionComposable()
                ActionBadge(mainAction) {
                    FloatingToolbarDefaults.StandardFloatingActionButton(
                        containerColor = MaterialTheme.colorScheme.actionContainerColor(mainAction) { primaryContainer },
                        contentColor = MaterialTheme.colorScheme.actionContentColor(mainAction) { onPrimaryContainer },
                        onClick = {
                            // FloatingActionButton cannot be disabled, so we do this to avoid double clicks while loading
                            if (mainAction.state?.isLoading != true) {
                                mainAction.onClick()
                            }
                        },
                    ) {
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
fun ActionFloatingActionButton(action: Action) {
    action.companionComposable()
    ActionBadge(action) {
        FloatingActionButton(
            containerColor = MaterialTheme.colorScheme.actionContainerColor(action) { primaryContainer },
            contentColor = MaterialTheme.colorScheme.actionContentColor(action) { onPrimaryContainer },
            onClick = {
                // FloatingActionButton cannot be disabled, so we do this to avoid double clicks while loading
                if (action.state?.isLoading != true) {
                    action.onClick()
                }
            },
        ) {
            DelayedActionIconLoadingIndicator(action.icon, contentDescription = action.name, action = action.state)
        }
    }
}

@Composable
fun ActionsVerticalMenu(actions: Actions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionsVerticalMenu(listOfNotNull(actions.mainAction))
        ActionsVerticalMenu(actions.otherActions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(action: Action, modifier: Modifier = Modifier) {
    action.companionComposable()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip { Text(action.name) }
        },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.actionContainerColor(action) { Color.Transparent })
                .clickable(enabled = action.state?.isLoading != true) {
                    action.onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            val contentColor = MaterialTheme.colorScheme.actionContentColor(action) { LocalContentColor.current }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ActionBadge(action) {
                    DelayedActionIconLoadingIndicator(action.icon, contentDescription = action.name, action = action.state)
                }
            }
        }
    }
}

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
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ) { Text(action.badgeLabel) }
            }
        },
    ) {
        content()
    }
}

// onPrimaryContainer is very bright. Blending it down a bit to make it easier on the eye.
private val ColorScheme.activeContainerColor get() = lerp(onPrimaryContainer, primaryContainer, 0.25f)
private val ColorScheme.activeContentColor get() = primaryContainer

private inline fun ColorScheme.actionContainerColor(action: Action, inactiveColor: ColorScheme.() -> Color): Color {
    return if (action.active) {
        activeContainerColor
    } else {
        inactiveColor()
    }
}

private inline fun ColorScheme.actionContentColor(action: Action, inactiveColor: ColorScheme.() -> Color): Color {
    return if (action.active) {
        activeContentColor
    } else {
        inactiveColor()
    }
}
