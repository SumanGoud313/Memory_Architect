package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.generation.LevelMirrorPolicy
import com.suman.memoryarchitect.domain.generation.LightingMoodPolicy
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.HintReveal
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import com.suman.memoryarchitect.ui.components.LightingOverlay
import com.suman.memoryarchitect.ui.components.OvershootEasing
import com.suman.memoryarchitect.ui.components.ParticleField
import com.suman.memoryarchitect.ui.components.RoomSkinOverlay
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.illustration.IdleAnimatedObject
import com.suman.memoryarchitect.ui.illustration.MAX_OBJECT_SIZE_SCALE
import com.suman.memoryarchitect.ui.illustration.ObjectArt
import com.suman.memoryarchitect.ui.illustration.ObjectArtRegistry
import com.suman.memoryarchitect.ui.illustration.RoomArt
import com.suman.memoryarchitect.ui.illustration.RoomArtRegistry
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import com.suman.memoryarchitect.ui.theme.ObjectMaterialVisualSpec
import com.suman.memoryarchitect.ui.theme.RoomSkinVisualSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PopIn = OvershootEasing(1.6f)
private const val POP_IN_DURATION_MS = 250

/** How much guaranteed, fully-settled viewing time the Memorize phase reserves at the very end,
 * on top of order-mode's sequential reveal - see [rememberRevealProgress]'s own doc for why this
 * exists: without it, the last object to appear in a high-object-count/order-mode level (e.g.
 * Classic L100, 16 objects) was popping in right as the phase's own timer expired, giving it
 * effectively zero real time on screen no matter how generous the total duration was. */
private const val MIN_SETTLE_MS = 2_500L

/** Every room's [com.suman.memoryarchitect.ui.illustration.RoomSlot.footprintFraction] was tuned
 * small (0.10-0.16 of room width) for a dense, cluttered-room look - genuinely harder to recall
 * than it needs to be. Scaling up once here, at the single shared render site every mode
 * (Classic/Practice/Daily/Weekly) already goes through, avoids re-tuning 100+ individual
 * per-slot values across 8 rooms while keeping every room's relative slot spacing intact.
 *
 * A flat [MAX_OBJECT_SIZE_SCALE] regardless of object count - this used to taper down to 1.05x
 * once a level had more than 9 objects, out of concern for visual overlap at the densest Classic
 * endgame levels. That concern is already fully retired: every room's slots were since
 * repositioned (see [com.suman.memoryarchitect.ui.illustration.autoResolveOverlaps]) specifically
 * so no two of a room's slots can ever visually overlap *at this exact scale*, independent of how
 * many are simultaneously occupied - [com.suman.memoryarchitect.ui.illustration.RoomLayoutOverlapTest]
 * is the permanent regression guard for that claim. Tapering down further than that was only ever
 * making the densest, hardest-to-read levels (L95-100, exactly where legibility matters most)
 * needlessly smaller for no remaining safety reason.
 */
private val objectSizeScale: Float = MAX_OBJECT_SIZE_SCALE

/**
 * Draws the room backdrop plus every currently visible object at its designed furniture slot
 * (see [RoomArt.slots]), with the same pop-in reveal (including order-mode's sequential
 * stagger) and distractor desaturation as before. Interactive rotate/pick-up gestures are only
 * attached during [GamePhase.RECONSTRUCT]. [dragActive] fades in a dashed outline over every
 * empty slot (hidden the rest of the time), with [highlightedSlotIndex] — the live magnetic
 * snap target, never the object's actual correct slot — glowing gold.
 */
@Composable
fun GameplayScenePanel(
    level: LevelSpec,
    phase: GamePhase,
    visibleObjects: List<SceneObjectSpec>,
    onRotate: (String) -> Unit,
    onPickUp: (String) -> Unit,
    modifier: Modifier = Modifier,
    dragActive: Boolean = false,
    highlightedSlotIndex: Int? = null,
    hintReveal: HintReveal? = null,
    onPanelPositioned: (LayoutCoordinates) -> Unit = {},
    // Premium ROOM_SKIN/OBJECT_MATERIAL cosmetics - both `null` (nothing equipped) is a true no-op,
    // reproducing today's exact look at every call site below. See RoomSkinOverlay's/
    // objectMaterialTint's own docs for the recolor techniques.
    roomSkin: RoomSkinVisualSpec? = null,
    objectMaterial: ObjectMaterialVisualSpec? = null,
) {
    val accentColor = roomSkin?.accentGlow ?: MemoryArchitectColors.accentGold
    var panelSizePx by remember { mutableStateOf(IntSize.Zero) }
    val progressMap = rememberRevealProgress(level, visibleObjects, phase)
    val mirrored = remember(level.seed) { LevelMirrorPolicy.isMirrored(level.seed) }
    val roomArt = remember(level.sceneType, mirrored) { RoomArtRegistry.get(level.sceneType).mirrored(mirrored) }
    val lightingMood = remember(level.seed) { LightingMoodPolicy.forSeed(level.seed) }
    val density = LocalDensity.current
    val occupiedIndices = remember(visibleObjects) { visibleObjects.map { it.slotIndex }.toSet() }
    // Read only inside SlotHintOverlay's own Canvas draw scope (never here in the composable
    // body via `by`) so the empty-slot dashed circles' fade in/out never forces this whole panel
    // to recompose every tick of the 220ms transition - only the overlay's own draw phase does.
    val hintAlpha = remember { Animatable(0f) }
    LaunchedEffect(dragActive) { hintAlpha.animateTo(if (dragActive) 1f else 0f, tween(220)) }

    // The spent-hint reveal keeps rendering at its last known slot through the exit fade even
    // after [hintReveal] itself has already gone back to null (the ViewModel clears it the
    // instant the 2-second window elapses) — otherwise AnimatedVisibility's exit transition would
    // have nothing left to position.
    var lastHintReveal by remember { mutableStateOf<HintReveal?>(null) }
    LaunchedEffect(hintReveal) { if (hintReveal != null) lastHintReveal = hintReveal }
    val hintSparkles = rememberParticleFieldState(ambientCount = 0)
    LaunchedEffect(hintReveal, panelSizePx) {
        val reveal = hintReveal ?: return@LaunchedEffect
        if (panelSizePx.width <= 0) return@LaunchedEffect
        val slot = roomArt.slotAt(reveal.slotIndex)
        val origin = Offset(slot.xFraction * panelSizePx.width, slot.yFraction * panelSizePx.height)
        hintSparkles.burst(origin = origin, colors = listOf(MemoryArchitectColors.accentGold, MemoryArchitectColors.textPrimary), count = 20)
    }
    // Room transition — the backdrop fades in the first time a given level composes instead of
    // appearing instantly, so a new room reads as a soft reveal rather than a hard cut.
    val backdropAlpha = remember(level.seed) { Animatable(0f) }
    LaunchedEffect(level.seed) { backdropAlpha.animateTo(1f, tween(400)) }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                panelSizePx = it.size
                onPanelPositioned(it)
            },
    ) {
        roomArt.backdrop(Modifier.fillMaxSize().graphicsLayer { alpha = backdropAlpha.value; if (mirrored) scaleX = -1f })
        LightingOverlay(mood = lightingMood, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = backdropAlpha.value }, seed = level.seed)
        RoomSkinOverlay(spec = roomSkin, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = backdropAlpha.value })

        // Always present (never gated by an `if` reading the animated value) - drawing a
        // fully-transparent overlay costs nothing extra, and Canvas only actually redraws while
        // hintAlpha.value is genuinely changing.
        SlotHintOverlay(roomArt = roomArt, occupiedIndices = occupiedIndices, highlightedSlotIndex = highlightedSlotIndex, alpha = hintAlpha, objectSizeScale = objectSizeScale, accentColor = accentColor)

        visibleObjects.forEach { obj ->
            val animatable = progressMap[obj.objectId] ?: return@forEach
            val slot = roomArt.slotAt(obj.slotIndex)
            val footprintPx = panelSizePx.width * slot.footprintFraction * objectSizeScale
            val centerPx = Offset(slot.xFraction * panelSizePx.width, slot.yFraction * panelSizePx.height)
            val sizeDp = with(density) { footprintPx.toDp() }
            val art = ObjectArtRegistry.get(obj.objectId)

            Box(
                modifier = Modifier
                    .offset { IntOffset((centerPx.x - footprintPx / 2f).toInt(), (centerPx.y - footprintPx / 2f).toInt()) }
                    .size(sizeDp),
            ) {
                // A soft grounding shadow — fades in with the object, then just quietly persists,
                // giving every placed object a sense of weight/depth against the room backdrop.
                // `animatable.value` is read inside each graphicsLayer block below (never hoisted
                // into a plain local first) so the pop-in/pulse animation only ever triggers a
                // redraw of these layers, never a recomposition of the whole visibleObjects.forEach.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(sizeDp * 0.7f)
                        .offset(y = sizeDp * 0.12f)
                        .graphicsLayer { alpha = animatable.value * 0.35f }
                        .blur(6.dp)
                        .background(roomSkin?.wallTint?.copy(alpha = 0.5f) ?: MemoryArchitectColors.bgBase, CircleShape),
                )
                // A brief confirmation pulse exactly when an object is placed during Reconstruct
                // — glow and a slight outward breathe rise and fade together as the pop-in settles
                // (half a sine cycle over `progress`), signalling "placed," never "correct":
                // whether it's in the right spot is only ever revealed at Submit, so this can't
                // leak scoring information mid-round. Deliberately the same [animatable] that
                // already drives the pop-in's bounce/scale - one driver, two coordinated effects,
                // no extra animation state to manage per object.
                if (phase == GamePhase.RECONSTRUCT) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                val pulse = sin(animatable.value.coerceIn(0f, 1f) * PI.toFloat())
                                alpha = pulse * 0.6f
                                val glowScale = 1f + pulse * 0.18f
                                scaleX = glowScale
                                scaleY = glowScale
                            }
                            .blur(10.dp)
                            .background(accentColor, CircleShape),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            rotationZ = obj.rotationDegrees.toFloat()
                            val progress = animatable.value
                            val scale = 0.6f + 0.4f * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = progress
                        }
                        .then(
                            if (phase == GamePhase.RECONSTRUCT) {
                                Modifier.combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onRotate(obj.objectId) },
                                    onLongClick = { onPickUp(obj.objectId) },
                                )
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    IdleAnimatedObject(
                        art = art,
                        modifier = Modifier
                            .size(sizeDp * 0.82f)
                            .offset(sizeDp * 0.09f, sizeDp * 0.09f)
                            .objectMaterialTint(objectMaterial)
                            .distractorDesaturation(obj.isDistractor),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = hintReveal != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
            exit = fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.85f),
        ) {
            lastHintReveal?.let { reveal ->
                val slot = roomArt.slotAt(reveal.slotIndex)
                val footprintPx = panelSizePx.width * slot.footprintFraction * objectSizeScale
                val centerPx = Offset(slot.xFraction * panelSizePx.width, slot.yFraction * panelSizePx.height)
                val sizeDp = with(density) { footprintPx.toDp() }
                HintRevealHighlight(
                    centerPx = centerPx,
                    sizeDp = sizeDp,
                    art = ObjectArtRegistry.get(reveal.objectId),
                    rotationDegrees = reveal.rotationDegrees,
                    accentColor = accentColor,
                    objectMaterial = objectMaterial,
                )
            }
        }
        ParticleField(state = hintSparkles, ambient = false, modifier = Modifier.matchParentSize())
    }
}

/**
 * The spent-hint highlight: a pulsing gold glow + ring around the object's correct slot, with a
 * bobbing arrow pointing straight at it, a translucent ghost of the object itself rotated to its
 * actual correct angle (including staying upright when [rotationDegrees] is 0 - that's still
 * real information, "leave this one as-is"), and a small rotate badge that only appears when a
 * turn is actually required, so the need to rotate reads at a glance before the player studies
 * the ghost's exact angle. Sparkles are rendered separately (a single shared [ParticleField] on
 * the panel) so the burst survives this composable's own fade-out. Never places anything -
 * purely informational, exactly matching every other slot indicator in this file (occupied-state
 * only ever comes from real placements).
 */
@Composable
private fun HintRevealHighlight(
    centerPx: Offset,
    sizeDp: Dp,
    art: ObjectArt,
    rotationDegrees: Int,
    modifier: Modifier = Modifier,
    accentColor: Color = MemoryArchitectColors.accentGold,
    objectMaterial: ObjectMaterialVisualSpec? = null,
) {
    val transition = rememberInfiniteTransition(label = "hintReveal")
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintRevealPulseScale",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.45f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintRevealGlowAlpha",
    )
    val arrowBob by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintRevealArrowBob",
    )

    val haloSizeDp = sizeDp * 1.9f
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (centerPx.x - haloSizeDp.toPx() / 2f).toInt(),
                    (centerPx.y - haloSizeDp.toPx() / 2f).toInt(),
                )
            }
            .size(haloSizeDp),
    ) {
        // Glow — a soft blurred wash that pulses under everything else.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .matchParentSize()
                .graphicsLayer {
                    alpha = glowAlpha
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .blur(20.dp)
                .background(
                    brush = Brush.radialGradient(listOf(accentColor, accentColor.copy(alpha = 0f))),
                    shape = CircleShape,
                ),
        )
        // Ring — a crisp stroke naming exactly one slot, not a fuzzy area.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(sizeDp * 1.05f)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .border(width = 3.dp, color = accentColor, shape = CircleShape),
        )
        // Ghost — a translucent preview of the object itself, rotated to its actual correct
        // angle, so the hint shows orientation as directly as it shows position: not just "here,"
        // but "here, facing like this."
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(sizeDp * 0.82f)
                .graphicsLayer {
                    alpha = 0.55f
                    rotationZ = rotationDegrees.toFloat()
                },
        ) {
            IdleAnimatedObject(art = art, modifier = Modifier.fillMaxSize().objectMaterialTint(objectMaterial))
        }
        // Rotate badge — only when a turn is actually required, so the need to rotate is obvious
        // before the player has to study the ghost's exact angle.
        if (rotationDegrees != 0) {
            Icon(
                imageVector = Icons.Filled.Rotate90DegreesCw,
                contentDescription = stringResource(R.string.gameplay_hint_rotation_needed),
                tint = MemoryArchitectColors.bgBase,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .background(accentColor, CircleShape)
                    .padding(3.dp),
            )
        }
        // Arrow — bobs above the ring, pointing straight down at the slot.
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(28.dp)
                .graphicsLayer { translationY = (-14).dp.toPx() - arrowBob * 10.dp.toPx() },
        )
    }
}

@Composable
private fun SlotHintOverlay(
    roomArt: RoomArt,
    occupiedIndices: Set<Int>,
    highlightedSlotIndex: Int?,
    alpha: Animatable<Float, AnimationVector1D>,
    objectSizeScale: Float,
    accentColor: Color = MemoryArchitectColors.accentGold,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Read inside the draw scope, not the composable body - this is what actually makes the
        // fade in/out free of any recomposition (see the caller's comment).
        val currentAlpha = alpha.value
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 7f), 0f)
        roomArt.slots.forEachIndexed { index, slot ->
            if (index in occupiedIndices) return@forEachIndexed
            val center = Offset(slot.xFraction * size.width, slot.yFraction * size.height)
            val radius = size.width * slot.footprintFraction * objectSizeScale * 0.5f
            val isHighlighted = index == highlightedSlotIndex
            if (isHighlighted) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.22f * currentAlpha),
                    radius = radius * 1.35f,
                    center = center,
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.95f * currentAlpha),
                    radius = radius * 1.1f,
                    center = center,
                    style = Stroke(width = 5f),
                )
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.32f * currentAlpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f, pathEffect = dash),
                )
            }
        }
    }
}

/**
 * Order-mode's sequential reveal used to spread every object's pop-in across the *entire*
 * Memorize window, [POP_IN_DURATION_MS] included - meaning the very last object to appear
 * finished animating in exactly as the phase timer expired, giving it effectively zero real
 * viewing time no matter how long the phase itself was. [MIN_SETTLE_MS] reserves guaranteed
 * fully-visible time at the end of the window instead: the stagger only spreads objects across
 * the window *up to* that reserved tail, so every object - including the last - is fully popped
 * in with real time left to actually look at the completed scene before it hides.
 */
@Composable
private fun rememberRevealProgress(
    level: LevelSpec,
    visibleObjects: List<SceneObjectSpec>,
    phase: GamePhase,
): androidx.compose.runtime.snapshots.SnapshotStateMap<String, Animatable<Float, AnimationVector1D>> {
    val progressMap = remember(level.seed) { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
    val idsKey = visibleObjects.joinToString(",") { it.objectId }

    LaunchedEffect(idsKey) {
        val visibleIds = visibleObjects.map { it.objectId }.toSet()
        val wasEmpty = progressMap.isEmpty()
        progressMap.keys.retainAll(visibleIds)
        val newIds = visibleIds - progressMap.keys
        if (newIds.isEmpty()) return@LaunchedEffect
        newIds.forEach { progressMap[it] = Animatable(0f) }

        if (phase == GamePhase.MEMORIZE && level.orderModeEnabled && wasEmpty) {
            val orderedNewIds = visibleObjects.map { it.objectId }.filter { it in newIds }
            val staggerWindowMs = (level.memorizeDurationMs - POP_IN_DURATION_MS - MIN_SETTLE_MS).coerceAtLeast(0L).toFloat()
            val perObjectDelayMs = if (orderedNewIds.size > 1) staggerWindowMs / (orderedNewIds.size - 1) else 0f
            orderedNewIds.forEachIndexed { index, id ->
                launch {
                    delay((index * perObjectDelayMs).toLong())
                    progressMap[id]?.animateTo(1f, tween(POP_IN_DURATION_MS, easing = PopIn))
                }
            }
        } else {
            newIds.forEach { id ->
                launch { progressMap[id]?.animateTo(1f, tween(POP_IN_DURATION_MS, easing = PopIn)) }
            }
        }
    }
    return progressMap
}

/**
 * A premium `OBJECT_MATERIAL`'s paint-transform - the same `saveLayer` + `ColorFilter` compositing
 * technique [distractorDesaturation] below already establishes, kept as a fully independent second
 * pass (never merged into that function's logic) so distractor behavior stays byte-for-byte
 * unchanged regardless of what material is equipped. Applied *before* [distractorDesaturation] in
 * every call site's modifier chain, so a distractor still desaturates on top of its material tint
 * rather than the two fighting over which one "wins." A true no-op when [material] is `null`.
 *
 * `internal`, not `private` - [com.suman.memoryarchitect.ui.screens.gameplay.GameplayScreen]'s
 * `PremiumDragGhost` (the floating drag ghost, a different composable/file in this same package)
 * reuses this exact function so the ghost's material tint matches the tray chip and placed object
 * exactly, rather than duplicating the technique a third time.
 *
 * [BlendMode.Color], not [BlendMode.Overlay] - this is a correction, not the original design.
 * `Overlay`'s neutral (no-brightness-change) point is raw value 0.5 on *each* R/G/B channel
 * independently, which has nothing to do with a color's overall (WCAG-weighted) luminance - a
 * `tintColor` can look "medium brightness" by luminance while one channel (usually blue, weighted
 * only ~7% in luminance) still sits well under 0.5, so `Overlay` quietly crushed every object
 * toward black regardless of which of the 7 materials was equipped. `BlendMode.Color` is defined
 * (CSS Compositing spec, which the platform implements exactly) to take the *source*'s hue/
 * saturation while preserving the *destination*'s own luminance exactly - so no tint color, now or
 * added later, can ever darken or wash out an object. See `CosmeticColorBlendTest.kt` for the
 * computed proof (not just an eyeballed claim) that this actually holds.
 */
internal fun Modifier.objectMaterialTint(material: ObjectMaterialVisualSpec?): Modifier {
    if (material == null) return this
    return this.drawWithCache {
        val filterPaint = Paint().apply {
            colorFilter = ColorFilter.tint(material.tintColor.copy(alpha = material.blendStrength), BlendMode.Color)
        }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), filterPaint)
                drawContent()
                canvas.restore()
            }
        }
    }
}

/**
 * Composites this content into an offscreen layer with a saturation-0.15 color filter Paint —
 * the Compose equivalent of the old `Drawable.colorFilter = distractorColorFilter` one-liner,
 * using `Canvas.saveLayer` (broadly supported) rather than the newer GraphicsLayer API.
 */
private fun Modifier.distractorDesaturation(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this.drawWithCache {
        val filterPaint = Paint().apply {
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
        }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), filterPaint)
                drawContent()
                canvas.restore()
            }
        }
    }
}
