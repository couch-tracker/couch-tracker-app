package io.github.couchtracker.ui

import androidx.compose.animation.core.tween

object AnimationDefaults {
    const val ANIMATION_DURATION_MS = 350
    val NAV_HOST_FADE_ANIMATION_SPEC = tween<Float>(ANIMATION_DURATION_MS)
}
