package com.suman.memoryarchitect.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suman.memoryarchitect.core.ads.InterstitialPacingPreferences
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
 *
 * Implements [InterstitialPacingPreferences] purely so [com.suman.memoryarchitect.core.ads.InterstitialPacingGate]
 * can depend on that small interface instead of this whole class - see that interface's own doc.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InterstitialPacingPreferences {

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
        val OWNED_PRODUCT_IDS = stringSetPreferencesKey("owned_product_ids")
        val STREAK_REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
        val DAILY_CHALLENGE_REMINDER_ENABLED = booleanPreferencesKey("daily_challenge_reminder_enabled")
        val LAST_INTERSTITIAL_SHOWN_AT_EPOCH_MS = longPreferencesKey("last_interstitial_shown_at_epoch_ms")
        val LAST_REWARDED_AD_SHOWN_AT_EPOCH_MS = longPreferencesKey("last_rewarded_ad_shown_at_epoch_ms")
        val GAMES_PLAYED_AT_LAST_INTERSTITIAL = intPreferencesKey("games_played_at_last_interstitial")
        val INTERSTITIALS_SHOWN_TODAY = intPreferencesKey("interstitials_shown_today")
        val INTERSTITIALS_SHOWN_TODAY_EPOCH_DAY = longPreferencesKey("interstitials_shown_today_epoch_day")
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

    /** A fast local cache of every non-Remove-Ads product this account owns (Premium Collections
     * today) - same "not the source of truth" reasoning as [hasRemovedAds] immediately above,
     * reconciled against Google Play on every [com.suman.memoryarchitect.core.billing.BillingManager.startConnection]/
     * `restorePurchases` call, never trusted alone. */
    val ownedProductIds: Flow<Set<String>> = context.dataStore.data.map { prefs -> prefs[Keys.OWNED_PRODUCT_IDS] ?: emptySet() }

    suspend fun setOwnedProductIds(productIds: Set<String>) {
        context.dataStore.edit { prefs -> prefs[Keys.OWNED_PRODUCT_IDS] = productIds }
    }

    /** Per-category opt-out for [com.suman.memoryarchitect.core.notifications.DailyReminderWorker] -
     * on by default (matches the system permission prompt's own "you'll get useful reminders"
     * framing), independent of whether the OS notification permission is actually granted. */
    val streakReminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.STREAK_REMINDER_ENABLED] ?: true }

    suspend fun setStreakReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.STREAK_REMINDER_ENABLED] = enabled }
    }

    val dailyChallengeReminderEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.DAILY_CHALLENGE_REMINDER_ENABLED] ?: true }

    suspend fun setDailyChallengeReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DAILY_CHALLENGE_REMINDER_ENABLED] = enabled }
    }

    // Everything below started out existing purely to back Firebase user properties
    // (lifetime_play_time, total_sessions, preferred_game_mode) - device-local counters. The two
    // read-facing Flows immediately below are also now the Statistics Dashboard's source for
    // "Total Play Time"/"Total Sessions" (see StatisticsViewModel) - still never gameplay/
    // progression truth, just now genuinely displayed rather than analytics-only.

    override val sessionCount: Flow<Int> = context.dataStore.data.map { prefs -> prefs[Keys.SESSION_COUNT] ?: 0 }

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

    /** Cooldown timestamps for [com.suman.memoryarchitect.core.ads.InterstitialPacingGate] - `null`
     * until the first interstitial/rewarded ad this install has ever shown. Persisted (not just
     * in-memory) so a cooldown genuinely survives a process restart, unlike the gate's own
     * session-scoped counters (interstitials-shown-this-session, level-completions-since-last),
     * which are correctly *not* persisted here - see that class's own doc. */
    override val lastInterstitialShownAtEpochMs: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.LAST_INTERSTITIAL_SHOWN_AT_EPOCH_MS] }

    override suspend fun setLastInterstitialShownAtEpochMs(epochMs: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_INTERSTITIAL_SHOWN_AT_EPOCH_MS] = epochMs }
    }

    override val lastRewardedAdShownAtEpochMs: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.LAST_REWARDED_AD_SHOWN_AT_EPOCH_MS] }

    override suspend fun setLastRewardedAdShownAtEpochMs(epochMs: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_REWARDED_AD_SHOWN_AT_EPOCH_MS] = epochMs }
    }

    /** [com.suman.memoryarchitect.domain.model.PlayerStatistics.gamesPlayed]'s value the moment the
     * most recent interstitial was shown - [com.suman.memoryarchitect.core.ads.InterstitialPacingGate]
     * compares this against the *current* `gamesPlayed` to implement "N levels completed since the
     * last interstitial" without needing a live in-memory counter wired through every scored-round
     * call site. `0` until this install's first interstitial ever shows. */
    override val gamesPlayedAtLastInterstitial: Flow<Int> = context.dataStore.data.map { prefs -> prefs[Keys.GAMES_PLAYED_AT_LAST_INTERSTITIAL] ?: 0 }

    override suspend fun setGamesPlayedAtLastInterstitial(gamesPlayed: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.GAMES_PLAYED_AT_LAST_INTERSTITIAL] = gamesPlayed }
    }

    override val interstitialsShownToday: Flow<Int> = context.dataStore.data.map { prefs -> prefs[Keys.INTERSTITIALS_SHOWN_TODAY] ?: 0 }

    override val interstitialsShownTodayEpochDay: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.INTERSTITIALS_SHOWN_TODAY_EPOCH_DAY] }

    override suspend fun setInterstitialsShownToday(count: Int, epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INTERSTITIALS_SHOWN_TODAY] = count
            prefs[Keys.INTERSTITIALS_SHOWN_TODAY_EPOCH_DAY] = epochDay
        }
    }

    /** Bumps this mode's pick count and returns whichever mode now has the highest count overall
     * ("preferred" = most-picked, not most-recent). */
    suspend fun recordModeSelectedAndGetPreferred(modeName: String): String {
        val key = intPreferencesKey(Keys.MODE_COUNT_PREFIX + modeName)
        context.dataStore.edit { prefs -> prefs[key] = (prefs[key] ?: 0) + 1 }
        val prefs = context.dataStore.data.first()
        return MODE_NAMES.maxBy { prefs[intPreferencesKey(Keys.MODE_COUNT_PREFIX + it)] ?: 0 }
    }

    /** Wipes every preference in this store back to its default - used only by account deletion,
     * so a deleted account leaves no residual PII (avatarUrl, country) or stale entitlement flags
     * (hasRemovedAds, ownedProductIds) behind for whatever plays next on this device. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val MODE_NAMES = listOf("CLASSIC", "DAILY_CHALLENGE", "WEEKLY_CHALLENGE", "PRACTICE")
    }
}