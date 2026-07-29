package com.suman.memoryarchitect.domain.model

/**
 * One entry in the centralized, static Google Play Billing catalog - see
 * `domain.progression.PremiumShopCatalog`, the single list every real-money product in this app is
 * defined in (Remove Ads included, folded in as [BillingEntitlementKind.REMOVE_ADS] rather than
 * living in a separate manager/catalog). Replaces the old `PremiumProduct`/`PremiumProductSource`
 * split - [type] and [entitlement] are the two orthogonal axes that split now expresses (see both
 * enums' own docs).
 *
 * Deliberately does **not** carry price, ownership, or purchase-state fields - those are live Play
 * Billing state, resolved at runtime by `core.billing.BillingManager` into
 * `BillingProductUiState`/`PurchaseUiState`, not static catalog data. This file stays plain Kotlin,
 * no Android imports, matching every other `domain/model` file in this app. [titleRes]/[taglineRes]/
 * [descriptionRes] are real marketing copy (unlike [CosmeticId.toDisplayName]'s deliberately-
 * unlocalized flavor text) so they're string-resource ids, resolved via `stringResource()` at the UI
 * layer.
 */
data class BillingCatalogProduct(
    val billingProductId: String,
    val type: BillingProductType,
    val entitlement: BillingEntitlementKind,
    val titleRes: Int,
    val taglineRes: Int,
    val descriptionRes: Int? = null,
    /** Non-empty only for [BillingEntitlementKind.COSMETIC_COLLECTION] - see
     * `domain.progression.PremiumCatalog` for what each id actually is. */
    val grantedCosmeticIds: List<CosmeticId> = emptyList(),
    val isBestValue: Boolean = false,
)
