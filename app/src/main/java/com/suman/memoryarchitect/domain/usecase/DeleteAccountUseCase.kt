package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.AccountDeletionRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountDeletionRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}
