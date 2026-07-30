package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.illustration.IdleAnimatedObject
import com.suman.memoryarchitect.ui.illustration.ObjectArtRegistry
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.launch

/**
 * Object tray shown during Reconstruct — rounded glass cards with generous spacing, a running
 * count of what's left to place, and a smooth reflow (via [Modifier.animateItem]) as chips are
 * removed. Dragging is a manual `detectDragGestures` implementation (native `View.DragEvent`
 * drag-and-drop needs API 34+, unavailable at this app's minSdk). The chip a player is actively
 * dragging is hidden here and rendered as a floating premium "ghost" by [GameplayScreen] instead,
 * so it can travel above the tray onto the scene panel and animate back if dropped with no snap
 * target — that same alpha-hide-on-drag already means a placed object never visibly "pops" out of
 * the tray, since it was already invisible the moment it was picked up.
 */
@Composable
fun GameplayTray(
    trayObjectIds: List<String>,
    draggedObjectId: String?,
    onDragStart: (objectId: String, positionInRoot: Offset, trayOriginInRoot: Offset) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isHintModeArmed: Boolean = false,
    onHintTargetSelected: (String) -> Unit = {},
    accentColor: Color = MemoryArchitectColors.accentGold,
) {
    val remainingCount = trayObjectIds.size
    Column(modifier = modifier) {
        Text(
            text = if (remainingCount == 0) {
                stringResource(R.string.gameplay_tray_all_placed)
            } else {
                pluralStringResource(R.plurals.gameplay_tray_remaining, remainingCount, remainingCount)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (remainingCount == 0) MemoryArchitectColors.accentGold else MemoryArchitectColors.textSecondary,
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(92.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            items(trayObjectIds, key = { it }) { objectId ->
                TrayChip(
                    objectId = objectId,
                    isBeingDragged = objectId == draggedObjectId,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    isHintModeArmed = isHintModeArmed,
                    onHintTargetSelected = onHintTargetSelected,
                    accentColor = accentColor,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun TrayChip(
    objectId: String,
    isBeingDragged: Boolean,
    onDragStart: (String, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isHintModeArmed: Boolean = false,
    onHintTargetSelected: (String) -> Unit = {},
    accentColor: Color = MemoryArchitectColors.accentGold,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isLifting by remember { mutableStateOf(false) }
    val art = remember(objectId) { ObjectArtRegistry.get(objectId) }
    val liftScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Fixed at exactly the GlassCard's own size so this chip's layout footprint inside the tray's
    // LazyRow is unchanged — the glow below is deliberately larger and overflows visually (same
    // non-clipping technique PremiumDragGhost uses above) without affecting row spacing/height.
    Box(modifier = modifier.size(68.dp), contentAlignment = Alignment.Center) {
        // A soft "just picked up" glow — the same blurred-wash technique used across the app's
        // other lift/glow moments (Mode Select's icon badges, the premium drag ghost).
        AnimatedVisibility(visible = isLifting, enter = fadeIn(tween(80)), exit = fadeOut(tween(160))) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .blur(14.dp)
                    .background(accentColor.copy(alpha = 0.4f), CircleShape),
            )
        }
        // While hint mode is armed, every chip pulses gently — this can't tell target from
        // distractor apart (that would leak scoring information for free), so it's deliberately
        // uniform across the whole tray; only the tap that follows actually costs a hint.
        if (isHintModeArmed) {
            val armedTransition = rememberInfiniteTransition(label = "trayChipHintArmed")
            val armedAlpha by armedTransition.animateFloat(
                initialValue = 0.25f, targetValue = 0.6f,
                animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "trayChipHintArmedAlpha",
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer { alpha = armedAlpha }
                    .blur(12.dp)
                    .background(accentColor, CircleShape),
            )
        }
        // A placed object never visibly "pops" out of the tray — it fades and shrinks away the
        // instant it starts being dragged (rather than an instant cut), and the real chip is
        // gone from the tray list entirely well before this alpha would ever animate back up.
        val removalAlpha by animateFloatAsState(if (isBeingDragged) 0f else 1f, tween(160), label = "trayChipRemovalAlpha")
        val removalScale by animateFloatAsState(if (isBeingDragged) 0.7f else 1f, tween(160), label = "trayChipRemovalScale")
        GlassCard(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer {
                    alpha = removalAlpha
                    scaleX = liftScale.value * removalScale
                    scaleY = liftScale.value * removalScale
                }
                .onGloballyPositioned { coordinates = it }
                .pointerInput(objectId) {
                    detectDragGestures(
                        onDragStart = { localOffset ->
                            val coords = coordinates ?: return@detectDragGestures
                            val chipCenter = coords.localToRoot(
                                Offset(coords.size.width / 2f, coords.size.height / 2f),
                            )
                            onDragStart(objectId, coords.localToRoot(localOffset), chipCenter)
                            isLifting = true
                            scope.launch {
                                liftScale.animateTo(1.1f, tween(100))
                                liftScale.animateTo(1f, tween(120))
                                isLifting = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() },
                    )
                }
                // A plain tap never crosses drag's touch slop, so detectDragGestures above never
                // consumes it — this independent tap detector sees the up event normally. Only
                // does anything while hint mode is armed; a normal tap otherwise stays a no-op,
                // exactly as it was before hints existed.
                .pointerInput(objectId, isHintModeArmed) {
                    detectTapGestures(
                        onTap = { if (isHintModeArmed) onHintTargetSelected(objectId) },
                    )
                },
        ) {
            IdleAnimatedObject(art = art, modifier = Modifier.fillMaxSize())
        }
    }
}

/** The floating chip that follows the finger while dragging from tray toward the scene panel. */
@Composable
fun TrayDragGhost(objectId: String, modifier: Modifier = Modifier) {
    val art = remember(objectId) { ObjectArtRegistry.get(objectId) }
    Box(modifier = modifier.size(68.dp)) {
        IdleAnimatedObject(art = art, modifier = Modifier.fillMaxSize())
    }
}
