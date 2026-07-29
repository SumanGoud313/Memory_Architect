package com.suman.memoryarchitect.core.feedback.audio

import com.suman.memoryarchitect.domain.model.SfxMaterialFamily

/** Every discrete sound effect this app can play - see [com.suman.memoryarchitect.core.feedback.FeedbackManager]
 * for which gameplay/UI event maps to which of these. Each one is a real asset file loaded by
 * [GameAudioManagerImpl] via [AudioAssetManager] - nothing here is generated in code. */
enum class SfxId {
    UI_TAP,
    UI_CONFIRM,
    UI_BACK,
    DIALOG_OPEN,
    DIALOG_CLOSE,
    SCREEN_OPEN,

    OBJECT_PICKUP,
    OBJECT_ROTATE,
    OBJECT_PLACE,

    COMBO_STEP,

    TIMER_TICK_SOFT,
    TIMER_TICK_URGENT,
    TIMER_FINAL_WARNING,

    RESULT_VICTORY,
    RESULT_GREAT,
    RESULT_ENCOURAGE,

    COIN_AWARDED,
    XP_AWARDED,
    STAR_AWARDED,
    ACHIEVEMENT_UNLOCKED,
    LEVEL_UNLOCKED,
    DAILY_REWARD_CLAIMED,
    WEEKLY_REWARD_CLAIMED,

    LUCKY_SPIN_ROTATE,
    LUCKY_SPIN_WIN,
}

/** Where [GameAudioManagerImpl] looks for this sound's asset file, relative to
 * `assets/audio/` - see `app/src/main/assets/audio/README.md` for the full manifest of exactly
 * which files are expected. Organized by category folder (ui/, sfx/, timer/, victory/, failure/)
 * per the asset layout brief. */
fun SfxId.assetPath(): String = when (this) {
    SfxId.UI_TAP -> "ui/tap.mp3"
    SfxId.UI_CONFIRM -> "ui/confirm.mp3"
    SfxId.UI_BACK -> "ui/back.mp3"
    SfxId.DIALOG_OPEN -> "ui/dialog_open.mp3"
    SfxId.DIALOG_CLOSE -> "ui/dialog_close.mp3"
    SfxId.SCREEN_OPEN -> "ui/screen_open.mp3"

    SfxId.OBJECT_PICKUP -> "sfx/object_pickup.mp3"
    SfxId.OBJECT_ROTATE -> "sfx/object_rotate.mp3"
    SfxId.OBJECT_PLACE -> "sfx/object_place.mp3"
    SfxId.COMBO_STEP -> "sfx/combo_step.mp3"
    SfxId.COIN_AWARDED -> "sfx/coin_awarded.mp3"
    SfxId.XP_AWARDED -> "sfx/xp_awarded.mp3"
    SfxId.STAR_AWARDED -> "sfx/star_awarded.mp3"
    SfxId.ACHIEVEMENT_UNLOCKED -> "sfx/achievement_unlocked.mp3"
    SfxId.LEVEL_UNLOCKED -> "sfx/level_unlocked.mp3"
    SfxId.DAILY_REWARD_CLAIMED -> "sfx/daily_reward_claimed.mp3"
    SfxId.WEEKLY_REWARD_CLAIMED -> "sfx/weekly_reward_claimed.mp3"
    SfxId.LUCKY_SPIN_ROTATE -> "sfx/lucky_spin_rotate.wav"

    SfxId.TIMER_TICK_SOFT -> "timer/tick_soft.mp3"
    SfxId.TIMER_TICK_URGENT -> "timer/tick_urgent.mp3"
    SfxId.TIMER_FINAL_WARNING -> "timer/final_warning.mp3"

    SfxId.RESULT_VICTORY -> "victory/victory_sting.mp3"
    SfxId.RESULT_GREAT -> "victory/great_sting.mp3"
    SfxId.RESULT_ENCOURAGE -> "failure/encourage_sting.mp3"
    SfxId.LUCKY_SPIN_WIN -> "victory/lucky_spin_win.wav"
}

/** Every candidate asset file [GameAudioManagerImpl] may pick between for one played instance of
 * this sound, in no particular order - [assetPath] stays the single source of truth for ids with
 * only one recording. The 3 gameplay-object sounds get real variety (see `assets/audio/sfx/README.md`)
 * so pickup/rotate/place never sounds like the exact same clip on every repetition; every other id
 * falls back to its one [assetPath], so this is purely additive and changes nothing for them.
 *
 * [family] additionally selects a premium `OBJECT_MATERIAL`'s themed variant set (e.g.
 * `object_pickup_metallic_1.wav`) for the 3 gameplay-object sounds - `null` (nothing equipped, or
 * any other [SfxId]) reproduces the exact untheme'd paths above. [GameAudioManagerImpl] falls back
 * to the untheme'd set on its own if a themed file doesn't exist yet, so passing a non-null
 * [family] here is never a silence risk. */
fun SfxId.variantAssetPaths(family: SfxMaterialFamily? = null): List<String> {
    val suffix = family?.name?.lowercase()?.let { "${it}_" } ?: ""
    // .wav, not .mp3, for these three specifically - SoundPool decodes either fine, and .wav
    // avoids MP3 encode/decode overhead entirely for clips this short (well under 1 second).
    return when (this) {
        SfxId.OBJECT_PICKUP -> listOf("sfx/object_pickup_${suffix}1.wav", "sfx/object_pickup_${suffix}2.wav", "sfx/object_pickup_${suffix}3.wav")
        SfxId.OBJECT_ROTATE -> listOf("sfx/object_rotate_${suffix}1.wav", "sfx/object_rotate_${suffix}2.wav", "sfx/object_rotate_${suffix}3.wav")
        SfxId.OBJECT_PLACE -> listOf("sfx/object_place_${suffix}1.wav", "sfx/object_place_${suffix}2.wav", "sfx/object_place_${suffix}3.wav")
        else -> listOf(assetPath())
    }
}
