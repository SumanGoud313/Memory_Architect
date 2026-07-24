package com.suman.memoryarchitect.core.analytics

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Firebase Remote Config, wired and ready but **not currently wired into
 * [com.suman.memoryarchitect.domain.repository.RemoteConfigRepository]** - this app's live
 * remote-config values (ad cadence, difficulty base, etc.) come from the dev mock backend today
 * (`RemoteConfigRepositoryImpl`, `mock-backend/index.js`), which is real, working infrastructure
 * this class deliberately doesn't replace. This exists per the "prepare architecture even if not
 * immediately used" brief, for a future production release: swapping
 * `RemoteConfigRepositoryImpl`'s network call for [fetchAndActivate] plus [getString] would be a
 * self-contained change to that one class, nothing else in the app needs to know.
 *
 * No-ops (returns empty/defaults) when [FirebaseAvailability.isConfigured] is false, same as
 * every other Firebase-backed class in this package.
 */
@Singleton
class FirebaseRemoteConfigSource @Inject constructor() {
    private val remoteConfig by lazy { Firebase.remoteConfig }

    suspend fun fetchAndActivate(): Boolean {
        if (!FirebaseAvailability.isConfigured) return false
        return try {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = MIN_FETCH_INTERVAL_SECONDS },
            ).await()
            remoteConfig.fetchAndActivate().await()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch remote config", t)
            false
        }
    }

    fun getString(key: String): String? {
        if (!FirebaseAvailability.isConfigured) return null
        return try {
            remoteConfig.getString(key).ifEmpty { null }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read remote config key $key", t)
            null
        }
    }

    private companion object {
        const val TAG = "FirebaseRemoteConfig"
        const val MIN_FETCH_INTERVAL_SECONDS = 3600L
    }
}
