package com.suman.memoryarchitect.core.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort, **advisory-only** Play Integrity check - never a hard gate anywhere in this app.
 * Requests a Play Integrity token opportunistically (see [checkOpportunistically], called once
 * from [com.suman.memoryarchitect.MemoryArchitectApp.onCreate]) and forwards it to the
 * `verifyDeviceIntegrity` Cloud Function, which records a pass/fail verdict at
 * `deviceIntegrity/{uid}` for [com.suman.memoryarchitect.data.repository.FirestoreProgressionRemoteSource]'s
 * server-side re-validation (`functions/src/index.ts`'s `validateProfileWrite`) to read.
 *
 * This is genuinely untestable in this environment: the Play Integrity API only returns real
 * verdicts once the app's Play Console listing is linked to this Firebase project (see
 * LEADERBOARD_SETUP.md's "Play Integrity" section) - on an emulator, a debug build, or before that
 * linking exists, [requestIntegrityToken]/the Function call both fail, and this class swallows
 * that failure silently (a `Log.i`, nothing louder) rather than surfacing any error to the player -
 * exactly because a failure here must never be distinguishable from "not configured yet" at the UI
 * layer. Every call site treats the complete absence of a `deviceIntegrity/{uid}` doc as the normal,
 * expected default state, not an error condition.
 */
@Singleton
class DeviceIntegrityChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Fire-and-forget - never throws, never blocks the caller. Safe to call speculatively on
     * app start and again before a scored submission (throttling repeated calls is intentionally
     * left to the caller, since this class has no opinion on how often is "often enough"). */
    suspend fun checkOpportunistically() {
        if (!FirebaseAvailability.isConfigured) return
        try {
            val token = requestIntegrityToken() ?: return
            Firebase.functions.getHttpsCallable("verifyDeviceIntegrity")
                .call(mapOf("integrityToken" to token))
                .await()
        } catch (failure: Throwable) {
            // Expected and routine until Play Console linking exists (see class doc) - never
            // reported to Crashlytics, this is not an unexpected failure from this app's own code.
            Log.i(TAG, "Device integrity check unavailable this session: ${failure.message}")
        }
    }

    private suspend fun requestIntegrityToken(): String? {
        val nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) })
        val request = IntegrityTokenRequest.builder().setNonce(nonce).build()
        val response = IntegrityManagerFactory.create(context).requestIntegrityToken(request).await()
        return response.token()
    }

    private companion object {
        const val TAG = "DeviceIntegrity"
        const val NONCE_BYTES = 16
    }
}
