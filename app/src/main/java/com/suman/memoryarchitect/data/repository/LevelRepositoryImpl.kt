package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.data.remote.LevelApi
import com.suman.memoryarchitect.data.remote.dto.LevelSpecDto
import com.suman.memoryarchitect.data.remote.dto.SceneObjectDto
import com.suman.memoryarchitect.domain.generation.DifficultyEngine
import com.suman.memoryarchitect.domain.generation.LevelGenerator
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.SceneObjectSpec
import com.suman.memoryarchitect.domain.progression.LevelCampaignEngine
import com.suman.memoryarchitect.domain.repository.LevelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Practice and Classic (the leveled campaign) generate on-device with no round-trip:
 * Practice levels are never scored, and Classic's campaign progress (unlock/best-time) is a
 * purely local concern layered on top of a normal score submission. Every other mode is
 * server-authoritative: on failure this returns [Outcome.Error] with no local or cached
 * substitute, because silently swapping in an unvalidated local layout for a scored mode
 * would undermine server-side anti-cheat.
 */
@Singleton
class LevelRepositoryImpl @Inject constructor(
    private val api: LevelApi,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : LevelRepository {

    private val difficultyEngine = DifficultyEngine()

    // Classic's 1..100 curve is its own dedicated engine, not a scaled variant of the shared
    // tier system — it's driven directly by level number, not by DifficultyTier/streak.
    private val campaignEngine = LevelCampaignEngine()
    private val levelGenerator = LevelGenerator()

    // The room actually shown last time (across Practice and Classic alike - the repeat feels
    // just as flat regardless of mode), excluded from the next pick so the same room can never
    // land twice in a row purely by chance. Process-lifetime only, not persisted: the thing this
    // fixes is back-to-back repeats within a single session, not across app restarts.
    @Volatile private var lastSceneShown: String? = null

    override suspend fun generateLevel(
        mode: GameMode,
        difficultyTier: DifficultyTier,
        streak: Int,
    ): Outcome<LevelSpec> = withContext(dispatchers.io) {
        if (mode == GameMode.PRACTICE || mode == GameMode.CLASSIC) {
            return@withContext Outcome.Success(generateLocally(mode, difficultyTier, streak))
        }

        try {
            val level = when (mode) {
                // Daily/Weekly are a single shared puzzle for everyone, not a per-player
                // generation request — they come from their own endpoints.
                GameMode.DAILY_CHALLENGE -> api.getDailyChallenge().level.toDomain(mode, difficultyTier)
                GameMode.WEEKLY_CHALLENGE -> api.getWeeklyChallenge().level.toDomain(mode, difficultyTier)
                else -> api.generateLevel(mode = mode.name, difficulty = difficultyTier.name).toDomain(mode, difficultyTier)
            }
            Outcome.Success(level)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    private fun generateLocally(mode: GameMode, tier: DifficultyTier, streak: Int): LevelSpec {
        // Classic's streak is level number - 1 (see LevelCampaignEngine.streakFor), so it
        // reconstructs cleanly here without widening the shared generateLevel(...) signature.
        val constraints = if (mode == GameMode.CLASSIC) {
            campaignEngine.constraintsFor(streak + 1)
        } else {
            difficultyEngine.computeConstraints(tier, streak, mode)
        }
        val excludedScenes = lastSceneShown?.let(::setOf).orEmpty()
        val level = levelGenerator.generate(
            seed = Random.nextLong(),
            mode = mode,
            difficultyTier = tier,
            constraints = constraints,
            excludedScenes = excludedScenes,
        )
        lastSceneShown = level.sceneType
        return level
    }

    private fun LevelSpecDto.toDomain(mode: GameMode, tier: DifficultyTier) = LevelSpec(
        seed = seed,
        mode = mode,
        difficultyTier = tier,
        sceneType = sceneType,
        objects = objects.map { it.toDomain() },
        timeLimitMs = timeLimitMs,
        memorizeDurationMs = memorizeDurationMs,
        orderModeEnabled = orderModeEnabled,
    )

    private fun SceneObjectDto.toDomain() = SceneObjectSpec(
        objectId = objectId,
        slotIndex = slotIndex,
        rotationDegrees = rotationDegrees,
        scale = scale,
        isDistractor = isDistractor,
    )
}
