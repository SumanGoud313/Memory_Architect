package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.GlobalLeaderboardStats
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PeriodicLeaderboardSubmission

/**
 * The one boundary between this app and the shared, multi-player Firestore backend - every other
 * repository in this app is either purely local or talks to the single-profile dev mock backend
 * (see `mock-backend/`). [Outcome.Error] with [com.suman.memoryarchitect.domain.model.AppError.FeatureUnavailable]
 * is the expected, non-exceptional result whenever there's no signed-in player identity yet (see
 * [com.suman.memoryarchitect.core.auth.PlayerIdentityManager]) or Firestore itself isn't reachable/
 * configured - callers always treat that as "leaderboards aren't available right now," never as a
 * reason to block gameplay or show an alarming error.
 */
interface LeaderboardRepository {
    /** Denormalized write to this player's `players/{uid}` doc - what the Global Leaderboard sorts
     * and displays. Safe to call after every scored round; each call fully overwrites the
     * previous snapshot (it's a point-in-time cumulative total, not an append). */
    suspend fun submitGlobalStats(stats: GlobalLeaderboardStats): Outcome<Unit>

    suspend fun submitDailyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit>

    suspend fun submitWeeklyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit>

    /** Same shape/reset mechanism as [submitDailyScore]/[submitWeeklyScore], keyed by calendar
     * month instead. */
    suspend fun submitMonthlyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit>

    /** Top [limit] players by lifetime total score, plus the current player's own rank (via a
     * separate, cheap count-aggregation query - see the Firestore-backed implementation - rather
     * than requiring them to be inside the fetched page). */
    suspend fun getGlobalLeaderboard(limit: Int = DEFAULT_PAGE_SIZE): Outcome<LeaderboardResult>

    /** Today's (UTC) Daily Challenge leaderboard - a fresh, empty collection every day, which is
     * also the entire "reset automatically" mechanism (see the implementation's period-key doc). */
    suspend fun getDailyLeaderboard(limit: Int = DEFAULT_PAGE_SIZE): Outcome<LeaderboardResult>

    /** This ISO week's Weekly Challenge leaderboard - same automatic-reset mechanism as
     * [getDailyLeaderboard], keyed by ISO year-week instead of calendar day. */
    suspend fun getWeeklyLeaderboard(limit: Int = DEFAULT_PAGE_SIZE): Outcome<LeaderboardResult>

    /** This calendar month's leaderboard - same automatic-reset mechanism as [getDailyLeaderboard],
     * keyed by `yyyy-MM` instead. */
    suspend fun getMonthlyLeaderboard(limit: Int = DEFAULT_PAGE_SIZE): Outcome<LeaderboardResult>

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
    }
}
