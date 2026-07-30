package com.suman.memoryarchitect.feature.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.AdConsentGate
import com.suman.memoryarchitect.core.ads.bannerAdsEnabled
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logAdLoadLatency
import com.suman.memoryarchitect.core.analytics.logAdRevenue
import com.suman.memoryarchitect.core.analytics.logBannerClicked
import com.suman.memoryarchitect.core.analytics.logBannerImpression
import com.suman.memoryarchitect.core.analytics.logBannerLoadFailed
import com.suman.memoryarchitect.core.billing.BillingManager
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs every [com.suman.memoryarchitect.core.ads.BannerAdView] placement - one shared ViewModel
 * class, one instance per screen (Hilt scopes `@HiltViewModel` per navigation back-stack entry by
 * default), rather than each screen re-deriving "should a banner show here" independently. */
@HiltViewModel
class BannerAdViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val adConsentGate: AdConsentGate,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    private val _remoteConfigBannerEnabled = MutableStateFlow(true)
    private val _canRequestAds = MutableStateFlow(adConsentGate.canRequestAds)

    init {
        viewModelScope.launch {
            val remoteConfig = (remoteConfigRepository.getRemoteConfig() as? Outcome.Success)?.data
            // A Remote Config failure with no cached fallback keeps the optimistic `true` default -
            // same "a banner that fails to show is much less harmful than one that blocks a screen"
            // reasoning GetActiveLiveEventUseCase's own doc already documents for this exact
            // failure mode.
            if (remoteConfig != null) _remoteConfigBannerEnabled.value = remoteConfig.bannerAdsEnabled()
        }
        // UMP policy: never request an ad before consent is gathered (where required) - see
        // AdConsentManager's own doc. Polled rather than a real Flow since ConsentInformation has
        // no listener API of its own; short-lived in practice, since MainViewModel.requestAdConsent
        // runs at app launch, well before a player reaches any banner-carrying screen.
        viewModelScope.launch {
            while (!adConsentGate.canRequestAds) {
                delay(250L)
            }
            _canRequestAds.value = true
        }
    }

    /** `true` only once every gate agrees: the player hasn't purchased Remove Ads, Remote Config's
     * `banner_ads_enabled`/`emergency_ads_disabled` combination allows it, and ad-consent is
     * resolved. Starts `true` (`WhileSubscribed`, not eagerly) rather than `false` so a slow first
     * Remote Config fetch never flashes "no banner" for players who are eligible - the
     * false-negative case here is a banner appearing a beat late, never one flashing then
     * disappearing. */
    val shouldShowBanner: StateFlow<Boolean> =
        combine(billingManager.hasRemovedAds, _remoteConfigBannerEnabled, _canRequestAds) { hasRemovedAds, enabled, canRequestAds ->
            !hasRemovedAds && enabled && canRequestAds
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun onBannerImpression(placement: String) = analytics.logBannerImpression(placement)

    fun onBannerClicked(placement: String) = analytics.logBannerClicked(placement)

    fun onBannerRevenue(placement: String, valueMicros: Long, currencyCode: String) =
        analytics.logAdRevenue(placement, valueMicros, currencyCode, adFormat = "banner")

    fun onBannerLoadFailed(placement: String) = analytics.logBannerLoadFailed(placement)

    fun onBannerLoadLatency(placement: String, latencyMs: Long, success: Boolean) =
        analytics.logAdLoadLatency("banner", placement, latencyMs, success)
}
