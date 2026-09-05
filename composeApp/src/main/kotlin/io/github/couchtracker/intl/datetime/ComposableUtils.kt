package io.github.couchtracker.intl.datetime

import android.content.Context
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mmauro.datetimepolyglot.ClockWrapper
import dev.mmauro.datetimepolyglot.SYSTEM_CLOCK
import dev.mmauro.datetimepolyglot.SYSTEM_TIMEZONE
import dev.mmauro.datetimepolyglot.TickingValueProvider
import dev.mmauro.datetimepolyglot.Zoned
import dev.mmauro.datetimepolyglot.collect
import dev.mmauro.datetimepolyglot.localizers.PolyglotLocalizer
import dev.mmauro.datetimepolyglot.localizers.PolyglotLocalizerOptions
import dev.mmauro.datetimepolyglot.localizers.PolyglotReferenceValueLocalizer
import dev.mmauro.datetimepolyglot.localizers.toTickingValueProvider
import io.github.couchtracker.utils.toAndroidULocale
import kotlinx.datetime.TimeZone
import kotlin.time.Duration

@Composable
fun <OPTIONS : PolyglotLocalizerOptions<LOCALIZER>, LOCALIZER : PolyglotLocalizer> rememberLocalizer(options: OPTIONS): LOCALIZER {
    return rememberLocalizer(options) { it }
}

@Composable
fun <O : PolyglotLocalizerOptions<L1>, L1 : PolyglotLocalizer, L2 : PolyglotLocalizer> rememberLocalizer(
    options: O,
    vararg keys: Any?,
    localizerFactory: (L1) -> L2,
): L2 {
    val locale = LocalLocale.current.platformLocale
    return remember(options, locale, *keys) { localizerFactory(options.localizer(locale.toAndroidULocale())) }
}

@Composable
fun <OPTIONS, LOCALIZER> rememberLocalizer(options: OPTIONS, localizerFactory: (Context, OPTIONS, ULocale) -> LOCALIZER): LOCALIZER {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    return remember(context, options, locale, localizerFactory) { localizerFactory(context, options, locale.toAndroidULocale()) }
}

@Composable
fun <T, R, LOCALIZER : PolyglotReferenceValueLocalizer<T, R>> LOCALIZER.localize(
    value: T,
    clock: ClockWrapper = SYSTEM_CLOCK.collectAsStateWithLifecycle().value,
    timeZone: TimeZone = SYSTEM_TIMEZONE.collectAsStateWithLifecycle().value,
    maxTick: Duration? = null,
): State<R> {
    val provider = remember(this, value) { toTickingValueProvider(value) }
    return provider.collectAsStateWithLifecycle(
        clock = clock,
        timeZone = timeZone,
        maxTick = maxTick,
    )
}

@Composable
fun <T> TickingValueProvider<T>.collectAsStateWithLifecycle(
    clock: ClockWrapper = SYSTEM_CLOCK.collectAsStateWithLifecycle().value,
    timeZone: TimeZone = SYSTEM_TIMEZONE.collectAsStateWithLifecycle().value,
    maxTick: Duration? = null,
): State<T> {
    val initialReference = remember(clock, timeZone) { Zoned(clock.clock.now(), timeZone) }
    val initialValue = remember(this, initialReference) { provide(initialReference) }

    val mutState = remember { mutableStateOf(initialValue.value) }

    // To avoid having to wait 1 frame for the LaunchedEffect to update to the newest value, we force the mutable state update with a
    // SideEffect here
    SideEffect(initialValue) {
        mutState.value = initialValue.value
    }

    LaunchedEffect(this, clock, timeZone, maxTick, initialReference, initialValue) {
        collect(
            clock = clock.clock,
            timeZone = timeZone,
            maxTick = maxTick,
            initialReference = initialReference,
            initialValue = initialValue,
        ) {
            mutState.value = it
        }
    }
    return mutState
}
