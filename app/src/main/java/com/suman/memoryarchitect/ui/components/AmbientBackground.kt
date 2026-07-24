package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * The standard top-level-screen backdrop: warm gradient + a soft off-center glow (depth cue) +
 * two particle planes drifting at different speeds (a cheap parallax illusion — no scroll or
 * sensor input needed, see [ParticleFieldState]'s ambient-depth params). [nearParticles] is the
 * plane the caller already owns for confetti/unlock bursts; a dimmer, slower, smaller "far"
 * plane is created and owned internally, purely decorative.
 *
 * Everything here renders *behind* [content] and stays low-alpha/slow-moving by design — never
 * used inside the actual play surface (see [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScenePanel],
 * which owns its own room backdrop), only in the chrome around it.
 *
 * Reads the equipped [CosmeticCategory.BACKGROUND_THEME] cosmetic (if any) and threads its colors
 * into the gradient, glow, and both particle planes - since every top-level screen wraps in this
 * one composable, this single read is what makes a Background Theme cosmetic recolor the entire
 * app's backdrop. Falls back to the app's own default palette whenever nothing's equipped, or
 * defensively if the equipped id somehow doesn't resolve to a known cosmetic - this composable
 * must never throw, since a crash here would blank every screen in the app, not just one surface.
 */
@Composable
fun AmbientBackground(nearParticles: ParticleFieldState, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val farParticles = rememberParticleFieldState(
        ambientCount = 10,
        ambientRadiusRange = 3f..8f,
        ambientDriftScale = 0.35f,
        ambientAlpha = 0.12f,
    )
    val equippedBackgroundId = LocalEquippedCosmetics.current[CosmeticCategory.BACKGROUND_THEME]
    val themeColors = remember(equippedBackgroundId) {
        equippedBackgroundId
            ?.takeIf { AllCosmeticsCatalog.definitionOrNull(it) != null }
            ?.let { CosmeticVisualCatalog.get(it).gradientColors }
    }
    val gradientColors = themeColors ?: MemoryArchitectColors.backgroundGradient
    val glowColor = themeColors?.first() ?: MemoryArchitectColors.accentGold
    val particleColors = themeColors ?: listOf(MemoryArchitectColors.accentTerracotta, MemoryArchitectColors.accentGold, MemoryArchitectColors.accentSage)

    Box(modifier = modifier.fillMaxSize().warmGradientBackground(gradientColors).warmGlowOverlay(glowColor)) {
        ParticleField(state = farParticles, modifier = Modifier.fillMaxSize(), ambientColors = particleColors)
        ParticleField(state = nearParticles, modifier = Modifier.fillMaxSize(), ambientColors = particleColors)
        content()
    }
}
