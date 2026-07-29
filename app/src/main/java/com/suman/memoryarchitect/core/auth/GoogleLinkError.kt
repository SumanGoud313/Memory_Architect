package com.suman.memoryarchitect.core.auth

/** [NO_GOOGLE_ACCOUNT] covers `NoCredentialException` - the device has no Google account for
 * Credential Manager to offer at all, the single most common reason the sign-in flow appears to
 * silently "do nothing" (an emulator with no account signed in, a work profile that blocks it,
 * etc.) - surfaced distinctly so it reads as "add a Google account to this device" rather than a
 * generic failure. Shared by [PlayerIdentityManager]'s two callers -
 * [com.suman.memoryarchitect.feature.profile.AccountViewModel] (the optional Profile upgrade) and
 * [com.suman.memoryarchitect.feature.auth.SignInGateViewModel] (the mandatory sign-in gate). */
enum class GoogleLinkError { ALREADY_LINKED_ELSEWHERE, NO_GOOGLE_ACCOUNT, UNKNOWN }
