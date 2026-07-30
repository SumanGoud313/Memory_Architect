package com.suman.memoryarchitect

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.ads.AdConsentManager
import com.suman.memoryarchitect.core.connectivity.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    private val adConsentManager: AdConsentManager,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    /** Runs the UMP consent flow (showing Google's own form only if this user/region requires
     * one) and only then initializes the Mobile Ads SDK - see [AdConsentManager]'s own doc. Safe to
     * call every time [com.suman.memoryarchitect.ui.ConnectivityGate] recomposes; a no-op once
     * already resolved. */
    fun requestAdConsent(activity: Activity) {
        viewModelScope.launch { adConsentManager.requestConsentAndInitializeAds(activity) }
    }
}