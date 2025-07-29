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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import androidx.compose.ui.unit.dp
import com.ibm.icu.text.MeasureFormat
import io.github.couchtracker.ui.rememberRelativeDurationText
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.str
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
                WatchedItemProgressIndicator(progressState, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun WatchedItemStartedFor(progressState: WatchedItemProgressState.CurrentlyWatching, type: WatchedItemType) {
    val formattedText = rememberRelativeDurationText(progressState.startedAt, type) { duration ->
        if (duration in -1.minutes..0.seconds) {
            TickingValue(type.startedLessThenAMinuteAgo(), nextTick = 1.minutes + duration)
        } else {
            format(duration).map { type.startedXAgo(it) }
        }
    }
    Text(formattedText)
}

@Composable
private fun WatchedItemStartEta(progressState: WatchedItemProgressState.StartingInTheFuture, type: WatchedItemType) {
    val formattedText = rememberRelativeDurationText(progressState.startsAt, type) { duration ->
        if (duration in 0.seconds..<1.minutes) {
            TickingValue(type.startingInLessThenAMinute(), nextTick = 1.minutes - duration)
        } else {
            format(duration).map { type.startingInX(it) }
        }
    }
    Text(formattedText)
}

@Composable
private fun WatchedItemFinishEta(progressState: WatchedItemProgressState.CurrentlyWatching, type: WatchedItemType) {
    if (progressState.endsAt != null) {
        val formattedText = rememberRelativeDurationText(progressState.endsAt, type) { duration ->
            if (duration in 0.seconds..<1.minutes) {
                TickingValue(type.lessThanAMinuteLeft(), nextTick = 1.minutes - duration)
            } else {
                format(duration).map { type.xLeft(it) }
            }
        }
        Text(formattedText)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchedItemProgressIndicator(progressState: WatchedItemProgressState.CurrentlyWatching, modifier: Modifier) {
    if (progressState.endsAt == null) {
        LinearWavyProgressIndicator(modifier = modifier)
    } else {
        LinearWavyProgressIndicator(
            modifier = modifier,
            amplitude = { 1f },
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
