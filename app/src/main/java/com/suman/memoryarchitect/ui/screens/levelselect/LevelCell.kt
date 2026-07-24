package com.suman.memoryarchitect.ui.screens.levelselect

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.core.common.toSecondsLabel
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.MemoryArchitectRadii

/** The diamond tint used for a flawless 3-star clear — matches [RewardId.BADGE_DIAMOND_KEY]'s
 * badge tint in RewardRow.kt so "diamond" reads consistently across Level Select and Profile. */
private val DiamondTint = Color(0xFFAEE9E8)

private enum class LevelStatus { LOCKED, CURRENT, COMPLETED, PERFECT }

private fun levelStatus(unlocked: Boolean, isFrontier: Boolean, stars: Int?): LevelStatus = when {
    !unlocked -> LevelStatus.LOCKED
    isFrontier -> LevelStatus.CURRENT
    stars == 3 -> LevelStatus.PERFECT
    else -> LevelStatus.COMPLETED
}

@Composable
fun LevelCell(
    levelNumber: Int,
    unlocked: Boolean,
    isFrontier: Boolean,
    bestTimeMs: Long?,
    stars: Int?,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(MemoryArchitectRadii.card)
    val status = levelStatus(unlocked, isFrontier, stars)

    Box(modifier = modifier.aspectRatio(1f)) {
        // "Play here next" glow — the single unlocked-but-not-yet-attempted frontier level. The
        // background itself is a fixed full-alpha color (a composition-time constant); the pulse
        // is applied as a graphicsLayer alpha multiplier instead of baking the animated alpha
        // into the Color directly, so reading the animated value stays inside the draw phase and
        // never recomposes this cell every pulse frame - the same technique HintButton/SubmitButton
        // use for their own glows.
        if (status == LevelStatus.CURRENT) {
            val transition = rememberInfiniteTransition(label = "currentGlow")
            val glowAlpha by transition.animateFloat(
                initialValue = 0.3f, targetValue = 0.55f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)),
                label = "currentGlowAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = glowAlpha }
                    .blur(10.dp)
                    .background(MemoryArchitectColors.accentGold, shape),
            )
        }
        GlassCard(
            shape = shape,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (unlocked) 1f else 0.5f }
                .then(if (status == LevelStatus.CURRENT) Modifier.idlePulse(minScale = 0.97f, maxScale = 1.02f, periodMs = 1800) else Modifier)
                .then(if (unlocked) Modifier.pressableScale(interactionSource) else Modifier)
                .then(
                    when (status) {
                        LevelStatus.CURRENT, LevelStatus.COMPLETED, LevelStatus.PERFECT ->
                            Modifier.border(2.dp, MemoryArchitectColors.accentGold, shape)
                        LevelStatus.LOCKED -> Modifier
                    },
                )
                .then(
                    if (unlocked) {
                        Modifier.clickable(interactionSource = interactionSource, indication = null) {
                            tick()
                            onClick(levelNumber)
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (unlocked) MemoryArchitectColors.accentTerracotta else MemoryArchitectColors.textTertiary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unlocked) {
                        Text(text = "$levelNumber", color = MemoryArchitectColors.bgBase, style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MemoryArchitectColors.bgBase, modifier = Modifier.size(18.dp))
                    }
                }
                if (unlocked && bestTimeMs != null) {
                    Text(
                        text = bestTimeMs.toSecondsLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (unlocked && stars != null && stars > 0) {
                    Text(
                        text = (1..3).joinToString("") { rank -> if (rank <= stars) "★" else "☆" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MemoryArchitectColors.accentGold,
                    )
                }
            }
        }
        if (status == LevelStatus.PERFECT) {
            DiamondBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
    }
}

/** Flawless-clear marker for a 3-star level — layered on top of [LevelCell]'s existing gold
 * border rather than replacing it, since a perfect level is still a completed one. */
@Composable
private fun DiamondBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .idlePulse(minScale = 0.92f, maxScale = 1.05f, periodMs = 2200)
            .background(MemoryArchitectColors.bgBase, CircleShape)
            .border(1.dp, DiamondTint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Diamond,
            contentDescription = null,
            tint = DiamondTint,
            modifier = Modifier.size(12.dp),
        )
    }
}
