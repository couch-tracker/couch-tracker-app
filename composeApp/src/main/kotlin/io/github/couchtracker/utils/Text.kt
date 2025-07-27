package io.github.couchtracker.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource

/**
 * Represents a piece of text that can be written in the app.
 *
 * This could be a localized string bundled in the app or something externally-provided.
 */
interface Text {

    @Composable
    @ReadOnlyComposable
    fun string(): String

    data class Literal(val literal: String) : Text {
        @Composable
        @ReadOnlyComposable
        override fun string() = literal
    }

    data class Resource(@StringRes val resource: Int) : Text {
        @Composable
        @ReadOnlyComposable
        override fun string() = stringResource(resource)
    }

    data class Lambda(private val lambda: @Composable () -> String) : Text {
        @Composable
        override fun string() = lambda()
    }
}

fun String.toText() = Text.Literal(this)
