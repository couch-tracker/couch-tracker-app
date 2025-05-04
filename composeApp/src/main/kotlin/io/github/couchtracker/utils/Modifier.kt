package io.github.couchtracker.utils

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

fun Modifier.heightWithAspectRatio(height: Dp, aspectRatio: Float): Modifier {
    return this.size(width = aspectRatio * height, height = height)
}

fun Modifier.widthWithAspectRatio(width: Dp, aspectRatio: Float): Modifier {
    return this.size(width = width, height = width / aspectRatio)
}
