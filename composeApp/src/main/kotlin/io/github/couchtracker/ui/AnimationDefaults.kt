package io.github.couchtracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween

object AnimationDefaults {
    const val ANIMATION_DURATION_MS = 350
    val NAV_HOST_FADE_ANIMATION_SPEC = tween<Float>(ANIMATION_DURATION_MS)

    object PulseAnimation {
        const val DURATION_MS = 150
        const val REPETITIONS = 2
        const val ALPHA = 0.3f

        suspend fun run(pulseAlpha: Animatable<Float, AnimationVector1D>) {
            repeat(REPETITIONS) {
                pulseAlpha.animateTo(
                    ALPHA,
                    animationSpec = tween(DURATION_MS),
                )
                pulseAlpha.animateTo(
                    0f,
                    animationSpec = tween(DURATION_MS),
                )
            }
        }
    }
}
