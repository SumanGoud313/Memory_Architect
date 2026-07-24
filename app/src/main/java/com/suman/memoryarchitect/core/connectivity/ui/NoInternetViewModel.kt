package com.suman.memoryarchitect.core.connectivity.ui

import androidx.lifecycle.ViewModel
import com.suman.memoryarchitect.core.connectivity.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NoInternetViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    fun retry() {
        networkMonitor.refresh()
    }
}