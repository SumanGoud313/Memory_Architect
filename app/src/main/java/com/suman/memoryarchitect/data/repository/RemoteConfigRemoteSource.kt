package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.RemoteConfig

/**
 * The server-authoritative half of remote config - same role [ProgressionRemoteSource] plays for
 * xp/coins/streak, same two implementations picked the same way (see
 * [RemoteConfigRepositoryImpl.activeRemoteSource]).
 *
 * Unlike [ProgressionRemoteSource]/[MissionRemoteSource], neither implementation needs a signed-in
 * player identity - remote config is the same app-wide values for every install, never
 * per-player state, so there's no Firestore-per-uid document here at all.
 *
 * [MockBackendRemoteConfigSource] wraps the original dev-only mock backend's `/v1/config/remote`
 * endpoint, unchanged from before this abstraction existed. [FirebaseRemoteConfigSource] is the
 * first value in this app actually read from live Firebase Remote Config (see that class's doc for
 * why event windows specifically are what this pass wires up) rather than the mock backend or a
 * Firestore document.
 */
interface RemoteConfigRemoteSource {
    suspend fun getRemoteConfig(): RemoteConfig
}
