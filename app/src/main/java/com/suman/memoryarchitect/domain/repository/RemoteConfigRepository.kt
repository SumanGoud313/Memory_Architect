package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RemoteConfig

interface RemoteConfigRepository {
    suspend fun getRemoteConfig(): Outcome<RemoteConfig>
}