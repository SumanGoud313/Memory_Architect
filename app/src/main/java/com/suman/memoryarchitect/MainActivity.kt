package com.suman.memoryarchitect

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logNotificationTapped
import com.suman.memoryarchitect.ui.ConnectivityGate
import com.suman.memoryarchitect.ui.theme.MemoryArchitectTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var analytics: AnalyticsLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // A short, plain fade - not a scale/flourish - because the real premium entrance (fade +
        // 0.95->1.0 scale + soft glow) is owned by the Compose SplashScreen composable that's
        // already rendering underneath by the time this fires (see ConnectivityGate.kt). This exit
        // is only bridging the native cold-start splash into that Compose splash, not performing
        // its own competing animation of the same logo.
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.iconView.animate()
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

        // A cold launch from tapping a DailyReminderWorker notification carries this extra - see
        // that class's doc. No deep-linking beyond opening the app normally: none of the two
        // notification categories point at a screen specific enough to justify one.
        intent?.getStringExtra(EXTRA_TAPPED_NOTIFICATION_CATEGORY)?.let { category ->
            analytics.logNotificationTapped(category)
        }

        setContent {
            MemoryArchitectTheme {
                ConnectivityGate()
            }
        }
    }

    companion object {
        const val EXTRA_TAPPED_NOTIFICATION_CATEGORY = "tapped_notification_category"
        private const val SPLASH_EXIT_DURATION_MS = 280L
    }
}
