package com.suman.memoryarchitect.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.suman.memoryarchitect.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject
import javax.inject.Singleton

/** The one thing every ad-request call site ([RewardedAdControllerImpl], [InterstitialAdControllerImpl],
 * [com.suman.memoryarchitect.feature.ads.BannerAdViewModel]) actually needs from [AdConsentManager] -
 * pulled into its own narrow interface purely for testability (a plain fake implements this with no
 * Android `Context`/`ConsentInformation` involved at all), the same convention
 * [InterstitialPacingPreferences] already establishes for [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore]. */
interface AdConsentGate {
    /** `true` either once consent has been gathered, or immediately if this user/region never
     * required it in the first place; `false` for the (typically sub-second, but real) window
     * before [AdConsentManager.requestConsentAndInitializeAds] has had a chance to run at all. */
    val canRequestAds: Boolean
}

/**
 * Google User Messaging Platform (UMP) integration - required by AdMob policy before requesting
 * any ad in the EEA/UK/Switzerland (and shown automatically anywhere else the SDK determines
 * consent is legally required). [requestConsentAndInitializeAds] should run once, as early as
 * possible with a real [Activity] on hand (see [com.suman.memoryarchitect.ui.ConnectivityGate]'s
 * call site) - it requests the latest consent status, shows Google's own consent form only if this
 * user/region actually requires one, then initializes the Mobile Ads SDK only once that's resolved,
 * matching Google's own documented ordering ("request ads only after consent is gathered").
 */
@Singleton
class AdConsentManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AdConsentGate {
    private val consentInformation: ConsentInformation by lazy { UserMessagingPlatform.getConsentInformation(context) }
    private val initMutex = Mutex()
    @Volatile private var mobileAdsInitialized = false

    override val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    suspend fun requestConsentAndInitializeAds(activity: Activity) {
        if (mobileAdsInitialized) return
        initMutex.withLock {
            if (mobileAdsInitialized) return@withLock
            val params = ConsentRequestParameters.Builder()
                .apply {
                    // Debug-build-only, and only ever forces the EEA consent flow to actually
                    // appear on this developer's own test device(s) - never affects a release
                    // build's real users, who always go through Google's genuine region detection.
                    if (BuildConfig.DEBUG) {
                        setConsentDebugSettings(
                            ConsentDebugSettings.Builder(context)
                                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                                .build(),
                        )
                    }
                }
                .build()

            val updateSucceeded = suspendCoroutine { continuation ->
                consentInformation.requestConsentInfoUpdate(
                    activity,
                    params,
                    { continuation.resume(true) },
                    { error ->
                        Log.w(TAG, "requestConsentInfoUpdate failed: ${error.message}")
                        continuation.resume(false)
                    },
                )
            }

            if (updateSucceeded) {
                // Loads and shows Google's own consent form only if this user/region genuinely
                // requires one - a no-op callback otherwise. Never throws outward; a form load
                // failure just means canRequestAds() below reflects whatever it already was.
                suspendCoroutine<Unit> { continuation ->
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) Log.w(TAG, "Consent form error: ${formError.message}")
                        continuation.resume(Unit)
                    }
                }
            }

            if (consentInformation.canRequestAds()) {
                suspendCoroutine<Unit> { continuation ->
                    MobileAds.initialize(context) {
                        mobileAdsInitialized = true
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "AdConsentManager"
    }
}
