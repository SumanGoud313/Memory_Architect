package com.suman.memoryarchitect.core.feedback.audio

import android.net.Uri

/**
 * The single place that knows how audio assets are laid out and resolves them - every other
 * audio class ([GameAudioManagerImpl], [MusicManagerImpl]) asks this "does this file exist" /
 * "give me a URI for it" rather than touching `AssetManager` directly. All paths are relative to
 * `assets/audio/` (e.g. `"music/home.mp3"`, `"sfx/object_pickup.mp3"`) - see
 * `app/src/main/assets/audio/README.md` for the full manifest of every file this app looks for
 * and exactly what should be recorded/composed for it.
 *
 * [assetExists] is the load-bearing method: until real audio files are dropped into
 * `assets/audio/`, every lookup returns false and the calling manager silently no-ops (no crash,
 * no placeholder beep, no fallback tone) - this app ships genuinely silent rather than faking
 * music with a generated substitute.
 */
interface AudioAssetManager {
    fun assetExists(relativePath: String): Boolean
    fun assetUri(relativePath: String): Uri
}
