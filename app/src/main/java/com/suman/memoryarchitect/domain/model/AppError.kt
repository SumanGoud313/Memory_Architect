package com.suman.memoryarchitect.domain.model

sealed class AppError {
    data object NoConnectivity : AppError()
    data class Network(val cause: Throwable) : AppError()
    data class Server(val code: Int, val message: String?) : AppError()
    data class Unknown(val cause: Throwable) : AppError()

    /** A feature that depends on an optional, environment-specific backend (Firestore
     * leaderboards not yet enabled in the Firebase console, no signed-in player identity yet)
     * is unavailable - distinct from [Network]/[Server], which imply "it's configured but this
     * one call failed." Never a crash, never blocks gameplay - see
     * [com.suman.memoryarchitect.domain.repository.LeaderboardRepository]. */
    data object FeatureUnavailable : AppError()
}