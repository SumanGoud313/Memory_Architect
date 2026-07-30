package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.EquippedCosmeticEntity
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.LuckySpinStateDao
import com.suman.memoryarchitect.core.database.LuckySpinStateEntity
import com.suman.memoryarchitect.core.database.MysteryChestAdStateDao
import com.suman.memoryarchitect.core.database.MysteryChestAdStateEntity
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticEntity
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.LuckySpinState
import com.suman.memoryarchitect.domain.model.MysteryChestAdClaimResult
import com.suman.memoryarchitect.domain.model.MysteryChestAdState
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PurchaseResult
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import com.suman.memoryarchitect.domain.model.SpinResult
import com.suman.memoryarchitect.domain.progression.LuckySpinEngine
import com.suman.memoryarchitect.domain.progression.PermanentFreeCosmetics
import com.suman.memoryarchitect.domain.repository.ShopRepository
import com.suman.memoryarchitect.domain.repository.SpinSource
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
    private val luckySpinStateDao: LuckySpinStateDao,
    private val mysteryChestAdStateDao: MysteryChestAdStateDao,
    private val inventoryItemDao: InventoryItemDao,
    private val equippedCosmeticsStore: EquippedCosmeticsStore,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : ShopRepository {

    private val spinEngine = LuckySpinEngine()

    /** Mirrors [ProgressionRepositoryImpl.activeRemoteSource] exactly - see its doc for why this
     * small policy is duplicated rather than shared (the codebase's existing convention). */
    private suspend fun activeRemoteSource(): ShopRemoteSource {
        if (!firebaseAvailabilityProvider.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    /** Unions in [PermanentFreeCosmetics.ids] - every player owns those unconditionally, free,
     * with no purchase/grant/migration ever needed (see that object's doc), so no persisted Room/
     * Firestore row for them has to exist at all. */
    override suspend fun getOwnedCosmeticIds(): Set<CosmeticId> = withContext(dispatchers.io) {
        ownedCosmeticDao.getAll().mapNotNull { entity -> runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() }.toSet() + PermanentFreeCosmetics.ids
    }

    /** [PermanentFreeCosmetics.defaultEquippedByCategory] fills in only the categories with no
     * explicit persisted row - a player who has equipped something else for that category keeps
     * their own choice; unequipping reverts to this default rather than a bare/unstyled look. */
    override suspend fun getEquippedCosmetics(): Map<CosmeticCategory, CosmeticId> = withContext(dispatchers.io) {
        val persisted = equippedCosmeticDao.getAll().mapNotNull { entity ->
            val category = runCatching { CosmeticCategory.valueOf(entity.category) }.getOrNull() ?: return@mapNotNull null
            val id = runCatching { CosmeticId.valueOf(entity.sku) }.getOrNull() ?: return@mapNotNull null
            category to id
        }.toMap()
        PermanentFreeCosmetics.defaultEquippedByCategory + persisted
    }

    override suspend fun purchase(id: CosmeticId, purchaseNonce: String, useDiscountCoupon: Boolean): Outcome<PurchaseResult> = withContext(dispatchers.io) {
        try {
            val (updatedProfile, updatedOwned) = activeRemoteSource().purchase(id, purchaseNonce, useDiscountCoupon)
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
        } catch (insufficientInventory: InsufficientInventoryException) {
            Outcome.Error(AppError.Server(code = 409, message = insufficientInventory.message))
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

    /** For [CosmeticCategory.BACKGROUND_THEME]/[CosmeticCategory.PROFILE_BORDER], the live
     * in-memory store falls straight back to [PermanentFreeCosmetics.defaultEquippedByCategory]
     * rather than going bare - same "there's no real empty state, only a default" reasoning
     * [getEquippedCosmetics] already documents, just applied immediately instead of waiting for
     * the next cold start's fresh read to reapply it. */
    override suspend fun unequip(category: CosmeticCategory): Outcome<Unit> = withContext(dispatchers.io) {
        equippedCosmeticDao.deleteByCategory(category.name)
        equippedCosmeticsStore.setEquipped(category, PermanentFreeCosmetics.defaultEquippedByCategory[category])
        try {
            activeRemoteSource().unequip(category)
            Outcome.Success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun getLuckySpinState(): LuckySpinState = withContext(dispatchers.io) {
        luckySpinStateDao.get()?.toDomain() ?: LuckySpinState.EMPTY
    }

    override suspend fun spin(spinNonce: String, source: SpinSource): Outcome<SpinResult> = withContext(dispatchers.io) {
        val cachedSpinState = luckySpinStateDao.get()?.toDomain() ?: LuckySpinState.EMPTY
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        val reward = spinEngine.spin(guaranteeCosmetic = !cachedSpinState.hasEverSpun, todayEpochDay = todayEpochDay)
        val request = when (reward) {
            is SpinRewardKind.Coins -> SpinRequest.Coins(reward.amount)
            is SpinRewardKind.Cosmetic -> SpinRequest.Cosmetic(reward.id, reward.rarity)
        }
        try {
            val outcome = activeRemoteSource().spin(request, spinNonce, source, todayEpochDay)
            progressDao.upsert(outcome.profile.toCacheEntity())
            luckySpinStateDao.upsert(outcome.spinState.toCacheEntity())
            if (outcome.reward is SpinRewardKind.Cosmetic) persistOwned(outcome.ownedSkus, acquiredVia = "SPIN")
            Outcome.Success(
                SpinResult(outcome.reward, outcome.wasDuplicate, outcome.coinsRefunded, outcome.profile, outcome.spinState),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (notAvailable: SpinNotAvailableException) {
            Outcome.Error(AppError.Server(code = 429, message = notAvailable.message))
        } catch (insufficient: InsufficientCoinsException) {
            Outcome.Error(AppError.Server(code = 402, message = insufficient.message))
        } catch (duplicate: DuplicatePurchaseException) {
            Outcome.Error(AppError.Server(code = 409, message = duplicate.message))
        } catch (insufficientInventory: InsufficientInventoryException) {
            Outcome.Error(AppError.Server(code = 409, message = insufficientInventory.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun getMysteryChestAdState(): MysteryChestAdState = withContext(dispatchers.io) {
        mysteryChestAdStateDao.get()?.toDomain() ?: MysteryChestAdState.EMPTY
    }

    override suspend fun claimAdMysteryChest(claimNonce: String): Outcome<MysteryChestAdClaimResult> = withContext(dispatchers.io) {
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        try {
            val outcome = activeRemoteSource().claimAdMysteryChest(claimNonce, todayEpochDay)
            InventoryItemKind.entries.forEach { kind ->
                inventoryItemDao.upsert(InventoryItemEntity(kind.name, outcome.inventoryQuantities[kind] ?: 0))
            }
            mysteryChestAdStateDao.upsert(outcome.claimState.toCacheEntity())
            Outcome.Success(MysteryChestAdClaimResult(Inventory(outcome.inventoryQuantities), outcome.claimState))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (notAvailable: MysteryChestClaimNotAvailableException) {
            Outcome.Error(AppError.Server(code = 430, message = notAvailable.message))
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

    // streakShields/journeyPoints were previously omitted here, silently zeroing both in the local
    // Room cache after any purchase/spin even though the real Firestore document still had the
    // correct values (every write there uses SetOptions.merge()) - see
    // FirestoreShopRemoteSource.toProfile's identical fix and its own doc for the full story.
    private fun LuckySpinStateEntity.toDomain() = LuckySpinState(
        lastFreeSpinEpochDay = lastFreeSpinEpochDay,
        lastAdSpinEpochDay = lastAdSpinEpochDay,
        adSpinsUsedToday = adSpinsUsedToday,
        hasEverSpun = hasEverSpun,
    )

    private fun LuckySpinState.toCacheEntity() = LuckySpinStateEntity(
        lastFreeSpinEpochDay = lastFreeSpinEpochDay,
        lastAdSpinEpochDay = lastAdSpinEpochDay,
        adSpinsUsedToday = adSpinsUsedToday,
        hasEverSpun = hasEverSpun,
    )

    private fun MysteryChestAdStateEntity.toDomain() = MysteryChestAdState(
        lastClaimEpochDay = lastClaimEpochDay,
        claimsUsedToday = claimsUsedToday,
    )

    private fun MysteryChestAdState.toCacheEntity() = MysteryChestAdStateEntity(
        lastClaimEpochDay = lastClaimEpochDay,
        claimsUsedToday = claimsUsedToday,
    )

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
}
