package com.suman.memoryarchitect.feature.shop

import com.suman.memoryarchitect.domain.model.AppError

/** Mirrors [com.suman.memoryarchitect.core.billing.BillingFailureReason]'s shape - same "plain
 * reason enum, message text lives only in the display-message extension" convention. */
enum class ShopFailureReason {
    INSUFFICIENT_COINS,
    NO_CONNECTIVITY,
    ALREADY_OWNED,
    /** [com.suman.memoryarchitect.data.repository.SpinNotAvailableException] - a Free/Ad Lucky
     * Spin allowance was already spent today. The Spin button is already disabled for this case
     * on the client, so this only ever surfaces from a stale/race-condition retry. */
    SPIN_NOT_AVAILABLE,
    GENERIC,
}

fun AppError.toShopFailureReason(): ShopFailureReason = when (this) {
    is AppError.NoConnectivity -> ShopFailureReason.NO_CONNECTIVITY
    is AppError.Network -> ShopFailureReason.NO_CONNECTIVITY
    is AppError.Server -> when (code) {
        402 -> ShopFailureReason.INSUFFICIENT_COINS
        409 -> ShopFailureReason.ALREADY_OWNED
        429 -> ShopFailureReason.SPIN_NOT_AVAILABLE
        else -> ShopFailureReason.GENERIC
    }
    else -> ShopFailureReason.GENERIC
}
