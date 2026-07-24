package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelConstraints
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Deterministic, seed-reproducible level generator. Mirrors the server-side generation
 * algorithm so the exact same seed always reconstructs the exact same layout — the server
 * relies on this same reproducibility to re-derive and validate a score submission. Used
 * directly (no round-trip) for Practice mode; scored modes always treat the server's
 * resolved layout as authoritative and never fall back to this locally.
 */
class LevelGenerator(private val rules: GenerationRules = GenerationRules.Default) {

    /**
     * [excludedScenes] lets a caller keep the same room from appearing twice in a row across
     * separate attempts (see [com.suman.memoryarchitect.data.repository.LevelRepositoryImpl]) -
     * purely a caller-side variety concern, so it falls back to the full pool rather than ever
     * producing an empty candidate list (e.g. a caller that excludes every scene by mistake still
     * gets a valid level, not a crash).
     */
    fun generate(
        seed: Long,
        mode: GameMode,
        difficultyTier: DifficultyTier,
        constraints: LevelConstraints,
        excludedScenes: Set<String> = emptySet(),
    ): LevelSpec {
        val random = Random(seed)
        val availableScenes = rules.scenePool.filterNot { it in excludedScenes }.ifEmpty { rules.scenePool }
        val sceneType = availableScenes[random.nextInt(availableScenes.size)]
        val objectPool = rules.objectPoolsByScene.getValue(sceneType)
        val slotCount = rules.roomSlotCountByScene.getValue(sceneType)

        val distractorCount = (constraints.objectCount * constraints.distractorRatio).roundToInt()
        val totalToPlace = (constraints.objectCount + distractorCount)
            .coerceAtMost(objectPool.size)
            .coerceAtMost(slotCount)
        val distractorStartIndex = (totalToPlace - distractorCount).coerceAtLeast(0)

        // Which objects appear (and which are distractors) is unaffected by placement — only
        // *where* each one lands changes, via assignSlots below. Distractors are drawn from
        // whatever's left after targets are chosen, preferring objects that share a target's
        // rough silhouette (see [ShapeFamily]) so a distractor can be genuinely easy to mistake
        // for the real thing at a glance, not just a random unrelated object.
        val shuffledPool = objectPool.shuffled(random)
        val targetIds = shuffledPool.take(distractorStartIndex)
        val chosenObjectIds = targetIds + pickDistractors(
            targetIds = targetIds,
            remainingPool = shuffledPool.drop(distractorStartIndex),
            count = totalToPlace - distractorStartIndex,
            random = random,
        )
        val chosenSlots = assignSlots(chosenObjectIds, sceneType, slotCount, random)
        val objects = chosenObjectIds.mapIndexed { index, objectId ->
            SceneObjectSpec(
                objectId = objectId,
                slotIndex = chosenSlots[index],
                rotationDegrees = if (constraints.rotationEnabled) {
                    randomRotation(random, constraints.rotationStepDegrees)
                } else {
                    0
                },
                scale = 0.85f + random.nextFloat() * 0.3f,
                isDistractor = index >= distractorStartIndex,
            )
        }

        return LevelSpec(
            seed = seed,
            mode = mode,
            difficultyTier = difficultyTier,
            sceneType = sceneType,
            objects = objects,
            timeLimitMs = constraints.timeLimitMs,
            // Floor, not override: never shorter than what the calling engine already decided
            // (e.g. a deliberately generous early-campaign onboarding window), only ever raised
            // when the real generated scene's content demands more than that engine assumed.
            memorizeDurationMs = maxOf(constraints.memorizeDurationMs, MemorizeTimeCalculator.compute(objects, rules)),
            orderModeEnabled = constraints.orderModeEnabled,
        )
    }

    /**
     * Picks [count] distractors from [remainingPool] (already excludes every chosen target),
     * preferring objects whose [ShapeFamily] matches at least one target's — falling back to any
     * remaining object once the similar-shaped candidates run out, so a room with few shape
     * overlaps never comes up short on distractors.
     */
    private fun pickDistractors(targetIds: List<String>, remainingPool: List<String>, count: Int, random: Random): List<String> {
        if (count <= 0) return emptyList()
        val targetFamilies = targetIds.mapNotNull { rules.objectShapeFamilyById[it] }.toSet()
        val (similarShape, other) = remainingPool.shuffled(random).partition { rules.objectShapeFamilyById[it] in targetFamilies }
        return (similarShape + other).take(count)
    }

    private fun randomRotation(random: Random, stepDegrees: Int): Int {
        val steps = 360 / stepDegrees
        return random.nextInt(steps) * stepDegrees
    }

    /**
     * Assigns each object (in [objectIds] order) a slot index, preferring a slot matching the
     * object's [GenerationRules.objectCategoryById] furniture-type — interior-design logic
     * instead of a pure random shuffle. Falls back to any remaining slot when the preferred
     * category has none left in this room (e.g. a laptop in the bedroom, which has no table).
     * Stays fully seed-deterministic: every random choice goes through the same [random]
     * instance in the same fixed order.
     */
    private fun assignSlots(
        objectIds: List<String>,
        sceneType: String,
        slotCount: Int,
        random: Random,
    ): List<Int> {
        val slotCategories = rules.roomSlotCategoriesByScene[sceneType]
        val remaining = (0 until slotCount).toMutableList()
        val byCategory = mutableMapOf<SlotCategory, MutableList<Int>>()
        if (slotCategories != null) {
            remaining.forEach { index ->
                val category = slotCategories.getOrNull(index) ?: return@forEach
                byCategory.getOrPut(category) { mutableListOf() }.add(index)
            }
        }

        fun popRandom(list: MutableList<Int>): Int = list.removeAt(random.nextInt(list.size))

        return objectIds.map { objectId ->
            val bucket = rules.objectCategoryById[objectId]?.let { byCategory[it] }
            val chosen = if (!bucket.isNullOrEmpty()) popRandom(bucket) else popRandom(remaining)
            remaining.remove(chosen)
            slotCategories?.getOrNull(chosen)?.let { byCategory[it]?.remove(chosen) }
            chosen
        }
    }
}