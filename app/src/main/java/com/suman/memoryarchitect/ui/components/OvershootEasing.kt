package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Easing

/**
 * Reproduces `android.view.animation.OvershootInterpolator(tension)`'s exact curve — Compose has
 * no built-in equivalent. Used for pop-in reveal animations so the ported gameplay scene
 * (see feature/gameplay) matches the old View-based bounce feel.
 */
class OvershootEasing(private val tension: Float = 1.6f) : Easing {
    override fun transform(fraction: Float): Float {
        val t = fraction - 1f
        return t * t * ((tension + 1f) * t + tension) + 1f
    }
}

val PopInOvershoot = OvershootEasing(1.6f)
