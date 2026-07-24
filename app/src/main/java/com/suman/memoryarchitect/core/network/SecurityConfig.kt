package com.suman.memoryarchitect.core.network

/**
 * Production TLS certificate pins. Empty until real production certificate hashes are
 * captured from the live backend — [NetworkModule] only applies pinning when this list is
 * non-empty, so local/dev builds against a placeholder host are unaffected. Populate with
 * real "sha256/..." pins (leaf + backup) before the first public release.
 */
object SecurityConfig {

    data class Pin(val hostnamePattern: String, val pin: String)

    val certificatePins: List<Pin> = emptyList()
}