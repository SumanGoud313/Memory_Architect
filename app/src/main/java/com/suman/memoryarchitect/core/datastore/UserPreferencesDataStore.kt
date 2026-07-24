package com.suman.memoryarchitect.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suman.memoryarchitect.domain.model.AvatarCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * Device-local, non-authoritative settings only (theme, sound, last-selected mode). Never
 * used as a source of truth for progression or gameplay state — that lives server-side.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REDUCE_HAPTICS = booleanPreferencesKey("reduce_haptics")
        val MUTE_ALL_AUDIO = booleanPreferencesKey("mute_all_audio")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val EFFECTS_VOLUME = floatPreferencesKey("effects_volume")
        val SESSION_COUNT = intPreferencesKey("analytics_session_count")
        val LIFETIME_PLAY_TIME_MS = longPreferencesKey("analytics_lifetime_play_time_ms")
        val MODE_COUNT_PREFIX = "analytics_mode_count_"
        val AVATAR_ID = stringPreferencesKey("avatar_id")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val COUNTRY = stringPreferencesKey("country")
        val HAS_REMOVED_ADS = booleanPreferencesKey("has_removed_ads")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.HAPTICS_ENABLED] ?: true }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAPTICS_ENABLED] = enabled }
    }

    /** Halves haptic amplitude everywhere rather than turning it off entirely - see
     * [com.suman.memoryarchitect.core.feedback.FeedbackManager]. */
    val reduceHaptics: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.REDUCE_HAPTICS] ?: false }

    suspend fun setReduceHaptics(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.REDUCE_HAPTICS] = enabled }
    }

    /** Silences both audio and haptics in one switch without changing the individual volume/
     * haptics-enabled preferences underneath - flipping it back off restores whatever those were. */
    val muteAllAudio: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.MUTE_ALL_AUDIO] ?: false }

    suspend fun setMuteAllAudio(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.MUTE_ALL_AUDIO] = enabled }
    }

    val masterVolume: Flow<Float> = context.dataStore.data.map { prefs -> prefs[Keys.MASTER_VOLUME] ?: 1f }

    suspend fun setMasterVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.MASTER_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    val musicVolume: Flow<Float> = context.dataStore.data.map { prefs -> prefs[Keys.MUSIC_VOLUME] ?: 0.7f }

    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    val effectsVolume: Flow<Float> = context.dataStore.data.map { prefs -> prefs[Keys.EFFECTS_VOLUME] ?: 1f }

    suspend fun setEffectsVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.EFFECTS_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    /** Which of [com.suman.memoryarchitect.domain.model.AvatarCatalog.options] the player picked -
     * denormalized onto `players/{uid}` (and every periodic leaderboard entry) by
     * [com.suman.memoryarchitect.data.repository.LeaderboardRepositoryImpl] at submission time.
     * Device-local, non-authoritative - the Firestore copy is what other players actually see. */
    val avatarId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.AVATAR_ID]?.takeIf { AvatarCatalog.isValidId(it) } ?: AvatarCatalog.DEFAULT_AVATAR_ID
    }

    suspend fun setAvatarId(avatarId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AVATAR_ID] = avatarId
            prefs.remove(Keys.AVATAR_URL) // picking a curated avatar clears any custom upload
        }
    }

    /** A custom-uploaded avatar's Storage download URL (see `AvatarUploadRepository`) - takes
     * priority over [avatarId] wherever both are present. `null` when the player has never
     * uploaded one or has since reverted to a curated avatar. */
    val avatarUrl: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.AVATAR_URL] }

    suspend fun setAvatarUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[Keys.AVATAR_URL] = url }
    }

    /** Optional, self-reported ISO 3166-1 alpha-2 country code - `null` until the player
     * deliberately sets one in Settings. Never inferred from IP/GPS. */
    val country: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.COUNTRY] }

    suspend fun setCountry(isoCountryCode: String?) {
        context.dataStore.edit { prefs ->
            if (isoCountryCode == null) prefs.remove(Keys.COUNTRY) else prefs[Keys.COUNTRY] = isoCountryCode
        }
    }

    /** A fast local cache of the lifetime "Remove Ads" entitlement, not the source of truth -
     * see [com.suman.memoryarchitect.core.billing.BillingManager], which re-verifies against
     * Google Play itself on every app start and only ever writes here to keep this cache in sync
     * for the next cold start's instant initial read (e.g. so the Remove Ads screen doesn't flash
     * "Buy Now" for the split second before the real Play Billing query resolves). Never read by
     * anything as the actual gate for entitlement - that's always [BillingManager]'s live state. */
    val hasRemovedAds: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.HAS_REMOVED_ADS] ?: false }

    suspend fun setHasRemovedAds(purchased: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HAS_REMOVED_ADS] = purchased }
    }

    // Everything below started out existing purely to back Firebase user properties
    // (lifetime_play_time, total_sessions, preferred_game_mode) - device-local counters. The two
    // read-facing Flows immediately below are also now the Statistics Dashboard's source for
    // "Total Play Time"/"Total Sessions" (see StatisticsViewModel) - still never gameplay/
    // progression truth, just now genuinely displayed rather than analytics-only.

    val sessionCount: Flow<Int> = context.dataStore.data.map { prefs -> prefs[Keys.SESSION_COUNT] ?: 0 }

    val lifetimePlayTimeMs: Flow<Long> = context.dataStore.data.map { prefs -> prefs[Keys.LIFETIME_PLAY_TIME_MS] ?: 0L }

    suspend fun incrementSessionCount(): Int {
        var updated = 0
        context.dataStore.edit { prefs ->
            updated = (prefs[Keys.SESSION_COUNT] ?: 0) + 1
            prefs[Keys.SESSION_COUNT] = updated
        }
        return updated
    }

    suspend fun addPlayTime(durationMs: Long): Long {
        var updated = 0L
        context.dataStore.edit { prefs ->
            updated = (prefs[Keys.LIFETIME_PLAY_TIME_MS] ?: 0L) + durationMs
            prefs[Keys.LIFETIME_PLAY_TIME_MS] = updated
        }
        return updated
    }

    /** Bumps this mode's pick count and returns whichever mode now has the highest count overall
     * ("preferred" = most-picked, not most-recent). */
    suspend fun recordModeSelectedAndGetPreferred(modeName: String): String {
        val key = intPreferencesKey(Keys.MODE_COUNT_PREFIX + modeName)
        context.dataStore.edit { prefs -> prefs[key] = (prefs[key] ?: 0) + 1 }
        val prefs = context.dataStore.data.first()
        return MODE_NAMES.maxBy { prefs[intPreferencesKey(Keys.MODE_COUNT_PREFIX + it)] ?: 0 }
    }

    private companion object {
        val MODE_NAMES = listOf("CLASSIC", "DAILY_CHALLENGE", "WEEKLY_CHALLENGE", "PRACTICE")
    }
}