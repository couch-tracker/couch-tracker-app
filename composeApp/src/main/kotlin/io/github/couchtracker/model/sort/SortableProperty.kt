package io.github.couchtracker.model.sort

sealed interface SortableProperty<out T : Any> {
    // The item supports and has the property
    data class Known<T : Any>(val value: T) : SortableProperty<T>

    // The item supports but doesn't have the property
    data object Unknown : SortableProperty<Nothing>

    // The item supports but there's an error obtaining the property
    data object Error : SortableProperty<Nothing>

    // The item doesn't support the property
    data object Unsupported : SortableProperty<Nothing>

    companion object {
        fun <T : Any> ofLoaded(value: T?): SortableProperty<T> {
            return when (value) {
                null -> Unknown
                else -> Known(value)
            }
        }
    }
}

fun <I : Any, O : Any> SortableProperty<I>.map(f: (I) -> O): SortableProperty<O> {
    return when (this) {
        is SortableProperty.Known -> SortableProperty.Known(f(value))
        SortableProperty.Unknown -> SortableProperty.Unknown
        SortableProperty.Error -> SortableProperty.Error
        SortableProperty.Unsupported -> SortableProperty.Unsupported
    }
}

/**
 * Places present values first (sorted taking [asc] into account), then unknown, then in error, then unsupported.
 */
fun <T : Comparable<T>> compareSortableProperties(asc: Boolean, a: SortableProperty<T>, b: SortableProperty<T>): Int {
    @Suppress("MagicNumber")
    fun rank(value: SortableProperty<T>): Int = when (value) {
        is SortableProperty.Known -> 0
        SortableProperty.Unknown -> 1
        SortableProperty.Error -> 2
        SortableProperty.Unsupported -> 3
    }

    val rankComparison = rank(a).compareTo(rank(b))

    return when {
        rankComparison != 0 -> rankComparison
        a is SortableProperty.Known && b is SortableProperty.Known -> if (asc) {
            a.value.compareTo(b.value)
        } else {
            b.value.compareTo(a.value)
        }
        else -> 0
    }
}
