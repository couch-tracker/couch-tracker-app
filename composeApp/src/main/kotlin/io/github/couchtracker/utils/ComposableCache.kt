package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf

/**
 * A cache that keeps track of elements as long as they are in the composition.
 */
class ComposableCache<V> {
    private val cache = mutableStateMapOf<Any, V>()
    val elements: Collection<V> get() = cache.values

    /**
     * Adds [element] to the cache.
     * The element will be automatically removed when it changes,
     * or this Compose node exits the composition.
     */
    @Composable
    fun put(element: V) {
        DisposableEffect(element) {
            val key = Any()
            cache[key] = element
            onDispose { cache.remove(key) }
        }
    }
}
