package com.suman.memoryarchitect.core.billing

import android.content.Context
import com.suman.memoryarchitect.R

/** Mirrors [com.suman.memoryarchitect.core.common.AppErrorMessages]'s shape for
 * [com.suman.memoryarchitect.domain.model.AppError] - same "plain reason enum, message text lives
 * only here" convention. */
fun BillingFailureReason.toDisplayMessage(context: Context): String = when (this) {
    BillingFailureReason.NOT_READY -> context.getString(R.string.remove_ads_error_not_ready)
    BillingFailureReason.ITEM_UNAVAILABLE -> context.getString(R.string.remove_ads_error_item_unavailable)
    BillingFailureReason.BILLING_UNAVAILABLE -> context.getString(R.string.remove_ads_error_billing_unavailable)
    BillingFailureReason.GENERIC -> context.getString(R.string.remove_ads_error_generic)
}
