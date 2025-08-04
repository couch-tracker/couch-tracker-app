package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private class WriteThroughAsyncMutableState<T>(
    val coroutineScope: CoroutineScope,
    val setValue: suspend (T) -> Unit,
    initialValue: T,
) : MutableState<T> {

    var asyncValue by mutableStateOf(initialValue to TimeSource.Monotonic.markNow())
    private var syncValue by mutableStateOf(asyncValue)

    override var value: T
        get() = (if (asyncValue.second > syncValue.second) asyncValue else syncValue).first
        set(value) {
            val now = TimeSource.Monotonic.markNow()
            syncValue = value to now
            coroutineScope.launch {
                setValue(value)
            }
        }

    override fun component1(): T = value

    override fun component2(): (T) -> Unit = { value = it }
}

/**
 * Exposes a [MutableState] backed by the given [asyncValue], using write-through to guarantee consistency.
 *
 * The [asyncValue] is updated via the [setValue] suspendable function. Because [setValue] is `suspend`, it may take some time for the new
 * value to propagate to [asyncValue].
 *
 * The returned [MutableState] will immediately reflect the updated value, and execute [setValue] in a newly launched coroutine on the given
 * [coroutineScope].
 */
@Composable
fun <T> rememberWriteThroughAsyncMutableState(
    asyncValue: T,
    setValue: suspend (T) -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MutableState<T> {
    val state = remember {
        WriteThroughAsyncMutableState(
            coroutineScope = coroutineScope,
            setValue = setValue,
            initialValue = asyncValue,
        )
    }

    LaunchedEffect(asyncValue) {
        state.asyncValue = asyncValue to TimeSource.Monotonic.markNow()
    }

    return state
}
