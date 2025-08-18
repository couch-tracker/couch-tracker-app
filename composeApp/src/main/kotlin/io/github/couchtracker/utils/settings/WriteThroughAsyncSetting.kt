package io.github.couchtracker.utils.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import io.github.couchtracker.utils.rememberWriteThroughAsyncMutableState

/**
 * Instance returned by [rememberWriteThroughAsyncSetting].
 *
 * Changes to either of the exposed [MutableState]s ([actual] and [loadedSetting]) will reflect to the other one.
 *
 * @property actual a [MutableState] instance that reflects the [LoadedSetting.actual] value, with write-though consistency
 * @property current a read-only [State] instance that reflects the [LoadedSetting.current] value, with write-though consistency
 * @property loadedSetting a [MutableState] instance the reflects the whole [LoadedSetting], with write-though consistency
 */
data class WriteThroughAsyncSetting<V : D, D>(
    val actual: MutableState<V?>,
    val current: State<D>,
    val loadedSetting: MutableState<LoadedSetting<V, D>>,
)

private class WriteThroughAsyncActualMutableState<V : D, D>(
    private val mutableState: MutableState<LoadedSetting<V, D>>,
    private val defaultValue: D,
) : MutableState<V?> {

    override var value: V?
        get() = mutableState.value.actual
        set(value) {
            mutableState.value = if (value != null) {
                LoadedSetting.Value(setting = mutableState.value.setting, actual = value)
            } else {
                LoadedSetting.Default(setting = mutableState.value.setting, default = defaultValue)
            }
        }

    override operator fun component1(): V? = value

    override operator fun component2(): (V?) -> Unit = { value = it }
}

/**
 * A wrapper around [rememberWriteThroughAsyncMutableState] that allows editing a [Setting] with write-through capabilities.
 *
 * This function does not return a [MutableState] directly, but an instance of [WriteThroughAsyncSetting] that exposes instead two different
 * [MutableState]s: one exposes the actual value [V], while the other is more generic and exposes the [LoadedSetting] instance.
 * Changes to either of those [MutableState]s will reflect the other one.
 *
 * @param value the [LoadedSettingWithDefault] to get write-through capabilities of. The default is required to be loaded so that setting
 * the actual value to `null` can accurately and immediately set the current value with the default value.
 */
@Composable
fun <V : D, D> rememberWriteThroughAsyncSetting(value: LoadedSettingWithDefault<V, D>): WriteThroughAsyncSetting<V, D> {
    val mutableState = rememberWriteThroughAsyncMutableState(
        asyncValue = value.loaded,
        setValue = {
            val actualValue = it.actual
            if (actualValue != null) {
                value.setting.set(actualValue)
            } else {
                value.setting.reset()
            }
        },
    )

    val current = remember {
        derivedStateOf {
            mutableState.value.current
        }
    }

    return remember(mutableState, current) {
        WriteThroughAsyncSetting(
            actual = WriteThroughAsyncActualMutableState(mutableState = mutableState, defaultValue = value.default),
            current = current,
            loadedSetting = mutableState,
        )
    }
}
