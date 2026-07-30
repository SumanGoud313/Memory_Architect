package com.suman.memoryarchitect.core.feedback

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.cosmetics.EquippedCosmeticsStore
import com.suman.memoryarchitect.core.feedback.audio.GameAudioManager
import com.suman.memoryarchitect.core.feedback.audio.MusicManager
import com.suman.memoryarchitect.core.feedback.audio.MusicTrack
import com.suman.memoryarchitect.core.feedback.audio.SfxId
import com.suman.memoryarchitect.core.feedback.audio.TimerAudioManager
import com.suman.memoryarchitect.core.feedback.haptics.HapticPattern
import com.suman.memoryarchitect.core.feedback.haptics.HapticsManager
import com.suman.memoryarchitect.domain.model.SfxMaterialFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * See [FeedbackManager] for the contract. This is the only class in the app that reads
 * [AudioSettings] to decide whether/how loud a given event should be - every other manager
 * ([GameAudioManager]/[MusicManager]/[HapticsManager]) just plays exactly what it's told.
 */
@Singleton
class FeedbackManagerImpl @Inject constructor(
    private val audioManager: GameAudioManager,
    private val musicManager: MusicManager,
    private val hapticsManager: HapticsManager,
    private val timerAudioManager: TimerAudioManager,
    private val audioSettingsManager: AudioSettingsManager,
    private val equippedCosmeticsStore: EquippedCosmeticsStore,
    private val dispatchers: DispatcherProvider,
) : FeedbackManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    init {
        audioManager.preload()
        timerAudioManager.preload()
        // A Settings change (volume slider, mute toggle) must be heard immediately on whatever's
        // already playing, not wait for the next unrelated event (a timer tick, a screen change)
        // to happen to read `settings.value` and push it through - see FeedbackManager Step 11.
        audioSettingsManager.settings
            .onEach { musicManager.setVolume(it.effectiveMusicVolume) }
            .launchIn(scope)
    }

    // Timer-tick scheduling state - see onTimerTick's doc. previousRemainingMs also doubles as
    // the "new phase started" detector (remainingMs only ever counts down within one phase, so
    // an increase means Memorize/Reconstruct just (re)started).
    private var previousRemainingMs = Long.MAX_VALUE
    private var nextTickAtMs = NOT_ARMED
    private var expiryFired = false
    private var countdownOverlayActive = false

    // See setMusicResumeSuppressed's doc - default false, so every existing caller/screen is
    // completely unaffected; only Gameplay's Privacy Shield ever sets this.
    private var musicResumeSuppressed = false

    private val settings get() = audioSettingsManager.settings.value

    private fun sfx(id: SfxId, volumeScale: Float = 1f, family: SfxMaterialFamily? = null) {
        val effectsVolume = settings.effectiveEffectsVolume
        if (effectsVolume <= 0f) return
        audioManager.play(id, (effectsVolume * volumeScale).coerceIn(0f, 1f), family)
    }

    /** The premium `OBJECT_MATERIAL` cosmetic category has been removed from the shop entirely -
     * always null now, kept as a function (rather than deleted, with its 3 call sites reverted to
     * argument-less `sfx()` calls) purely to minimize the diff. */
    private fun currentObjectMaterialFamily(): SfxMaterialFamily? = null

    private fun haptic(pattern: HapticPattern) {
        val current = settings
        if (!current.effectiveHapticsEnabled) return
        hapticsManager.play(pattern, current.hapticIntensityScale)
    }

    // --- UI chrome -----------------------------------------------------------------------

    override fun onUiTap() {
        sfx(SfxId.UI_TAP, 0.6f)
        haptic(HapticPattern.TAP)
    }

    override fun onUiConfirm() {
        sfx(SfxId.UI_CONFIRM, 0.7f)
        haptic(HapticPattern.SUCCESS)
    }

    override fun onUiBack() {
        sfx(SfxId.UI_BACK, 0.5f)
        haptic(HapticPattern.TAP)
    }

    override fun onDialogOpen() {
        sfx(SfxId.DIALOG_OPEN, 0.5f)
        haptic(HapticPattern.TAP)
    }

    override fun onDialogClose() {
        sfx(SfxId.DIALOG_CLOSE, 0.4f)
    }

    override fun onScreenOpen(track: MusicTrack) {
        sfx(SfxId.SCREEN_OPEN, 0.35f)
        musicManager.setTrack(track, settings.effectiveMusicVolume)
    }

    override fun setMusicTrack(track: MusicTrack) {
        musicManager.setTrack(track, settings.effectiveMusicVolume)
    }

    override fun pauseMusic() {
        musicManager.pause()
        timerAudioManager.pause()
    }

    override fun resumeMusic() {
        if (musicResumeSuppressed) return
        musicManager.resume()
        timerAudioManager.resume()
    }

    override fun setMusicResumeSuppressed(suppressed: Boolean) {
        musicResumeSuppressed = suppressed
    }

    // --- Gameplay --------------------------------------------------------------------------

    override fun onObjectPickup() {
        sfx(SfxId.OBJECT_PICKUP, 0.6f, currentObjectMaterialFamily())
        haptic(HapticPattern.PICKUP)
    }

    override fun onObjectRotate() {
        sfx(SfxId.OBJECT_ROTATE, 0.4f, currentObjectMaterialFamily())
        haptic(HapticPattern.ROTATE)
    }

    override fun onObjectPlace() {
        sfx(SfxId.OBJECT_PLACE, 0.75f, currentObjectMaterialFamily())
        haptic(HapticPattern.PLACE)
    }

    override fun onComboStep(step: Int) {
        // Each successive step reads as "rising" via an increasing volume ramp across steps
        // (capped) - the COMBO_STEP asset itself is one fixed clip, not resynthesized per step.
        val boosted = (0.5f + step * 0.08f).coerceAtMost(1f)
        sfx(SfxId.COMBO_STEP, boosted)
        haptic(HapticPattern.COMBO)
    }

    /**
     * The countdown is deliberately audio-only - no haptic pulse of any kind is tied to the
     * timer, on purpose: it isn't in the meaningful-action set haptics are reserved for (button/
     * card taps, object pickup/drop, rewards, results), and a repeating vibration tied to a clock
     * reads as nagging rather than exciting. Urgency comes from two coordinated elements: (1) a
     * real countdown clock doesn't tick at a flat rate right up to zero - it ticks once a second
     * while there's plenty of time, then audibly speeds up as it runs out, so the tick interval
     * shrinks continuously from 1000ms (at 5s remaining) down to [MIN_TICK_INTERVAL_MS] (at 0s);
     * and (2), Reconstruct-only, the countdown bed itself fades in under the gameplay theme at
     * exactly [CALM_THRESHOLD_MS] remaining (see [MusicManager.startCountdownOverlay]) so the
     * whole mix leans in rather than a separate alarm cutting across it. Calm and silent above
     * [CALM_THRESHOLD_MS]. [nextTickAtMs] is the next remainingMs threshold a tick is due at, so
     * this is safe to call unconditionally from a 100ms polling loop.
     */
    override fun onTimerTick(remainingMs: Long, isReconstructPhase: Boolean) {
        updateTimerAudio(remainingMs, isReconstructPhase)

        if (remainingMs > previousRemainingMs) {
            // A new phase (Memorize, then later Reconstruct - including a Rewatch replay's own
            // Memorize) just started - re-arm from scratch and drop any leftover overlay from the
            // previous countdown rather than carrying it over.
            nextTickAtMs = NOT_ARMED
            expiryFired = false
            stopCountdownOverlayIfActive()
        }
        previousRemainingMs = remainingMs

        if (remainingMs <= 0) {
            if (!expiryFired) {
                expiryFired = true
                sfx(SfxId.TIMER_FINAL_WARNING, 0.8f)
                stopCountdownOverlayIfActive()
            }
            return
        }
        if (remainingMs > CALM_THRESHOLD_MS) return // calm phase - deliberately silent

        if (isReconstructPhase && !countdownOverlayActive) {
            countdownOverlayActive = true
            musicManager.startCountdownOverlay(
                duckedVolume = settings.effectiveMusicVolume * DUCKED_GAMEPLAY_RATIO,
                overlayVolume = settings.effectiveMusicVolume,
            )
        }

        if (nextTickAtMs == NOT_ARMED) nextTickAtMs = remainingMs
        if (remainingMs > nextTickAtMs) return

        val urgent = remainingMs <= URGENT_THRESHOLD_MS
        sfx(if (urgent) SfxId.TIMER_TICK_URGENT else SfxId.TIMER_TICK_SOFT, if (urgent) 0.55f else 0.35f)
        val interval = if (urgent) {
            val t = remainingMs.toDouble() / URGENT_THRESHOLD_MS // 1.0 at 5s remaining, ~0 near 0s
            (MIN_TICK_INTERVAL_MS + (1000L - MIN_TICK_INTERVAL_MS) * t).toLong()
        } else {
            1000L
        }
        nextTickAtMs = (remainingMs - interval).coerceAtLeast(0)
    }

    /** If a round ends by explicit submit rather than timeout, no further [onTimerTick] ever
     * fires to notice the overlay should stop - but that's harmless: [MusicManagerImpl.setTrack]
     * already tears down any active countdown overlay the instant the results screen sets
     * [MusicTrack.VICTORY]/[MusicTrack.FAILURE] moments later, so this flag simply goes stale
     * without ever needing an explicit close call from outside [onTimerTick]. */
    private fun stopCountdownOverlayIfActive() {
        if (!countdownOverlayActive) return
        countdownOverlayActive = false
        musicManager.stopCountdownOverlay(settings.effectiveMusicVolume)
    }

    /** The crossfade/stop half of the looping tick-tock bed ([TimerAudioManager]) - the starting
     * half is [onTimerPhaseStarted], an explicit call from [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]
     * rather than anything inferred here (see that method's doc for why inferring "new phase"
     * purely from this tick stream doesn't work). Muted outright (rather than started at volume 0)
     * when [AudioSettings.effectiveEffectsVolume] is zero, matching [sfx]'s existing "skip
     * entirely when muted" behavior instead of wastefully looping silence. */
    private fun updateTimerAudio(remainingMs: Long, isReconstructPhase: Boolean) {
        val effectsVolume = settings.effectiveEffectsVolume
        if (effectsVolume <= 0f) {
            timerAudioManager.stop()
            return
        }
        if (remainingMs <= 0L) {
            timerAudioManager.stop()
            return
        }
        val finalThresholdMs = if (isReconstructPhase) RECONSTRUCT_TIMER_AUDIO_FINAL_MS else MEMORIZE_TIMER_AUDIO_FINAL_MS
        if (remainingMs <= finalThresholdMs) {
            timerAudioManager.crossfadeToFinal(effectsVolume * TIMER_AUDIO_VOLUME_SCALE)
        }
    }

    override fun onTimerPhaseStarted() {
        val effectsVolume = settings.effectiveEffectsVolume
        if (effectsVolume <= 0f) return
        // Scaled down like every other timer-related cue already is (TIMER_TICK_SOFT/URGENT below
        // play at 0.35/0.55, never full volume) - a continuous loop sitting under a whole phase
        // needs to read as ambient/felt, not as loud as a one-shot accent.
        timerAudioManager.startNormal(effectsVolume * TIMER_AUDIO_VOLUME_SCALE)
    }

    override fun stopTimerAudio() {
        timerAudioManager.stop()
    }

    // --- Results ---------------------------------------------------------------------------

    override fun onResultsRevealed(mood: ResultMood, passed: Boolean) {
        when (mood) {
            ResultMood.VICTORY -> {
                sfx(SfxId.RESULT_VICTORY, 1f)
                haptic(HapticPattern.VICTORY)
            }
            ResultMood.GREAT -> {
                sfx(SfxId.RESULT_GREAT, 0.85f)
                haptic(HapticPattern.SUCCESS)
            }
            ResultMood.ENCOURAGE -> {
                sfx(SfxId.RESULT_ENCOURAGE, 0.6f)
                haptic(HapticPattern.ENCOURAGE)
            }
        }
        // The real pass/fail verdict picks the music, independent of the tier-based sfx/haptic
        // flavor above - see FeedbackManager.onResultsRevealed's doc.
        musicManager.setTrack(if (passed) MusicTrack.VICTORY else MusicTrack.FAILURE, settings.effectiveMusicVolume)
    }

    override fun onCoinsAwarded() {
        sfx(SfxId.COIN_AWARDED, 0.7f)
        haptic(HapticPattern.REWARD)
    }

    override fun onXpAwarded() {
        sfx(SfxId.XP_AWARDED, 0.6f)
    }

    override fun onStarAwarded() {
        sfx(SfxId.STAR_AWARDED, 0.75f)
        haptic(HapticPattern.REWARD)
    }

    override fun onAchievementUnlocked() {
        sfx(SfxId.ACHIEVEMENT_UNLOCKED, 0.85f)
        haptic(HapticPattern.UNLOCK)
    }

    override fun onLevelUnlocked() {
        sfx(SfxId.LEVEL_UNLOCKED, 0.75f)
        haptic(HapticPattern.UNLOCK)
    }

    override fun onDailyRewardClaimed() {
        sfx(SfxId.DAILY_REWARD_CLAIMED, 0.8f)
        haptic(HapticPattern.REWARD)
    }

    override fun onWeeklyRewardClaimed() {
        sfx(SfxId.WEEKLY_REWARD_CLAIMED, 0.9f)
        haptic(HapticPattern.UNLOCK)
    }

    override fun onLuckySpinStarted() {
        sfx(SfxId.LUCKY_SPIN_ROTATE, 0.75f)
        haptic(HapticPattern.TAP)
    }

    override fun onLuckySpinRevealed() {
        sfx(SfxId.LUCKY_SPIN_WIN, 0.9f)
        haptic(HapticPattern.VICTORY)
    }

    // --- Denials / warnings ------------------------------------------------------------------

    override fun onWarning() {
        sfx(SfxId.UI_BACK, 0.5f)
        haptic(HapticPattern.WARNING)
    }

    override fun onError() {
        haptic(HapticPattern.ERROR)
    }

    private companion object {
        const val CALM_THRESHOLD_MS = 10_000L
        const val URGENT_THRESHOLD_MS = 5_000L
        const val MIN_TICK_INTERVAL_MS = 150L
        const val NOT_ARMED = -1L
        // How far the gameplay bed ducks under the countdown overlay - audible enough that the
        // countdown clearly takes the lead, never so far it disappears ("blend naturally," not a
        // hard mute underneath it).
        const val DUCKED_GAMEPLAY_RATIO = 0.35f
        // Remaining-time thresholds for the looping tick-tock bed's normal->final crossfade -
        // different per phase since Memorize and Reconstruct durations are on very different
        // scales (a handful of seconds vs. up to a minute).
        const val MEMORIZE_TIMER_AUDIO_FINAL_MS = 4_000L
        const val RECONSTRUCT_TIMER_AUDIO_FINAL_MS = 8_000L
        // Reported as too loud at full effects volume - halved to sit under the mix like the
        // rest of the timer-cue family (see onTimerPhaseStarted's doc), tune further from here.
        const val TIMER_AUDIO_VOLUME_SCALE = 0.5f
    }
}
