package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.RemoteConfigCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online-first with a cache bridge: always attempts a network refresh first and writes
 * through to the TTL'd Room cache; only falls back to the cache when the network call
 * itself fails, and only to bridge a brief reconnect (never as a substitute source of truth).
 *
 * Which backend "the server" actually is lives entirely behind [activeRemoteSource] - see
 * [RemoteConfigRemoteSource]'s doc. Unlike [ProgressionRepositoryImpl]/[MissionRepositoryImpl],
 * there's no per-player identity to await here - remote config is the same values for every
 * install, so [FirebaseAvailabilityProvider.isConfigured] alone decides which source answers.
 * Routed through the injectable [FirebaseAvailabilityProvider] rather than reading
 * [com.suman.memoryarchitect.core.analytics.FirebaseAvailability] directly - see that
 * interface's doc for why this one call site needs to be test-substitutable.
 */
@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendRemoteConfigSource,
    private val firebaseSource: FirebaseRemoteConfigSource,
    private val dao: RemoteConfigDao,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : RemoteConfigRepository {

    private fun activeRemoteSource(): RemoteConfigRemoteSource =
        if (firebaseAvailabilityProvider.isConfigured) firebaseSource else mockBackendSource

    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> = withContext(dispatchers.io) {
        try {
            val config = activeRemoteSource().getRemoteConfig()
            dao.upsert(
                config.values.map { (key, value) ->
                    RemoteConfigCacheEntity(configKey = key, value = value, fetchedAt = config.fetchedAt)
                },
            )
            Outcome.Success(config)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            cachedFallback() ?: Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    private suspend fun cachedFallback(): Outcome.Success<RemoteConfig>? {
        val cached = dao.getAll()
        if (cached.isEmpty()) return null
        return Outcome.Success(
            RemoteConfig(
                values = cached.associate { it.configKey to it.value },
                fetchedAt = cached.minOf { it.fetchedAt },
            ),
        )
    }
}