package com.suman.memoryarchitect.core.feedback.haptics

/** How strong a [HapticPattern] reads to the player - see [HapticsManagerImpl] for the exact
 * amplitude/duration band each tier maps to. [STRONG] is reserved for genuinely major moments
 * (only [HapticPattern.VICTORY] uses it) so it never gets diluted into feeling routine. */
enum class HapticStrength { TINY, LIGHT, MEDIUM, STRONG }

/** Every distinct feel this app ever produces on the device's vibrator - deliberately closed
 * (not a raw duration/amplitude parameter) so every call site expresses *intent*, and the actual
 * waveform tuning lives in exactly one place ([HapticsManagerImpl]). Reserved only for the
 * meaningful-action set this app actually uses: button/card taps, object pickup/drop, achievement/
 * reward/unlock, and level results. Deliberately absent: anything tied to the timer, ambient
 * sound, or music - see [com.suman.memoryarchitect.core.feedback.FeedbackManagerImpl.onTimerTick],
 * which is audio-only by design. */
enum class HapticPattern(val strength: HapticStrength) {
    /** Generic button/card tap - the lightest, most frequent pattern in the app. */
    TAP(HapticStrength.LIGHT),

    /** Picking an object up off the tray or off a slot. */
    PICKUP(HapticStrength.MEDIUM),

    /** Rotating a placed object one step - the smallest, most frequent gameplay gesture. */
    ROTATE(HapticStrength.TINY),

    /** Dropping an object onto a slot - deliberately neutral (never reveals correctness). */
    PLACE(HapticStrength.MEDIUM),

    /** A positive confirmation short of full victory - reward claimed, achievement row tapped. */
    SUCCESS(HapticStrength.MEDIUM),

    /** Something needs attention but nothing failed - low-hint warning, denial (e.g. "out of hints"). */
    WARNING(HapticStrength.LIGHT),

    /** A gentle negative - never used for gameplay failure (see [ENCOURAGE]), only for rejected
     * actions (e.g. an invalid drop). */
    ERROR(HapticStrength.LIGHT),

    /** One step of a rising combo reveal on the results screen. */
    COMBO(HapticStrength.LIGHT),

    /** Best-tier results reveal (Perfect/Memory Master) - the only [HapticStrength.STRONG]
     * pattern in the app, reserved for this one genuinely major celebration. */
    VICTORY(HapticStrength.STRONG),

    /** A soft, motivating pulse for a middling/low result - explicitly NOT punishing, matching
     * this app's "never shows red/punishing styling" results design. */
    ENCOURAGE(HapticStrength.MEDIUM),

    /** Coins/XP/star count-up ticks landing. */
    REWARD(HapticStrength.MEDIUM),

    /** Level or achievement unlock. */
    UNLOCK(HapticStrength.MEDIUM),
}

/** The only thing in this app allowed to touch [android.os.Vibrator]/[android.os.VibratorManager]
 * - see [com.suman.memoryarchitect.core.feedback.FeedbackManager], the facade every ViewModel and
 * Composable actually calls instead of this directly. */
interface HapticsManager {
    /** [intensityScale] (0..1) folds in the Settings "reduce haptics" preference - already
     * computed by [com.suman.memoryarchitect.core.feedback.FeedbackManager] before calling this. */
    fun play(pattern: HapticPattern, intensityScale: Float = 1f)
}
