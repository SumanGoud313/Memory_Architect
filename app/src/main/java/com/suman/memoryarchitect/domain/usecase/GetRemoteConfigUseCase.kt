package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import javax.inject.Inject

class GetRemoteConfigUseCase @Inject constructor(
    private val repository: RemoteConfigRepository,
) {
    suspend operator fun invoke(): Outcome<RemoteConfig> = repository.getRemoteConfig()
}