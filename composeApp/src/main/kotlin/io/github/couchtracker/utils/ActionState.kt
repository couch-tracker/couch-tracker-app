package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Represents the state of an action.
 *
 * @property coroutineScope the coroutine scope to use to launch a new job when calling [execute].
 * @property state the current state of the action, starts out as [Actionable.Idle]
 * @property decorator decorates the block given to [execute] and returns a [Result]
 *
 * @param T the success type: what the state contains on successful cases
 * @param E the error type: what the state contains on errors
 * @param I the input type: what the [execute] lambda expects as input. It's responsibility of the [decorator] to provide this value
 * @param O the output type: what the [execute] lambda returns. It's responsibility of the [decorator] to convert this to a suitable
 * [Result]<[T], [E]>
 */
class ActionState<T, E, I, O>(
    private val coroutineScope: CoroutineScope,
    private val state: MutableState<Actionable<T, E>>,
    private val decorator: suspend (block: (I) -> O) -> Result<T, E>,
    private val onSuccess: (T) -> Unit,
) {
    val current by state

    val isInitial get() = current is Actionable.Idle
    val isLoading get() = current is Loadable.Loading

    /**
     * Resets the action to its initial state ([Actionable.Idle]).
     */
    fun reset() {
        state.value = Actionable.Idle
    }

    /**
     * Executes the given [block] of code on [coroutineScope] and decorates with [decorator].
     */
    fun execute(block: (I) -> O) {
        state.value = Loadable.Loading
        coroutineScope.launch {
            val newState = decorator(block)
            state.value = newState
            if (newState is Result.Value) {
                onSuccess(newState.value)
            }
        }
    }
}

/**
 * Creates and remember an [ActionState], useful to handle the state of an action. See its doc for more info.
 *
 * @param decorator decorates the block given to [ActionState.execute] and returns a [Result]
 * @param onSuccess callback called when an action triggered by [ActionState.execute] succeeds.
 */
@Composable
fun <T, E, I, O> rememberActionState(
    decorator: suspend ((I) -> O) -> Result<T, E>,
    onSuccess: (T) -> Unit = {},
): ActionState<T, E, I, O> {
    val coroutineScope = rememberCoroutineScope()
    val state = remember { mutableStateOf<Actionable<T, E>>(Actionable.Idle) }

    return remember(coroutineScope, state, decorator, onSuccess) {
        ActionState(
            coroutineScope = coroutineScope,
            state = state,
            decorator = decorator,
            onSuccess = onSuccess,
        )
    }
}

/**
 * Specialization of [rememberActionState] with no input.
 */
@Composable
fun <T, E, R> rememberActionState(
    mapper: (R) -> Result<T, E>,
    onSuccess: (T) -> Unit = {},
): ActionState<T, E, Unit, R> {
    return rememberActionState(
        decorator = { block -> mapper(block(Unit)) },
        onSuccess = onSuccess,
    )
}

/**
 * Specialization of [rememberActionState] with no input and whose [ActionState.execute] lambda already returns a [Result].
 */
@Composable
fun <T, E> rememberActionState(
    onSuccess: (T) -> Unit = {},
): ActionState<T, E, Unit, Result<T, E>> {
    return rememberActionState(
        decorator = { block -> block(Unit) },
        onSuccess = onSuccess,
    )
}
