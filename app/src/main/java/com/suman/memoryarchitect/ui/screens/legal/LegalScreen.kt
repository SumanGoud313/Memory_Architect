package com.suman.memoryarchitect.ui.screens.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.ScreenHeader
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Which static legal document to show - one screen shell, two bodies, matching how
 * [com.suman.memoryarchitect.ui.screens.shop.CosmeticsHubScreen] shares one shell across tabs
 * rather than duplicating [ScreenHeader]/[AmbientBackground] wiring per document. */
enum class LegalDocument { PRIVACY_POLICY, TERMS_AND_CONDITIONS }

/** Plain Kotlin string literals rather than `strings.xml` resources - deliberately: this is
 * long-form legal text, never localized like the rest of this app's UI copy, and `strings.xml`
 * would force escaping every apostrophe/quote in it. Settings' Privacy Policy/Terms rows are the
 * in-app entry points; the same text should also be hosted at a public URL for the Play Console
 * listing's Privacy Policy field, which this codebase can't provision on your behalf - see the
 * Play Store readiness report. */
@Composable
fun LegalScreen(document: LegalDocument, onBack: () -> Unit) {
    val particles = rememberParticleFieldState()
    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = if (document == LegalDocument.PRIVACY_POLICY) "Privacy Policy" else "Terms & Conditions",
                onBack = onBack,
                modifier = Modifier.fillMaxWidth().padding(24.dp).staggeredReveal(0),
            )
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Last updated: 2026-07-31",
                    style = MaterialTheme.typography.labelMedium,
                    color = MemoryArchitectColors.textTertiary,
                )
                when (document) {
                    LegalDocument.PRIVACY_POLICY -> PrivacyPolicyBody()
                    LegalDocument.TERMS_AND_CONDITIONS -> TermsBody()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LegalHeading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = MemoryArchitectColors.accentGold)
}

@Composable
private fun LegalBody(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textSecondary)
}

@Composable
private fun PrivacyPolicyBody() {
    LegalHeading("Overview")
    LegalBody(
        "Memory Architect (\"the app\") is a single/multiplayer-competitive memory puzzle game. " +
            "This policy explains what information the app collects, why, and how you can delete it.",
    )
    LegalHeading("Account & sign-in")
    LegalBody(
        "The app requires signing in with your Google account before you can play. We receive your " +
            "Google display name and profile photo URL from this sign-in, shown on your Profile and on " +
            "public leaderboards alongside your gameplay stats. We never receive or store your Google " +
            "password, and we never ask for it.",
    )
    LegalHeading("Gameplay & progression data")
    LegalBody(
        "Your XP, coins, streaks, achievements, mission progress, owned cosmetics, and leaderboard " +
            "scores are stored in Firebase Firestore (a Google Cloud database), tied to your account. " +
            "Global/Daily/Weekly/Monthly leaderboards are public and show your display name, chosen " +
            "avatar, optional self-reported country, and competitive stats to other players.",
    )
    LegalHeading("Purchases")
    LegalBody(
        "In-app purchases (Remove Ads, cosmetic bundles) are processed entirely by Google Play " +
            "Billing. We never see or store your payment card details - only a purchase confirmation " +
            "token, used to grant what you bought and prevent the same purchase being redeemed twice.",
    )
    LegalHeading("Advertising")
    LegalBody(
        "The app shows ads via Google AdMob (banner, interstitial, and optional rewarded ads for " +
            "bonus in-game rewards) unless you've purchased Remove Ads. AdMob may use device/advertising " +
            "identifiers to serve and measure ads. Where required by law (e.g. the EEA, UK, and " +
            "Switzerland), you'll be asked for consent to personalized advertising the first time the " +
            "app is used, and can change that choice at any time from Settings.",
    )
    LegalHeading("Notifications")
    LegalBody(
        "With your permission, the app schedules local reminder notifications (e.g. a streak " +
            "about to lapse, or a new Daily Challenge available) directly on your device. These are " +
            "generated on-device from your own gameplay data - nothing is sent to a server to produce " +
            "them, and no separate notification/device token is collected. You can turn reminders off " +
            "at any time from Settings, or by revoking the notification permission in your device's " +
            "system settings.",
    )
    LegalHeading("Analytics & crash reporting")
    LegalBody(
        "We use Firebase Analytics (feature usage, to understand what's working) and Firebase " +
            "Crashlytics (crash/error reports, to fix bugs) - both Google services, both operating on " +
            "device/usage data, never on the contents of your Google account.",
    )
    LegalHeading("Optional data you choose to provide")
    LegalBody(
        "A custom profile photo (stored in Firebase Storage) and your country (shown on public " +
            "leaderboards) are both entirely optional and can be changed or cleared at any time from " +
            "Profile → Account.",
    )
    LegalHeading("Data retention & deletion")
    LegalBody(
        "Your data is kept for as long as your account exists. You can permanently delete your " +
            "account and every piece of data described above at any time, from within the app, at " +
            "Settings → Delete Account. This removes your Firestore data, your Firebase Auth account, " +
            "and all locally-cached data on your device. A small number of already-closed competitive " +
            "leaderboard periods (e.g. a past month you competed in) may retain an anonymized historical " +
            "entry, the same way a completed sports season's standings aren't rewritten after the fact.",
    )
    LegalHeading("Children's privacy")
    LegalBody(
        "This app is not directed at children under 13 and we do not knowingly collect personal " +
            "information from children under 13. Sign-in with a Google account is required to play.",
    )
    LegalHeading("Third parties this app relies on")
    LegalBody(
        "Google Firebase (Authentication, Firestore, Storage, Analytics, Crashlytics, Remote Config), " +
            "Google AdMob, and Google Play Billing. Each operates under Google's own privacy policy in " +
            "addition to this one.",
    )
    LegalHeading("Contact")
    LegalBody("Questions about this policy or your data: sumangoud.ekkurthi99@gmail.com")
}

@Composable
private fun TermsBody() {
    LegalHeading("Acceptance")
    LegalBody(
        "By downloading, installing, or playing Memory Architect (\"the app\"), you agree to these " +
            "Terms & Conditions and the accompanying Privacy Policy.",
    )
    LegalHeading("Accounts")
    LegalBody(
        "Playing requires signing in with a Google account. You're responsible for activity under " +
            "your account. You may delete your account and all associated data at any time from " +
            "Settings → Delete Account.",
    )
    LegalHeading("Virtual items & purchases")
    LegalBody(
        "Coins, cosmetics, tickets, and other in-app items have no real-world monetary value, cannot " +
            "be exchanged for cash, and exist solely for use within the app. Real-money purchases are " +
            "processed by Google Play Billing and are subject to Google Play's own refund policy. " +
            "Lucky Spin and Mystery Chest are randomized reward mechanics with no purchase required to " +
            "participate (spins/chests are earned through play, a daily allowance, or watching a " +
            "rewarded ad) - the exact odds of every outcome are shown in-app on the Lucky Spin screen.",
    )
    LegalHeading("Fair play")
    LegalBody(
        "Using modified clients, exploits, or automation to manipulate scores, leaderboards, or the " +
            "in-game economy is prohibited and may result in account or leaderboard action.",
    )
    LegalHeading("Advertising")
    LegalBody(
        "The app is supported by advertising via Google AdMob unless you've purchased Remove Ads. " +
            "See the Privacy Policy for what that involves.",
    )
    LegalHeading("Availability")
    LegalBody(
        "The app is provided \"as is,\" without warranty of any kind. Features that depend on an " +
            "internet connection or a signed-in account (leaderboards, cloud-synced progress) may be " +
            "unavailable while offline.",
    )
    LegalHeading("Changes")
    LegalBody(
        "These terms may be updated from time to time; continued use of the app after a change " +
            "constitutes acceptance of the updated terms.",
    )
    LegalHeading("Contact")
    LegalBody("Questions about these terms: sumangoud.ekkurthi99@gmail.com")
}
