package com.suman.memoryarchitect.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Per-destination transitions — replaces the old `res/anim/nav_slide_*.xml`. Horizontal slide
 * for the Home/ModeSelect/LevelSelect/Profile shell (matches the old slide direction), fade+scale
 * entering Gameplay (fits the "a scene is about to appear" mood), simple fade for Profile.
 */
object NavAnimations {
    private const val DURATION_MS = 320

    val slideEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(DURATION_MS)) { it / 3 } + fadeIn(tween(DURATION_MS))
    }
    val slideExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(DURATION_MS)) { -it / 3 } + fadeOut(tween(DURATION_MS))
    }
    val slidePopEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideInHorizontally(animationSpec = tween(DURATION_MS)) { -it / 3 } + fadeIn(tween(DURATION_MS))
    }
    val slidePopExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutHorizontally(animationSpec = tween(DURATION_MS)) { it / 3 } + fadeOut(tween(DURATION_MS))
    }

    val gameplayEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        scaleIn(initialScale = 0.92f, animationSpec = tween(DURATION_MS)) + fadeIn(tween(DURATION_MS))
    }
    val gameplayExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        scaleOut(targetScale = 0.92f, animationSpec = tween(DURATION_MS)) + fadeOut(tween(DURATION_MS))
    }

    val fadeEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = { fadeIn(tween(DURATION_MS)) }
    val fadeExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = { fadeOut(tween(DURATION_MS)) }
}
