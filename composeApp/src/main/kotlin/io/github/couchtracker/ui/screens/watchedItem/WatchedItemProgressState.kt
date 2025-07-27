package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import io.github.couchtracker.db.profile.WatchedItem
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemWrapper
import io.github.couchtracker.db.profile.toType
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.rememberTickingValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface WatchedItemProgressState {

    sealed interface InProgress : WatchedItemProgressState


    data class StartingInTheFuture(val startsAt: Instant) : InProgress

    data class CurrentlyWatching(val startedAt: Instant, val endsAt: Instant?) : InProgress

    data object Watched : WatchedItemProgressState

    data object Unknown : WatchedItemProgressState
}

@Composable
fun rememberWatchedItemProgressState(
    watchedItem: WatchedItemWrapper,
    mediaRuntime: Duration?,
): State<WatchedItemProgressState> {
    val watchAt = watchedItem.watchAt
    val startedAt = remember(watchAt) {
        // If the watchAt is not precise enough (e.g. doesn't have a time), it doesn't make much sense to show a progress because it would
        // be too imprecise (e.g. start at midnight).
        if (watchAt != null && watchAt.local is PartialDateTime.Local.DateTime) {
            when (watchAt) {
                is PartialDateTime.Local -> watchAt.toInstant(TimeZone.currentSystemDefault())
                is PartialDateTime.Zoned -> watchAt.toInstant()
            }
        } else {
            null
        }
    }

    return rememberTickingValue(startedAt, mediaRuntime) {
        // If non-resting state, we recompute at most every 30 seconds to avoid any error from long delay() calls
        val elapsed = startedAt?.elapsed()
        val approximateMediaRuntime = mediaRuntime ?: watchedItem.itemId.toType().fallbackRuntime
        if (elapsed == null) {
            TickingValue(
                value = WatchedItemProgressState.Unknown,
                nextTick = null,
            )
        } else if (elapsed.isNegative()) {
            TickingValue(
                value = WatchedItemProgressState.StartingInTheFuture(startsAt = startedAt),
                nextTick = elapsed.absoluteValue.coerceAtMost(30.seconds),
            )
        } else if (elapsed < approximateMediaRuntime) {
            TickingValue(
                value = WatchedItemProgressState.CurrentlyWatching(startedAt = startedAt, endsAt = mediaRuntime?.let { startedAt + it }),
                nextTick = (approximateMediaRuntime - elapsed).coerceAtMost(30.seconds),
            )
        } else {
            TickingValue(
                value = WatchedItemProgressState.Watched,
                nextTick = null,
            )
        }
    }
}

private fun Instant.elapsed(): Duration = Clock.System.now() - this
