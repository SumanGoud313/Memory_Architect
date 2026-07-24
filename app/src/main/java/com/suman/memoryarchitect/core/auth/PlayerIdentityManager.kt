package com.suman.memoryarchitect.core.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one source of "who is this player" for every leaderboard read/write - a stable per-device
 * identity via Firebase Anonymous Auth, never a username/password/email account. No login screen,
 * no PII: [uid] is a random Firebase-issued ID, silently established the first time the app runs
 * and persisted by the Firebase Auth SDK itself across restarts (survives app restarts and, unlike
 * this app's other local-only state, even a Room wipe via Settings' Reset Progress, since it's not
 * stored there - un-reinstalling is the only thing that changes it).
 *
 * [uid] is `null` until sign-in completes (or forever, if [FirebaseAvailability.isConfigured] is
 * false or the device is offline on first launch) - every leaderboard call site treats that as
 * "leaderboard temporarily unavailable," never blocks gameplay, exactly the same non-blocking
 * philosophy [com.suman.memoryarchitect.core.analytics.FirebaseRemoteConfigSource] already uses
 * for its own optional Firebase dependency.
 */
interface PlayerIdentityManager {
    val uid: StateFlow<String?>

    /** `true` once this device's identity has a real, non-anonymous provider attached (currently
     * only Google, via [linkWithGoogle]) - "this player upgraded from anonymous," the same meaning
     * a leaderboard's verified badge carries in top games. Mirrors
     * `Firebase.auth.currentUser?.isAnonymous == false` directly; never a separately-stored flag
     * that could drift from the actual auth state. */
    val isVerified: StateFlow<Boolean>

    /** The linked Google account's own display name/profile photo URL - `null` until
     * [isVerified] is `true` (an anonymous identity has neither). Read from the `"google.com"`
     * entry in `FirebaseUser.providerData`, not the top-level `FirebaseUser.displayName`/`.photoUrl`
     * - when *linking* a credential onto an already-existing anonymous user (as opposed to a fresh
     * sign-in), Firebase does not reliably copy the newly-linked provider's profile fields up to
     * the top level, but the linked provider's own `UserInfo` entry inside `providerData` always
     * has them. Shown only to the player themselves in their own Account section; the *public*
     * leaderboard display name stays the anonymized `Architect#XXXX` form regardless of
     * verification (see [com.suman.memoryarchitect.data.repository.LeaderboardRepositoryImpl]'s
     * `toPlayerDisplayName`) - this is not a channel for real names to reach other players. */
    val displayName: StateFlow<String?>
    val photoUrl: StateFlow<String?>

    /** Best-effort - safe to call speculatively (e.g. from [com.suman.memoryarchitect.MemoryArchitectApp.onCreate]);
     * a no-op if already signed in or already in flight. */
    fun ensureSignedIn()

    /** Returns [uid] immediately if already resolved, otherwise gives [ensureSignedIn]'s in-flight
     * sign-in a brief window to finish rather than reporting unavailable right away - relevant for
     * any Firestore-backed call made within the first second or two of a cold launch, before
     * anonymous auth has round-tripped. Shared by every Firestore-backed repository
     * ([com.suman.memoryarchitect.data.repository.LeaderboardRepositoryImpl],
     * [com.suman.memoryarchitect.data.repository.FirestoreProgressionRemoteSource]) so this
     * "how long to wait" policy lives in exactly one place. */
    suspend fun awaitUid(timeoutMs: Long = 4_000L): String?

    /** Upgrades the current anonymous identity to a real Google account via
     * [FirebaseAuth.currentUser.linkWithCredential][com.google.firebase.auth.FirebaseUser] -
     * **linking**, never a fresh sign-in, so [uid] and everything already stored under it
     * (`playerProfiles/{uid}`, `players/{uid}`, local Room caches keyed by nothing but the device
     * itself) carry over completely unchanged. [idToken] is obtained by the caller via Credential
     * Manager (see `feature/settings/AccountUpgradeViewModel.kt`) - this function only performs the
     * Firebase-side link, it has no UI/Credential Manager dependency itself.
     *
     * Fails (a [Result.failure], never a thrown exception) if this Google account is already linked
     * to a *different* uid elsewhere (`ERROR_CREDENTIAL_ALREADY_IN_USE`) - the caller must surface
     * that distinctly from a generic network failure, since silently swallowing it would leave the
     * player thinking they're verified when they aren't. */
    suspend fun linkWithGoogle(idToken: String): Result<Unit>
}

@Singleton
class PlayerIdentityManagerImpl @Inject constructor() : PlayerIdentityManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val signInMutex = Mutex()
    private var signInAttempted = false

    private val _uid = MutableStateFlow<String?>(null)
    override val uid: StateFlow<String?> = _uid.asStateFlow()

    private val _isVerified = MutableStateFlow(false)
    override val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    override val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _photoUrl = MutableStateFlow<String?>(null)
    override val photoUrl: StateFlow<String?> = _photoUrl.asStateFlow()

    override suspend fun awaitUid(timeoutMs: Long): String? =
        _uid.value ?: withTimeoutOrNull(timeoutMs) { _uid.filterNotNull().first() }

    override fun ensureSignedIn() {
        if (!FirebaseAvailability.isConfigured) return
        if (_uid.value != null || signInAttempted) return
        scope.launch {
            signInMutex.withLock {
                if (_uid.value != null || signInAttempted) return@withLock
                signInAttempted = true
                try {
                    val auth = Firebase.auth
                    val existing = auth.currentUser
                    val user = existing ?: auth.signInAnonymously().await().user
                    applyUser(user)
                } catch (t: Throwable) {
                    // Leave uid null and signInAttempted true - every leaderboard call site already
                    // treats a null uid as "unavailable right now" rather than erroring the whole
                    // screen. A future ensureSignedIn() call from a fresh app session will retry.
                    Log.w(TAG, "Anonymous sign-in failed - leaderboard features unavailable this session", t)
                }
            }
        }
    }

    override suspend fun linkWithGoogle(idToken: String): Result<Unit> {
        val currentUser = Firebase.auth.currentUser
            ?: return Result.failure(IllegalStateException("No signed-in user to upgrade - ensureSignedIn() must complete first"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = currentUser.linkWithCredential(credential).await()
            applyUser(result.user)
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.w(TAG, "Google account linking failed", t)
            Result.failure(t)
        }
    }

    private fun applyUser(user: FirebaseUser?) {
        _uid.value = user?.uid
        _isVerified.value = user?.isAnonymous == false
        // Prefer the "google.com" entry in providerData - see displayName/photoUrl's doc for why
        // the top-level FirebaseUser fields can't be trusted after a *link* (vs. a fresh sign-in).
        val googleInfo = user?.providerData?.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        _displayName.value = googleInfo?.displayName ?: user?.displayName
        _photoUrl.value = (googleInfo?.photoUrl ?: user?.photoUrl)?.toString()
    }

    private companion object {
        const val TAG = "PlayerIdentity"
    }
}
