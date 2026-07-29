package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.domain.model.RemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real counterpart to [MockBackendRemoteConfigSource] - see [RemoteConfigRemoteSource]'s doc.
 * The first value this app actually reads from live Firebase Remote Config, rather than the mock
 * backend: [com.suman.memoryarchitect.domain.progression.LiveEventCatalog]'s three `event_*` keys.
 * Every other key this app defines (`ad_cadence`, `difficulty_curve_base`) stays on the mock
 * backend for now - see the plan doc's "Recommended sequencing" for why event windows specifically
 * are the smallest, lowest-risk piece to prove the swap works before migrating anything else: they
 * change often (a handful of times a year) and benefit most from not needing a release to update,
 * while the other keys are barely consumed yet and in no hurry to move.
 *
 * [DEFAULTS] must be set *before* the very first [getRemoteConfig] call - Firebase Remote Config
 * only ever falls back to these when a value hasn't been fetched yet at all (never overwritten by
 * a real fetch, even an empty one), so a cold install with no connectivity still resolves a sane,
 * inert (no event active) state instead of throwing or reading empty strings.
 */
@Singleton
class FirebaseRemoteConfigSource @Inject constructor() : RemoteConfigRemoteSource {

    private val remoteConfig by lazy {
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(
                remoteConfigSettings {
                    // The default 12-hour throttle would make every debug-build change to the
                    // console invisible for half a day - fine in release, useless while iterating.
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0L else 3600L
                },
            )
            setDefaultsAsync(DEFAULTS)
        }
    }

    override suspend fun getRemoteConfig(): RemoteConfig {
        remoteConfig.fetchAndActivate().await()
        val values = remoteConfig.all.mapValues { (_, value) -> value.asString() }
        return RemoteConfig(values, System.currentTimeMillis())
    }

    private companion object {
        val DEFAULTS: Map<String, Any> = mapOf(
            "ad_cadence" to "3",
            "difficulty_curve_base" to "5.0",
            // Empty id / zero window = "no event active" - see LiveEventCatalog.activeEvent's doc.
            "event_active_id" to "",
            "event_start_epoch" to "0",
            "event_end_epoch" to "0",
        )
    }
}
