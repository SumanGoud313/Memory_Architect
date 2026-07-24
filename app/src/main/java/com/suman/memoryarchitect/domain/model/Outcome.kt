package com.suman.memoryarchitect.domain.model

/**
 * Result wrapper for use-case/repository calls. Named to avoid clashing with [kotlin.Result],
 * which several data-layer helpers (e.g. runCatching) already return locally.
 */
sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val error: AppError) : Outcome<Nothing>()
}