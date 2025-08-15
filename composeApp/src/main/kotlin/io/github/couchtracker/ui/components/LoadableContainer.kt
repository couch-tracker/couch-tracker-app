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
import io.github.couchtracker.utils.Result

@Composable
fun <T> LoadableContainer(
    model: Loadable.NoError<T>,
    modifier: Modifier = Modifier,
    crossFadeDurationMillis: Int = AnimationDefaults.SMALL_ANIMATION_DURATION_MS,
    placeholderDelayMillis: Int = 0,
    placeholder: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    // Animating the appearance of the Loading state
    val state = remember {
        MutableTransitionState(
            initialState = when (model) {
                is Loadable.Loading -> null
                else -> model
            },
        )
    }
    state.targetState = model
    val transition = rememberTransition(state)
    transition.AnimatedContent(
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            val fadeInDelay = when (targetState) {
                is Loadable.Loading -> placeholderDelayMillis
                else -> 0
            }
            fadeIn(tween(crossFadeDurationMillis, delayMillis = fadeInDelay)) togetherWith
                fadeOut(tween(crossFadeDurationMillis))
        },

        ) { text ->
        when (text) {
            null -> {}
            is Result.Value -> content(text.value)
            Loadable.Loading -> placeholder()
        }
    }
}

@Composable
fun <T : Any> LoadableContainer(
    model: Loadable.NoError<T>,
    modifier: Modifier = Modifier,
    crossFadeDurationMillis: Int = AnimationDefaults.SMALL_ANIMATION_DURATION_MS,
    placeholderDelayMillis: Int = 0,
    placeholderAboveContent: @Composable BoxScope.() -> Unit,
    content: @Composable (T?) -> Unit,
) {
    LoadableContainer(
        model = model,
        modifier = modifier,
        content = content,
        crossFadeDurationMillis = crossFadeDurationMillis,
        placeholderDelayMillis = placeholderDelayMillis,
        placeholder = {
            Box {
                content(null)
                placeholderAboveContent()
            }
        },
    )
}

@Composable
fun LoadableText(
    text: Loadable.NoError<String>,
    modifier: Modifier = Modifier,
    placeholderLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    LoadableContainer(
        model = text,
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
        placeholderDelayMillis = 500,
        placeholderAboveContent = {
            val color = LocalContentColor.current.copy(alpha = 0.1f)
            Canvas(modifier = Modifier.matchParentSize()) {
                val padding = 2.dp.toPx()
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
