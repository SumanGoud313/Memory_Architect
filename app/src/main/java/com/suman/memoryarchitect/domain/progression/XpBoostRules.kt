package com.suman.memoryarchitect.domain.progression

/** Tuning for [com.suman.memoryarchitect.domain.model.InventoryItemKind.XP_BOOST] - consuming one
 * grants a flat, immediate XP bonus, the same "consumed for an instant, self-contained effect"
 * shape Hint/Redo/Rewatch tokens already use (see
 * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.useInventoryHintToken]'s doc),
 * deliberately simpler than a time-limited multiplier: no expiry state to persist anywhere, and
 * nothing that could desync across an offline session the way a "boosted for the next N minutes"
 * window could. [xpGrantedPerBoost] stays comfortably under `functions/src/index.ts`'s
 * `MAX_PLAUSIBLE_XP_GAIN_PER_WRITE` (5,000/write). */
data class XpBoostRules(val xpGrantedPerBoost: Long = 250L) {
    companion object {
        val Default = XpBoostRules()
    }
}
