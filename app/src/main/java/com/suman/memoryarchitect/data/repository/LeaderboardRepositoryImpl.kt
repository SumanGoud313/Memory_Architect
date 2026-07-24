package com.suman.memoryarchitect.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.AvatarCatalog
import com.suman.memoryarchitect.domain.model.GlobalLeaderboardStats
import com.suman.memoryarchitect.domain.model.League
import com.suman.memoryarchitect.domain.model.LeaderboardEntry
import com.suman.memoryarchitect.domain.model.LeaderboardResult
import com.suman.memoryarchitect.domain.model.LeaderboardType
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PeriodicLeaderboardSubmission
import com.suman.memoryarchitect.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See [LeaderboardRepository] for the contract. Firestore layout (see LEADERBOARD_SETUP.md for
 * the console steps + security rules this depends on):
 *
 * - `players/{uid}` - one denormalized doc per player, the Global Leaderboard's source: every
 *   field the leaderboard sorts/displays lives directly on this doc (no join, no second read).
 *   Writing it is a full overwrite each time (see [submitGlobalStats]) - it's a point-in-time
 *   cumulative snapshot, not an event log, so there's nothing to accumulate server-side.
 * - `leaderboardDaily/{yyyy-MM-dd}/entries/{uid}` and `leaderboardWeekly/{yyyy-'W'ww}/entries/{uid}`
 *   - period-keyed subcollections, one per calendar day / ISO week. This *is* the "reset
 *   automatically" mechanism: a new day/week is a brand-new, empty subcollection, nothing to
 *   clean up, no scheduled job needed. Keying each entry doc by [PlayerIdentityManager.uid]
 *   (never an auto-generated ID) means a resubmission the same period overwrites in place rather
 *   than creating a duplicate row - the core of this app's leaderboard anti-duplicate-submission
 *   guarantee, enforced by the schema itself rather than application logic that could be bypassed.
 *
 * Every write/read is guarded by [FirebaseAvailability.isConfigured] and a resolved
 * [PlayerIdentityManager.uid], and every Firestore exception is caught and mapped to
 * [Outcome.Error] - a leaderboard being unreachable (Firestore not yet enabled in the console, the
 * device is offline, security rules reject an unexpected shape) never throws past this class and
 * never blocks or degrades gameplay, exactly the same non-blocking philosophy
 * [com.suman.memoryarchitect.core.analytics.FirebaseRemoteConfigSource] already established for
 * this app's other optional Firebase dependency.
 */
@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val preferences: UserPreferencesDataStore,
    private val progressDao: PlayerProgressDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : LeaderboardRepository {

    private val firestore by lazy { Firebase.firestore }

    override suspend fun submitGlobalStats(stats: GlobalLeaderboardStats): Outcome<Unit> = withContext(dispatchers.io) {
        guarded { uid ->
            val identity = currentIdentityFields()
            val doc = mapOf(
                "displayName" to uid.toPlayerDisplayName(),
                "highestLevel" to stats.highestLevel,
                "totalStars" to stats.totalStars,
                "totalScore" to stats.totalScore,
                "perfectLevels" to stats.perfectLevels,
                "averageAccuracy" to stats.averageAccuracy.toDouble(),
                "averageCompletionTimeMs" to stats.averageCompletionTimeMs,
                "totalObjectsMemorized" to stats.totalObjectsMemorized,
                "totalLevelsCompleted" to stats.totalLevelsCompleted,
                "totalPlayTimeMs" to stats.totalPlayTimeMs,
                "winStreak" to stats.winStreak,
                "longestWinStreak" to stats.longestWinStreak,
                "achievementCount" to stats.achievementCount,
                "updatedAtEpochMs" to clock.millis(),
            ) + identity
            playersCollection().document(uid).set(doc).await()
            Unit
        }
    }

    override suspend fun submitDailyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> =
        withContext(dispatchers.io) {
            guarded { uid -> writePeriodicEntry(dailyEntriesCollection(), uid, submission) }
        }

    override suspend fun submitWeeklyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> =
        withContext(dispatchers.io) {
            guarded { uid -> writePeriodicEntry(weeklyEntriesCollection(), uid, submission) }
        }

    override suspend fun submitMonthlyScore(submission: PeriodicLeaderboardSubmission): Outcome<Unit> =
        withContext(dispatchers.io) {
            guarded { uid -> writePeriodicEntry(monthlyEntriesCollection(), uid, submission) }
        }

    private suspend fun writePeriodicEntry(collection: CollectionReference, uid: String, submission: PeriodicLeaderboardSubmission): Unit {
        // Never let a resubmission (the same Daily/Weekly/Monthly puzzle attempted again after the
        // first clear, or a retried network call) overwrite a better prior score with a worse one -
        // the schema's overwrite-by-uid semantics already stop duplicates, this stops regressions.
        val existingScore = collection.document(uid).get().await().getLong("score") ?: -1L
        if (submission.score <= existingScore) return
        val doc = mapOf(
            "displayName" to uid.toPlayerDisplayName(),
            "score" to submission.score,
            "accuracy" to submission.accuracy.toDouble(),
            "completionTimeMs" to submission.completionTimeMs,
            "isPerfectRun" to submission.isPerfectRun,
            "submittedAtEpochMs" to clock.millis(),
        ) + currentIdentityFields()
        collection.document(uid).set(doc).await()
    }

    /** avatarId/league/verified/country, denormalized onto every leaderboard doc (Global and every
     * periodic entry) at write time - the Firestore-cost-optimization principle behind this whole
     * denormalized-per-doc design (see [LeaderboardEntry]'s doc): rendering a leaderboard page
     * reads exactly one collection, never a second lookup per row into `players/{uid}` to resolve
     * what avatar/league/badge each row should show. [League] is always recomputed fresh from the
     * player's current xp (never trusted from a stored value - see [League]'s own doc); [verified]
     * mirrors [PlayerIdentityManager.isVerified] directly, so a client can no more fake it than it
     * can fake having linked a real Google account. */
    private suspend fun currentIdentityFields(): Map<String, Any?> {
        val xp = progressDao.get()?.xp ?: 0L
        return mapOf(
            "avatarId" to preferences.avatarId.first(),
            "avatarUrl" to preferences.avatarUrl.first(),
            "country" to preferences.country.first(),
            "verified" to playerIdentityManager.isVerified.value,
            "league" to League.forXp(xp).name,
        )
    }

    override suspend fun getGlobalLeaderboard(limit: Int): Outcome<LeaderboardResult> = withContext(dispatchers.io) {
        guarded { uid ->
            fetchLeaderboard(
                type = LeaderboardType.GLOBAL,
                collection = playersCollection(),
                sortField = "totalScore",
                accuracyField = "averageAccuracy",
                timeField = "averageCompletionTimeMs",
                limit = limit,
                uid = uid,
            )
        }
    }

    override suspend fun getDailyLeaderboard(limit: Int): Outcome<LeaderboardResult> = withContext(dispatchers.io) {
        guarded { uid ->
            fetchLeaderboard(
                type = LeaderboardType.DAILY,
                collection = dailyEntriesCollection(),
                sortField = "score",
                accuracyField = "accuracy",
                timeField = "completionTimeMs",
                limit = limit,
                uid = uid,
            )
        }
    }

    override suspend fun getWeeklyLeaderboard(limit: Int): Outcome<LeaderboardResult> = withContext(dispatchers.io) {
        guarded { uid ->
            fetchLeaderboard(
                type = LeaderboardType.WEEKLY,
                collection = weeklyEntriesCollection(),
                sortField = "score",
                accuracyField = "accuracy",
                timeField = "completionTimeMs",
                limit = limit,
                uid = uid,
            )
        }
    }

    override suspend fun getMonthlyLeaderboard(limit: Int): Outcome<LeaderboardResult> = withContext(dispatchers.io) {
        guarded { uid ->
            fetchLeaderboard(
                type = LeaderboardType.MONTHLY,
                collection = monthlyEntriesCollection(),
                sortField = "score",
                accuracyField = "accuracy",
                timeField = "completionTimeMs",
                limit = limit,
                uid = uid,
            )
        }
    }

    private suspend fun fetchLeaderboard(
        type: LeaderboardType,
        collection: CollectionReference,
        sortField: String,
        accuracyField: String,
        timeField: String,
        limit: Int,
        uid: String,
    ): LeaderboardResult {
        val snapshot = collection.orderBy(sortField, Query.Direction.DESCENDING).limit(limit.toLong()).get().await()
        val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
            val displayName = doc.getString("displayName") ?: return@mapIndexedNotNull null
            val avatarId = doc.getString("avatarId")?.takeIf { AvatarCatalog.isValidId(it) } ?: AvatarCatalog.DEFAULT_AVATAR_ID
            val league = doc.getString("league")?.let { runCatching { League.valueOf(it) }.getOrNull() } ?: League.APPRENTICE
            LeaderboardEntry(
                rank = index + 1,
                uid = doc.id,
                displayName = displayName,
                primaryValue = doc.getLong(sortField) ?: 0L,
                accuracy = doc.getDouble(accuracyField)?.toFloat() ?: 0f,
                completionTimeMs = doc.getLong(timeField) ?: 0L,
                isCurrentPlayer = doc.id == uid,
                avatarId = avatarId,
                league = league,
                verified = doc.getBoolean("verified") ?: false,
                country = doc.getString("country"),
                avatarUrl = doc.getString("avatarUrl"),
            )
        }
        val rankFromPage = entries.firstOrNull { it.isCurrentPlayer }?.rank
        val rank = rankFromPage ?: rankOutsidePage(collection, uid, sortField)
        return LeaderboardResult(type, entries, rank)
    }

    /** For a player who didn't place inside [fetchLeaderboard]'s fetched page - a server-side
     * COUNT() aggregation query (`whereGreaterThan` + `.count()`) rather than downloading the
     * whole collection to find their position, so this stays cheap and scalable regardless of how
     * many players exist: it's one small aggregate read, not O(total players). */
    private suspend fun rankOutsidePage(collection: CollectionReference, uid: String, sortField: String): Int? {
        val mySnapshot = collection.document(uid).get().await()
        if (!mySnapshot.exists()) return null
        val myValue = mySnapshot.getLong(sortField) ?: return null
        val aboveCount = collection.whereGreaterThan(sortField, myValue)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
        return aboveCount.toInt() + 1
    }

    private fun playersCollection(): CollectionReference = firestore.collection(PLAYERS_COLLECTION)

    private fun dailyEntriesCollection(): CollectionReference =
        firestore.collection(DAILY_LEADERBOARD_COLLECTION).document(dailyPeriodKey()).collection(ENTRIES_SUBCOLLECTION)

    private fun weeklyEntriesCollection(): CollectionReference =
        firestore.collection(WEEKLY_LEADERBOARD_COLLECTION).document(weeklyPeriodKey()).collection(ENTRIES_SUBCOLLECTION)

    private fun monthlyEntriesCollection(): CollectionReference =
        firestore.collection(MONTHLY_LEADERBOARD_COLLECTION).document(monthlyPeriodKey()).collection(ENTRIES_SUBCOLLECTION)

    /** UTC calendar date, e.g. "2026-07-20" - matches the mock backend's own UTC-based Daily
     * Challenge seeding (`mock-backend/index.js`'s `utcDateParts`), so "today's leaderboard" and
     * "today's puzzle" always agree on what day it is. */
    private fun dailyPeriodKey(): String = LocalDate.now(ZoneOffset.UTC).toString()

    /** ISO year + ISO week number, e.g. "2026-W29" - matches the mock backend's own ISO week
     * computation (`isoWeekNumber` in mock-backend/index.js) for the same "leaderboard period
     * agrees with puzzle period" reason as [dailyPeriodKey]. */
    private fun weeklyPeriodKey(): String {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fields = WeekFields.ISO
        val week = today.get(fields.weekOfWeekBasedYear())
        val year = today.get(fields.weekBasedYear())
        return String.format(Locale.ROOT, "%04d-W%02d", year, week)
    }

    /** UTC calendar year-month, e.g. "2026-07" - same automatic-reset-via-fresh-subcollection
     * mechanism as [dailyPeriodKey]/[weeklyPeriodKey], just keyed a month at a time. */
    private fun monthlyPeriodKey(): String {
        val today = LocalDate.now(ZoneOffset.UTC)
        return String.format(Locale.ROOT, "%04d-%02d", today.year, today.monthValue)
    }

    /** Runs [block] with a resolved player uid, uniformly turning "not signed in yet" and any
     * Firestore failure into [Outcome.Error] - the one place every public method's guard/try/catch
     * boilerplate lives instead of repeating it five times. */
    private suspend fun <T> guarded(block: suspend (uid: String) -> T): Outcome<T> {
        if (!FirebaseAvailability.isConfigured) return Outcome.Error(AppError.FeatureUnavailable)
        val uid = playerIdentityManager.awaitUid() ?: return Outcome.Error(AppError.FeatureUnavailable)
        return try {
            Outcome.Success(block(uid))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (firestoreError: FirebaseFirestoreException) {
            Log.w(TAG, "Leaderboard call failed: ${firestoreError.code}", firestoreError)
            Outcome.Error(firestoreError.toAppError())
        } catch (t: Throwable) {
            Log.w(TAG, "Leaderboard call failed", t)
            Outcome.Error(AppError.Unknown(t))
        }
    }

    /** [FirebaseFirestoreException.Code.UNAVAILABLE] (offline/unreachable) and
     * [FirebaseFirestoreException.Code.PERMISSION_DENIED] (Firestore Database or its security
     * rules not yet set up in the console - see LEADERBOARD_SETUP.md) both mean the same thing to
     * a caller: the feature isn't available right now, not "something is broken." Every other
     * code is a genuine unexpected failure. */
    private fun FirebaseFirestoreException.toAppError(): AppError = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        -> AppError.FeatureUnavailable
        else -> AppError.Unknown(this)
    }

    private companion object {
        const val TAG = "LeaderboardRepository"
        const val PLAYERS_COLLECTION = "players"
        const val DAILY_LEADERBOARD_COLLECTION = "leaderboardDaily"
        const val WEEKLY_LEADERBOARD_COLLECTION = "leaderboardWeekly"
        const val MONTHLY_LEADERBOARD_COLLECTION = "leaderboardMonthly"
        const val ENTRIES_SUBCOLLECTION = "entries"
    }
}

/** A stable, non-PII display name derived purely from the uid - no username/login flow exists in
 * this app (see [PlayerIdentityManager]), so this is the only "name" a leaderboard entry ever has.
 * Deterministic (same uid always formats the same way) rather than random-per-call, so it doesn't
 * change between one submission and the next. */
internal fun String.toPlayerDisplayName(): String {
    val suffix = takeLast(4).uppercase(Locale.ROOT)
    return "Architect#$suffix"
}
