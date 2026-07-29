package com.suman.memoryarchitect.core.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.suman.memoryarchitect.BuildConfig

/** Launches Credential Manager's Google ID flow and reports the outcome via exactly one of
 * [onIdToken]/[onFailure] - never neither, which is what made the sign-in button read as "just
 * doing nothing" before this existed: [GetCredentialException] has several real-failure subtypes
 * (most commonly [NoCredentialException] - no Google account available on this device at all) that
 * used to be caught by the same broad handler as a genuine user-cancellation and silently dropped.
 * Only an actual [GetCredentialCancellationException] (the player backed out of the picker) stays
 * silent now - every other failure reaches [onFailure] so the caller's own link-error state can
 * show it. Shared by [com.suman.memoryarchitect.ui.screens.profile.AccountSection] (the optional
 * Profile upgrade) and [com.suman.memoryarchitect.ui.screens.auth.SignInGateScreen] (the mandatory
 * sign-in gate) - one Credential Manager integration, not two. Callers must guard on
 * [BuildConfig.GOOGLE_WEB_CLIENT_ID] being non-blank themselves before reaching this - a blank
 * client ID means Google Sign-In hasn't been configured for this Firebase project yet (see
 * LEADERBOARD_SETUP.md). */
suspend fun signInWithGoogle(context: Context, onIdToken: (String) -> Unit, onFailure: (GoogleLinkError) -> Unit) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    try {
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
        onIdToken(credential.idToken)
    } catch (cancelled: GetCredentialCancellationException) {
        Log.i(TAG, "Google Sign-In cancelled by the player")
    } catch (noAccount: NoCredentialException) {
        Log.i(TAG, "No Google account available on this device for sign-in")
        onFailure(GoogleLinkError.NO_GOOGLE_ACCOUNT)
    } catch (other: GetCredentialException) {
        Log.w(TAG, "Google Sign-In failed", other)
        onFailure(GoogleLinkError.UNKNOWN)
    }
}

private const val TAG = "GoogleSignInFlow"
