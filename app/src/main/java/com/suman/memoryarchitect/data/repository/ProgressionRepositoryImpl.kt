package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionDao
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.StatisticsCacheEntity
import com.suman.memoryarchitect.core.database.StatisticsDao
import com.suman.memoryarchitect.core.sync.PendingScoreSyncScheduler
import com.suman.memoryarchitect.core.database.UnlockedAchievementDao
import com.suman.memoryarchitect.core.database.UnlockedAchievementEntity
import com.suman.memoryarchitect.core.database.UnlockedRewardDao
import com.suman.memoryarchitect.core.database.UnlockedRewardEntity
import com.suman.memoryarchitect.domain.achievements.AchievementEvaluator
import com.suman.memoryarchitect.domain.achievements.ProgressSnapshot
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.MemoryJourneyRules
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.ReturningPlayerGiftClaimResult
import com.suman.memoryarchitect.domain.model.RewardDefinition
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult
import com.suman.memoryarchitect.domain.progression.LevelCampaignRules
import com.suman.memoryarchitect.domain.progression.MemoryJourneyCatalog
import com.suman.memoryarchitect.domain.progression.ProgressionRules
import com.suman.memoryarchitect.domain.progression.RewardEvaluator
import com.suman.memoryarchitect.domain.progression.StreakCalculator
import com.suman.memoryarchitect.domain.progression.XpCurve
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * Online-first, same cache-bridge policy as [RemoteConfigRepositoryImpl] for reads. Writes
 * (score submissions) are different: a failed submission is never silently dropped — it's
 * applied optimistically to the local profile and queued in [PendingScoreSubmissionDao] for
 * a future sync worker to flush, with [ScoreSubmissionResult.isPendingSync] telling the UI
 * the number isn't server-confirmed yet. Statistics and achievements are evaluated against
 * local data unconditionally — they're recognition, not an anti-cheat concern, so they
 * don't wait on server confirmation the way the profile's XP does.
 *
 * Which backend "the server" actually is lives entirely behind [activeRemoteSource] - see
 * [ProgressionRemoteSource]'s doc. Everything below (caching, optimistic updates, the
 * pending-sync queue, achievement/reward evaluation) is unchanged regardless of which one answers.
 */
@Singleton
class ProgressionRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendProgressionRemoteSource,
    private val firestoreSource: FirestoreProgressionRemoteSource,
    private val playerIdentityManager: PlayerIdentityManager,
    private val progressDao: PlayerProgressDao,
    private val pendingDao: PendingScoreSubmissionDao,
    private val statisticsDao: StatisticsDao,
    private val unlockedAchievementDao: UnlockedAchievementDao,
    private val unlockedRewardDao: UnlockedRewardDao,
    private val levelCampaignRepository: LevelCampaignRepository,
    private val inventoryItemDao: InventoryItemDao,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
    private val pendingSyncScheduler: PendingScoreSyncScheduler,
) : ProgressionRepository {

    private val xpCurve = XpCurve()
    private val streakCalculator = StreakCalculator()
    private val achievementEvaluator = AchievementEvaluator()
    private val rewardEvaluator = RewardEvaluator()

    /** [FirestoreProgressionRemoteSource] whenever it's actually usable (a real Firebase project
     * is connected and a player identity has resolved) - a real, per-player, transactional
     * datastore, safe for any number of concurrent real players. Falls back to
     * [MockBackendProgressionRemoteSource] otherwise (no `google-services.json`, or Firestore/
     * Anonymous Auth not yet enabled in the console - see LEADERBOARD_SETUP.md) purely so local
     * development keeps working exactly as it always has without that setup. Since
     * [FirebaseAvailabilityProvider.isConfigured] is a build-time constant and sign-in is kicked off
     * eagerly in [com.suman.memoryarchitect.MemoryArchitectApp.onCreate], a configured app only
     * ever takes the mock-backend branch for the first second or two of a cold launch. */
    private suspend fun activeRemoteSource(): ProgressionRemoteSource {
        if (!firebaseAvailabilityProvider.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    override suspend fun getProfile(): Outcome<PlayerProfile> = withContext(dispatchers.io) {
        try {
            val profile = activeRemoteSource().getProfile()
            progressDao.upsert(profile.toCacheEntity())
            Outcome.Success(profile)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            progressDao.get()?.toDomain()?.let { Outcome.Success(it) } ?: Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun getStatistics(): PlayerStatistics = withContext(dispatchers.io) {
        statisticsDao.get()?.toDomain() ?: PlayerStatistics.EMPTY
    }

    override suspend fun getUnlockedAchievementIds(): Set<AchievementId> = withContext(dispatchers.io) {
        fetchUnlockedAchievementIds()
    }

    override suspend fun getUnlockedRewardIds(): Set<RewardId> = withContext(dispatchers.io) {
        fetchUnlockedRewardIds()
    }

    override suspend fun getDailyRewardStatus(todayEpochDay: Long): Outcome<DailyRewardStatus> = withContext(dispatchers.io) {
        try {
            Outcome.Success(activeRemoteSource().getDailyRewardStatus(todayEpochDay))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun claimDailyReward(todayEpochDay: Long): Outcome<DailyRewardClaimResult> = withContext(dispatchers.io) {
        try {
            val result = activeRemoteSource().claimDailyReward(todayEpochDay)
            progressDao.upsert(result.profile.toCacheEntity())
            result.inventory.quantities.forEach { (kind, quantity) -> inventoryItemDao.upsert(InventoryItemEntity(kind.name, quantity)) }
            Outcome.Success(result)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (alreadyClaimed: DailyRewardAlreadyClaimedException) {
            // Same routine, expected condition mock-backend/index.js reports as an HTTP 409 -
            // matched to the same AppError shape here rather than falling through to the generic
            // branch below, which would report it to Crashlytics as an unexpected failure.
            Outcome.Error(AppError.Server(code = 409, message = alreadyClaimed.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun claimReturningPlayerGift(todayEpochDay: Long): Outcome<ReturningPlayerGiftClaimResult> = withContext(dispatchers.io) {
        try {
            val result = activeRemoteSource().claimReturningPlayerGift(todayEpochDay)
            result.inventory.quantities.forEach { (kind, quantity) -> inventoryItemDao.upsert(InventoryItemEntity(kind.name, quantity)) }
            Outcome.Success(result)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (alreadyClaimed: ReturningPlayerGiftAlreadyClaimedException) {
            Outcome.Error(AppError.Server(code = 409, message = alreadyClaimed.message))
        } catch (notEligible: ReturningPlayerGiftNotEligibleException) {
            Outcome.Error(AppError.Server(code = 400, message = notEligible.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun submitScore(
        mode: GameMode,
        levelSeed: Long,
        score: ScoreResult,
        playedOnEpochDay: Long,
        timeTakenMs: Long,
        submissionNonce: String,
        awardXp: Boolean,
    ): Outcome<ScoreSubmissionResult> = withContext(dispatchers.io) {
        val cachedProfile = progressDao.get()?.toDomain() ?: PlayerProfile.EMPTY
        val xpAwarded = if (awardXp) (score.finalScore * ProgressionRules.Default.xpPerScorePoint).roundToLong() else 0L
        val coinsAwarded = coinsAwardedFor(mode, score)
        val streakUpdate = streakCalculator.updateStreak(
            cachedProfile.lastPlayedEpochDay,
            playedOnEpochDay,
            cachedProfile.currentStreak,
            cachedProfile.longestStreak,
            cachedProfile.streakShields,
        )
        val previousLevel = xpCurve.levelForXp(cachedProfile.xp)
        // A guess at what the server will stamp (see mock-backend/progression.js's
        // applyScoreSubmission) so the lock takes effect immediately even before the submission
        // round-trips - overwritten by the authoritative server value below on success.
        val nowEpochSecond = System.currentTimeMillis() / 1000
        val optimisticProfile = cachedProfile.copy(
            xp = cachedProfile.xp + xpAwarded,
            coins = cachedProfile.coins + coinsAwarded,
            currentStreak = streakUpdate.currentStreak,
            longestStreak = streakUpdate.longestStreak,
            streakShields = streakUpdate.streakShields,
            lastPlayedEpochDay = playedOnEpochDay,
            dailyChallengeWonAtEpochSecond = if (mode == GameMode.DAILY_CHALLENGE && coinsAwarded > 0) nowEpochSecond else cachedProfile.dailyChallengeWonAtEpochSecond,
            weeklyChallengeWonAtEpochSecond = if (mode == GameMode.WEEKLY_CHALLENGE && coinsAwarded > 0) nowEpochSecond else cachedProfile.weeklyChallengeWonAtEpochSecond,
        )
        val leveledUp = xpCurve.levelForXp(optimisticProfile.xp) > previousLevel

        val updatedStatistics = updateStatistics(mode, score, playedOnEpochDay, timeTakenMs)
        val newlyUnlocked = evaluateAndPersistAchievements(optimisticProfile, updatedStatistics, playedOnEpochDay)
        val newlyUnlockedRewards = evaluateAndPersistRewards(xpCurve.levelForXp(optimisticProfile.xp), playedOnEpochDay)
        val journeyPointsAwarded = journeyPointsAwardedFor(score.sceneAccuracy, streakUpdate.milestoneReached, newlyUnlocked.size)
        val finalOptimisticProfile = optimisticProfile.copy(journeyPoints = cachedProfile.journeyPoints + journeyPointsAwarded)
        val previousJourneyTierId = MemoryJourneyCatalog.tierFor(cachedProfile.journeyPoints)?.id

        try {
            val serverProfile = activeRemoteSource().submitScore(mode, score, levelSeed, playedOnEpochDay, submissionNonce, newlyUnlocked.size, awardXp)
            progressDao.upsert(serverProfile.toCacheEntity())
            Outcome.Success(
                ScoreSubmissionResult(
                    serverProfile, xpAwarded, coinsAwarded, leveledUp, isPendingSync = false, updatedStatistics, newlyUnlocked, newlyUnlockedRewards,
                    streakMilestoneReached = streakUpdate.milestoneReached,
                    streakShieldGranted = streakUpdate.shieldGranted,
                    streakShieldConsumed = streakUpdate.shieldConsumed,
                    journeyPointsAwarded = serverProfile.journeyPoints - cachedProfile.journeyPoints,
                    journeyTierReached = MemoryJourneyCatalog.tierFor(serverProfile.journeyPoints)?.id?.takeIf { it != previousJourneyTierId },
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (duplicate: DuplicateSubmissionException) {
            // Same routine, expected condition as DailyRewardAlreadyClaimedException below - a
            // replayed/duplicated network call for a round that already landed server-side, not an
            // unexpected failure worth a Crashlytics report. The optimistic local update already
            // applied above (xp/coins/statistics/achievements) stays in place - it reflects the one
            // real successful submission this nonce was minted for, and reverting it here would
            // wrongly claw back credit for a round that did, in fact, count.
            Outcome.Error(AppError.Server(code = 409, message = duplicate.message))
        } catch (failure: Throwable) {
            progressDao.upsert(finalOptimisticProfile.toCacheEntity())
            pendingDao.insert(
                PendingScoreSubmissionEntity(
                    mode = mode.name,
                    levelSeed = levelSeed,
                    finalScore = score.finalScore,
                    sceneAccuracy = score.sceneAccuracy,
                    playedOnEpochDay = playedOnEpochDay,
                    createdAt = playedOnEpochDay,
                    submissionNonce = submissionNonce,
                    comboCount = score.comboCount,
                    newlyUnlockedAchievementCount = newlyUnlocked.size,
                    awardXp = awardXp,
                ),
            )
            pendingSyncScheduler.scheduleRetry()
            Outcome.Success(
                ScoreSubmissionResult(
                    finalOptimisticProfile, xpAwarded, coinsAwarded, leveledUp, isPendingSync = true, updatedStatistics, newlyUnlocked, newlyUnlockedRewards,
                    streakMilestoneReached = streakUpdate.milestoneReached,
                    streakShieldGranted = streakUpdate.shieldGranted,
                    streakShieldConsumed = streakUpdate.shieldConsumed,
                    journeyPointsAwarded = journeyPointsAwarded,
                    journeyTierReached = MemoryJourneyCatalog.tierFor(finalOptimisticProfile.journeyPoints)?.id?.takeIf { it != previousJourneyTierId },
                ),
            )
        }
    }

    override suspend fun retryPendingSubmissions(): Unit = withContext(dispatchers.io) {
        // An explicit snapshot, never iterated live against the DAO - each successful/duplicate
        // entry below is deleted mid-loop, and iterating a collection while mutating its backing
        // store (even indirectly, through a DAO that happens to return a live reference rather
        // than a fresh query result) throws ConcurrentModificationException.
        val pending = pendingDao.getAll().toList()
        for (entity in pending) {
            val mode = runCatching { GameMode.valueOf(entity.mode) }.getOrNull()
            if (mode == null) {
                // A corrupt/unrecognized row can never succeed - drop it rather than blocking every
                // entry queued after it on a retry forever.
                pendingDao.delete(entity)
                continue
            }
            // Only the three fields actually transmitted over the wire (see
            // ScoreSubmissionRequestDto/PendingScoreSubmissionEntity's doc) need to be reconstructed
            // accurately - objectScores/placementScore/timeBonus/comboBonus never reach either
            // backend and play no part in the server-authoritative xp/coins calculation.
            val score = ScoreResult(
                objectScores = emptyList(),
                sceneAccuracy = entity.sceneAccuracy,
                placementScore = 0,
                timeBonus = 0,
                comboBonus = 0,
                finalScore = entity.finalScore,
                comboCount = entity.comboCount,
            )
            try {
                val serverProfile = activeRemoteSource().submitScore(
                    mode, score, entity.levelSeed, entity.playedOnEpochDay, entity.submissionNonce, entity.newlyUnlockedAchievementCount, entity.awardXp,
                )
                progressDao.upsert(serverProfile.toCacheEntity())
                pendingDao.delete(entity)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (duplicate: DuplicateSubmissionException) {
                // The server already has this one - its original response just never reached the
                // client. Reconcile against the real profile rather than treating this as a failure.
                runCatching { activeRemoteSource().getProfile() }.getOrNull()?.let { progressDao.upsert(it.toCacheEntity()) }
                pendingDao.delete(entity)
            } catch (failure: Throwable) {
                // Still can't reach the server - leave this and every later entry queued, and let
                // the caller (PendingScoreSyncWorker) back off and retry the whole queue later
                // rather than silently reordering it by skipping ahead.
                throw failure
            }
        }
    }

    /** Mirrors `applyScoreSubmission` in mock-backend/progression.js's own copy - a round only
     * ever reaches here having already passed (see [submitScore]'s "no partial credit" doc), so
     * [MemoryJourneyRules.pointsPerLevelCompleted] always applies; the other three sources are
     * each conditional on this exact round/streak-update actually having triggered them. */
    private fun journeyPointsAwardedFor(sceneAccuracy: Float, streakMilestoneReached: Int?, newlyUnlockedAchievementCount: Int): Long {
        val rules = MemoryJourneyRules.Default
        var points = rules.pointsPerLevelCompleted
        if (sceneAccuracy >= 1f) points += rules.perfectAccuracyBonus
        if (streakMilestoneReached != null) points += rules.pointsPerStreakMilestone
        points += rules.pointsPerAchievementUnlocked * newlyUnlockedAchievementCount
        return points
    }

    /** Mirrors `applyScoreSubmission` in mock-backend/progression.js so client and server agree.
     * Daily/Weekly Challenge bypass the score-based formula entirely - a flat win bonus (or
     * nothing, short of a win) instead of a variable one, matching their fixed object count. */
    private fun coinsAwardedFor(mode: GameMode, score: ScoreResult): Long {
        val rules = ProgressionRules.Default
        val won = score.sceneAccuracy >= rules.challengeWinAccuracyThreshold
        return when (mode) {
            GameMode.DAILY_CHALLENGE -> if (won) rules.dailyChallengeWinCoins else 0L
            GameMode.WEEKLY_CHALLENGE -> if (won) rules.weeklyChallengeWinCoins else 0L
            else -> {
                val base = (score.finalScore * rules.coinsPerScorePoint).roundToLong()
                val comboBonus = (score.comboCount - 1).coerceAtLeast(0) * rules.comboBonusCoinsPerStep
                base + comboBonus
            }
        }
    }

    private suspend fun updateStatistics(
        mode: GameMode,
        score: ScoreResult,
        playedOnEpochDay: Long,
        timeTakenMs: Long,
    ): PlayerStatistics {
        val cached = statisticsDao.get()?.toDomain() ?: PlayerStatistics.EMPTY
        val won = score.sceneAccuracy >= ProgressionRules.Default.challengeWinAccuracyThreshold
        // updateStatistics only ever runs on a pass now (see GameplayViewModel.submitReconstruction's
        // passedThisLevel gate) - every call is itself a won round, so the streak always extends;
        // it's only ever broken by resetWinStreak(), called on an explicit failed attempt.
        val newWinStreak = cached.currentWinStreak + 1
        val updated = cached.copy(
            gamesPlayed = cached.gamesPlayed + 1,
            currentWinStreak = newWinStreak,
            longestWinStreak = maxOf(cached.longestWinStreak, newWinStreak),
            totalScore = cached.totalScore + score.finalScore,
            bestAccuracy = maxOf(cached.bestAccuracy, score.sceneAccuracy),
            bestScore = maxOf(cached.bestScore, score.finalScore),
            totalAccuracySum = cached.totalAccuracySum + score.sceneAccuracy,
            totalTimeTakenMs = cached.totalTimeTakenMs + timeTakenMs.coerceAtLeast(0L),
            fastestCompletionMs = if (timeTakenMs > 0) {
                cached.fastestCompletionMs?.let { minOf(it, timeTakenMs) } ?: timeTakenMs
            } else {
                cached.fastestCompletionMs
            },
            perfectGames = cached.perfectGames + if (score.sceneAccuracy >= 1f) 1 else 0,
            highestCombo = maxOf(cached.highestCombo, score.comboCount),
            objectsMemorized = cached.objectsMemorized + score.objectScores.size,
            daysPlayed = cached.daysPlayed + if (playedOnEpochDay != cached.lastStatsUpdateEpochDay) 1 else 0,
            lastStatsUpdateEpochDay = playedOnEpochDay,
            dailyChallengesWon = cached.dailyChallengesWon + if (mode == GameMode.DAILY_CHALLENGE && won) 1 else 0,
            weeklyChallengesWon = cached.weeklyChallengesWon + if (mode == GameMode.WEEKLY_CHALLENGE && won) 1 else 0,
        )
        statisticsDao.upsert(updated.toCacheEntity())
        return updated
    }

    override suspend fun resetWinStreak(): Unit = withContext(dispatchers.io) {
        val cached = statisticsDao.get()?.toDomain() ?: return@withContext
        if (cached.currentWinStreak == 0) return@withContext
        statisticsDao.upsert(cached.copy(currentWinStreak = 0).toCacheEntity())
    }

    override suspend fun recordLeaderboardRank(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long): List<AchievementId> =
        withContext(dispatchers.io) {
            val cached = statisticsDao.get()?.toDomain() ?: PlayerStatistics.EMPTY
            val updatedDaily = betterRank(cached.dailyBestRank, dailyRank)
            val updatedWeekly = betterRank(cached.weeklyBestRank, weeklyRank)
            if (updatedDaily == cached.dailyBestRank && updatedWeekly == cached.weeklyBestRank) return@withContext emptyList()

            val updatedStatistics = cached.copy(dailyBestRank = updatedDaily, weeklyBestRank = updatedWeekly)
            statisticsDao.upsert(updatedStatistics.toCacheEntity())
            val profile = progressDao.get()?.toDomain() ?: PlayerProfile.EMPTY
            evaluateAndPersistAchievements(profile, updatedStatistics, todayEpochDay)
        }

    /** Lower is better (rank 1 beats rank 50) - `null` means "no rank recorded yet," which always
     * loses to any real rank. */
    private fun betterRank(current: Int?, candidate: Int?): Int? = when {
        candidate == null -> current
        current == null -> candidate
        else -> minOf(current, candidate)
    }

    private suspend fun evaluateAndPersistAchievements(
        profile: PlayerProfile,
        statistics: PlayerStatistics,
        playedOnEpochDay: Long,
    ): List<AchievementId> {
        val alreadyUnlocked = fetchUnlockedAchievementIds()
        // Best-stars-by-level already only records a level once it's actually been passed (see
        // LevelCampaignRepositoryImpl.recordCompletion), so both checks below are "at least one
        // pass ever," not "currently unlocked/frontier reached" - a player can sit at the level
        // 100 frontier without having beaten it, and this must not count that as completion.
        val bestStars = levelCampaignRepository.getAllBestStars()
        val snapshot = ProgressSnapshot(
            statistics = statistics,
            profile = profile,
            level = xpCurve.levelForXp(profile.xp),
            hasCompletedCampaign = bestStars.containsKey(LevelCampaignRules.Default.maxLevel),
            hasThreeStarClear = bestStars.values.any { it >= 3 },
        )
        val newlyUnlocked = achievementEvaluator.evaluateNewlyUnlocked(snapshot, alreadyUnlocked)
        newlyUnlocked.forEach { id ->
            unlockedAchievementDao.upsert(UnlockedAchievementEntity(id.name, playedOnEpochDay))
        }
        return newlyUnlocked
    }

    private suspend fun fetchUnlockedAchievementIds(): Set<AchievementId> = unlockedAchievementDao.getAll()
        .mapNotNull { entity -> runCatching { AchievementId.valueOf(entity.achievementId) }.getOrNull() }
        .toSet()

    private suspend fun evaluateAndPersistRewards(level: Int, playedOnEpochDay: Long): List<RewardDefinition> {
        val alreadyUnlocked = fetchUnlockedRewardIds()
        val newlyUnlocked = rewardEvaluator.evaluateNewlyUnlocked(level, alreadyUnlocked)
        newlyUnlocked.forEach { definition ->
            unlockedRewardDao.upsert(UnlockedRewardEntity(definition.id.name, playedOnEpochDay))
        }
        return newlyUnlocked
    }

    private suspend fun fetchUnlockedRewardIds(): Set<RewardId> = unlockedRewardDao.getAll()
        .mapNotNull { entity -> runCatching { RewardId.valueOf(entity.rewardId) }.getOrNull() }
        .toSet()

    private fun PlayerProfile.toCacheEntity() = PlayerProgressCacheEntity(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        journeyPoints = journeyPoints,
        lastSyncedAt = System.currentTimeMillis(),
    )

    private fun PlayerProgressCacheEntity.toDomain() = PlayerProfile(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        journeyPoints = journeyPoints,
    )

    private fun PlayerStatistics.toCacheEntity() = StatisticsCacheEntity(
        gamesPlayed = gamesPlayed,
        totalScore = totalScore,
        bestAccuracy = bestAccuracy,
        bestScore = bestScore,
        totalAccuracySum = totalAccuracySum,
        totalTimeTakenMs = totalTimeTakenMs,
        fastestCompletionMs = fastestCompletionMs,
        perfectGames = perfectGames,
        highestCombo = highestCombo,
        objectsMemorized = objectsMemorized,
        daysPlayed = daysPlayed,
        lastStatsUpdateEpochDay = lastStatsUpdateEpochDay,
        dailyChallengesWon = dailyChallengesWon,
        weeklyChallengesWon = weeklyChallengesWon,
        dailyBestRank = dailyBestRank,
        weeklyBestRank = weeklyBestRank,
        currentWinStreak = currentWinStreak,
        longestWinStreak = longestWinStreak,
    )

    private fun StatisticsCacheEntity.toDomain() = PlayerStatistics(
        gamesPlayed = gamesPlayed,
        totalScore = totalScore,
        bestAccuracy = bestAccuracy,
        bestScore = bestScore,
        totalAccuracySum = totalAccuracySum,
        totalTimeTakenMs = totalTimeTakenMs,
        fastestCompletionMs = fastestCompletionMs,
        perfectGames = perfectGames,
        highestCombo = highestCombo,
        objectsMemorized = objectsMemorized,
        daysPlayed = daysPlayed,
        lastStatsUpdateEpochDay = lastStatsUpdateEpochDay,
        dailyChallengesWon = dailyChallengesWon,
        weeklyChallengesWon = weeklyChallengesWon,
        dailyBestRank = dailyBestRank,
        weeklyBestRank = weeklyBestRank,
        currentWinStreak = currentWinStreak,
        longestWinStreak = longestWinStreak,
    )
}
