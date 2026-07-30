package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun buildRepository() = LevelRepositoryImpl(dispatchers = ImmediateDispatcherProvider)

class LevelRepositoryImplTest {

    @Test
    fun `practice mode always succeeds with a purely local generation`() = runTest {
        val repository = buildRepository()

        val result = repository.generateLevel(GameMode.PRACTICE, DifficultyTier.EASY, streak = 0)

        assertTrue(result is Outcome.Success)
    }

    @Test
    fun `daily challenge is deterministic - every call today returns the identical puzzle`() = runTest {
        val repository = buildRepository()

        val first = repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)
        val second = repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(first is Outcome.Success && second is Outcome.Success)
        val firstLevel = (first as Outcome.Success).data
        val secondLevel = (second as Outcome.Success).data
        assertEquals(firstLevel.seed, secondLevel.seed)
        assertEquals(firstLevel.sceneType, secondLevel.sceneType)
        assertEquals(firstLevel.objects, secondLevel.objects)
    }

    @Test
    fun `weekly challenge is deterministic - every call this week returns the identical puzzle`() = runTest {
        val repository = buildRepository()

        val first = repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)
        val second = repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(first is Outcome.Success && second is Outcome.Success)
        val firstLevel = (first as Outcome.Success).data
        val secondLevel = (second as Outcome.Success).data
        assertEquals(firstLevel.seed, secondLevel.seed)
        assertEquals(firstLevel.sceneType, secondLevel.sceneType)
    }

    @Test
    fun `daily and weekly challenge never produce the same seed`() = runTest {
        val repository = buildRepository()

        val daily = (repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0) as Outcome.Success).data
        val weekly = (repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0) as Outcome.Success).data

        assertNotEquals(daily.seed, weekly.seed)
    }

    @Test
    fun `the same room is never shown twice in a row across consecutive local generations`() = runTest {
        val repository = buildRepository()

        var previousScene: String? = null
        repeat(200) {
            val result = repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
            val scene = (result as Outcome.Success).data.sceneType
            if (previousScene != null) {
                assertNotEquals(previousScene, scene)
            }
            previousScene = scene
        }
    }

    @Test
    fun `the room-repeat guard is shared across modes, not reset between Practice and Classic`() = runTest {
        val repository = buildRepository()

        val practiceResult = repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
        val practiceScene = (practiceResult as Outcome.Success).data.sceneType

        // Only the very next call is guaranteed to differ - the guard only ever excludes the
        // single immediately-previous room, so a later call could legitimately cycle back to it.
        val classicResult = repository.generateLevel(GameMode.CLASSIC, DifficultyTier.MEDIUM, streak = 0)
        val classicScene = (classicResult as Outcome.Success).data.sceneType

        assertNotEquals(practiceScene, classicScene)
    }

    @Test
    fun `daily and weekly challenge never participate in the room-repeat guard`() = runTest {
        val repository = buildRepository()

        // Daily/Weekly must stay deterministic regardless of what was played locally before them -
        // this would fail if generateLevel routed them through the same excludedScenes/lastSceneShown
        // path Practice/Classic use.
        repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
        val before = (repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0) as Outcome.Success).data

        repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
        repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
        val after = (repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0) as Outcome.Success).data

        assertEquals(before.sceneType, after.sceneType)
    }
}
