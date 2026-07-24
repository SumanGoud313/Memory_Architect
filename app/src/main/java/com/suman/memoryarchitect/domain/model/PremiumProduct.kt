package com.suman.memoryarchitect.domain.model

/** Distinguishes the one pre-existing real-money product (`remove_ads_lifetime`, owned entirely by
 * [com.suman.memoryarchitect.core.billing.BillingManager] - untouched by this feature) from the
 * new cosmetic-bundle products (owned by `core.billing.PremiumShopManager`) - see
 * [com.suman.memoryarchitect.domain.progression.PremiumShopCatalog]'s doc for why both are
 * represented in one list despite having two different owning managers. */
enum class PremiumProductSource { LEGACY_REMOVE_ADS, COSMETIC_BUNDLE }

/** One entry in the Premium Shop tab. [titleRes]/[taglineRes] are real marketing copy (unlike
 * [CosmeticId.toDisplayName]'s deliberately-unlocalized flavor text) so they're string-resource
 * ids, resolved via `stringResource()` at the UI layer - this file stays plain Kotlin, no Android
 * imports, matching every other `domain/model` file in this app.
 *
 * [grantedCosmeticIds] is empty for [PremiumProductSource.LEGACY_REMOVE_ADS] (it grants an
 * ads-removal entitlement, not cosmetics) and non-empty for every [PremiumProductSource.COSMETIC_BUNDLE]
 * - see [com.suman.memoryarchitect.domain.progression.PremiumCatalog] for what each id actually is. */
data class PremiumProduct(
    val billingProductId: String,
    val source: PremiumProductSource,
    val titleRes: Int,
    val taglineRes: Int,
    val grantedCosmeticIds: List<CosmeticId> = emptyList(),
    val isBestValue: Boolean = false,
)
