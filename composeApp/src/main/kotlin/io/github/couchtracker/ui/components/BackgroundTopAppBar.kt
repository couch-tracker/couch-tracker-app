@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlin.math.pow
import kotlin.math.roundToInt

private const val GRADIENT_STEPS = 16
private const val BACKGROUND_START_ALPHA = 0.4f
private val APP_BAR_MAX_ELEVATION = 16.dp
private val BACKGROUND_MAX_BLUR = 16.dp

@Composable
fun BackgroundTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    image: @Composable (Modifier) -> Unit,
    appBar: @Composable (TopAppBarColors) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    Box(
        Modifier.graphicsLayer {
            val scrollAmount = -scrollBehavior.state.contentOffset
            shadowElevation = scrollAmount.coerceAtMost(APP_BAR_MAX_ELEVATION.toPx())
            ambientShadowColor = backgroundColor
        },
    ) {
        image(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val radius = scrollBehavior.state.collapsedFraction * BACKGROUND_MAX_BLUR.toPx()
                    if (radius > 0) {
                        this.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    }
                    this.clip = true
                },
        )
        Box(Modifier.background(createGradientBrush(backgroundColor))) {
            appBar(
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun BackgroundTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    backdrop: ImageRequest?,
    appBar: @Composable (TopAppBarColors) -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    BackgroundTopAppBar(
        scrollBehavior = scrollBehavior,
        image = { modifier ->
            // The image fills the width of the AppBar, and it's vertically centered, with the constraint of not leaving gaps at the top
            AsyncImage(
                modifier = modifier,
                alignment = Alignment.CenterHorizontally + Alignment.Vertical { size: Int, space: Int ->
                    // Size of the image when the AppBar is expanded
                    val maxSpace = space - scrollBehavior.state.heightOffset
                    // Size of the image when the AppBar is collapsed
                    val minSpace = maxSpace + scrollBehavior.state.heightOffsetLimit
                    // Image alignment when the AppBar is expanded
                    val initialAlign = (maxSpace - size).coerceAtMost(0f) / 2f
                    // Image alignment when the AppBar is collapsed
                    val finalAlign = (minSpace - size).coerceAtMost(0f) / 2f
                    lerp(
                        start = initialAlign,
                        stop = finalAlign,
                        fraction = scrollBehavior.state.collapsedFraction,
                    ).roundToInt()
                },
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            )
        },
        appBar = appBar,
        backgroundColor = backgroundColor,
    )
}

private fun createGradientBrush(color: Color): Brush {
    return Brush.verticalGradient(
        List(GRADIENT_STEPS + 1) { step ->
            val progress = step.toFloat() / GRADIENT_STEPS
            // The derivative of the alpha is 0 when progress is 1.
            val progressAdjusted = 2 * progress - progress.pow(2)
            color.copy(alpha = BACKGROUND_START_ALPHA + progressAdjusted * (1f - BACKGROUND_START_ALPHA))
        },
    )
}
