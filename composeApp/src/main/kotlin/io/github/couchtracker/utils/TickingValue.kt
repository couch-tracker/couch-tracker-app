package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
data class TickingValue<T>(val value: T, val nextTick: Duration?) {
    init {
        if (nextTick != null) {
            require(!nextTick.isNegative()) { "next tick cannot be negative ($nextTick)" }
            require(nextTick.isFinite()) { "next tick must be finite ($nextTick)" }
        }
    }
}

@Composable
fun <T> rememberTickingValue(
    vararg keys: Any?,
    compute: () -> TickingValue<T>,
): State<T> {
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
                delay(nextTick)
            }
        }
    }

    return state
}


@Composable
fun <T> rememberFixedTickingValue(
    tick: Duration,
    vararg keys: Any,
    compute: () -> T,
): State<T> {
    return rememberTickingValue(*keys) {
        TickingValue(value = compute(), nextTick = tick)
    }
}
