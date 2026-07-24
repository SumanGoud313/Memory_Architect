package com.suman.memoryarchitect.domain.repository

interface LocalProgressResetRepository {
    suspend fun resetAll()
}
