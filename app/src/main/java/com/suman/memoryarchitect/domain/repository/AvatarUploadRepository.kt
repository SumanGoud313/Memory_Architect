package com.suman.memoryarchitect.domain.repository

import com.suman.memoryarchitect.domain.model.Outcome

/** Custom avatar photo upload, backed by Firebase Storage - the counterpart to the curated,
 * no-upload [com.suman.memoryarchitect.domain.model.AvatarCatalog] set. Same
 * [com.suman.memoryarchitect.core.analytics.FirebaseAvailability]-guarded, fails-soft posture as
 * every other Firestore/Storage-backed repository in this app (see `storage.rules` at the project
 * root for the server-side size/content-type enforcement this depends on). */
interface AvatarUploadRepository {
    /** [jpegBytes] must already be downscaled/compressed by the caller (see
     * `ui/screens/settings/AccountSection.kt`) - this function only uploads and returns the
     * resulting public download URL, it performs no image processing of its own. */
    suspend fun uploadAvatar(jpegBytes: ByteArray): Outcome<String>
}
