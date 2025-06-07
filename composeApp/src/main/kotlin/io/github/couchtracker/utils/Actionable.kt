package io.github.couchtracker.utils

/**
 * Extension of [Loadable] that also allows an [Idle] state.
 */
sealed interface Actionable<out T, out E> {
    data object Idle : Actionable<Nothing, Nothing>
}
