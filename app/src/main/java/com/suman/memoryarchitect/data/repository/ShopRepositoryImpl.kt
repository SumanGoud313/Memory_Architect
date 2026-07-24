package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.EquippedCosmeticEntity
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.model.SpinResult
import com.suman.memoryarchitect.domain.progression.LuckySpinEngine
import com.suman.memoryarchitect.domain.repository.ShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deliberately a sibling of [ProgressionRepositoryImpl], not an extension of it - see
 * [ShopRepository]'s doc. Shares the exact same physical coin-balance row
 * ([PlayerProgressCacheEntity] via [progressDao]) so "coins" only ever has one cache location,
 * without depending on [ProgressionRepositoryImpl] itself.
 *
 * Purchase/spin do NOT use [ProgressionRepositoryImpl.submitScore]'s optimistic-then-pending-sync
 * pattern - on failure they return a clean [Outcome.Error] with no local grant, since a purchase
 * is a real economic transaction that must never be double-credited by an offline retry (matches
 * [com.suman.memoryarchitect.core.billing.BillingManager]'s UX-honesty pattern, not the currently-dead
 * [PendingScoreSubmissionDao] queue). [equip] applies optimistically - no economic stake.
 */
@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendShopRemoteSource,
    private val firestoreSource: FirestoreShopRemoteSource,
    private val playerIdentityManager: PlayerIdentityManager,
    private val progressDao: PlayerProgressDao,
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val equippedCosmeticDao: EquippedCosmeticDao,
    private val equippedCosmeticsStore: EquippedCosmeticsStore,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : ShopRepository {

    private val spinEngine = LuckySpinEngine()

    /** Mirrors [ProgressionRepositoryImpl.activeRemoteSource] exactly - see its doc for why this
     * small policy is duplicated rather than shared (the codebase's existing convention). */
    private suspend fun activeRemoteSource(): ShopRemoteSource {
        if (!FirebaseAvailability.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    override suspend fun getOwnedCosmeticIds(): Set<CosmeticId> = withContext(dispatchers.io) {
        ownedCosmeticDao.getAll().mapNotNull { entity -> runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() }.toSet()
    }

    override suspend fun getEquippedCosmetics(): Map<CosmeticCategory, CosmeticId> = withContext(dispatchers.io) {
        equippedCosmeticDao.getAll().mapNotNull { entity ->
            val category = runCatching { CosmeticCategory.valueOf(entity.category) }.getOrNull() ?: return@mapNotNull null
            val id = runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() ?: return@mapNotNull null
            category to id
        }.toMap()
    }

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String): Outcome<PurchaseResult> = withContext(dispatchers.io) {
        try {
            val (updatedProfile, updatedOwned) = activeRemoteSource().purchase(id, purchaseNonce)
            progressDao.upsert(updatedProfile.toCacheEntity())
            persistOwned(updatedOwned, acquiredVia = "PURCHASE")
            Outcome.Success(PurchaseResult(id, updatedProfile))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (alreadyOwned: AlreadyOwnedCosmeticException) {
            Outcome.Error(AppError.Server(code = 409, message = alreadyOwned.message))
        } catch (insufficient: InsufficientCoinsException) {
            Outcome.Error(AppError.Server(code = 402, message = insufficient.message))
        } catch (duplicate: DuplicatePurchaseException) {
            Outcome.Error(AppError.Server(code = 409, message = duplicate.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun equip(category: CosmeticCategory, id: CosmeticId): Outcome<Unit> = withContext(dispatchers.io) {
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        equippedCosmeticDao.upsert(EquippedCosmeticEntity(category.name, id.name, todayEpochDay))
        // Local-only "Recently Used" browsing convenience - see ShopRepository.getRecentlyUsedCosmeticIds's
        // doc for why this never touches Firestore.
        ownedCosmeticDao.touchLastEquipped(id.name, clock.millis())
        // Pushed before the remote call, same "optimistic, local always wins" spot as the Room
        // upsert above - this is what makes an equipped border/etc. appear on every button
        // app-wide immediately, with no restart or screen re-navigation needed (see
        // EquippedCosmeticsStore's doc).
        equippedCosmeticsStore.setEquipped(category, id)
        try {
            activeRemoteSource().equip(category, id)
            Outcome.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            // Applied optimistically above regardless - see the class doc. The local equip stands
            // even if the background sync fails; a future successful call (or a fresh getEquipped
            // read once connectivity returns) reconciles it.
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun unequip(category: CosmeticCategory): Outcome<Unit> = withContext(dispatchers.io) {
        equippedCosmeticDao.deleteByCategory(category.name)
        equippedCosmeticsStore.setEquipped(category, null)
        try {
            activeRemoteSource().unequip(category)
            Outcome.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun spin(spinNonce: String): Outcome<SpinResult> = withContext(dispatchers.io) {
        val roll = spinEngine.spin()
        try {
            val outcome = activeRemoteSource().spin(roll.id, roll.rarity, spinNonce)
            progressDao.upsert(outcome.profile.toCacheEntity())
            persistOwned(outcome.ownedSkus, acquiredVia = "SPIN")
            Outcome.Success(
                SpinResult(outcome.awardedId, roll.rarity, outcome.wasDuplicate, outcome.coinsRefunded, outcome.profile),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (insufficient: InsufficientCoinsException) {
            Outcome.Error(AppError.Server(code = 402, message = insufficient.message))
        } catch (duplicate: DuplicatePurchaseException) {
            Outcome.Error(AppError.Server(code = 409, message = duplicate.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun toggleFavorite(id: CosmeticId) = withContext(dispatchers.io) {
        val current = ownedCosmeticDao.getAll().firstOrNull { it.sku == id.name } ?: return@withContext
        ownedCosmeticDao.setFavorite(id.name, !current.isFavorite)
    }

    override suspend fun getFavoriteCosmeticIds(): Set<CosmeticId> = withContext(dispatchers.io) {
        ownedCosmeticDao.getFavorites().mapNotNull { entity -> runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() }.toSet()
    }

    override suspend fun getRecentlyUsedCosmeticIds(limit: Int): List<CosmeticId> = withContext(dispatchers.io) {
        ownedCosmeticDao.getRecentlyUsed(limit).mapNotNull { entity -> runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() }
    }

    /** Upserts every sku currently in [skus] - a no-op for skus already owned locally (their row
     * just gets re-stamped), so this is safe to call with the server's full owned-set on every
     * purchase/spin rather than needing to diff for "what's new." */
    private suspend fun persistOwned(skus: Set<String>, acquiredVia: String) {
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        skus.forEach { sku ->
            ownedCosmeticDao.upsert(OwnedCosmeticEntity(sku, todayEpochDay, acquiredVia, System.currentTimeMillis()))
        }
    }

    private fun PlayerProfile.toCacheEntity() = PlayerProgressCacheEntity(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        lastSyncedAt = System.currentTimeMillis(),
    )
}
