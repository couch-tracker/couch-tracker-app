package io.github.couchtracker.ui.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class Action(
    val name: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val companionComposable: @Composable () -> Unit = {},
    val onClick: () -> Unit,
)
