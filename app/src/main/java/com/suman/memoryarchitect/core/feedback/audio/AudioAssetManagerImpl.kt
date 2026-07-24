package com.suman.memoryarchitect.core.feedback.audio

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioAssetManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AudioAssetManager {

    // AssetManager.open() is a real filesystem/APK-zip lookup - worth caching since every SFX
    // play() and every music track change calls this, often for the same handful of paths.
    private val existenceCache = ConcurrentHashMap<String, Boolean>()

    override fun assetExists(relativePath: String): Boolean = existenceCache.getOrPut(relativePath) {
        runCatching { context.assets.open(fullPath(relativePath)).use { } }.isSuccess
    }

    override fun assetUri(relativePath: String): Uri = Uri.parse("asset:///${fullPath(relativePath)}")

    private fun fullPath(relativePath: String) = "audio/$relativePath"
}
