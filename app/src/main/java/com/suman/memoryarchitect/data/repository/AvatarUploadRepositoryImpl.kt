package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.AvatarUploadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Uploads to `avatars/{uid}/avatar.jpg` - always that exact fixed filename, so a re-upload
 * overwrites in place rather than accumulating orphaned files under the player's own path (no
 * cleanup job needed, mirrors the same "keyed by uid, overwrite not append" principle
 * [LeaderboardRepositoryImpl]'s doc already establishes for leaderboard entries). Every write is
 * bounded by `storage.rules`' owner-only + size + content-type checks - this class trusts those
 * rules as the real enforcement, it doesn't re-validate the byte array itself beyond what the
 * caller already downscaled/compressed. */
@Singleton
class AvatarUploadRepositoryImpl @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val dispatchers: DispatcherProvider,
) : AvatarUploadRepository {

    private val storage by lazy { Firebase.storage }

    override suspend fun uploadAvatar(jpegBytes: ByteArray): Outcome<String> = withContext(dispatchers.io) {
        if (!FirebaseAvailability.isConfigured) return@withContext Outcome.Error(AppError.FeatureUnavailable)
        val uid = playerIdentityManager.awaitUid() ?: return@withContext Outcome.Error(AppError.FeatureUnavailable)
        try {
            val ref = storage.reference.child("avatars/$uid/avatar.jpg")
            ref.putBytes(jpegBytes).await()
            val url = ref.downloadUrl.await().toString()
            Outcome.Success(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(AppError.Unknown(failure))
        }
    }
}
