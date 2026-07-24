package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorizeTimeCalculatorTest {

    private fun obj(id: String, rotation: Int = 0, distractor: Boolean = false) =
        SceneObjectSpec(objectId = id, slotIndex = 0, rotationDegrees = rotation, scale = 1f, isDistractor = distractor)

    private val rules = GenerationRules.Default

    @Test
    fun `empty scene returns the minimum floor`() {
        assertEquals(4_000L, MemorizeTimeCalculator.compute(emptyList(), rules))
    }

    @Test
    fun `scales with plain object count when nothing else adds load`() {
        val objects = listOf(obj("book"), obj("globe"), obj("hourglass"))
        // 3 * 900ms base, no rotation, no lookalike distractors.
        assertEquals(2_700L.coerceAtLeast(4_000L), MemorizeTimeCalculator.compute(objects, rules))
    }

    @Test
    fun `rotated objects cost more than unrotated ones`() {
        val flat = List(6) { obj("book_$it") }
        val rotated = List(6) { obj("book_$it", rotation = 90) }

        val flatTime = MemorizeTimeCalculator.compute(flat, rules)
        val rotatedTime = MemorizeTimeCalculator.compute(rotated, rules)

        assertTrue(rotatedTime > flatTime)
    }

    @Test
    fun `a distractor sharing a target's shape family costs more than an unrelated one`() {
        // Five targets with distinct families, chosen so the base cost alone already clears the
        // minimum floor -- otherwise both scenarios would round to the same clamped value and
        // the bonus would never be observable.
        val targets = listOf(
            obj("coffee_mug"), // VESSEL
            obj("globe"), // ROUND_SMALL
            obj("hourglass"), // TALL_NARROW
            obj("backpack"), // SOFT
            obj("trophy"), // ORNAMENT
        )
        val lookalikeDistractor = obj("teapot", distractor = true) // VESSEL, matches coffee_mug
        val unrelatedDistractor = obj("poster", distractor = true) // FLAT_RECT, does not match

        val withLookalike = MemorizeTimeCalculator.compute(targets + lookalikeDistractor, rules)
        val withUnrelated = MemorizeTimeCalculator.compute(targets + unrelatedDistractor, rules)

        assertTrue(withLookalike > withUnrelated)
    }

    @Test
    fun `never exceeds the maximum ceiling even for very large scenes`() {
        val hugeRotatedScene = List(30) { obj("item_$it", rotation = 90) }
        assertEquals(18_000L, MemorizeTimeCalculator.compute(hugeRotatedScene, rules))
    }

    @Test
    fun `never drops below the minimum for a single simple object`() {
        assertEquals(4_000L, MemorizeTimeCalculator.compute(listOf(obj("book")), rules))
    }

    @Test
    fun `every mode and tier gets at least the base per-object pace after generation`() {
        val engine = DifficultyEngine()
        val generator = LevelGenerator()

        DifficultyTier.entries.forEach { tier ->
            listOf(GameMode.CLASSIC, GameMode.PRACTICE, GameMode.DAILY_CHALLENGE, GameMode.WEEKLY_CHALLENGE).forEach { mode ->
                repeat(5) { seed ->
                    val constraints = engine.computeConstraints(tier, streak = 0, mode = mode)
                    val level = generator.generate(seed.toLong() * 31 + tier.ordinal, mode, tier, constraints)

                    val msPerObject = level.memorizeDurationMs.toDouble() / level.objects.size
                    assertTrue(
                        "$mode/$tier seed=$seed gave only ${"%.0f".format(msPerObject)}ms/object " +
                            "(${level.objects.size} objects in ${level.memorizeDurationMs}ms)",
                        msPerObject >= 900.0,
                    )
                }
            }
        }
    }
}
