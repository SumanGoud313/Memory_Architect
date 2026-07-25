package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.InventoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Online-first, same cache-bridge policy [ProgressionRepositoryImpl.getProfile] uses for reads -
 * a failed [getInventory] falls back to the local cache rather than surfacing an error, since a
 * stale balance is far less disruptive than blocking the Inventory screen entirely. */
@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendMissionRemoteSource,
    private val firestoreSource: FirestoreMissionRemoteSource,
    private val playerIdentityManager: PlayerIdentityManager,
    private val inventoryItemDao: InventoryItemDao,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : InventoryRepository {

    private suspend fun activeRemoteSource(): MissionRemoteSource {
        if (!FirebaseAvailability.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    override suspend fun getInventory(): Outcome<Inventory> = withContext(dispatchers.io) {
        try {
            val inventory = activeRemoteSource().getInventory()
            persistCache(inventory)
            Outcome.Success(inventory)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            val cached = Inventory(
                inventoryItemDao.getAll().mapNotNull { entity ->
                    runCatching { InventoryItemKind.valueOf(entity.kind) }.getOrNull()?.let { it to entity.quantity }
                }.toMap(),
            )
            if (cached.quantities.isNotEmpty()) Outcome.Success(cached) else Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun consumeItem(kind: InventoryItemKind, quantity: Int): Outcome<Inventory> = withContext(dispatchers.io) {
        try {
            val inventory = activeRemoteSource().consumeInventoryItem(kind, quantity)
            persistCache(inventory)
            Outcome.Success(inventory)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (insufficient: InsufficientInventoryException) {
            Outcome.Error(AppError.Server(code = 409, message = insufficient.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun cacheInventory(inventory: Inventory): Unit = withContext(dispatchers.io) {
        persistCache(inventory)
    }

    private suspend fun persistCache(inventory: Inventory) {
        InventoryItemKind.entries.forEach { kind ->
            inventoryItemDao.upsert(InventoryItemEntity(kind.name, inventory.quantityOf(kind)))
        }
    }
}
