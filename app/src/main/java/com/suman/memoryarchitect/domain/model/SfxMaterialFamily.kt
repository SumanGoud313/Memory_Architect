package com.suman.memoryarchitect.domain.model

/** Which pickup/rotate/place sound family plays during gameplay when a premium
 * [com.suman.memoryarchitect.ui.theme.ObjectMaterialVisualCatalog] is equipped - see
 * `core/feedback/FeedbackManagerImpl.kt`. Deliberately 3 shared families, not one bespoke set per
 * collection (7 fully bespoke sets would be a much larger synthesis/tuning effort) - still gives
 * every premium product an audibly distinct feel from the unthemed baseline. Plain `domain.model`
 * enum (no Android/audio dependency) so both the UI-layer visual catalog and the audio-layer
 * feedback manager can reference it without a layering inversion. */
enum class SfxMaterialFamily {
    /** Royal, Luxury, Cyber - hard, resonant, metal-on-metal character. */
    METALLIC,

    /** Nature, Founder's Pack, Starter Bundle - soft, warm, wood/fabric/earth character. */
    ORGANIC,

    /** Space Collection - bright, glassy, chime-like character. */
    CRYSTALLINE,
}
