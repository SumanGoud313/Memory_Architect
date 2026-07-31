package com.suman.memoryarchitect.core.feedback.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real device vibration via [VibrationEffect] (minSdk 26, so no legacy no-amplitude fallback is
 * needed) - the Compose-side `LocalHapticFeedback` API used elsewhere in this app for plain taps
 * only exposes a handful of fixed system patterns, not the differentiated feel this system needs
 * per event (see [HapticPattern]). [intensityScale] folds in the Settings "reduce haptics"
 * preference (0.5 when on, 1.0 otherwise) - already computed by
 * [com.suman.memoryarchitect.core.feedback.FeedbackManager], which also skips calling this
 * entirely when haptics are off/muted, so this class never reads settings itself. Android's own
 * system-level haptics toggle (if the user disabled vibration device-wide) is honored for free -
 * the platform simply no-ops [Vibrator.vibrate] in that case, nothing to check here.
 *
 * [device.vibrate] is called with an explicit [VibrationAttributes] usage of
 * `USAGE_PHYSICAL_EMULATION` (API 33+; older versions fall back to the plain untagged call, before
 * this per-category system existed) rather than leaving the usage unspecified. Confirmed via a
 * real device (`dumpsys vibrator_manager`'s `VibrationIntensities`) that an untagged vibrate() call
 * resolves to the `TOUCH` category, which many users (and this device) turn `OFF` entirely as part
 * of disabling generic keyboard/UI touch-click buzz - silently zeroing out every haptic in this
 * app, not just gameplay ones, on any such device. `PHYSICAL_EMULATION` ("vibrations which
 * simulate physical hardware reactions") is the correct semantic category for object pickup/
 * rotate/place and every other pattern here anyway, and - critically - is a category most users
 * never think to separately disable, unlike generic touch feedback.
 *
 * Every pattern's peak amplitude is anchored to its [HapticPattern.strength] tier
 * ([TIER_AMPLITUDE]) - only the waveform *shape* (single pulse vs. a short multi-pulse flourish)
 * varies per pattern for character. This keeps "how strong does this feel" consistent and
 * intentional rather than each of the dozen patterns having been hand-tuned to a slightly
 * different, uncoordinated amplitude.
 */
@Singleton
class HapticsManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HapticsManager {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val vibrationAttributes: VibrationAttributes? by lazy {
        if (Build.VERSION.SDK_INT >= 33) {
            VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION).build()
        } else {
            null
        }
    }

    override fun play(pattern: HapticPattern, intensityScale: Float) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        val effect = effectFor(pattern, intensityScale.coerceIn(0.2f, 1f))
        val attributes = vibrationAttributes
        runCatching {
            // The `attributes != null` half is what Kotlin's null-safety actually needs to smart-cast
            // `attributes` to non-null below; the `SDK_INT >= 33` half is redundant with it at
            // runtime (attributes is only ever non-null on 33+ per the lazy property above) but is
            // what lint's own NewApi check needs to see, right at this call site, to verify the
            // `device.vibrate(effect, attributes)` overload (API 33+) is actually guarded - it
            // doesn't trace a null-check here back to the SDK_INT branch in a different property.
            if (Build.VERSION.SDK_INT >= 33 && attributes != null) {
                device.vibrate(effect, attributes)
            } else {
                device.vibrate(effect)
            }
        }
    }

    private fun effectFor(pattern: HapticPattern, scale: Float): VibrationEffect {
        val tierAmp = TIER_AMPLITUDE.getValue(pattern.strength)
        fun amp(fraction: Float = 1f): Int = (tierAmp * fraction * scale).toInt().coerceIn(1, 255)
        return when (pattern) {
            HapticPattern.TAP -> VibrationEffect.createOneShot(20, amp())
            HapticPattern.ROTATE -> VibrationEffect.createOneShot(12, amp())
            HapticPattern.PICKUP -> VibrationEffect.createOneShot(26, amp())
            HapticPattern.PLACE -> VibrationEffect.createOneShot(30, amp())
            HapticPattern.WARNING -> VibrationEffect.createOneShot(32, amp())
            HapticPattern.ERROR -> VibrationEffect.createWaveform(
                longArrayOf(0, 18, 32, 18), intArrayOf(0, amp(), 0, amp()), -1,
            )
            HapticPattern.COMBO -> VibrationEffect.createOneShot(16, amp())
            HapticPattern.SUCCESS -> VibrationEffect.createWaveform(
                longArrayOf(0, 24, 28, 38), intArrayOf(0, amp(), 0, amp(0.8f)), -1,
            )
            HapticPattern.ENCOURAGE -> VibrationEffect.createOneShot(65, amp())
            HapticPattern.REWARD -> VibrationEffect.createWaveform(
                longArrayOf(0, 18, 26, 20), intArrayOf(0, amp(), 0, amp(0.85f)), -1,
            )
            HapticPattern.UNLOCK -> VibrationEffect.createWaveform(
                longArrayOf(0, 36, 22, 55), intArrayOf(0, amp(0.9f), 0, amp()), -1,
            )
            HapticPattern.VICTORY -> VibrationEffect.createWaveform(
                longArrayOf(0, 30, 28, 30, 28, 60), intArrayOf(0, amp(0.7f), 0, amp(0.85f), 0, amp()), -1,
            )
        }
    }

    private companion object {
        val TIER_AMPLITUDE: Map<HapticStrength, Int> = mapOf(
            HapticStrength.TINY to 95,
            HapticStrength.LIGHT to 150,
            HapticStrength.MEDIUM to 195,
            HapticStrength.STRONG to 235,
        )
    }
}
