package io.github.couchtracker.utils

/**
 * Returns a new map with only keys of type [R] preserved.
 */
inline fun <K, reified R : K, V> Map<K, V>.filterKeysOfInstance(): Map<R, V> {
    val original = this@filterKeysOfInstance
    return buildMap {
        for ((key, value) in original) {
            if (key is R) {
                this[key] = value
            }
        }
    }
}
