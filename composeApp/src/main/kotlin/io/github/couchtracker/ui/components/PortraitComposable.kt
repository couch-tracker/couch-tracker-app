package io.github.couchtracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.couchtracker.ui.rememberPlaceholderPainter

object PortraitComposableDefaults {
    const val POSTER_ASPECT_RATIO = 2f / 3
    val SUGGESTED_WIDTH = 120.dp
    const val POSTER_ALPHA_WITH_OVERLAY = 0.5f
}

@Composable
fun BasePortraitComposable(
    modifier: Modifier,
    image: @Composable (w: Int, h: Int) -> Unit,
    label: @Composable () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.small,
            shadowElevation = 8.dp,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PortraitComposableDefaults.POSTER_ASPECT_RATIO),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                image(this.constraints.maxWidth, this.constraints.minHeight)
            }
        }
        Spacer(Modifier.height(8.dp))
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleSmall) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                label()
            }
        }
    }
}

@Composable
fun PortraitComposable(
    modifier: Modifier,
    imageModel: @Composable ((w: Int, h: Int) -> Any?)?,
    elementTypeIcon: ImageVector,
    label: String,
    labelMinLines: Int = 1,
    onClick: (() -> Unit)?,
    overlayIcon: ImageVector? = null,
    extraContent: @Composable () -> Unit = {},
) {
    BasePortraitComposable(
        modifier,
        image = { w, h ->
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel(w, h),
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (overlayIcon != null) PortraitComposableDefaults.POSTER_ALPHA_WITH_OVERLAY else 1f)
                        .let {
                            if (onClick != null) {
                                it.clickable(onClick = onClick)
                            } else {
                                it
                            }
                        },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    fallback = if (overlayIcon == null) {
                        rememberPlaceholderPainter(elementTypeIcon, isError = false)
                    } else {
                        null
                    },
                    error = if (overlayIcon == null) {
                        rememberPlaceholderPainter(elementTypeIcon, isError = true)
                    } else {
                        null
                    },
                )
            }
            if (overlayIcon != null) {
                Icon(overlayIcon, contentDescription = null)
            }
        },
        label = {
            Text(
                label,
                textAlign = TextAlign.Center,
                minLines = labelMinLines,
            )
            extraContent()
        },
    )
}
