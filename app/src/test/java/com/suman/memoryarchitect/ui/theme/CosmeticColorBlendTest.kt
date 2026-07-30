package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real, permanent validation for `ROOM_SKIN`/`OBJECT_MATERIAL` - proves, by computing the
 * actual `BlendMode.Color` composite math (see [ContrastValidation.simulateColorBlend]), that
 * every current cosmetic in these two categories can never meaningfully darken or wash out the
 * gameplay it's drawn over, for a realistic spread of destination pixel brightnesses (dark shadow
 * tones through bright highlight tones, spanning both neutral and warm/cool hues - real objects
 * are never one flat color).
 *
 * This test exists because the *first* version of this validation system checked the wrong thing:
 * it validated each tint color's own WCAG luminance against a hand-picked "safe range," which
 * silently assumed the compositing technique was `BlendMode.Overlay`. `Overlay`'s neutral point is
 * raw value 0.5 on each R/G/B channel independently - unrelated to a color's overall luminance -
 * so a tint that looked "medium brightness" by that check could still crush gameplay toward black.
 * That bug (every equipped `OBJECT_MATERIAL` visibly dimming the Memorize/Reconstruct scene) is
 * what prompted both the `BlendMode.Overlay` -> `BlendMode.Color` fix in `GameplayScenePanel.kt`/
 * `RoomSkinOverlay.kt` and this replacement test, which validates the actual property that matters
 * ("does the destination's own brightness survive") instead of a proxy that turned out not to
 * predict it.
 */
class CosmeticColorBlendTest {

    /** Dark shadow tone, dark-mid, mid, mid-bright, bright highlight tone - neutral greys spanning
     * the range real gameplay objects render across (see `ArtPrimitives.kt`'s contactShadow/
     * matteNoise/rimLight/highlight layering) - plus a warm and a cool tone, since real objects are
     * never perfectly neutral. */
    private val representativeDestinations = listOf(
        Color(0xFF2A2A2A), // dark shadow
        Color(0xFF5C5C5C), // dark-mid
        Color(0xFF8F8F8F), // mid
        Color(0xFFC2C2C2), // mid-bright
        Color(0xFFEDEDED), // bright highlight
        Color(0xFF7A5A3C), // warm wood-ish mid tone
        Color(0xFF3C5A7A), // cool slate-ish mid tone
    )

    @Test
    fun `every ROOM_SKIN wallTint and floorTint preserves destination brightness across realistic room tones`() {
        val roomSkinIds = AllCosmeticsCatalog.definitionsOfCategory(CosmeticCategory.ROOM_SKIN).map { it.id }
        assertTrue("Expected at least one ROOM_SKIN cosmetic to exist", roomSkinIds.isNotEmpty())

        val failures = mutableListOf<String>()
        val washAlpha = 0.16f // RoomSkinOverlay's fixed wash alpha - keep in sync if that ever changes
        roomSkinIds.forEach { id ->
            val spec = RoomSkinVisualCatalog.get(id)
            listOf("wallTint" to spec.wallTint, "floorTint" to spec.floorTint).forEach { (fieldName, tint) ->
                representativeDestinations.forEach { destination ->
                    val before = ContrastValidation.relativeLuminance(destination)
                    val after = ContrastValidation.relativeLuminance(ContrastValidation.simulateColorBlend(tint, destination, washAlpha))
                    val shift = Math.abs(after - before)
                    if (shift > ContrastValidation.MAX_BRIGHTNESS_SHIFT) {
                        failures += "$id.$fieldName ($tint) over destination $destination: luminance shifted from " +
                            "${"%.3f".format(before)} to ${"%.3f".format(after)} (Δ${"%.3f".format(shift)}), exceeds the " +
                            "${ContrastValidation.MAX_BRIGHTNESS_SHIFT} floor - room would visibly dim or wash out"
                    }
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    // The OBJECT_MATERIAL category (and its ObjectMaterialVisualCatalog specs) was removed from
    // the shop entirely after a device-specific gameplay rendering bug - see
    // GameplayScenePanel.kt's distractorDesaturation doc, and ShopCatalogTest.kt's
    // nonMaterialIds for why the underlying CosmeticId/CosmeticCategory enum constants themselves
    // stay defined regardless. Nothing left in that category to validate here anymore.
}
