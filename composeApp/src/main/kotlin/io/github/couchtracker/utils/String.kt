package io.github.couchtracker.utils

inline fun String?.ifNullOrBlank(defaultValue: () -> String): String {
    return if (this.isNullOrBlank()) {
        defaultValue()
    } else {
        this
    }
}
