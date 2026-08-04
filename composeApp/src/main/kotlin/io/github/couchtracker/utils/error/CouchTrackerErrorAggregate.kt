package io.github.couchtracker.utils.error

import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text

/**
 * This class aggregates a set of errors.
 * It is biased towards returning the information of an error that `requiresUserAttention`.
 */
data class CouchTrackerErrorAggregate(
    val errors: Set<CouchTrackerError>,
) : CouchTrackerError {

    init {
        require(errors.size > 1)
        require(errors.none { it is CouchTrackerErrorAggregate })
    }

    private val mainError = errors.maxBy { it.requiresUserAttention }

    override val debugMessage: String
        get() = errors.joinToString { it.debugMessage }

    override val cause: Exception?
        get() = mainError.cause

    override val title: Text
        get() = mainError.title

    override val details: Text?
        get() = mainError.details

    override val isRetriable: Boolean
        get() = errors.any { it.isRetriable }

    override val requiresUserAttention: Boolean
        get() = errors.any { it.requiresUserAttention }
}

private fun Collection<CouchTrackerError?>.flattenInto(list: MutableCollection<CouchTrackerError>) {
    for (error in this) {
        when (error) {
            null -> {}
            is CouchTrackerErrorAggregate -> error.errors.flattenInto(list)
            else -> list.add(error)
        }
    }
}

fun Collection<CouchTrackerError?>.aggregateErrorOrNull(): CouchTrackerError? {
    val uniqueErrors = mutableSetOf<CouchTrackerError>()
    this.flattenInto(uniqueErrors)
    return when (uniqueErrors.size) {
        0 -> null
        1 -> uniqueErrors.single()
        else -> CouchTrackerErrorAggregate(uniqueErrors)
    }
}

fun Collection<CouchTrackerError>.aggregateError(): CouchTrackerError {
    return aggregateErrorOrNull() ?: error("No errors found")
}

/**
 * Transforms a list of results to a result of list.
 * Returns the list of all loaded values, the aggregated error, or Loading.
 */
fun <T> Collection<CouchTrackerLoadable<T>>.aggregateResults(): CouchTrackerLoadable<List<T>> {
    // Function optimized for iterating elements once
    val loaded = mutableListOf<T>()
    val errors = mutableListOf<CouchTrackerError>()
    var hasLoadings = false
    for (result in this) {
        when (result) {
            is Loadable.Loaded -> when (result.value) {
                is Result.Value -> {
                    if (errors.isEmpty() && !hasLoadings) {
                        loaded.add(result.value.value)
                    }
                }
                is Result.Error -> errors.add(result.value.error)
            }
            Loadable.Loading -> hasLoadings = true
        }
    }
    return if (errors.isNotEmpty()) {
        Loadable.error(errors.aggregateError())
    } else if (hasLoadings) {
        Loadable.Loading
    } else {
        Loadable.value(loaded)
    }
}
