package io.github.couchtracker.utils

import androidx.compose.runtime.saveable.Saver

inline fun <reified Savable : Any, Original> Saver<Original, Savable>.restoreAny(value: Any): Original? {
    return restore(
        value = when (value) {
            is Savable -> value
            else -> return null
        }
    )
}
