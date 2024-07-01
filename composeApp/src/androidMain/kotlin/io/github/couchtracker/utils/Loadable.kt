package io.github.couchtracker.utils

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Loaded<T>(val value: T) : Loadable<T>
}
