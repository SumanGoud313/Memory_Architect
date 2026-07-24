package com.suman.memoryarchitect.core.feedback

import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers as KotlinDispatchers

@Singleton
class AudioSettingsManagerImpl @Inject constructor(
    private val preferences: UserPreferencesDataStore,
) : AudioSettingsManager {

    // Owned for this singleton's whole process lifetime, same reasoning as GameAudioManagerImpl/
    // MusicManagerImpl's playback scopes - there is no natural shorter scope to tie this to since
    // every screen in the app needs live settings.
    private val scope = CoroutineScope(SupervisorJob() + KotlinDispatchers.Default)

    // kotlinx.coroutines.flow.combine only has typed overloads up to 5 flows - nesting a second
    // combine for the 6th (muteAllAudio) avoids the untyped Array<Any> vararg overload.
    override val settings: StateFlow<AudioSettings> = combine(
        preferences.masterVolume,
        preferences.musicVolume,
        preferences.effectsVolume,
        preferences.hapticsEnabled,
        preferences.reduceHaptics,
    ) { master, music, effects, hapticsEnabled, reduceHaptics ->
        AudioSettings(
            masterVolume = master,
            musicVolume = music,
            effectsVolume = effects,
            hapticsEnabled = hapticsEnabled,
            reduceHaptics = reduceHaptics,
        )
    }.combine(preferences.muteAllAudio) { partial, muteAll -> partial.copy(muteAll = muteAll) }
        .stateIn(scope, SharingStarted.Eagerly, AudioSettings())

    override suspend fun setMasterVolume(volume: Float) = preferences.setMasterVolume(volume)
    override suspend fun setMusicVolume(volume: Float) = preferences.setMusicVolume(volume)
    override suspend fun setEffectsVolume(volume: Float) = preferences.setEffectsVolume(volume)
    override suspend fun setHapticsEnabled(enabled: Boolean) = preferences.setHapticsEnabled(enabled)
    override suspend fun setReduceHaptics(enabled: Boolean) = preferences.setReduceHaptics(enabled)
    override suspend fun setMuteAll(enabled: Boolean) = preferences.setMuteAllAudio(enabled)
}
