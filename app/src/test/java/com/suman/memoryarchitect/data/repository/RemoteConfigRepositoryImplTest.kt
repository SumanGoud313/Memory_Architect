package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.CrashReporter
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.common.ImmediateDispatcherProvider
import com.suman.memoryarchitect.core.database.RemoteConfigCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.data.remote.RemoteConfigApi
import com.suman.memoryarchitect.data.remote.dto.RemoteConfigResponseDto
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private object RemoteConfigRepoNoopCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
}
private val testErrorMapper = ErrorMapper(RemoteConfigRepoNoopCrashReporter)

private class FakeRemoteConfigApi(
    private val response: RemoteConfigResponseDto? = null,
    private val error: Throwable? = null,
) : RemoteConfigApi {
    override suspend fun getRemoteConfig(): RemoteConfigResponseDto {
        error?.let { throw it }
        return requireNotNull(response)
    }
}

/** Forces the mock-backend path deterministically, regardless of whether this machine's local
 * `app/google-services.json` (see FIREBASE_SETUP.md) happens to exist - see
 * [FirebaseAvailabilityProvider]'s doc for why [RemoteConfigRepositoryImpl] takes this as an
 * injected seam instead of reading [com.suman.memoryarchitect.core.analytics.FirebaseAvailability]
 * directly. These tests only ever exercise [MockBackendRemoteConfigSource]'s cache-bridge
 * behavior - [FirebaseRemoteConfigSource] can't run at all in a plain JVM unit test (it needs a
 * real, initialized `FirebaseApp`), so nothing here can or should depend on Firebase actually
 * being configured on the machine running the suite. */
private class FakeFirebaseAvailabilityProvider(override val isConfigured: Boolean = false) : FirebaseAvailabilityProvider

private class FakeRemoteConfigDao : RemoteConfigDao {
    private val store = mutableMapOf<String, RemoteConfigCacheEntity>()

    override suspend fun getByKey(key: String) = store[key]

    override suspend fun getAll() = store.values.toList()

    override suspend fun upsert(entities: List<RemoteConfigCacheEntity>) {
        entities.forEach { store[it.configKey] = it }
    }

    override suspend fun deleteFetchedBefore(olderThan: Long) {
        store.entries.removeAll { it.value.fetchedAt < olderThan }
    }
}

class RemoteConfigRepositoryImplTest {

    @Test
    fun `network success writes through to cache and returns values`() = runTest {
        val dao = FakeRemoteConfigDao()
        val repository = RemoteConfigRepositoryImpl(
            mockBackendSource = MockBackendRemoteConfigSource(FakeRemoteConfigApi(response = RemoteConfigResponseDto(mapOf("ad_cadence" to "3")))),
            firebaseSource = FirebaseRemoteConfigSource(),
            dao = dao,
            dispatchers = ImmediateDispatcherProvider,
            errorMapper = testErrorMapper,
            firebaseAvailabilityProvider = FakeFirebaseAvailabilityProvider(),
        )

        val result = repository.getRemoteConfig()

        assertTrue(result is Outcome.Success)
        assertEquals("3", (result as Outcome.Success).data.values["ad_cadence"])
        assertEquals("3", dao.getByKey("ad_cadence")?.value)
    }

    @Test
    fun `network failure with existing cache falls back to cached values`() = runTest {
        val dao = FakeRemoteConfigDao().apply {
            upsert(listOf(RemoteConfigCacheEntity("ad_cadence", "2", fetchedAt = 1_000L)))
        }
        val repository = RemoteConfigRepositoryImpl(
            mockBackendSource = MockBackendRemoteConfigSource(FakeRemoteConfigApi(error = IOException("no route to host"))),
            firebaseSource = FirebaseRemoteConfigSource(),
            dao = dao,
            dispatchers = ImmediateDispatcherProvider,
            errorMapper = testErrorMapper,
            firebaseAvailabilityProvider = FakeFirebaseAvailabilityProvider(),
        )

        val result = repository.getRemoteConfig()

        assertTrue(result is Outcome.Success)
        assertEquals("2", (result as Outcome.Success).data.values["ad_cadence"])
    }

    @Test
    fun `network failure with empty cache returns error`() = runTest {
        val repository = RemoteConfigRepositoryImpl(
            mockBackendSource = MockBackendRemoteConfigSource(FakeRemoteConfigApi(error = IOException("no route to host"))),
            firebaseSource = FirebaseRemoteConfigSource(),
            dao = FakeRemoteConfigDao(),
            dispatchers = ImmediateDispatcherProvider,
            errorMapper = testErrorMapper,
            firebaseAvailabilityProvider = FakeFirebaseAvailabilityProvider(),
        )

        val result = repository.getRemoteConfig()

        assertTrue(result is Outcome.Error)
        assertTrue((result as Outcome.Error).error is AppError.Network)
    }
}