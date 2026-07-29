package com.suman.memoryarchitect.ui.theme

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real, permanent "Cosmetic Validation System" for [CosmeticCategory.BACKGROUND_THEME] -
 * computes actual WCAG contrast for every such cosmetic that currently exists against this app's
 * fixed text colors, and fails with the offending cosmetic id named in the assertion message if
 * any value regresses below [ContrastValidation.MIN_TEXT_CONTRAST]. Runs on every
 * `testDebugUnitTest`, so a future palette addition that's too light/dark is caught here, not by a
 * hoped-for visual spot-check.
 *
 * [CosmeticCategory.ROOM_SKIN]/[CosmeticCategory.OBJECT_MATERIAL] are validated separately, in
 * [CosmeticColorBlendTest] - a genuinely different mechanism (a `BlendMode.Color` composite
 * against *dynamic* gameplay pixels, not a fixed gradient behind fixed text), so it needs a
 * different proof, not the same WCAG-contrast check reused for a different purpose.
 *
 * Deliberately does NOT brute-force cross-category combinations (Background x Room x Material x
 * ...) - this app's cosmetic categories render in completely disjoint screen regions with zero
 * shared pixels (a NAME_COLOR never touches a ROOM_SKIN pixel; a BACKGROUND_THEME's gradient never
 * touches an OBJECT_MATERIAL's tint), so a combination's legibility is always exactly as good as
 * its worst *individual* member - which per-category validation already guarantees.
 */
class CosmeticContrastTest {

    @Test
    fun `every BACKGROUND_THEME gradient stop stays legible against both fixed text colors`() {
        val backgroundIds = AllCosmeticsCatalog.definitionsOfCategory(CosmeticCategory.BACKGROUND_THEME).map { it.id }
        assertTrue("Expected at least one BACKGROUND_THEME cosmetic to exist", backgroundIds.isNotEmpty())

        val failures = mutableListOf<String>()
        backgroundIds.forEach { id ->
            val spec = CosmeticVisualCatalog.get(id)
            spec.gradientColors.forEachIndexed { stopIndex, stop ->
                val primaryRatio = ContrastValidation.contrastRatio(stop, MemoryArchitectColors.textPrimary)
                if (primaryRatio < ContrastValidation.MIN_TEXT_CONTRAST) {
                    failures += "$id gradient stop $stopIndex ($stop) has only ${"%.2f".format(primaryRatio)}:1 contrast " +
                        "against textPrimary - needs >= ${ContrastValidation.MIN_TEXT_CONTRAST}:1"
                }
                val secondaryRatio = ContrastValidation.contrastRatio(stop, MemoryArchitectColors.textSecondary)
                if (secondaryRatio < ContrastValidation.MIN_TEXT_CONTRAST) {
                    failures += "$id gradient stop $stopIndex ($stop) has only ${"%.2f".format(secondaryRatio)}:1 contrast " +
                        "against textSecondary - needs >= ${ContrastValidation.MIN_TEXT_CONTRAST}:1"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
