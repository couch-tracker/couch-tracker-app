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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ibm.icu.text.MeasureFormat
import io.github.couchtracker.ui.rememberRelativeDurationText
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.TickingValue
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun WatchedItemProgress(state: WatchedItemProgressState, modifier: Modifier = Modifier) {
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
                            WatchedItemStartedFor(progressState)
                            WatchedItemFinishEta(progressState)
                        }

                        is WatchedItemProgressState.StartingInTheFuture -> {
                            WatchedItemStartEta(progressState)
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

@Composable
private fun WatchedItemStartedFor(progressState: WatchedItemProgressState.CurrentlyWatching) {
    val formattedText by rememberRelativeDurationText(
        instant = progressState.startedAt,
        formatMapper = { "Started $it ago" }, // TODO
        formatOverride = {
            if (it.isNegative() && it > -1.minutes) {
                // TODO
                TickingValue(Text.Literal("Started less than a minute ago"), nextTick = 1.minutes + it)
            } else {
                null
            }
        },
        formatWidth = MeasureFormat.FormatWidth.SHORT,
    )

    Text(formattedText.string())
}

@Composable
private fun WatchedItemStartEta(progressState: WatchedItemProgressState.StartingInTheFuture) {
    val formattedText by rememberRelativeDurationText(
        instant = progressState.startsAt,
        formatWidth = MeasureFormat.FormatWidth.SHORT,
        formatMapper = { "Starting in $it" }, // TODO
        formatOverride = {
            if (it.isPositive() && it < 1.minutes) {
                // TODO
                TickingValue(Text.Literal("Starting in less than a minute"), nextTick = 1.minutes - it)
            } else {
                null
            }
        },
    )

    Text(formattedText.string())
}

@Composable
private fun WatchedItemFinishEta(progressState: WatchedItemProgressState.CurrentlyWatching) {
    if (progressState.endsAt != null) {
        val formattedText by rememberRelativeDurationText(
            instant = progressState.endsAt,
            formatWidth = MeasureFormat.FormatWidth.SHORT,
            formatMapper = { "$it left" }, // TODO
            formatOverride = {
                if (it.isPositive() && it < 1.minutes) {
                    // TODO
                    TickingValue(Text.Literal("Less than a minute left"), nextTick = 1.minutes - it)
                } else {
                    null
                }
            },
        )

        Text(formattedText.string())
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WatchedItemProgressIndicator(progressState: WatchedItemProgressState.CurrentlyWatching) {
    if (progressState.endsAt == null) {
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        LinearWavyProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            amplitude = { 1f },
            progress = {
                val runtime = progressState.endsAt - progressState.startedAt
                val elapsed = Clock.System.now() - progressState.startedAt
                (elapsed / runtime).toFloat()
            },
        )
    }
}
