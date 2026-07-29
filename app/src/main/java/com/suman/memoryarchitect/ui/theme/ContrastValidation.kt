package com.suman.memoryarchitect.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * The real math behind this app's cosmetic legibility guarantee - computed on actual [Color]
 * values rather than eyeballed. [CosmeticContrastTest] validates [CosmeticVisualCatalog]'s
 * `BACKGROUND_THEME` entries with the WCAG relative-luminance/contrast-ratio functions below (a
 * gradient rendered directly behind fixed-color text - a standard "is this readable" question).
 * [CosmeticColorBlendTest] validates `RoomSkinVisualCatalog`/`ObjectMaterialVisualCatalog` with
 * [simulateColorBlend] instead - a genuinely different question ("can this tint ever darken or
 * wash out what's underneath it, regardless of which color is chosen"), because those two
 * categories composite via [androidx.compose.ui.graphics.BlendMode.Color] against *dynamic*
 * content (gameplay objects/room pixels), not a fixed color pair. Mixing the two up was exactly
 * the bug in this system's first version - see [simulateColorBlend]'s doc for the full story.
 *
 * This is what "automatic" cosmetic validation honestly means in a codebase with no server-side
 * config to hot-patch: a permanent, real CI gate, not a runtime color-mutating engine (this app's
 * cosmetic categories render in disjoint screen regions with zero shared pixels, so a fixed,
 * pre-validated palette is strictly better than an unpredictable live-adjusted one).
 */
object ContrastValidation {
    /** WCAG AA's "large text" threshold (3.0:1, vs. 4.5:1 for normal-size body text) - correct
     * here since virtually every piece of text this app renders directly on a themed [Color] (a
     * `BACKGROUND_THEME` gradient) is titleMedium/labelLarge scale or larger. */
    const val MIN_TEXT_CONTRAST: Double = 3.0

    /** How far a `ROOM_SKIN`/`OBJECT_MATERIAL` tint is allowed to shift a destination pixel's WCAG
     * relative luminance, in either direction, at that category's real equip-time blend strength.
     * [simulateColorBlend] preserves the destination's brightness *exactly* in the blend mode's own
     * internal (Rec. 601-style) luma measure by construction - this floor exists only because that
     * measure and WCAG's (different, linear-light, green-weighted) relative luminance aren't
     * numerically identical, so a small residual shift is expected, not because the blend can ever
     * fail outright the way `BlendMode.Overlay` could. */
    const val MAX_BRIGHTNESS_SHIFT: Double = 0.12

    /** WCAG relative luminance - per-channel sRGB gamma correction, then the standard 0.2126/
     * 0.7152/0.0722 weighted sum (human vision is far more sensitive to green than red or blue). */
    fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val cs = c.toDouble().coerceIn(0.0, 1.0)
            return if (cs <= 0.03928) cs / 12.92 else Math.pow((cs + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    /** WCAG contrast ratio between two colors, always >= 1.0 (identical colors) and up to 21.0
     * (pure black on pure white) - the standard `(L_lighter + 0.05) / (L_darker + 0.05)` formula. */
    fun contrastRatio(a: Color, b: Color): Double {
        val l1 = relativeLuminance(a) + 0.05
        val l2 = relativeLuminance(b) + 0.05
        return if (l1 > l2) l1 / l2 else l2 / l1
    }

    /**
     * Reproduces exactly what `objectMaterialTint`/`RoomSkinOverlay` compute for real via
     * `ColorFilter.tint(tint, BlendMode.Color)` / `drawRect(blendMode = BlendMode.Color)` - the CSS
     * Compositing & Blending spec's non-separable "Color" blend (`SetLum(tintColor, Luma(destination))`,
     * the platform's own exact implementation), then a standard alpha mix with the untouched
     * [destination] at [srcAlpha]. Used by [CosmeticColorBlendTest] to *prove*, not assume, that a
     * material/room tint can never meaningfully darken or brighten what it's drawn over - the whole
     * point of using this blend mode instead of the original `BlendMode.Overlay`. `Overlay`'s
     * neutral (no-change) point is raw value 0.5 on *each* R/G/B channel independently, which has
     * no relationship to a color's overall (WCAG-weighted) luminance - a tint color could look
     * "medium brightness" while one channel (usually blue, ~7% of WCAG luminance) still sat well
     * under 0.5, so `Overlay` quietly crushed every equipped material/room toward black regardless
     * of which of the 7 was chosen. `BlendMode.Color` doesn't have that failure mode by
     * construction: it explicitly takes the destination's own brightness and only transfers the
     * source's hue/saturation.
     */
    fun simulateColorBlend(tintColor: Color, destination: Color, srcAlpha: Float): Color {
        val blended = setLuma(tintColor, luma(destination))
        val a = srcAlpha.coerceIn(0f, 1f)
        return Color(
            red = destination.red * (1 - a) + blended.red * a,
            green = destination.green * (1 - a) + blended.green * a,
            blue = destination.blue * (1 - a) + blended.blue * a,
        )
    }

    /** CSS Compositing spec's `Lum()` helper for the non-separable blend modes (Hue/Saturation/
     * Color/Luminosity) - Rec. 601-style weights operating directly on gamma-encoded channel
     * values, deliberately different from [relativeLuminance] above (different spec, different
     * purpose: this is what the platform's `BlendMode.Color` implementation itself uses to decide
     * "how bright is this color," not a perceptual/accessibility measure). */
    private fun luma(color: Color): Float = 0.3f * color.red + 0.59f * color.green + 0.11f * color.blue

    /** CSS Compositing spec's `ClipColor()` helper - pulls an out-of-gamut color (produced by
     * `setLuma`'s additive shift) back into the valid 0..1 range per channel while preserving its
     * `Lum()`. */
    private fun clipColor(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val l = 0.3f * r + 0.59f * g + 0.11f * b
        val n = min(r, min(g, b))
        val x = max(r, max(g, b))
        var cr = r
        var cg = g
        var cb = b
        if (n < 0f && l != n) {
            cr = l + (cr - l) * l / (l - n)
            cg = l + (cg - l) * l / (l - n)
            cb = l + (cb - l) * l / (l - n)
        }
        if (x > 1f && x != l) {
            cr = l + (cr - l) * (1f - l) / (x - l)
            cg = l + (cg - l) * (1f - l) / (x - l)
            cb = l + (cb - l) * (1f - l) / (x - l)
        }
        return Triple(cr.coerceIn(0f, 1f), cg.coerceIn(0f, 1f), cb.coerceIn(0f, 1f))
    }

    /** CSS Compositing spec's `SetLum()` helper - the actual definition of `BlendMode.Color`:
     * shift [color] until its own [luma] equals [targetLuma] (the destination's), clipping back
     * into gamut afterward. This is what guarantees a `BlendMode.Color` composite can never be
     * darker or lighter than what it's drawn over, by construction, for any [color]. */
    private fun setLuma(color: Color, targetLuma: Float): Color {
        val d = targetLuma - luma(color)
        val (r, g, b) = clipColor(color.red + d, color.green + d, color.blue + d)
        return Color(r, g, b)
    }
}
