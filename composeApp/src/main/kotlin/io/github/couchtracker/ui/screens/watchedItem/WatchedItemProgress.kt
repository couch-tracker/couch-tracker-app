package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ExperimentalTickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationOptions
import dev.mmauro.datetimepolyglot.localizers.transformation.map
import dev.mmauro.datetimepolyglot.map
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.intl.datetime.localize
import io.github.couchtracker.intl.datetime.rememberLocalizer
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

@Composable
fun WatchedItemProgress(state: WatchedItemProgressState, type: WatchedItemType, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val transition = updateTransition(state)
        transition.AnimatedContent(
            contentKey = { it is WatchedItemProgressState.InProgress },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { progressState ->
            if (progressState is WatchedItemProgressState.InProgress) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    when (progressState) {
                        is WatchedItemProgressState.CurrentlyWatching -> {
                            WatchedItemStartedFor(progressState, type)
                            WatchedItemFinishEta(progressState, type)
                        }

                        is WatchedItemProgressState.StartingInTheFuture -> {
                            WatchedItemStartEta(progressState, type)
                        }
                    }
                }
            }
        }
        transition.AnimatedContent(
            contentKey = { state is WatchedItemProgressState.CurrentlyWatching },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { progressState ->
            if (progressState is WatchedItemProgressState.CurrentlyWatching) {
                WatchedItemProgressIndicator(progressState)
            }
        }
    }
}

@OptIn(ExperimentalTickingDurationLocalizer::class)
private val TICKING_DURATION_OPTIONS = TickingDurationOptions(
    abs = true,
    durationOptions = DurationOptions(
        style = DurationStyle.SHORT,
        minUnit = DurationUnit.MINUTES,
    ),
)

@OptIn(ExperimentalTickingDurationLocalizer::class)
@Composable
private fun WatchedItemText(
    value: Instant,
    vararg keys: Any,
    map: (Duration, TickingValue<String>) -> TickingValue<Text>,
) {
    val localizer = rememberLocalizer(TICKING_DURATION_OPTIONS, *keys) {
        it.map { value, localized, reference ->
            val duration = value - reference.value
            map(duration, localized)
        }
    }
    val localized by localizer.localize(value)
    Text(localized.string())
}

@Composable
private fun WatchedItemStartedFor(progressState: WatchedItemProgressState.CurrentlyWatching, type: WatchedItemType) {
    return WatchedItemText(progressState.startedAt, type) { duration, localized ->
        if (duration in -1.minutes..0.seconds) {
            TickingValue(type.startedLessThenAMinuteAgo(), nextTick = 1.minutes + duration)
        } else {
            localized.map { type.startedXAgo(it) }
        }
    }
}

@Composable
private fun WatchedItemStartEta(progressState: WatchedItemProgressState.StartingInTheFuture, type: WatchedItemType) {
    return WatchedItemText(progressState.startsAt, type) { duration, localized ->
        if (duration in 0.seconds..<1.minutes) {
            TickingValue(type.startingInLessThenAMinute(), nextTick = 1.minutes - duration)
        } else {
            localized.map { type.startingInX(it) }
        }
    }
}

@Composable
private fun WatchedItemFinishEta(progressState: WatchedItemProgressState.CurrentlyWatching, type: WatchedItemType) {
    if (progressState.endsAt != null) {
        WatchedItemText(progressState.endsAt, type) { duration, localized ->
            if (duration in 0.seconds..<1.minutes) {
                TickingValue(type.lessThanAMinuteLeft(), nextTick = 1.minutes - duration)
            } else {
                localized.map { type.xLeft(it) }
            }
        }
    }
}

@Composable
private fun WatchedItemProgressIndicator(progressState: WatchedItemProgressState.CurrentlyWatching) {
    if (progressState.endsAt == null) {
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearWavyProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            amplitude = { 1f }, // TODO amplitude getting lower with progress
            progress = {
                val runtime = progressState.endsAt - progressState.startedAt
                val elapsed = Clock.System.now() - progressState.startedAt
                (elapsed / runtime).toFloat()
            },
        )
    }
}

private fun WatchedItemType.startingInLessThenAMinute() = Text.Resource(
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_starting_in_less_than_a_minute_ago
        WatchedItemType.EPISODE -> R.string.episode_progress_starting_in_less_than_a_minute_ago
    },
)

private fun WatchedItemType.startingInX(value: String) = Text.Lambda {
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_starting_in_x.str(value)
        WatchedItemType.EPISODE -> R.string.episode_progress_starting_in_x.str(value)
    }
}

private fun WatchedItemType.startedLessThenAMinuteAgo() = Text.Resource(
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_started_less_than_a_minute_ago
        WatchedItemType.EPISODE -> R.string.episode_progress_started_less_than_a_minute_ago
    },
)

private fun WatchedItemType.startedXAgo(value: String) = Text.Lambda {
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_started_x_ago.str(value)
        WatchedItemType.EPISODE -> R.string.episode_progress_started_x_ago.str(value)
    }
}

private fun WatchedItemType.lessThanAMinuteLeft() = Text.Resource(
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_less_than_a_minute_left
        WatchedItemType.EPISODE -> R.string.episode_progress_less_than_a_minute_left
    },
)

private fun WatchedItemType.xLeft(value: String) = Text.Lambda {
    when (this) {
        WatchedItemType.MOVIE -> R.string.movie_progress_x_left.str(value)
        WatchedItemType.EPISODE -> R.string.episode_progress_x_left.str(value)
    }
}
