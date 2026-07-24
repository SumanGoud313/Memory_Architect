package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.RewardKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardEvaluatorTest {

    private val evaluator = RewardEvaluator()

    @Test
    fun `below the first cadence level unlocks nothing`() {
        val unlocked = evaluator.evaluateNewlyUnlocked(level = 4, alreadyUnlocked = emptySet())

        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `reaching the first cadence level unlocks exactly one reward`() {
        val unlocked = evaluator.evaluateNewlyUnlocked(level = 5, alreadyUnlocked = emptySet())

        assertEquals(1, unlocked.size)
        assertEquals(5, unlocked.single().unlockLevel)
    }

    @Test
    fun `already unlocked rewards are never returned again`() {
        val first = evaluator.evaluateNewlyUnlocked(level = 5, alreadyUnlocked = emptySet())

        val second = evaluator.evaluateNewlyUnlocked(level = 5, alreadyUnlocked = first.map { it.id }.toSet())

        assertTrue(second.isEmpty())
    }

    @Test
    fun `jumping past several cadence levels at once unlocks all of them together`() {
        val unlocked = evaluator.evaluateNewlyUnlocked(level = 22, alreadyUnlocked = emptySet())

        assertEquals(listOf(5, 10, 15, 20), unlocked.map { it.unlockLevel })
    }
}

class RewardCatalogTest {

    @Test
    fun `timeline is evenly spaced by the cadence with no duplicate ids`() {
        val timeline = RewardCatalog.timeline

        assertTrue(timeline.isNotEmpty())
        timeline.forEach { assertEquals(0, it.unlockLevel % RewardRules.Default.cadenceLevels) }
        assertEquals(timeline.map { it.unlockLevel }, timeline.map { it.unlockLevel }.sorted())
        assertEquals(timeline.size, timeline.map { it.id }.toSet().size)
    }

    @Test
    fun `first reward is a room theme, cycling through every kind`() {
        val timeline = RewardCatalog.timeline

        assertEquals(RewardKind.ROOM_THEME, timeline.first().kind)
        assertTrue(RewardKind.entries.all { kind -> timeline.any { it.kind == kind } })
    }

    @Test
    fun `equippedTitle and equippedPalette pick the highest unlocked entry of their kind`() {
        val appTitle = RewardCatalog.timeline.first { it.kind == RewardKind.TITLE }
        val laterTitle = RewardCatalog.timeline.filter { it.kind == RewardKind.TITLE }[1]

        val equipped = RewardCatalog.equippedTitle(setOf(appTitle.id, laterTitle.id))

        assertEquals(laterTitle.id, equipped)
    }

    @Test
    fun `equippedPalette is null when no palette is unlocked`() {
        assertEquals(null, RewardCatalog.equippedPalette(setOf(RewardId.BADGE_BRONZE_KEY)))
    }

    @Test
    fun `the timeline covers every cadence milestone through the final campaign level with no drought`() {
        val timeline = RewardCatalog.timeline
        val maxLevel = LevelCampaignRules.Default.maxLevel
        val expectedMilestones = (RewardRules.Default.cadenceLevels..maxLevel step RewardRules.Default.cadenceLevels).toList()

        val dispensedLevels = timeline.map { it.unlockLevel }.filter { it <= maxLevel }.toSet()

        expectedMilestones.forEach { milestone ->
            assertTrue("expected a reward at level $milestone, timeline had none there", milestone in dispensedLevels)
        }
    }
}