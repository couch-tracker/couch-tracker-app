package io.github.couchtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.utils.ActionState
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DelayedActionLoadingIndicator(
    action: ActionState<*, *, *, *>,
    modifier: Modifier = Modifier,
    delay: Duration = 250.milliseconds,
    indicator: @Composable () -> Unit = {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
    },
) {
    var showProgressBar by remember { mutableStateOf(false) }

    LaunchedEffect(action.isLoading) {
        if (action.isLoading) {
            delay(delay)
            showProgressBar = true
        } else {
            showProgressBar = false
        }
    }

    AnimatedVisibility(visible = showProgressBar, modifier = modifier) {
        indicator()
    }
}
