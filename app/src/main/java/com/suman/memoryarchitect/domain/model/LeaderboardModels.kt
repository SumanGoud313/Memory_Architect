package com.suman.memoryarchitect.domain.model

enum class LeaderboardType { GLOBAL, DAILY, WEEKLY, MONTHLY }

/** One row of a leaderboard list. [primaryValue] is whichever metric that board is sorted by
 * (lifetime total score for [LeaderboardType.GLOBAL], that period's score for Daily/Weekly/Monthly).
 * [league]/[verified]/[avatarId]/[country] are denormalized onto every entry doc at submission time
 * (see `LeaderboardRepositoryImpl.writePeriodicEntry`) specifically so rendering a leaderboard page
 * never needs a second per-row lookup into `players/{uid}` - one list read is the whole cost. */
data class LeaderboardEntry(
    val rank: Int,
    val uid: String,
    val displayName: String,
    val primaryValue: Long,
    val accuracy: Float,
    val completionTimeMs: Long,
    val isCurrentPlayer: Boolean,
    val avatarId: String = AvatarCatalog.DEFAULT_AVATAR_ID,
    val league: League = League.APPRENTICE,
    val verified: Boolean = false,
    val country: String? = null,
    val avatarUrl: String? = null,
)

/** [currentPlayerRank] is `null` when the player hasn't submitted to this board yet, or ranks
 * outside the window [LeaderboardRepository]'s rank lookup covers - never a loading/error state
 * (those are [Outcome.Error] at the call site), just "no rank to show yet." */
data class LeaderboardResult(
    val type: LeaderboardType,
    val entries: List<LeaderboardEntry>,
    val currentPlayerRank: Int?,
)

/** Denormalized snapshot written to Firestore's `players/{uid}` doc after every scored round
 * resolves - see `LeaderboardRepository.submitGlobalStats`. Mirrors the Global Leaderboard
 * requirements directly; [PlayerStatistics] stays the local-only source most of these are computed
 * from. Deliberately does NOT carry `league`/`verified`/`avatarId`/`country` - those aren't
 * game-round-derived the way everything below is, they're identity/settings state the *repository*
 * attaches at write time (verified from [com.suman.memoryarchitect.core.auth.PlayerIdentityManager],
 * avatarId/country from [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore], league
 * computed fresh from [xp] via [League.forXp] rather than trusted from any caller) - keeping the
 * scoring call site ([com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]) free of
 * identity/settings concerns it has no natural reason to know about. */
data class GlobalLeaderboardStats(
    val highestLevel: Int,
    val totalStars: Int,
    val totalScore: Long,
    val perfectLevels: Int,
    val averageAccuracy: Float,
    val averageCompletionTimeMs: Long,
    val totalObjectsMemorized: Int,
    val totalLevelsCompleted: Int,
    val totalPlayTimeMs: Long,
    val xp: Long = 0L,
    val winStreak: Int = 0,
    val longestWinStreak: Int = 0,
    val achievementCount: Int = 0,
)

/** One Daily/Weekly/Monthly Challenge attempt's leaderboard-relevant numbers - written to that
 * period's `entries/{uid}` doc, keyed by [PlayerIdentityManager.uid] so a resubmission the same
 * period overwrites rather than duplicates (see [com.suman.memoryarchitect.core.auth.PlayerIdentityManager]). */
data class PeriodicLeaderboardSubmission(
    val score: Long,
    val accuracy: Float,
    val completionTimeMs: Long,
    val isPerfectRun: Boolean,
)
