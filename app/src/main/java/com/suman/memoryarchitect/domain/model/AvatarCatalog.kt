package com.suman.memoryarchitect.domain.model

/** The curated, built-in avatar set - no Firebase Storage, no upload, no moderation risk. Every
 * [AvatarOption.id] is a stable key stored as `avatarId` on `players/{uid}`; unknown/missing ids
 * (an older client, a not-yet-synced value) fall back to [DEFAULT_AVATAR_ID]. A player may instead
 * set a custom `avatarUrl` (Storage-backed upload - see `ui/screens/settings/AvatarPicker.kt`),
 * which takes priority over `avatarId` wherever both are present. */
object AvatarCatalog {
    const val DEFAULT_AVATAR_ID = "architect_gold"

    val options: List<AvatarOption> = listOf(
        AvatarOption("architect_gold", "🏛️"),
        AvatarOption("compass", "🧭"),
        AvatarOption("hourglass", "⏳"),
        AvatarOption("key", "🗝️"),
        AvatarOption("lantern", "🏮"),
        AvatarOption("crystal", "💎"),
        AvatarOption("owl", "🦉"),
        AvatarOption("fox", "🦊"),
        AvatarOption("dragon", "🐉"),
        AvatarOption("phoenix", "🔥"),
        AvatarOption("star", "⭐"),
        AvatarOption("moon", "🌙"),
        AvatarOption("comet", "☄️"),
        AvatarOption("shield", "🛡️"),
    )

    fun glyphFor(avatarId: String): String = options.firstOrNull { it.id == avatarId }?.glyph
        ?: options.first { it.id == DEFAULT_AVATAR_ID }.glyph

    fun isValidId(avatarId: String): Boolean = options.any { it.id == avatarId }
}

/** [glyph] is a single emoji rendered inside a themed circular backing wherever an avatar shows -
 * no image assets, no Storage read, works identically offline. */
data class AvatarOption(val id: String, val glyph: String)
