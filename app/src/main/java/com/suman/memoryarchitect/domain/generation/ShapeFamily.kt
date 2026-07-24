package com.suman.memoryarchitect.domain.generation

/**
 * A rough silhouette bucket for an object — not a precise shape classifier, just enough to let
 * [LevelGenerator] prefer distractors that are easy to mistake for a target at a glance (a
 * coffee mug distractor next to a teapot target, not a random gaming controller). See
 * [GenerationRules.objectShapeFamilyById].
 */
enum class ShapeFamily {
    /** Round hollow containers — mugs, pots, jars, bottles, bowls. */
    VESSEL,

    /** Flat rectangles — books, frames, boards, panels, posters. */
    FLAT_RECT,

    /** Tall and narrow — lamps, candles, pens, telescopes. */
    TALL_NARROW,

    /** Small and round or compact — clocks, glasses, trinkets. */
    ROUND_SMALL,

    /** Electronics and gadgets. */
    DEVICE,

    /** Soft goods — pillows, plush, fabric, bags. */
    SOFT,

    /** Hand tools and utensils. */
    TOOL,

    /** Small food items. */
    FOOD,

    /** Chair/stool/cart-scale furniture rendered as a tray object. */
    FURNITURE_SMALL,

    /** Purely decorative trinkets and figures. */
    ORNAMENT,
}
