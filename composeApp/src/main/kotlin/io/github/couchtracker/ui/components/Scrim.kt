package io.github.couchtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun BoxScope.Scrim(visible: Boolean, color: Color, onDismissRequest: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween())
    val dismissModifier = if (visible) {
        Modifier.clickable(onClick = onDismissRequest)
    } else {
        Modifier
    }
    Canvas(
        Modifier
            .matchParentSize()
            .then(dismissModifier),
    ) {
        drawRect(color = color, alpha = alpha)
    }
}

@Composable
fun BoxWithScrim(visible: Boolean, color: Color, onDismissRequest: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box {
        content()
        Scrim(visible = visible, color = color, onDismissRequest = onDismissRequest)
    }
}
