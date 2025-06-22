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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result

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
fun <T, E> LoadableScreen(
    data: Loadable<T, E>,
    onError: @Composable (error: E) -> Unit,
    onLoading: @Composable () -> Unit = { DefaultLoadingScreen() },
    content: @Composable (value: T) -> Unit,
) {
    // Animating the appearance of the Loading state
    val state = remember {
        MutableTransitionState(
            initialState = when (data) {
                is Loadable.Loading -> null
                else -> data
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
            is Result.Error -> onError(content.error)
            is Result.Value -> content(content.value)
        }
    }
}

@Composable
fun <T> LoadableScreen(
    data: Loadable<T, Nothing>,
    onLoading: @Composable () -> Unit = { DefaultLoadingScreen() },
    content: @Composable (value: T) -> Unit,
) {
    LoadableScreen(
        data = data,
        onError = { error("Error state shouldn't be possible") },
        onLoading = onLoading,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DefaultLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(
            modifier = Modifier.size(DEFAULT_INDICATOR_SIZE),
        )
    }
}

@Composable
fun DefaultErrorScreen(message: String, retry: (() -> Unit)? = null) {
    ErrorMessageComposable(Modifier.fillMaxSize(), message, retry)
}
