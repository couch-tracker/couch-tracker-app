package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Represents an icon that can be drawn in the app.
 *
 * This could be an icon bundled in the app or something externally-provided.
 */
sealed interface Text {

    @Composable
    fun string(): String

    data class Literal(val literal: String) : Text {
        @Composable
        override fun string() = literal
    }

    data class Resource(val resource: StringResource) : Text {
        @Composable
        override fun string() = stringResource(resource)
    }
}

fun String.toText() = Text.Literal(this)
fun StringResource.toText() = Text.Resource(this)
