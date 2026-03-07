package io.github.couchtracker.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.couchtracker.utils.ActionState

data class Action(
    val name: String,
    val icon: ImageVector,
    val state: ActionState<*, *, *, *>? = null,
    val companionComposable: @Composable () -> Unit = {},
    val onClick: () -> Unit,
)
