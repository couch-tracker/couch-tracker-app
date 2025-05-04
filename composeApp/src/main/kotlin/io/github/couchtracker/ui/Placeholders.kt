package io.github.couchtracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun rememberPlaceholderPainter(
    vector: ImageVector,
    isError: Boolean,
): Painter {
    val iconPainter = rememberVectorPainter(vector)
    val (backgroundColor, iconColor) = when (isError) {
        true -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        false -> Color.Transparent to MaterialTheme.colorScheme.secondary
    }

    return remember(iconPainter, backgroundColor, iconColor) {
        object : Painter() {
            override val intrinsicSize = iconPainter.intrinsicSize

            override fun DrawScope.onDraw() {
                drawRect(backgroundColor)
                translate(
                    left = (size.width - intrinsicSize.width) / 2,
                    top = (size.height - intrinsicSize.height) / 2,
                ) {
                    with(iconPainter) {
                        draw(size = intrinsicSize, colorFilter = ColorFilter.tint(iconColor))
                    }
                }
            }
        }
    }
}
