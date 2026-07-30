package com.suman.memoryarchitect.domain.repository

/** Full account + data deletion - Settings' "Delete Account," required by Play Data Safety since
 * this app supports account creation (Google Sign-In). Broader than
 * [LocalProgressResetRepository]'s "Reset Progress" in every way: removes every Firestore document
 * this player owns (not just the 5 [LocalProgressResetRepository] narrows itself to), deletes the
 * underlying Firebase Auth account, and wipes local Room/DataStore state, leaving the device ready
 * for a completely fresh sign-in. */
interface AccountDeletionRepository {
    /** [Result.failure] means nothing was deleted yet (a network failure reaching Firestore, or not
     * signed in) - safe to retry. Once the Firestore deletes themselves succeed, the rest of this
     * call (local wipe, Auth deletion, sign-out) never fails outward - see
     * [com.suman.memoryarchitect.core.auth.PlayerIdentityManager.signOutAfterAccountDeletion]'s own
     * doc for why. */
    suspend fun deleteAccount(): Result<Unit>
}
