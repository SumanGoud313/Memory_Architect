package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.data.remote.RemoteConfigApi
import com.suman.memoryarchitect.domain.model.RemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps `mock-backend/`'s REST API exactly as [RemoteConfigRepositoryImpl] talked to it before
 * [RemoteConfigRemoteSource] existed - same request, same response mapping, byte-for-byte the
 * original behavior. See [RemoteConfigRemoteSource]'s doc for why this is dev-only. */
@Singleton
class MockBackendRemoteConfigSource @Inject constructor(
    private val api: RemoteConfigApi,
) : RemoteConfigRemoteSource {

    override suspend fun getRemoteConfig(): RemoteConfig {
        val response = api.getRemoteConfig()
        return RemoteConfig(response.values, System.currentTimeMillis())
    }
}
