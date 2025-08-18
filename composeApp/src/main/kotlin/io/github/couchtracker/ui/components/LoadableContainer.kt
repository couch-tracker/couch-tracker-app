package io.github.couchtracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.couchtracker.ui.AnimationDefaults
import io.github.couchtracker.utils.Loadable

private const val TEXT_LOADING_INDICATOR_DELAY_MS = 500
private const val TEXT_LOADING_INDICATOR_ALPHA = 0.1f
private val TEXT_LOADING_INDICATOR_PADDING = 2.dp

@Composable
fun <T> LoadableContainer(
    data: Loadable<T>,
    modifier: Modifier = Modifier,
    crossFadeDurationMillis: Int = AnimationDefaults.ANIMATION_DURATION_MS,
    loadingDelayMillis: Int = 0,
    onLoading: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
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
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            val fadeInDelay = when (targetState) {
                is Loadable.Loading -> loadingDelayMillis
                else -> 0
            }
            fadeIn(tween(crossFadeDurationMillis + fadeInDelay, delayMillis = fadeInDelay)) togetherWith fadeOut(
                tween(
                    crossFadeDurationMillis,
                ),
            )
        },
    ) { text ->
        when (text) {
            null -> {}
            is Loadable.Loaded -> content(text.value)
            Loadable.Loading -> onLoading()
        }
    }
}

@Composable
fun <T : Any> LoadableContainerWithOverlay(
    data: Loadable<T>,
    modifier: Modifier = Modifier,
    crossFadeDurationMillis: Int = AnimationDefaults.ANIMATION_DURATION_MS,
    placeholderDelayMillis: Int = 0,
    loadingOverlay: @Composable BoxScope.() -> Unit,
    content: @Composable (T?) -> Unit,
) {
    LoadableContainer(
        data = data,
        modifier = modifier,
        content = content,
        crossFadeDurationMillis = crossFadeDurationMillis,
        loadingDelayMillis = placeholderDelayMillis,
        onLoading = {
            Box {
                content(null)
                loadingOverlay()
            }
        },
    )
}

@Composable
fun LoadableText(
    text: Loadable<String>,
    modifier: Modifier = Modifier,
    placeholderLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    LoadableContainerWithOverlay(
        data = text,
        modifier = modifier,
        content = { text ->
            Text(
                text.orEmpty(),
                minLines = if (text == null) placeholderLines else 1,
                maxLines = maxLines,
                style = style,
                overflow = overflow,
            )
        },
        placeholderDelayMillis = TEXT_LOADING_INDICATOR_DELAY_MS,
        loadingOverlay = {
            val color = LocalContentColor.current.copy(alpha = TEXT_LOADING_INDICATOR_ALPHA)
            Canvas(modifier = Modifier.matchParentSize()) {
                val padding = TEXT_LOADING_INDICATOR_PADDING.toPx()
                val lineHeight = (this.size.height - padding) / placeholderLines - padding
                val lineSize = Size(this.size.width, lineHeight)
                repeat(placeholderLines) { line ->
                    drawRect(
                        color,
                        Offset(padding, padding + line * (lineHeight + padding)),
                        size = lineSize,
                    )
                }
            }
        },
    )
}
