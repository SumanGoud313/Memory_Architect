package com.suman.memoryarchitect.core.feedback.audio

/**
 * Plays short, one-shot sound effects loaded from real asset files (see [SfxId.assetPath],
 * [AudioAssetManager]) via [android.media.SoundPool] - the only thing in this app allowed to
 * touch it. See [com.suman.memoryarchitect.core.feedback.FeedbackManager], the facade every
 * ViewModel/Composable actually calls. [volume] is a pre-computed 0..1 linear gain (already
 * folded with master/effects volume and mute state by the caller) so this class never needs to
 * know about user settings.
 */
interface GameAudioManager {
    /** Loads every [SfxId] whose asset file currently exists, up front on a background
     * dispatcher, so the first real playback of any sound never pays a load-latency hit
     * mid-gameplay. Anything without a file yet is silently skipped. */
    fun preload()

    fun play(sfx: SfxId, volume: Float = 1f)
}
