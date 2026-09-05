package io.github.couchtracker.ui.screens.watchedItem

import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.TickingValueProvider
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import kotlinx.datetime.TimeZone
import kotlin.let
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Represents the progress state of a [WatchedItem] in the time:
 * - Starting in the future
 * - Currently watching
 * - Watched in the past
 * - Unknown or too imprecise date
 */
sealed interface WatchedItemProgressState {

    sealed interface InProgress : WatchedItemProgressState

    data class StartingInTheFuture(val startsAt: Instant, val endsAt: Instant?) : InProgress

    data class CurrentlyWatching(val startedAt: Instant, val endsAt: Instant?) : InProgress

    data class Watched(val startedAt: Instant, val endedAt: Instant?) : WatchedItemProgressState

    data object Unknown : WatchedItemProgressState
}

fun WatchedItemWrapper.watchedItemProgressState(mediaRuntime: Duration?): TickingValueProvider<WatchedItemProgressState> {
    // If the watchAt is not precise enough (e.g. doesn't have a time), it doesn't make much sense to show a progress because it would
    // be too imprecise (e.g. start at midnight).
    val startInstant = watchAt.let { watchAt ->
        if (watchAt != null && watchAt.local is PartialDateTime.Local.DateTime) {
            // FIXME: timezone change
            when (watchAt) {
                is PartialDateTime.Local -> watchAt.toInstant(TimeZone.currentSystemDefault())
                is PartialDateTime.Zoned -> watchAt.toInstant()
            }
        } else {
            null
        }
    }

    return TickingValueProvider { reference ->
        val elapsed = startInstant?.let { reference.value - it }
        val approximateMediaRuntime = mediaRuntime ?: type().fallbackRuntime
        if (elapsed == null) {
            TickingValue(
                value = WatchedItemProgressState.Unknown,
                nextTick = null,
            )
        } else {
            val endInstant = mediaRuntime?.let { startInstant + it }
            if (elapsed.isNegative()) {
                TickingValue(
                    value = WatchedItemProgressState.StartingInTheFuture(startsAt = startInstant, endsAt = endInstant),
                    nextTick = elapsed.absoluteValue,
                )
            } else if (elapsed < approximateMediaRuntime) {
                TickingValue(
                    value = WatchedItemProgressState.CurrentlyWatching(startedAt = startInstant, endsAt = endInstant),
                    nextTick = approximateMediaRuntime - elapsed,
                )
            } else {
                TickingValue(
                    value = WatchedItemProgressState.Watched(startedAt = startInstant, endedAt = endInstant),
                    nextTick = null,
                )
            }
        }
    }
}
