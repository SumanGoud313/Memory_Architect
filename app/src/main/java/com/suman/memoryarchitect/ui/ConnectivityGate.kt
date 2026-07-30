package com.suman.memoryarchitect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.MainViewModel
import com.suman.memoryarchitect.feature.auth.SignInGateViewModel
import com.suman.memoryarchitect.ui.navigation.MemoryArchitectNavHost
import com.suman.memoryarchitect.ui.screens.auth.SignInGateScreen
import com.suman.memoryarchitect.ui.screens.nointernet.NoInternetScreen
import com.suman.memoryarchitect.ui.screens.splash.SplashScreen
import com.suman.memoryarchitect.ui.screens.splash.SplashTiming
import kotlinx.coroutines.delay

/**
 * Replaces the old `MainActivity` push/pop of `NoInternetFragment` as a real back-stack node.
 * The real [MemoryArchitectNavHost] stays permanently mounted underneath; a full-screen overlay
 * covers it whenever offline, and the back button backgrounds the app (never dismisses the gate
 * or touches the real back stack) while the overlay is visible — same behavior as before, just
 * expressed as a Compose overlay instead of a navigation destination.
 *
 * The Google Sign-In gate ([SignInGateScreen]) is a second overlay, same mechanism, shown whenever
 * online but not yet verified - every player must sign in with Google before reaching any gameplay
 * screen. Checked as `isOnline && !isVerified` rather than independently of connectivity: Google
 * Sign-In cannot succeed offline anyway, so the "No Internet" overlay above already covers that
 * case, and the two overlays never need to compete for which one shows.
 *
 * [SplashScreen] is a third overlay, topmost and shown unconditionally for exactly one cold start -
 * it covers whichever of the states above happens to be true underneath (Home, No Internet, or the
 * Sign-In gate) while those initialize normally in the background, then fades away to reveal
 * whichever one is actually correct once its own entrance animation finishes. [rememberSaveable]
 * (not [androidx.compose.runtime.remember]) so a configuration change mid-splash doesn't replay it,
 * while a genuine process restart correctly does.
 *
 * Also requests the `POST_NOTIFICATIONS` runtime permission (API 33+) on every cold start via a
 * plain [LaunchedEffect]\(Unit\), so it re-fires each time this Composable is freshly created -
 * i.e. each real app open - rather than only once ever. This is safe to call unconditionally: the
 * platform itself stops showing the system dialog (and just returns denied) once the user has
 * denied it twice, so repeating the request on later opens never re-annoys someone who said no.
 * Skipped entirely once already granted. Needed so [com.suman.memoryarchitect.core.notifications.DailyReminderWorker]
 * (streak reminders, daily challenge alerts) can actually post - both reminder toggles default to
 * on in [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore], but without this
 * permission the worker silently no-ops.
 */
@Composable
fun ConnectivityGate(viewModel: MainViewModel = hiltViewModel(), signInGateViewModel: SignInGateViewModel = hiltViewModel()) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isVerified by signInGateViewModel.isVerified.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MemoryArchitectNavHost(modifier = Modifier.fillMaxSize())

    AnimatedVisibility(visible = !isOnline, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = true) { activity?.moveTaskToBack(true) }
        NoInternetScreen()
    }

    AnimatedVisibility(visible = isOnline && signInGateViewModel.canSignIn && !isVerified, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = true) { activity?.moveTaskToBack(true) }
        SignInGateScreen(viewModel = signInGateViewModel)
    }

    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(SplashTiming.EXIT_FADE_DURATION_MS.toInt())),
    ) {
        SplashScreen()
    }

    if (showSplash) {
        LaunchedEffect(Unit) {
            delay(SplashTiming.ENTRANCE_DURATION_MS + SplashTiming.HOLD_DURATION_MS)
            showSplash = false
        }
    }
}
