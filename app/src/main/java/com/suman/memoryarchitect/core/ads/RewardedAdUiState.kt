package com.suman.memoryarchitect.core.ads

/**
 * Lifecycle of a single rewarded-ad-gated feature's "Watch Ad" control - shared by every feature
 * [RewardedAdFlow] drives (Hint, Redo, Rewatch, or any future one) rather than each feature
 * defining its own copy of the same three states. A successful reward is never modeled here: it's
 * always a visible, feature-specific effect (a counter bumping, a scene replaying) that the caller
 * reacts to directly, not a fourth state this type would need to add.
 */
sealed interface RewardedAdUiState {
    data object Idle : RewardedAdUiState
    data object Loading : RewardedAdUiState
    data class Failed(val reason: RewardedAdFailureReason) : RewardedAdUiState
}
