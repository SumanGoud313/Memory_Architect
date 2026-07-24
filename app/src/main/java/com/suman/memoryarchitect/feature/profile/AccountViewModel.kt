package com.suman.memoryarchitect.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.AvatarCatalog
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.AvatarUploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [NO_GOOGLE_ACCOUNT] covers `NoCredentialException` - the device has no Google account for
 * Credential Manager to offer at all, the single most common reason the sign-in flow appears to
 * silently "do nothing" (an emulator with no account signed in, a work profile that blocks it,
 * etc.) - surfaced distinctly so it reads as "add a Google account to this device" rather than a
 * generic failure. */
enum class GoogleLinkError { ALREADY_LINKED_ELSEWHERE, NO_GOOGLE_ACCOUNT, UNKNOWN }

/** Backs Profile's Account section - verification status (via [PlayerIdentityManager.isVerified]),
 * the Google Sign-In upgrade flow, curated avatar selection, and the optional self-reported
 * country. Deliberately reads/writes [PlayerIdentityManager]/[UserPreferencesDataStore] directly
 * rather than through use-case wrappers - these are device/account settings, not gameplay domain
 * logic, the same reasoning [com.suman.memoryarchitect.feature.settings.SettingsViewModel] already
 * applies talking to [com.suman.memoryarchitect.core.feedback.AudioSettingsManager] directly. */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val preferences: UserPreferencesDataStore,
    private val avatarUploadRepository: AvatarUploadRepository,
) : ViewModel() {

    val isVerified: StateFlow<Boolean> = playerIdentityManager.isVerified
    val googleDisplayName: StateFlow<String?> = playerIdentityManager.displayName
    val googlePhotoUrl: StateFlow<String?> = playerIdentityManager.photoUrl

    val avatarId: StateFlow<String> = preferences.avatarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AvatarCatalog.DEFAULT_AVATAR_ID)

    val avatarUrl: StateFlow<String?> = preferences.avatarUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val country: StateFlow<String?> = preferences.country
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLinking = MutableStateFlow(false)
    val isLinking: StateFlow<Boolean> = _isLinking.asStateFlow()

    private val _linkError = MutableStateFlow<GoogleLinkError?>(null)
    val linkError: StateFlow<GoogleLinkError?> = _linkError.asStateFlow()

    /** [idToken] comes from the caller's own Credential Manager request (see
     * `ui/screens/profile/AccountSection.kt`) - this function only performs the Firebase-side
     * link, it has no Activity/Credential Manager dependency of its own. */
    fun linkWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLinking.value = true
            _linkError.value = null
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
     * account on this device) through the same [linkError] surface [linkWithGoogle] uses, so
     * "picker failed before we even had a token" and "Firebase rejected the token" both give the
     * player visible feedback instead of one of them silently doing nothing. */
    fun reportSignInPickerFailure(error: GoogleLinkError) {
        _linkError.value = error
    }

    fun dismissLinkError() {
        _linkError.value = null
    }

    fun setAvatarId(id: String) {
        viewModelScope.launch { preferences.setAvatarId(id) }
    }

    fun setCountry(isoCountryCode: String?) {
        viewModelScope.launch { preferences.setCountry(isoCountryCode) }
    }

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    private val _avatarUploadFailed = MutableStateFlow(false)
    val avatarUploadFailed: StateFlow<Boolean> = _avatarUploadFailed.asStateFlow()

    /** [jpegBytes] must already be downscaled/compressed by the caller (see
     * `ui/screens/profile/AccountSection.kt`'s photo-picker launcher) - on success, persists the
     * resulting download URL locally so it takes effect immediately without waiting for the next
     * scored round's leaderboard push. */
    fun uploadAvatar(jpegBytes: ByteArray) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _avatarUploadFailed.value = false
            when (val outcome = avatarUploadRepository.uploadAvatar(jpegBytes)) {
                is Outcome.Success -> preferences.setAvatarUrl(outcome.data)
                is Outcome.Error -> _avatarUploadFailed.value = true
            }
            _isUploadingAvatar.value = false
        }
    }

    fun dismissAvatarUploadError() {
        _avatarUploadFailed.value = false
    }
}
