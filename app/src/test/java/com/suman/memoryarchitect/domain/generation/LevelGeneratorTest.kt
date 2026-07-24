package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelConstraints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    private val engine = DifficultyEngine()
    private val generator = LevelGenerator()

    @Test
    fun `same seed produces an identical level`() {
        val constraints = engine.computeConstraints(DifficultyTier.MEDIUM, streak = 3, mode = GameMode.CLASSIC)

        val first = generator.generate(42L, GameMode.CLASSIC, DifficultyTier.MEDIUM, constraints)
        val second = generator.generate(42L, GameMode.CLASSIC, DifficultyTier.MEDIUM, constraints)

        assertEquals(first, second)
    }

    @Test
    fun `generated objects never share a slot`() {
        val constraints = engine.computeConstraints(DifficultyTier.EXPERT, streak = 20, mode = GameMode.CLASSIC)
        val level = generator.generate(7L, GameMode.CLASSIC, DifficultyTier.EXPERT, constraints)

        val slotIndices = level.objects.map { it.slotIndex }
        assertEquals(slotIndices.size, slotIndices.toSet().size)
    }

    @Test
    fun `rotation is always zero when constraints disable rotation`() {
        val constraints = engine.computeConstraints(DifficultyTier.BEGINNER, streak = 0, mode = GameMode.PRACTICE)
        val level = generator.generate(5L, GameMode.PRACTICE, DifficultyTier.BEGINNER, constraints)

        assertTrue(level.objects.all { it.rotationDegrees == 0 })
    }

    @Test
    fun `practice levels have no time limit`() {
        val constraints = engine.computeConstraints(DifficultyTier.MEDIUM, streak = 0, mode = GameMode.PRACTICE)
        val level = generator.generate(9L, GameMode.PRACTICE, DifficultyTier.MEDIUM, constraints)

        assertNull(level.timeLimitMs)
    }

    @Test
    fun `scored levels never drop below the minimum time limit`() {
        val constraints = engine.computeConstraints(DifficultyTier.EXPERT, streak = 0, mode = GameMode.WEEKLY_CHALLENGE)
        val level = generator.generate(11L, GameMode.WEEKLY_CHALLENGE, DifficultyTier.EXPERT, constraints)

        assertTrue(requireNotNull(level.timeLimitMs) >= GenerationRules.Default.minTimeLimitMs)
    }

    @Test
    fun `objects only skip their preferred furniture category when no slot of that type is free`() {
        val rules = GenerationRules.Default
        val constraints = engine.computeConstraints(DifficultyTier.EXPERT, streak = 20, mode = GameMode.CLASSIC)

        // Every scene, so the category data for every room in the pool gets exercised.
        rules.scenePool.forEachIndexed { sceneIndex, _ ->
            val level = generator.generate(100L + sceneIndex, GameMode.CLASSIC, DifficultyTier.EXPERT, constraints)
            val slotCategories = rules.roomSlotCategoriesByScene.getValue(level.sceneType)
            val occupiedSlots = level.objects.map { it.slotIndex }.toSet()

            level.objects.forEach { obj ->
                val preferred = rules.objectCategoryById[obj.objectId] ?: return@forEach
                val actual = slotCategories.getOrNull(obj.slotIndex)
                if (actual == preferred) return@forEach

                val preferredSlots = slotCategories.indices.filter { slotCategories[it] == preferred }
                val everyPreferredSlotWasTaken = preferredSlots.all { it in occupiedSlots }
                assertTrue(
                    "${obj.objectId} landed on $actual instead of $preferred in ${level.sceneType}, " +
                        "but a $preferred slot was free",
                    everyPreferredSlotWasTaken,
                )
            }
        }
    }

    @Test
    fun `distractors prefer objects sharing a target's shape family`() {
        // Two same-size families with no overlap — the single target lands in one family, and
        // exactly enough same-family objects remain to fill every distractor slot, so a correct
        // bias means *every* distractor should match the target's family, deterministically,
        // regardless of shuffle order or which specific object became the target.
        val rules = GenerationRules(
            scenePool = listOf("test_room"),
            objectPoolsByScene = mapOf("test_room" to listOf("a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4")),
            roomSlotCountByScene = mapOf("test_room" to 8),
            objectShapeFamilyById = mapOf(
                "a1" to ShapeFamily.VESSEL, "a2" to ShapeFamily.VESSEL, "a3" to ShapeFamily.VESSEL, "a4" to ShapeFamily.VESSEL,
                "b1" to ShapeFamily.DEVICE, "b2" to ShapeFamily.DEVICE, "b3" to ShapeFamily.DEVICE, "b4" to ShapeFamily.DEVICE,
            ),
        )
        val testGenerator = LevelGenerator(rules)
        val constraints = LevelConstraints(
            objectCount = 1,
            distractorRatio = 3f,
            memorizeDurationMs = 5_000L,
            rotationEnabled = false,
            rotationStepDegrees = 90,
            orderModeEnabled = false,
            themeComplexity = 0,
            timeLimitMs = 30_000L,
        )

        repeat(20) { seed ->
            val level = testGenerator.generate(seed.toLong(), GameMode.PRACTICE, DifficultyTier.MEDIUM, constraints)
            val target = level.objects.single { !it.isDistractor }
            val distractors = level.objects.filter { it.isDistractor }
            val targetFamily = rules.objectShapeFamilyById.getValue(target.objectId)

            assertEquals(3, distractors.size)
            assertTrue(distractors.all { rules.objectShapeFamilyById[it.objectId] == targetFamily })
        }
    }

    @Test
    fun `excluded scenes are never picked`() {
        val constraints = engine.computeConstraints(DifficultyTier.MEDIUM, streak = 0, mode = GameMode.PRACTICE)
        val excluded = setOf("kitchen")

        repeat(50) { seed ->
            val level = generator.generate(seed.toLong(), GameMode.PRACTICE, DifficultyTier.MEDIUM, constraints, excludedScenes = excluded)
            assertTrue(level.sceneType !in excluded)
        }
    }

    @Test
    fun `excluding every scene falls back to the full pool instead of an empty selection`() {
        val constraints = engine.computeConstraints(DifficultyTier.MEDIUM, streak = 0, mode = GameMode.PRACTICE)
        val everyScene = GenerationRules.Default.scenePool.toSet()

        val level = generator.generate(1L, GameMode.PRACTICE, DifficultyTier.MEDIUM, constraints, excludedScenes = everyScene)

        assertTrue(level.sceneType in everyScene)
    }
}