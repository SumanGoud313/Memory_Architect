package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.RemoteConfigCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.data.remote.RemoteConfigApi
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
 */
@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val api: RemoteConfigApi,
    private val dao: RemoteConfigDao,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : RemoteConfigRepository {

    override suspend fun getRemoteConfig(): Outcome<RemoteConfig> = withContext(dispatchers.io) {
        try {
            val response = api.getRemoteConfig()
            val fetchedAt = System.currentTimeMillis()
            dao.upsert(
                response.values.map { (key, value) ->
                    RemoteConfigCacheEntity(configKey = key, value = value, fetchedAt = fetchedAt)
                },
            )
            Outcome.Success(RemoteConfig(response.values, fetchedAt))
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