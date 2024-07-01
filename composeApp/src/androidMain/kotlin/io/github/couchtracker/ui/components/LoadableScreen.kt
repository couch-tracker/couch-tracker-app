@file:OptIn(ExperimentalTransitionApi::class)

package io.github.couchtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.retry_action
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str

private const val ANIMATION_DURATION_MS = 300
private val DEFAULT_INDICATOR_SIZE = 64.dp

/**
 * A Composable to use at the root of the screen, when the content can also be loading or in error.
 * This Composable is optimised to minimise the visual impact of the loading state, as it often
 * appears.
 * In particular, the loading screen:
 *  - has no background
 *  - animates its appearance (even if it's the initial state)
 *  - it shows up with a delay
 */
@Composable
fun <T : Any> LoadableScreen(
    data: Loadable<T>,
    onLoading: @Composable () -> Unit = { DefaultLoadingScreen() },
    onError: @Composable (message: String) -> Unit = { DefaultErrorScreen(it) },
    onLoaded: @Composable (value: T) -> Unit,
) {
    // Animating the appearance of the Loading state
    val state = remember {
        MutableTransitionState(
            initialState = if (data is Loadable.Loading) {
                null
            } else {
                data
            },
        )
    }
    state.targetState = data

    val transition = rememberTransition(state)
    transition.AnimatedContent(
        Modifier,
        contentKey = { it?.javaClass },
        transitionSpec = {
            val fadeInDelay = if (targetState is Loadable.Loading) {
                ANIMATION_DURATION_MS / 2
            } else {
                0
            }
            fadeIn(tween(ANIMATION_DURATION_MS, delayMillis = fadeInDelay)) togetherWith
                fadeOut(tween(ANIMATION_DURATION_MS))
        },
    ) { content ->
        when (content) {
            null -> Spacer(Modifier.fillMaxSize())
            Loadable.Loading -> onLoading()
            is Loadable.Error -> onError(content.message)
            is Loadable.Loaded -> onLoaded(content.value)
        }
    }
}

@Composable
fun DefaultLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            Modifier.size(DEFAULT_INDICATOR_SIZE),
            strokeWidth = 6.dp,
        )
    }
}

@Composable
fun DefaultErrorScreen(errorMessage: String, retry: (() -> Unit)? = null) {
    Surface {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(DEFAULT_INDICATOR_SIZE))
            Spacer(Modifier.height(16.dp))
            Text(errorMessage, textAlign = TextAlign.Center)
            if (retry != null) {
                Spacer(Modifier.height(16.dp))
                Button(retry) {
                    Text(Res.string.retry_action.str())
                }
            }
        }
    }
}
