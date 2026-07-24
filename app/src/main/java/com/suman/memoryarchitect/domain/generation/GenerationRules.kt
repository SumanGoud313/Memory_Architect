package com.suman.memoryarchitect.domain.generation

/**
 * Data-driven tuning knobs for procedural generation and difficulty scaling. Intentionally
 * a plain data class (not hardcoded logic) so a future Remote-Config-backed source can
 * supply live-tuned values without touching [DifficultyEngine] or [LevelGenerator].
 */
data class GenerationRules(
    val minObjectCount: Int = 4,
    val maxObjectCount: Int = 16,
    val baseObjectCount: Double = 5.0,
    val objectCountPerTier: Double = 1.5,
    val streakRampFactor: Double = 0.6,
    val distractorRatio: Float = 0.25f,
    val baseMemorizeDurationMs: Long = 6_000L,
    // Softened on purpose: difficulty should come from more objects/distractors/rotation, not a
    // shrinking memorize window — see DifficultyEngine.
    val memorizeDurationStepMs: Long = 150L,
    val minMemorizeDurationMs: Long = 3_500L,
    val rotationUnlockTier: Int = 1,
    val rotationStepDegrees: Int = 90,
    val orderUnlockTier: Int = 3,
    val baseTimeLimitMs: Long = 60_000L,
    val timeLimitStepMs: Long = 5_000L,
    val minTimeLimitMs: Long = 30_000L,
    val scenePool: List<String> = listOf(
        "kitchen", "bedroom", "coffee_shop", "gaming_room", "office", "library", "garden", "space_station",
    ),
    val objectPoolsByScene: Map<String, List<String>> = mapOf(
        "kitchen" to listOf(
            "kettle", "frying_pan", "cooking_pot", "toaster", "coffee_mug", "dinner_plate",
            "mixing_bowl", "cutting_board", "chef_knife", "whisk", "spatula", "colander",
            "teapot", "spice_jar", "apple", "banana", "oven_mitt", "blender",
        ),
        "bedroom" to listOf(
            "pillow", "table_lamp", "alarm_clock", "book", "picture_frame", "potted_plant",
            "mirror", "jewelry_box", "candle", "teddy_bear", "folded_blanket", "slippers",
            "coat_hanger", "perfume_bottle", "laptop", "eyeglasses", "tissue_box", "flower_vase",
        ),
        "coffee_shop" to listOf(
            "espresso_machine", "coffee_mug", "book", "laptop", "potted_plant", "latte_cup",
            "pastry_croissant", "coffee_bean_bag", "coffee_grinder", "chalkboard_menu",
            "bar_stool", "pendant_lamp", "milk_frother_pitcher", "tip_jar", "sugar_shaker",
            "napkin_holder",
        ),
        "gaming_room" to listOf(
            "potted_plant", "gaming_pc_tower", "monitor", "gaming_chair", "headset",
            "mechanical_keyboard", "gaming_mouse", "controller", "desk_fan", "led_strip",
            "poster", "energy_drink_can", "figurine_collectible", "trophy", "backpack",
        ),
        "office" to listOf(
            "laptop", "coffee_mug", "desk_lamp", "pen_cup", "stapler", "binder",
            "award_plaque", "book", "wall_clock", "calendar", "potted_plant",
            "office_chair", "waste_bin", "backpack",
        ),
        "library" to listOf(
            "book", "bookend", "globe", "quill_pen", "inkwell", "chess_set", "candle",
            "eyeglasses", "wax_seal_stamp", "coffee_mug", "magnifying_glass", "hourglass",
            "leather_journal", "potted_plant", "armchair", "telescope", "wall_map", "reading_lamp",
        ),
        "garden" to listOf(
            "flower_pot", "garden_trowel", "gardening_gloves", "coffee_mug", "book",
            "picnic_basket", "sunhat", "potted_plant", "watering_can", "garden_gnome",
            "bird_bath", "wheelbarrow",
        ),
        "space_station" to listOf(
            "control_panel", "communicator", "star_chart", "oxygen_tank", "food_pouch",
            "satellite_model", "space_helmet", "robot_companion", "space_boots",
            "hydroponic_pod", "coffee_mug", "book",
        ),
    ),
    /**
     * How many designed positions each room has for movable objects to land on (see
     * [com.suman.memoryarchitect.ui.illustration.RoomArt.slots] for where those positions
     * actually are — this side only needs the count). Set to exactly that room's object-pool
     * size so every object always has somewhere to appear and no slot ever sits unusable.
     */
    val roomSlotCountByScene: Map<String, Int> = mapOf(
        "kitchen" to 18,
        "bedroom" to 18,
        "coffee_shop" to 16,
        "gaming_room" to 15,
        "office" to 14,
        "library" to 18,
        "garden" to 12,
        "space_station" to 12,
    ),
    /**
     * Furniture-type of each designed slot, **index-aligned with that room's
     * [com.suman.memoryarchitect.ui.illustration.RoomArt.slots] order** — the same manual-sync
     * contract [roomSlotCountByScene] already relies on, just extended to categories. Lets
     * [LevelGenerator] place an object where it would actually belong (see [objectCategoryById])
     * instead of on any empty slot at random.
     */
    val roomSlotCategoriesByScene: Map<String, List<SlotCategory>> = mapOf(
        "bedroom" to listOf(
            // Shelf boards (0-5)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Nightstand top (6-7)
            SlotCategory.BED_SIDE, SlotCategory.BED_SIDE,
            // Windowsill (8-9)
            SlotCategory.WINDOWSILL, SlotCategory.WINDOWSILL,
            // Wardrobe top (10-11)
            SlotCategory.STORAGE_TOP, SlotCategory.STORAGE_TOP,
            // Bed corner / pillow area (12-13)
            SlotCategory.BED_SIDE, SlotCategory.BED_SIDE,
            // Floor rug spots (14-16)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
            // Wall hook (17)
            SlotCategory.WALL,
        ),
        "kitchen" to listOf(
            // Counter run (0-4)
            SlotCategory.COUNTER, SlotCategory.COUNTER, SlotCategory.COUNTER,
            SlotCategory.COUNTER, SlotCategory.COUNTER,
            // Open shelf (5-7)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Windowsill (8-9)
            SlotCategory.WINDOWSILL, SlotCategory.WINDOWSILL,
            // Kitchen table top (10-12)
            SlotCategory.TABLE, SlotCategory.TABLE, SlotCategory.TABLE,
            // Far counter end (13)
            SlotCategory.COUNTER,
            // Floor near the table (14-15)
            SlotCategory.FLOOR, SlotCategory.FLOOR,
            // Cabinet-top display spots (16-17)
            SlotCategory.STORAGE_TOP, SlotCategory.STORAGE_TOP,
        ),
        "coffee_shop" to listOf(
            // Counter run (0-4)
            SlotCategory.COUNTER, SlotCategory.COUNTER, SlotCategory.COUNTER,
            SlotCategory.COUNTER, SlotCategory.COUNTER,
            // Wall shelves (5-8)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Table top (9-10)
            SlotCategory.TABLE, SlotCategory.TABLE,
            // Windowsill (11-12)
            SlotCategory.WINDOWSILL, SlotCategory.WINDOWSILL,
            // Floor spots (13-15)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
        ),
        "gaming_room" to listOf(
            // Desk surface (0-4)
            SlotCategory.DESK, SlotCategory.DESK, SlotCategory.DESK,
            SlotCategory.DESK, SlotCategory.DESK,
            // Wall shelf (5-7)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Wall / poster area (8-9)
            SlotCategory.WALL, SlotCategory.WALL,
            // Floor space (10-14)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
            SlotCategory.FLOOR, SlotCategory.FLOOR,
        ),
        "office" to listOf(
            // Desk surface — laptop, coffee mug (0-1), lamp/pen cup/stapler (2-4)
            SlotCategory.TABLE, SlotCategory.TABLE,
            SlotCategory.DESK, SlotCategory.DESK, SlotCategory.DESK,
            // Shelf boards (5-7)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Wall — clock, calendar (8-9)
            SlotCategory.WALL, SlotCategory.WALL,
            // Windowsill (10)
            SlotCategory.WINDOWSILL,
            // Floor — chair, waste bin, backpack (11-13)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
        ),
        "library" to listOf(
            // Built-in bookshelf, 3 rows x 3 columns (0-8)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Reading table (9-12)
            SlotCategory.TABLE, SlotCategory.TABLE, SlotCategory.TABLE, SlotCategory.TABLE,
            // Windowsill (13)
            SlotCategory.WINDOWSILL,
            // Floor — armchair, telescope (14-15)
            SlotCategory.FLOOR, SlotCategory.FLOOR,
            // Wall — map (16)
            SlotCategory.WALL,
            // Reading desk — lamp (17)
            SlotCategory.DESK,
        ),
        "garden" to listOf(
            // Potting bench (0-2)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Picnic table (3-5)
            SlotCategory.TABLE, SlotCategory.TABLE, SlotCategory.TABLE,
            // Fence hook — sunhat (6)
            SlotCategory.WALL,
            // Grass (7-11)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
        ),
        "space_station" to listOf(
            // Control console (0-2)
            SlotCategory.DESK, SlotCategory.DESK, SlotCategory.DESK,
            // Storage rack (3-5)
            SlotCategory.SHELF, SlotCategory.SHELF, SlotCategory.SHELF,
            // Floor (6-8)
            SlotCategory.FLOOR, SlotCategory.FLOOR, SlotCategory.FLOOR,
            // Viewport ledge (9)
            SlotCategory.WINDOWSILL,
            // Crew table (10-11)
            SlotCategory.TABLE, SlotCategory.TABLE,
        ),
    ),
    /**
     * Each object's primary furniture-type preference — the interior-design logic behind level
     * generation (books go on shelves, a mug goes on a table, a plant sits by the window). An id
     * whose preferred category has no slots in a given room (e.g. `laptop`→TABLE in the
     * bedroom, which has no table) simply falls back to any remaining slot at generation time —
     * not every object needs a perfect match in every room, this just biases toward one.
     */
    val objectCategoryById: Map<String, SlotCategory> = mapOf(
        // Shared
        "book" to SlotCategory.SHELF,
        "coffee_mug" to SlotCategory.TABLE,
        "laptop" to SlotCategory.TABLE,
        "potted_plant" to SlotCategory.WINDOWSILL,
        // Bedroom
        "pillow" to SlotCategory.BED_SIDE,
        "table_lamp" to SlotCategory.BED_SIDE,
        "alarm_clock" to SlotCategory.BED_SIDE,
        "picture_frame" to SlotCategory.STORAGE_TOP,
        "mirror" to SlotCategory.STORAGE_TOP,
        "jewelry_box" to SlotCategory.SHELF,
        "candle" to SlotCategory.SHELF,
        "teddy_bear" to SlotCategory.FLOOR,
        "folded_blanket" to SlotCategory.FLOOR,
        "slippers" to SlotCategory.FLOOR,
        "coat_hanger" to SlotCategory.WALL,
        "perfume_bottle" to SlotCategory.SHELF,
        "eyeglasses" to SlotCategory.SHELF,
        "tissue_box" to SlotCategory.BED_SIDE,
        "flower_vase" to SlotCategory.WINDOWSILL,
        // Kitchen
        "kettle" to SlotCategory.COUNTER,
        "frying_pan" to SlotCategory.COUNTER,
        "cooking_pot" to SlotCategory.FLOOR,
        "toaster" to SlotCategory.COUNTER,
        "dinner_plate" to SlotCategory.TABLE,
        "mixing_bowl" to SlotCategory.FLOOR,
        "cutting_board" to SlotCategory.COUNTER,
        "chef_knife" to SlotCategory.COUNTER,
        "whisk" to SlotCategory.SHELF,
        "spatula" to SlotCategory.SHELF,
        "colander" to SlotCategory.SHELF,
        "teapot" to SlotCategory.TABLE,
        "spice_jar" to SlotCategory.STORAGE_TOP,
        "apple" to SlotCategory.WINDOWSILL,
        "banana" to SlotCategory.WINDOWSILL,
        "oven_mitt" to SlotCategory.STORAGE_TOP,
        "blender" to SlotCategory.COUNTER,
        // Coffee shop
        "espresso_machine" to SlotCategory.COUNTER,
        "latte_cup" to SlotCategory.COUNTER,
        "pastry_croissant" to SlotCategory.COUNTER,
        "coffee_bean_bag" to SlotCategory.SHELF,
        "coffee_grinder" to SlotCategory.COUNTER,
        "chalkboard_menu" to SlotCategory.WALL,
        "bar_stool" to SlotCategory.FLOOR,
        "pendant_lamp" to SlotCategory.WALL,
        "milk_frother_pitcher" to SlotCategory.COUNTER,
        "tip_jar" to SlotCategory.COUNTER,
        "sugar_shaker" to SlotCategory.TABLE,
        "napkin_holder" to SlotCategory.TABLE,
        // Gaming room
        "gaming_pc_tower" to SlotCategory.FLOOR,
        "monitor" to SlotCategory.DESK,
        "gaming_chair" to SlotCategory.FLOOR,
        "headset" to SlotCategory.DESK,
        "mechanical_keyboard" to SlotCategory.DESK,
        "gaming_mouse" to SlotCategory.DESK,
        "controller" to SlotCategory.DESK,
        "desk_fan" to SlotCategory.DESK,
        "led_strip" to SlotCategory.DESK,
        "poster" to SlotCategory.WALL,
        "energy_drink_can" to SlotCategory.DESK,
        "figurine_collectible" to SlotCategory.SHELF,
        "trophy" to SlotCategory.SHELF,
        "backpack" to SlotCategory.FLOOR,
        // Office
        "desk_lamp" to SlotCategory.DESK,
        "pen_cup" to SlotCategory.DESK,
        "stapler" to SlotCategory.DESK,
        "binder" to SlotCategory.SHELF,
        "award_plaque" to SlotCategory.SHELF,
        "wall_clock" to SlotCategory.WALL,
        "calendar" to SlotCategory.WALL,
        "office_chair" to SlotCategory.FLOOR,
        "waste_bin" to SlotCategory.FLOOR,
        // Library
        "bookend" to SlotCategory.SHELF,
        "globe" to SlotCategory.SHELF,
        "quill_pen" to SlotCategory.SHELF,
        "inkwell" to SlotCategory.SHELF,
        "chess_set" to SlotCategory.SHELF,
        "wax_seal_stamp" to SlotCategory.SHELF,
        "magnifying_glass" to SlotCategory.TABLE,
        "hourglass" to SlotCategory.TABLE,
        "leather_journal" to SlotCategory.TABLE,
        "armchair" to SlotCategory.FLOOR,
        "telescope" to SlotCategory.FLOOR,
        "wall_map" to SlotCategory.WALL,
        "reading_lamp" to SlotCategory.DESK,
        // Garden
        "watering_can" to SlotCategory.FLOOR,
        "garden_gnome" to SlotCategory.FLOOR,
        "flower_pot" to SlotCategory.SHELF,
        "garden_trowel" to SlotCategory.SHELF,
        "bird_bath" to SlotCategory.FLOOR,
        "wheelbarrow" to SlotCategory.FLOOR,
        "gardening_gloves" to SlotCategory.SHELF,
        "sunhat" to SlotCategory.WALL,
        "picnic_basket" to SlotCategory.TABLE,
        // Space Station
        "control_panel" to SlotCategory.DESK,
        "communicator" to SlotCategory.DESK,
        "star_chart" to SlotCategory.DESK,
        "oxygen_tank" to SlotCategory.SHELF,
        "food_pouch" to SlotCategory.SHELF,
        "satellite_model" to SlotCategory.SHELF,
        "space_helmet" to SlotCategory.FLOOR,
        "robot_companion" to SlotCategory.FLOOR,
        "space_boots" to SlotCategory.FLOOR,
        "hydroponic_pod" to SlotCategory.WINDOWSILL,
    ),
    /**
     * A rough silhouette tag per object (see [ShapeFamily]) — [LevelGenerator] uses this to bias
     * distractor selection toward objects that could be mistaken for a target at a glance,
     * instead of drawing distractors uniformly at random from whatever's left in the room's pool.
     */
    val objectShapeFamilyById: Map<String, ShapeFamily> = mapOf(
        // Shared
        "book" to ShapeFamily.FLAT_RECT,
        "coffee_mug" to ShapeFamily.VESSEL,
        "laptop" to ShapeFamily.DEVICE,
        "potted_plant" to ShapeFamily.ORNAMENT,
        // Kitchen
        "kettle" to ShapeFamily.VESSEL,
        "frying_pan" to ShapeFamily.TOOL,
        "cooking_pot" to ShapeFamily.VESSEL,
        "toaster" to ShapeFamily.DEVICE,
        "dinner_plate" to ShapeFamily.FLAT_RECT,
        "mixing_bowl" to ShapeFamily.VESSEL,
        "cutting_board" to ShapeFamily.FLAT_RECT,
        "chef_knife" to ShapeFamily.TOOL,
        "whisk" to ShapeFamily.TOOL,
        "spatula" to ShapeFamily.TOOL,
        "colander" to ShapeFamily.VESSEL,
        "teapot" to ShapeFamily.VESSEL,
        "spice_jar" to ShapeFamily.VESSEL,
        "apple" to ShapeFamily.FOOD,
        "banana" to ShapeFamily.FOOD,
        "oven_mitt" to ShapeFamily.SOFT,
        "blender" to ShapeFamily.DEVICE,
        // Bedroom
        "pillow" to ShapeFamily.SOFT,
        "table_lamp" to ShapeFamily.TALL_NARROW,
        "alarm_clock" to ShapeFamily.ROUND_SMALL,
        "picture_frame" to ShapeFamily.FLAT_RECT,
        "mirror" to ShapeFamily.FLAT_RECT,
        "jewelry_box" to ShapeFamily.ORNAMENT,
        "candle" to ShapeFamily.TALL_NARROW,
        "teddy_bear" to ShapeFamily.SOFT,
        "folded_blanket" to ShapeFamily.SOFT,
        "slippers" to ShapeFamily.SOFT,
        "coat_hanger" to ShapeFamily.TOOL,
        "perfume_bottle" to ShapeFamily.VESSEL,
        "eyeglasses" to ShapeFamily.ROUND_SMALL,
        "tissue_box" to ShapeFamily.FLAT_RECT,
        "flower_vase" to ShapeFamily.VESSEL,
        // Coffee shop
        "espresso_machine" to ShapeFamily.DEVICE,
        "latte_cup" to ShapeFamily.VESSEL,
        "pastry_croissant" to ShapeFamily.FOOD,
        "coffee_bean_bag" to ShapeFamily.SOFT,
        "coffee_grinder" to ShapeFamily.DEVICE,
        "chalkboard_menu" to ShapeFamily.FLAT_RECT,
        "bar_stool" to ShapeFamily.FURNITURE_SMALL,
        "pendant_lamp" to ShapeFamily.TALL_NARROW,
        "milk_frother_pitcher" to ShapeFamily.VESSEL,
        "tip_jar" to ShapeFamily.VESSEL,
        "sugar_shaker" to ShapeFamily.VESSEL,
        "napkin_holder" to ShapeFamily.FLAT_RECT,
        // Gaming room
        "gaming_pc_tower" to ShapeFamily.DEVICE,
        "monitor" to ShapeFamily.DEVICE,
        "gaming_chair" to ShapeFamily.FURNITURE_SMALL,
        "headset" to ShapeFamily.DEVICE,
        "mechanical_keyboard" to ShapeFamily.DEVICE,
        "gaming_mouse" to ShapeFamily.DEVICE,
        "controller" to ShapeFamily.DEVICE,
        "desk_fan" to ShapeFamily.DEVICE,
        "led_strip" to ShapeFamily.DEVICE,
        "poster" to ShapeFamily.FLAT_RECT,
        "energy_drink_can" to ShapeFamily.VESSEL,
        "figurine_collectible" to ShapeFamily.ORNAMENT,
        "trophy" to ShapeFamily.ORNAMENT,
        "backpack" to ShapeFamily.SOFT,
        // Office
        "desk_lamp" to ShapeFamily.TALL_NARROW,
        "pen_cup" to ShapeFamily.VESSEL,
        "stapler" to ShapeFamily.TOOL,
        "binder" to ShapeFamily.FLAT_RECT,
        "award_plaque" to ShapeFamily.FLAT_RECT,
        "wall_clock" to ShapeFamily.ROUND_SMALL,
        "calendar" to ShapeFamily.FLAT_RECT,
        "office_chair" to ShapeFamily.FURNITURE_SMALL,
        "waste_bin" to ShapeFamily.VESSEL,
        // Library
        "bookend" to ShapeFamily.ORNAMENT,
        "globe" to ShapeFamily.ROUND_SMALL,
        "quill_pen" to ShapeFamily.TOOL,
        "inkwell" to ShapeFamily.VESSEL,
        "chess_set" to ShapeFamily.FURNITURE_SMALL,
        "wax_seal_stamp" to ShapeFamily.TOOL,
        "magnifying_glass" to ShapeFamily.TOOL,
        "hourglass" to ShapeFamily.TALL_NARROW,
        "leather_journal" to ShapeFamily.FLAT_RECT,
        "armchair" to ShapeFamily.FURNITURE_SMALL,
        "telescope" to ShapeFamily.TALL_NARROW,
        "wall_map" to ShapeFamily.FLAT_RECT,
        "reading_lamp" to ShapeFamily.TALL_NARROW,
        // Garden
        "flower_pot" to ShapeFamily.ORNAMENT,
        "garden_trowel" to ShapeFamily.TOOL,
        "gardening_gloves" to ShapeFamily.SOFT,
        "picnic_basket" to ShapeFamily.VESSEL,
        "sunhat" to ShapeFamily.SOFT,
        "watering_can" to ShapeFamily.VESSEL,
        "garden_gnome" to ShapeFamily.ORNAMENT,
        "bird_bath" to ShapeFamily.FURNITURE_SMALL,
        "wheelbarrow" to ShapeFamily.FURNITURE_SMALL,
        // Space Station
        "control_panel" to ShapeFamily.DEVICE,
        "communicator" to ShapeFamily.DEVICE,
        "star_chart" to ShapeFamily.FLAT_RECT,
        "oxygen_tank" to ShapeFamily.VESSEL,
        "food_pouch" to ShapeFamily.SOFT,
        "satellite_model" to ShapeFamily.DEVICE,
        "space_helmet" to ShapeFamily.ROUND_SMALL,
        "robot_companion" to ShapeFamily.DEVICE,
        "space_boots" to ShapeFamily.SOFT,
        "hydroponic_pod" to ShapeFamily.ORNAMENT,
    ),
) {
    companion object {
        val Default = GenerationRules()
    }
}