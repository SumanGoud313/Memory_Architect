package com.suman.memoryarchitect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.GoogleLinkError
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs [com.suman.memoryarchitect.ui.screens.auth.SignInGateScreen] - the mandatory,
 * non-dismissible "sign in to play" overlay [com.suman.memoryarchitect.ui.ConnectivityGate] shows
 * whenever [isVerified] is false, the exact same trimmed-down shape
 * [com.suman.memoryarchitect.feature.profile.AccountViewModel]'s Google-linking slice already has
 * for the optional Profile upgrade - both ultimately just call
 * [PlayerIdentityManager.linkWithGoogle]. */
@HiltViewModel
class SignInGateViewModel @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : ViewModel() {

    val isVerified: StateFlow<Boolean> = playerIdentityManager.isVerified

    /** Mirrors [com.suman.memoryarchitect.ui.screens.profile.AccountStatusCard]'s own combined
     * guard exactly - if either half is false, Google Sign-In is impossible in this build (a
     * dev/test variant with no Firebase project wired up), so
     * [com.suman.memoryarchitect.ui.ConnectivityGate] skips the gate entirely rather than
     * permanently blocking that variant. Never false in the real shipped build. */
    val canSignIn: Boolean = firebaseAvailabilityProvider.isConfigured && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    private val _isLinking = MutableStateFlow(false)
    val isLinking: StateFlow<Boolean> = _isLinking.asStateFlow()

    private val _linkError = MutableStateFlow<GoogleLinkError?>(null)
    val linkError: StateFlow<GoogleLinkError?> = _linkError.asStateFlow()

    /** [idToken] comes from the caller's own Credential Manager request (see
     * `core/auth/GoogleSignInFlow.kt`) - this function only performs the Firebase-side link. Waits
     * on [PlayerIdentityManager.awaitUid] first: unlike
     * [com.suman.memoryarchitect.feature.profile.AccountViewModel] (whose Google-link button is
     * only ever reachable long after app start), this is the mandatory gate shown at the very
     * moment [com.suman.memoryarchitect.MemoryArchitectApp.onCreate]'s `ensureSignedIn()` anonymous
     * sign-in may still be in flight - [PlayerIdentityManager.linkWithGoogle] needs an existing
     * `Firebase.auth.currentUser` to link onto, so this avoids a real race on cold launch. */
    fun linkWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLinking.value = true
            _linkError.value = null
            playerIdentityManager.awaitUid()
            playerIdentityManager.linkWithGoogle(idToken).onFailure { failure ->
                _linkError.value = if (failure is FirebaseAuthUserCollisionException) {
                    GoogleLinkError.ALREADY_LINKED_ELSEWHERE
                } else {
                    GoogleLinkError.UNKNOWN
                }
            }
            _isLinking.value = false
        }
    }

    /** Reports a Credential Manager-side failure (never reached Firebase at all - e.g. no Google
     * account on this device) through the same [linkError] surface [linkWithGoogle] uses. */
    fun reportSignInPickerFailure(error: GoogleLinkError) {
        _linkError.value = error
    }
}
