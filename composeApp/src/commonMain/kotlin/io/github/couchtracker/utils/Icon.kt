package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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

    data class Resource(val resource: DrawableResource) : Icon {
        @Composable
        override fun painter() = painterResource(resource)
    }
}

fun ImageVector.toIcon() = Icon.Vector(this)
fun DrawableResource.toIcon() = Icon.Resource(this)
