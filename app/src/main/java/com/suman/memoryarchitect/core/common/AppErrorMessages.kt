package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.AppError

fun AppError.toDisplayMessage(context: Context): String = when (this) {
    is AppError.NoConnectivity -> context.getString(R.string.error_no_connectivity)
    is AppError.Network -> context.getString(R.string.error_network)
    is AppError.Server -> context.getString(R.string.error_server, code)
    is AppError.Unknown -> context.getString(R.string.error_unknown)
    is AppError.FeatureUnavailable -> context.getString(R.string.leaderboard_unavailable)
}