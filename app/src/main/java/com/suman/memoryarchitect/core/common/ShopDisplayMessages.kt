package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.feature.shop.ShopFailureReason

/** Mirrors [com.suman.memoryarchitect.core.billing.BillingDisplayMessages]'s shape. */
fun ShopFailureReason.toDisplayMessage(context: Context): String = when (this) {
    ShopFailureReason.INSUFFICIENT_COINS -> context.getString(R.string.shop_error_insufficient_coins)
    ShopFailureReason.NO_CONNECTIVITY -> context.getString(R.string.shop_error_no_connectivity)
    ShopFailureReason.ALREADY_OWNED -> context.getString(R.string.shop_error_already_owned)
    ShopFailureReason.SPIN_NOT_AVAILABLE -> context.getString(R.string.shop_error_spin_not_available)
    ShopFailureReason.GENERIC -> context.getString(R.string.shop_error_generic)
}
