package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.repository.AccountDeletionRepository
import com.suman.memoryarchitect.domain.repository.LocalProgressResetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See [AccountDeletionRepository] for the contract. Order matters: every Firestore delete below
 * runs first, while [PlayerIdentityManager.awaitUid] is still a real, authenticated uid (every
 * `allow delete` rule in `firestore.rules` requires `request.auth.uid == uid`) - only once those
 * succeed does [PlayerIdentityManager.signOutAfterAccountDeletion] tear down the Auth session
 * itself, since doing that first would make every subsequent delete attempt fail
 * `PERMISSION_DENIED` with no signed-in user left to own them.
 */
@Singleton
class AccountDeletionRepositoryImpl @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
    private val localProgressResetRepository: LocalProgressResetRepository,
    private val preferences: UserPreferencesDataStore,
    private val dispatchers: DispatcherProvider,
) : AccountDeletionRepository {

    private val firestore by lazy { Firebase.firestore }

    override suspend fun deleteAccount(): Result<Unit> = withContext(dispatchers.io) {
        if (!firebaseAvailabilityProvider.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Firebase isn't configured - nothing to delete"))
        }
        val uid = playerIdentityManager.awaitUid()
            ?: return@withContext Result.failure(IllegalStateException("No signed-in player yet - try again in a moment"))

        try {
            (DIRECT_COLLECTIONS.map { collection -> async { firestore.collection(collection).document(uid).delete().await() } } +
                currentPeriodLeaderboardRefs(uid).map { ref -> async { ref.delete().await() } })
                .awaitAll()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            return@withContext Result.failure(failure)
        }

        // Best-effort from here on - the player's data is already gone at this point, which is
        // what actually matters; see PlayerIdentityManager.signOutAfterAccountDeletion's own doc
        // for why an Auth-side failure specifically is never surfaced as a failure of this call.
        localProgressResetRepository.resetAll()
        playerIdentityManager.signOutAfterAccountDeletion()
        preferences.clearAll()
        Result.success(Unit)
    }

    /** Only the *current* Daily/Weekly/Monthly period's entry is reachable this way (each period's
     * entries live under that period's own document, and there's no practical client-side way to
     * enumerate every past period this player ever competed in) - a past, already-closed period's
     * entry may still exist after this. Documented as a known limitation in the privacy policy. */
    private fun currentPeriodLeaderboardRefs(uid: String) = listOf(
        firestore.collection("leaderboardDaily").document(dailyPeriodKey()).collection("entries").document(uid),
        firestore.collection("leaderboardWeekly").document(weeklyPeriodKey()).collection("entries").document(uid),
        firestore.collection("leaderboardMonthly").document(monthlyPeriodKey()).collection("entries").document(uid),
    )

    // Mirrors LeaderboardRepositoryImpl's own period-key functions exactly - keep both in sync.
    private fun dailyPeriodKey(): String = LocalDate.now(ZoneOffset.UTC).toString()

    private fun weeklyPeriodKey(): String {
        val today = LocalDate.now(ZoneOffset.UTC)
        val fields = WeekFields.ISO
        return String.format(Locale.ROOT, "%04d-W%02d", today.get(fields.weekBasedYear()), today.get(fields.weekOfWeekBasedYear()))
    }

    private fun monthlyPeriodKey(): String {
        val today = LocalDate.now(ZoneOffset.UTC)
        return String.format(Locale.ROOT, "%04d-%02d", today.year, today.monthValue)
    }

    private companion object {
        // Every single-document-per-uid collection this app writes, beyond the narrower 5
        // LocalProgressResetRepository already covers (playerProfiles/dailyRewards/inventory/
        // missionClaims/returningPlayerGifts) - see that repository's own doc for why its scope is
        // deliberately narrower (a routine "Reset Progress" isn't meant to erase leaderboard
        // history or owned cosmetics; full account deletion has no such restraint).
        val DIRECT_COLLECTIONS = listOf(
            "players", "playerProfiles", "playerCosmetics", "dailyRewards", "returningPlayerGifts",
            "inventory", "missionClaims", "missionCategoryBonusClaims", "missionRefreshState",
            "luckySpinState", "mysteryChestAdState",
        )
    }
}
