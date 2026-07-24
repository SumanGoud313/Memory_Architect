package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.data.remote.LevelApi
import com.suman.memoryarchitect.data.remote.dto.ChallengeResponseDto
import com.suman.memoryarchitect.data.remote.dto.LevelSpecDto
import com.suman.memoryarchitect.data.remote.dto.SceneObjectDto
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private object LevelRepoNoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}
private val testErrorMapper = ErrorMapper(LevelRepoNoopCrashReporter)

private fun sampleLevelDto(seed: Long) = LevelSpecDto(
    seed = seed,
    sceneType = "kitchen",
    objects = listOf(SceneObjectDto("star", 0, 0, 1f, false)),
    timeLimitMs = 45_000L,
    memorizeDurationMs = 5_000L,
)

private class FakeLevelApi(
    private val generateResponse: LevelSpecDto? = null,
    private val dailyResponse: ChallengeResponseDto? = null,
    private val weeklyResponse: ChallengeResponseDto? = null,
    private val error: Throwable? = null,
    private val onGenerateCalled: () -> Unit = {},
    private val onDailyCalled: () -> Unit = {},
    private val onWeeklyCalled: () -> Unit = {},
) : LevelApi {
    override suspend fun generateLevel(mode: String, difficulty: String): LevelSpecDto {
        onGenerateCalled()
        error?.let { throw it }
        return requireNotNull(generateResponse)
    }

    override suspend fun getDailyChallenge(): ChallengeResponseDto {
        onDailyCalled()
        error?.let { throw it }
        return requireNotNull(dailyResponse)
    }

    override suspend fun getWeeklyChallenge(): ChallengeResponseDto {
        onWeeklyCalled()
        error?.let { throw it }
        return requireNotNull(weeklyResponse)
    }
}

class LevelRepositoryImplTest {

    @Test
    fun `practice mode never calls the network`() = runTest {
        var called = false
        val repository = LevelRepositoryImpl(
            api = FakeLevelApi(onGenerateCalled = { called = true }),
            dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper,
        )

        val result = repository.generateLevel(GameMode.PRACTICE, DifficultyTier.EASY, streak = 0)

        assertTrue(result is Outcome.Success)
        assertFalse(called)
    }

    @Test
    fun `scored mode maps server response to the domain model`() = runTest {
        val dto = sampleLevelDto(seed = 99L)
        val repository = LevelRepositoryImpl(
            api = FakeLevelApi(weeklyResponse = ChallengeResponseDto(dto, expiresAtEpochSecond = 1_000L)),
            dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper,
        )

        val result = repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(result is Outcome.Success)
        assertEquals(99L, (result as Outcome.Success).data.seed)
        assertEquals(1, result.data.objects.size)
    }

    @Test
    fun `scored mode network failure returns error with no local fallback`() = runTest {
        val repository = LevelRepositoryImpl(
            api = FakeLevelApi(weeklyResponse = ChallengeResponseDto(sampleLevelDto(1L), expiresAtEpochSecond = 1_000L), error = IOException("offline")),
            dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper,
        )

        val result = repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(result is Outcome.Error)
    }

    @Test
    fun `daily challenge hits the dedicated daily endpoint, not the generic generate endpoint`() = runTest {
        var generateCalled = false
        var dailyCalled = false
        val repository = LevelRepositoryImpl(
            api = FakeLevelApi(
                dailyResponse = ChallengeResponseDto(sampleLevelDto(seed = 42L), expiresAtEpochSecond = 1_000L),
                onGenerateCalled = { generateCalled = true },
                onDailyCalled = { dailyCalled = true },
            ),
            dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper,
        )

        val result = repository.generateLevel(GameMode.DAILY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(result is Outcome.Success)
        assertEquals(42L, (result as Outcome.Success).data.seed)
        assertTrue(dailyCalled)
        assertFalse(generateCalled)
    }

    @Test
    fun `weekly challenge hits the dedicated weekly endpoint, not the generic generate endpoint`() = runTest {
        var generateCalled = false
        var weeklyCalled = false
        val repository = LevelRepositoryImpl(
            api = FakeLevelApi(
                weeklyResponse = ChallengeResponseDto(sampleLevelDto(seed = 7L), expiresAtEpochSecond = 2_000L),
                onGenerateCalled = { generateCalled = true },
                onWeeklyCalled = { weeklyCalled = true },
            ),
            dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper,
        )

        val result = repository.generateLevel(GameMode.WEEKLY_CHALLENGE, DifficultyTier.MEDIUM, streak = 0)

        assertTrue(result is Outcome.Success)
        assertEquals(7L, (result as Outcome.Success).data.seed)
        assertTrue(weeklyCalled)
        assertFalse(generateCalled)
    }

    @Test
    fun `the same room is never shown twice in a row across consecutive local generations`() = runTest {
        val repository = LevelRepositoryImpl(api = FakeLevelApi(), dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper)

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
        val repository = LevelRepositoryImpl(api = FakeLevelApi(), dispatchers = ImmediateDispatcherProvider, errorMapper = testErrorMapper)

        val practiceResult = repository.generateLevel(GameMode.PRACTICE, DifficultyTier.MEDIUM, streak = 0)
        val practiceScene = (practiceResult as Outcome.Success).data.sceneType

        // Only the very next call is guaranteed to differ - the guard only ever excludes the
        // single immediately-previous room, so a later call could legitimately cycle back to it.
        val classicResult = repository.generateLevel(GameMode.CLASSIC, DifficultyTier.MEDIUM, streak = 0)
        val classicScene = (classicResult as Outcome.Success).data.sceneType

        assertNotEquals(practiceScene, classicScene)
    }
}
