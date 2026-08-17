package com.suman.memoryarchitect.core.billing

import com.suman.memoryarchitect.domain.model.RemoteConfig

/** Master switch for every real-money purchase surface this app has - Mode Select's Remove Ads
 * button and the Shop's 💎 Premium tab alike - same "flip it in the Remote Config console, no
 * release needed" pattern [com.suman.memoryarchitect.core.ads.emergencyAdsDisabled] already uses
 * for ads. Defaults to `true` (shown) whenever the key is missing/unparseable/not yet fetched, the
 * same "never hide a real surface by accident" convention every other `RemoteConfig` accessor in
 * this app already follows for its own default.
 *
 * Deliberately never touches [BillingManager]/[SharedBillingClient] themselves - this only decides
 * whether the *entry points* (button, tab) render. An install that already owns `remove_ads_lifetime`
 * or a Premium bundle keeps that entitlement regardless of this flag; turning it off only stops
 * *new* purchases from being started, it never revokes one already granted. */
fun RemoteConfig.premiumStoreEnabled(): Boolean = values["premium_store_enabled"]?.toBooleanStrictOrNull() ?: true
