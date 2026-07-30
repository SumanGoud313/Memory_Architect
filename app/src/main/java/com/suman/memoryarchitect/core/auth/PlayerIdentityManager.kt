package com.suman.memoryarchitect.core.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
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
 * [uid] is `null` until sign-in completes (or forever, if [FirebaseAvailabilityProvider.isConfigured] is
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
     * has them. Shown in the player's own Account section *and* on the public leaderboard - every
     * player reaches gameplay only after signing in with Google (see
     * [com.suman.memoryarchitect.ui.ConnectivityGate]'s mandatory sign-in gate), so this is the
     * real name every leaderboard entry shows now (see
     * [com.suman.memoryarchitect.data.repository.LeaderboardRepositoryImpl]'s
     * `playerDisplayName`) - no longer anonymized. */
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
     * If this Google account is already linked to a *different* uid elsewhere
     * (`ERROR_CREDENTIAL_ALREADY_IN_USE` - a reinstall, a cleared app, or a second device), this
     * falls back to [FirebaseAuth.signInWithCredential][com.google.firebase.auth.FirebaseAuth] with
     * that same credential instead of just failing - that authenticates as the *existing* linked
     * account, restoring its progress, rather than stranding the player on a fresh, disposable
     * anonymous identity with no way back to the account they already verified. The current
     * anonymous identity's own progress (if any - typically none, since this only ever triggers
     * on a device that hasn't accumulated anything of its own yet) is what's discarded here, never
     * the linked account's. Only a genuine failure of *that* fallback (offline, malformed token)
     * still returns [Result.failure] - the caller surfaces that distinctly from a generic network
     * failure so the player isn't left thinking they're verified when they aren't. */
    suspend fun linkWithGoogle(idToken: String): Result<Unit>

    /** Called only by account deletion, once every Firestore document under this [uid] has already
     * been removed. Best-effort deletes the underlying `FirebaseAuth` user - if that specifically
     * fails (e.g. the sign-in is no longer "recent" per Firebase's own re-authentication policy),
     * it's logged and swallowed, never surfaced as a failure, since the player's data is already
     * gone either way. Unconditionally resets this manager's own in-memory state to signed-out
     * afterward, so the mandatory sign-in gate reappears immediately, exactly like a fresh
     * install - leaving the app "verified" against an account whose data was just wiped would be a
     * far more broken state than an orphaned, dataless Auth record. */
    suspend fun signOutAfterAccountDeletion()
}

@Singleton
class PlayerIdentityManagerImpl @Inject constructor(
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : PlayerIdentityManager {
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
        if (!firebaseAvailabilityProvider.isConfigured) return
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
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val result = currentUser.linkWithCredential(credential).await()
            applyUser(result.user)
            Result.success(Unit)
        } catch (collision: FirebaseAuthUserCollisionException) {
            // This exact credential is already linked to a different (older) uid - see this
            // function's own doc for why signing in as that account, not just failing, is the
            // right recovery here.
            try {
                val result = Firebase.auth.signInWithCredential(credential).await()
                applyUser(result.user)
                Result.success(Unit)
            } catch (t: Throwable) {
                Log.w(TAG, "Sign-in fallback after Google account collision failed", t)
                Result.failure(collision)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Google account linking failed", t)
            Result.failure(t)
        }
    }

    override suspend fun signOutAfterAccountDeletion() {
        try {
            Firebase.auth.currentUser?.delete()?.await()
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseAuth account deletion failed - local sign-out still proceeds", t)
        }
        _uid.value = null
        _isVerified.value = false
        _displayName.value = null
        _photoUrl.value = null
        signInAttempted = false
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
