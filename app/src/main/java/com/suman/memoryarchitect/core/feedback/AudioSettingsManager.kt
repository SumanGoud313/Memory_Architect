package com.suman.memoryarchitect.core.feedback

import kotlinx.coroutines.flow.StateFlow

/** A snapshot of every player-controllable audio/haptics preference - see Settings' Sound &amp;
 * Haptics section. [masterVolume] scales both [musicVolume] and [effectsVolume] multiplicatively
 * (e.g. master 0.5 + effects 1.0 plays effects at half volume); [muteAll] and [hapticsEnabled]/
 * [reduceHaptics] are independent switches layered on top by [FeedbackManager], not baked into
 * these numbers, so toggling mute never loses the volume levels underneath it. */
data class AudioSettings(
    val masterVolume: Float = 1f,
    // Lowered from 0.7 -> 0.45 -> 0.3 - music is meant to sit quietly under the game by default,
    // never compete with it or announce itself. The player can always raise it in Settings (the
    // Music slider under Sound & Haptics); this only changes what a first-time install starts at.
    val musicVolume: Float = 0.3f,
    val effectsVolume: Float = 1f,
    val hapticsEnabled: Boolean = true,
    val reduceHaptics: Boolean = false,
    val muteAll: Boolean = false,
) {
    val effectiveMusicVolume: Float get() = if (muteAll) 0f else masterVolume * musicVolume
    val effectiveEffectsVolume: Float get() = if (muteAll) 0f else masterVolume * effectsVolume
    val effectiveHapticsEnabled: Boolean get() = hapticsEnabled && !muteAll
    val hapticIntensityScale: Float get() = if (reduceHaptics) 0.5f else 1f
}

/**
 * Single source of truth for every audio/haptics preference, read reactively by
 * [FeedbackManager] so a Settings change takes effect on the very next event with no restart or
 * re-fetch needed anywhere. Backed by [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore],
 * same "device-local, non-authoritative" tier as the existing theme/haptics prefs it lives
 * alongside - never gameplay or progression state.
 */
interface AudioSettingsManager {
    val settings: StateFlow<AudioSettings>

    suspend fun setMasterVolume(volume: Float)
    suspend fun setMusicVolume(volume: Float)
    suspend fun setEffectsVolume(volume: Float)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setReduceHaptics(enabled: Boolean)
    suspend fun setMuteAll(enabled: Boolean)
}
