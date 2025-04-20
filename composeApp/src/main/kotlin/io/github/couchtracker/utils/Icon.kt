package io.github.couchtracker.utils

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

/**
 * Represents an icon that can be drawn in the app.
 *
 * This could be an icon bundled in the app or something externally-provided.
 */
sealed interface Icon {

    @Composable
    fun painter(): Painter

    data class Vector(val vector: ImageVector) : Icon {
        @Composable
        override fun painter() = rememberVectorPainter(vector)
    }

    data class Resource(@DrawableRes val resource: Int) : Icon {
        @Composable
        override fun painter() = painterResource(resource)
    }
}

fun ImageVector.toIcon() = Icon.Vector(this)
