package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * @param value the computed value
 * @param nextTick after how much time the next value should be re-computed. `null` means that no more computations are needed.
 */
data class TickingValue<out T>(val value: T, val nextTick: Duration?) {
    init {
        if (nextTick != null) {
            require(!nextTick.isNegative()) { "next tick cannot be negative ($nextTick)" }
            require(nextTick.isFinite()) { "next tick must be finite ($nextTick)" }
        }
    }
}

/**
 * Remembers a value and recomputes after the specified amount of type.
 *
 * [compute] needs to return a [TickingValue] containing the value of type [T] and the amount of time to wait before another recomputation.
 * Note: it will be called twice immediately the first time.
 *
 * @param keys forces re-trigger of computation when any of the keys change
 * @param maxWaitTime if not `null`, [compute] will be called at most after [maxWaitTime]
 * @param compute runs the computation
 * @return the latest computed value of [T]
 */
@Composable
fun <T> rememberTickingValue(
    vararg keys: Any?,
    maxWaitTime: Duration = Duration.INFINITE,
    compute: () -> TickingValue<T>,
): T {
    val state = remember { mutableStateOf(compute().value) }

    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        isRunning = true
        onPauseOrDispose { isRunning = false }
    }
    LaunchedEffect(*keys) {
        // if keys change, isFinished is cancelled
        isFinished = false
    }

    LaunchedEffect(isRunning, *keys) {
        while (isRunning && !isFinished) {
            val (newValue, nextTick) = compute()
            state.value = newValue
            if (nextTick == null) {
                isFinished = true
            } else {
                delay(nextTick.coerceAtMost(maxWaitTime))
            }
        }
    }

    return state.value
}

/**
 * Transforms the [TickingValue.value] from this instance from type [T] to [R] using [transform] and returns a new [TickingValue] with it
 * and the same [TickingValue.nextTick].
 */
fun <T, R> TickingValue<T>.map(transform: (T) -> R): TickingValue<R> {
    return TickingValue(value = transform(value), nextTick = nextTick)
}
