package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.domain.generation.DifficultyEngine
import com.suman.memoryarchitect.domain.generation.LevelGenerator
import com.suman.memoryarchitect.domain.generation.PeriodicChallengeGenerator
import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.progression.LevelCampaignEngine
import com.suman.memoryarchitect.domain.repository.LevelRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Every mode generates on-device now - Firebase Spark (no Cloud Functions, no billed-tier
 * dependency) can't run a server-side generator, so Daily/Weekly Challenge moved from a
 * Cloud-Function-backed shared Firestore document to [PeriodicChallengeGenerator] - see its own
 * doc for why a shared document isn't needed at all once generation is a pure function of the date.
 */
@Singleton
class LevelRepositoryImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : LevelRepository {

    private val difficultyEngine = DifficultyEngine()

    // Classic's 1..100 curve is its own dedicated engine, not a scaled variant of the shared
    // tier system — it's driven directly by level number, not by DifficultyTier/streak.
    private val campaignEngine = LevelCampaignEngine()
    private val levelGenerator = LevelGenerator()

    // The room actually shown last time (across Practice and Classic alike - the repeat feels
    // just as flat regardless of mode), excluded from the next pick so the same room can never
    // land twice in a row purely by chance. Process-lifetime only, not persisted: the thing this
    // fixes is back-to-back repeats within a single session, not across app restarts. Daily/Weekly
    // Challenge deliberately never reads or updates this - see generatePeriodicChallenge's own doc.
    @Volatile private var lastSceneShown: String? = null

    override suspend fun generateLevel(
        mode: GameMode,
        difficultyTier: DifficultyTier,
        streak: Int,
    ): Outcome<LevelSpec> = withContext(dispatchers.io) {
        val level = when (mode) {
            GameMode.PRACTICE, GameMode.CLASSIC -> generateLocally(mode, difficultyTier, streak)
            GameMode.DAILY_CHALLENGE -> PeriodicChallengeGenerator.generateDailyChallenge()
            GameMode.WEEKLY_CHALLENGE -> PeriodicChallengeGenerator.generateWeeklyChallenge()
        }
        Outcome.Success(level)
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
}
