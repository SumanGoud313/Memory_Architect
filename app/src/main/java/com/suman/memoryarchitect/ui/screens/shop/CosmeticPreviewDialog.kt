package com.suman.memoryarchitect.ui.screens.shop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticDefinition
import com.suman.memoryarchitect.ui.components.ConfettiBurst
import com.suman.memoryarchitect.ui.components.animatedGradientBorder
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.warmGradientBackground
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay

private const val PREVIEW_VISUAL_SIZE_DP = 96
private const val LOOP_PREVIEW_DELAY_MS = 2500L
private const val TIMER_PREVIEW_ROTATION_MS = 2400

/** A bigger, standalone look at one item before committing coins to it - reuses [CosmeticGlyph]
 * (the same renderer every Shop/Collections row already uses) at a larger size rather than
 * inventing new visual code, matching every other dialog in this app ([AlertDialog], same shape as
 * `AccountSection.kt`'s picker dialogs). One context-appropriate action: Buy when unowned, Equip
 * when owned-and-unequipped, Unequip when owned-and-equipped.
 *
 * The hero visual (see [PreviewVisual]) actually animates for the categories whose value *is*
 * motion - a static preview of an animated border/victory effect/timer ring was the cosmetic
 * audit's top Shop finding, since it can't show the one thing that differentiates the premium
 * tiers. Everything else still renders the plain static [CosmeticGlyph] - nothing to animate. */
@Composable
fun CosmeticPreviewDialog(
    definition: CosmeticDefinition,
    isOwned: Boolean,
    isEquipped: Boolean,
    onBuy: (() -> Unit)? = null,
    onEquip: (() -> Unit)? = null,
    onUnequip: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    // Shop always passes a non-null onBuy even for an already-owned item (purchase() just isn't
    // reachable for it via the confirmButton's own `!isOwned` guard below) - so "no action to
    // offer" has to be judged by the equip/unequip callbacks specifically, not onBuy's presence.
    val offersEquipAction = onEquip != null || onUnequip != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(definition.id.toDisplayName()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PreviewVisual(definition = definition)
                Text(
                    text = definition.rarity.name.lowercase().replaceFirstChar(Char::uppercase),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
                when {
                    !isOwned -> Text(
                        text = "${definition.priceCoins}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    // Owned, but this dialog instance offers no equip/unequip action (Shop's
                    // context - that lives in Collections) - say so explicitly instead of leaving
                    // the space where the price used to be blank.
                    !offersEquipAction -> Text(
                        text = stringResource(R.string.shop_owned),
                        style = MaterialTheme.typography.titleMedium,
                        color = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            when {
                !isOwned && onBuy != null -> TextButton(onClick = { onBuy(); onDismiss() }) {
                    Text(stringResource(R.string.shop_buy_for_coins, definition.priceCoins), color = MemoryArchitectColors.accentGold)
                }
                isOwned && isEquipped && onUnequip != null -> TextButton(onClick = { onUnequip(); onDismiss() }) {
                    Text(stringResource(R.string.collections_unequip), color = MemoryArchitectColors.accentGold)
                }
                isOwned && !isEquipped && onEquip != null -> TextButton(onClick = { onEquip(); onDismiss() }) {
                    Text(stringResource(R.string.collections_equip), color = MemoryArchitectColors.accentGold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/** Picks the animated preview for categories whose whole value proposition is motion; every other
 * category falls through to the plain static [CosmeticGlyph], unchanged from before. Driven by
 * [definition]'s own colors, not by what's actually equipped - a player can preview an animated
 * item's motion before ever owning or equipping it.
 *
 * `internal`, not `private` - [com.suman.memoryarchitect.ui.screens.shop.PremiumProductDetailDialog]
 * reuses this same per-category preview (rather than duplicating it) so a Premium Shop bundle's
 * Background Theme/border/timer/victory/confetti grants all get the identical live-preview
 * treatment a Coin Shop item already gets. */
@Composable
internal fun PreviewVisual(definition: CosmeticDefinition) {
    val sizeDp = PREVIEW_VISUAL_SIZE_DP.dp
    when {
        definition.category == CosmeticCategory.PROFILE_BORDER && definition.isAnimated -> {
            val colors = remember(definition.id) { CosmeticVisualCatalog.get(definition.id).gradientColors }
            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .animatedGradientBorder(shape = CircleShape, colors = colors, strokeWidthDp = 3.dp)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                CosmeticGlyph(id = definition.id, category = definition.category, isOwned = true, sizeDp = sizeDp - 28.dp)
            }
        }
        definition.category == CosmeticCategory.VICTORY_ANIMATION || definition.category == CosmeticCategory.CONFETTI_EFFECT -> {
            val colors = remember(definition.id) { CosmeticVisualCatalog.get(definition.id).gradientColors }
            val particles = rememberParticleFieldState(ambientCount = 0)
            val center = remember(sizeDp) { Offset(PREVIEW_VISUAL_SIZE_DP / 2f, PREVIEW_VISUAL_SIZE_DP / 2f) }
            LaunchedEffect(definition.id) {
                while (true) {
                    particles.confettiBurst(center, colors = colors, count = 26)
                    delay(LOOP_PREVIEW_DELAY_MS)
                }
            }
            Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
                ConfettiBurst(state = particles, modifier = Modifier.size(sizeDp))
                CosmeticGlyph(id = definition.id, category = definition.category, isOwned = true, sizeDp = sizeDp)
            }
        }
        definition.category == CosmeticCategory.TIMER_STYLE -> {
            val colors = remember(definition.id) { CosmeticVisualCatalog.get(definition.id).gradientColors }
            val infiniteTransition = rememberInfiniteTransition(label = "timerPreview")
            val rotationDegrees by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(TIMER_PREVIEW_ROTATION_MS, easing = LinearEasing)),
                label = "timerPreviewRotation",
            )
            Canvas(modifier = Modifier.size(sizeDp)) {
                rotate(rotationDegrees) {
                    val strokeWidth = size.minDimension * 0.12f
                    drawArc(
                        brush = Brush.sweepGradient(colors),
                        startAngle = -90f,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }
        }
        definition.category == CosmeticCategory.BACKGROUND_THEME -> {
            val colors = remember(definition.id) { CosmeticVisualCatalog.get(definition.id).gradientColors }
            Box(
                modifier = Modifier.size(sizeDp).clip(CircleShape).warmGradientBackground(colors),
                contentAlignment = Alignment.Center,
            ) {
                CosmeticGlyph(id = definition.id, category = definition.category, isOwned = true, sizeDp = sizeDp - 28.dp)
            }
        }
        else -> CosmeticGlyph(id = definition.id, category = definition.category, isOwned = true, sizeDp = sizeDp)
    }
}
