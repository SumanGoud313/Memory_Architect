package com.suman.memoryarchitect.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId

/** The one `CompositionLocal` in this codebase (first one introduced) - the whole equipped-map
 * changes together as a single atomic value on every equip/unequip, and every consumer reads the
 * whole map (never a sub-field independently), so `staticCompositionLocalOf`'s coarser-but-cheaper
 * invalidation is the right fit (its documented use case exactly). Provided once, at the app root,
 * by `ui/navigation/MemoryArchitectNavHost.kt` via `core/cosmetics/RootCosmeticsViewModel.kt` -
 * every screen/dialog reachable through the nav graph inherits it automatically, including any
 * future button, with zero per-screen wiring. Defaults to an empty map so anything composed
 * outside the provider (e.g. a `@Preview`) still renders correctly - just with nothing equipped. */
val LocalEquippedCosmetics = staticCompositionLocalOf<Map<CosmeticCategory, CosmeticId>> { emptyMap() }
