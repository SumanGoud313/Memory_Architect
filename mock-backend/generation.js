'use strict';

// Mirrors GenerationRules.Default from the Android client (domain/generation/GenerationRules.kt).
const RULES = {
  minObjectCount: 4,
  maxObjectCount: 16,
  baseObjectCount: 5.0,
  objectCountPerTier: 1.5,
  streakRampFactor: 0.6,
  distractorRatio: 0.25,
  baseMemorizeDurationMs: 6000,
  // Softened on purpose: difficulty should come from more objects/distractors/rotation, not a
  // shrinking memorize window.
  memorizeDurationStepMs: 150,
  minMemorizeDurationMs: 3500,
  zenMemorizeDurationMs: 10000,
  rotationUnlockTier: 1,
  rotationStepDegrees: 90,
  baseTimeLimitMs: 60000,
  timeLimitStepMs: 5000,
  timeAttackLimitMs: 30000,
  scenePool: ['kitchen', 'bedroom', 'coffee_shop', 'gaming_room', 'office', 'library', 'garden', 'space_station'],
  objectPoolsByScene: {
    kitchen: [
      'kettle', 'frying_pan', 'cooking_pot', 'toaster', 'coffee_mug', 'dinner_plate',
      'mixing_bowl', 'cutting_board', 'chef_knife', 'whisk', 'spatula', 'colander',
      'teapot', 'spice_jar', 'apple', 'banana', 'oven_mitt', 'blender',
    ],
    bedroom: [
      'pillow', 'table_lamp', 'alarm_clock', 'book', 'picture_frame', 'potted_plant',
      'mirror', 'jewelry_box', 'candle', 'teddy_bear', 'folded_blanket', 'slippers',
      'coat_hanger', 'perfume_bottle', 'laptop', 'eyeglasses', 'tissue_box', 'flower_vase',
    ],
    coffee_shop: [
      'espresso_machine', 'coffee_mug', 'book', 'laptop', 'potted_plant', 'latte_cup',
      'pastry_croissant', 'coffee_bean_bag', 'coffee_grinder', 'chalkboard_menu',
      'bar_stool', 'pendant_lamp', 'milk_frother_pitcher', 'tip_jar', 'sugar_shaker',
      'napkin_holder',
    ],
    gaming_room: [
      'potted_plant', 'gaming_pc_tower', 'monitor', 'gaming_chair', 'headset',
      'mechanical_keyboard', 'gaming_mouse', 'controller', 'desk_fan', 'led_strip',
      'poster', 'energy_drink_can', 'figurine_collectible', 'trophy', 'backpack',
    ],
    office: [
      'laptop', 'coffee_mug', 'desk_lamp', 'pen_cup', 'stapler', 'binder',
      'award_plaque', 'book', 'wall_clock', 'calendar', 'potted_plant',
      'office_chair', 'waste_bin', 'backpack',
    ],
    library: [
      'book', 'bookend', 'globe', 'quill_pen', 'inkwell', 'chess_set', 'candle',
      'eyeglasses', 'wax_seal_stamp', 'coffee_mug', 'magnifying_glass', 'hourglass',
      'leather_journal', 'potted_plant', 'armchair', 'telescope', 'wall_map', 'reading_lamp',
    ],
    garden: [
      'flower_pot', 'garden_trowel', 'gardening_gloves', 'coffee_mug', 'book',
      'picnic_basket', 'sunhat', 'potted_plant', 'watering_can', 'garden_gnome',
      'bird_bath', 'wheelbarrow',
    ],
    space_station: [
      'control_panel', 'communicator', 'star_chart', 'oxygen_tank', 'food_pouch',
      'satellite_model', 'space_helmet', 'robot_companion', 'space_boots',
      'hydroponic_pod', 'coffee_mug', 'book',
    ],
  },
  // Mirrors GenerationRules.roomSlotCountByScene -- must equal each room's object-pool size.
  roomSlotCountByScene: {
    kitchen: 18,
    bedroom: 18,
    coffee_shop: 16,
    gaming_room: 15,
    office: 14,
    library: 18,
    garden: 12,
    space_station: 12,
  },
  // Mirrors GenerationRules.roomSlotCategoriesByScene -- index-aligned with each room's
  // RoomArt.slots order on the client. Keep both in sync.
  roomSlotCategoriesByScene: {
    bedroom: [
      'SHELF', 'SHELF', 'SHELF', 'SHELF', 'SHELF', 'SHELF',
      'BED_SIDE', 'BED_SIDE',
      'WINDOWSILL', 'WINDOWSILL',
      'STORAGE_TOP', 'STORAGE_TOP',
      'BED_SIDE', 'BED_SIDE',
      'FLOOR', 'FLOOR', 'FLOOR',
      'WALL',
    ],
    kitchen: [
      'COUNTER', 'COUNTER', 'COUNTER', 'COUNTER', 'COUNTER',
      'SHELF', 'SHELF', 'SHELF',
      'WINDOWSILL', 'WINDOWSILL',
      'TABLE', 'TABLE', 'TABLE',
      'COUNTER',
      'FLOOR', 'FLOOR',
      'STORAGE_TOP', 'STORAGE_TOP',
    ],
    coffee_shop: [
      'COUNTER', 'COUNTER', 'COUNTER', 'COUNTER', 'COUNTER',
      'SHELF', 'SHELF', 'SHELF', 'SHELF',
      'TABLE', 'TABLE',
      'WINDOWSILL', 'WINDOWSILL',
      'FLOOR', 'FLOOR', 'FLOOR',
    ],
    gaming_room: [
      'DESK', 'DESK', 'DESK', 'DESK', 'DESK',
      'SHELF', 'SHELF', 'SHELF',
      'WALL', 'WALL',
      'FLOOR', 'FLOOR', 'FLOOR', 'FLOOR', 'FLOOR',
    ],
    office: [
      // Desk surface -- laptop, coffee mug (0-1), lamp/pen cup/stapler (2-4)
      'TABLE', 'TABLE',
      'DESK', 'DESK', 'DESK',
      // Shelf boards (5-7)
      'SHELF', 'SHELF', 'SHELF',
      // Wall -- clock, calendar (8-9)
      'WALL', 'WALL',
      // Windowsill (10)
      'WINDOWSILL',
      // Floor -- chair, waste bin, backpack (11-13)
      'FLOOR', 'FLOOR', 'FLOOR',
    ],
    library: [
      // Built-in bookshelf, 3 rows x 3 columns (0-8)
      'SHELF', 'SHELF', 'SHELF',
      'SHELF', 'SHELF', 'SHELF',
      'SHELF', 'SHELF', 'SHELF',
      // Reading table (9-12)
      'TABLE', 'TABLE', 'TABLE', 'TABLE',
      // Windowsill (13)
      'WINDOWSILL',
      // Floor -- armchair, telescope (14-15)
      'FLOOR', 'FLOOR',
      // Wall -- map (16)
      'WALL',
      // Reading desk -- lamp (17)
      'DESK',
    ],
    garden: [
      // Potting bench (0-2)
      'SHELF', 'SHELF', 'SHELF',
      // Picnic table (3-5)
      'TABLE', 'TABLE', 'TABLE',
      // Fence hook -- sunhat (6)
      'WALL',
      // Grass (7-11)
      'FLOOR', 'FLOOR', 'FLOOR', 'FLOOR', 'FLOOR',
    ],
    space_station: [
      // Control console (0-2)
      'DESK', 'DESK', 'DESK',
      // Storage rack (3-5)
      'SHELF', 'SHELF', 'SHELF',
      // Floor (6-8)
      'FLOOR', 'FLOOR', 'FLOOR',
      // Viewport ledge (9)
      'WINDOWSILL',
      // Crew table (10-11)
      'TABLE', 'TABLE',
    ],
  },
  // Mirrors GenerationRules.objectCategoryById -- each object's primary furniture-type
  // preference (interior-design logic behind placement). An id whose category has no slots in
  // a given room falls back to any remaining slot at generation time -- see generateLevel.
  objectCategoryById: {
    book: 'SHELF', coffee_mug: 'TABLE', laptop: 'TABLE', potted_plant: 'WINDOWSILL',
    pillow: 'BED_SIDE', table_lamp: 'BED_SIDE', alarm_clock: 'BED_SIDE',
    picture_frame: 'STORAGE_TOP', mirror: 'STORAGE_TOP', jewelry_box: 'SHELF',
    candle: 'SHELF', teddy_bear: 'FLOOR', folded_blanket: 'FLOOR', slippers: 'FLOOR',
    coat_hanger: 'WALL', perfume_bottle: 'SHELF', eyeglasses: 'SHELF', tissue_box: 'BED_SIDE',
    flower_vase: 'WINDOWSILL',
    kettle: 'COUNTER', frying_pan: 'COUNTER', cooking_pot: 'FLOOR', toaster: 'COUNTER',
    dinner_plate: 'TABLE', mixing_bowl: 'FLOOR', cutting_board: 'COUNTER', chef_knife: 'COUNTER',
    whisk: 'SHELF', spatula: 'SHELF', colander: 'SHELF', teapot: 'TABLE',
    spice_jar: 'STORAGE_TOP', apple: 'WINDOWSILL', banana: 'WINDOWSILL', oven_mitt: 'STORAGE_TOP',
    blender: 'COUNTER',
    espresso_machine: 'COUNTER', latte_cup: 'COUNTER', pastry_croissant: 'COUNTER',
    coffee_bean_bag: 'SHELF', coffee_grinder: 'COUNTER', chalkboard_menu: 'WALL',
    bar_stool: 'FLOOR', pendant_lamp: 'WALL', milk_frother_pitcher: 'COUNTER',
    tip_jar: 'COUNTER', sugar_shaker: 'TABLE', napkin_holder: 'TABLE',
    gaming_pc_tower: 'FLOOR', monitor: 'DESK', gaming_chair: 'FLOOR', headset: 'DESK',
    mechanical_keyboard: 'DESK', gaming_mouse: 'DESK', controller: 'DESK', desk_fan: 'DESK',
    led_strip: 'DESK', poster: 'WALL', energy_drink_can: 'DESK', figurine_collectible: 'SHELF',
    trophy: 'SHELF', backpack: 'FLOOR',
    // Office
    desk_lamp: 'DESK', pen_cup: 'DESK', stapler: 'DESK', binder: 'SHELF',
    award_plaque: 'SHELF', wall_clock: 'WALL', calendar: 'WALL', office_chair: 'FLOOR',
    waste_bin: 'FLOOR',
    // Library
    bookend: 'SHELF', globe: 'SHELF', quill_pen: 'SHELF', inkwell: 'SHELF',
    chess_set: 'SHELF', wax_seal_stamp: 'SHELF', magnifying_glass: 'TABLE',
    hourglass: 'TABLE', leather_journal: 'TABLE', armchair: 'FLOOR', telescope: 'FLOOR',
    wall_map: 'WALL', reading_lamp: 'DESK',
    // Garden
    watering_can: 'FLOOR', garden_gnome: 'FLOOR', flower_pot: 'SHELF',
    garden_trowel: 'SHELF', bird_bath: 'FLOOR', wheelbarrow: 'FLOOR',
    gardening_gloves: 'SHELF', sunhat: 'WALL', picnic_basket: 'TABLE',
    // Space Station
    control_panel: 'DESK', communicator: 'DESK', star_chart: 'DESK', oxygen_tank: 'SHELF',
    food_pouch: 'SHELF', satellite_model: 'SHELF', space_helmet: 'FLOOR',
    robot_companion: 'FLOOR', space_boots: 'FLOOR', hydroponic_pod: 'WINDOWSILL',
  },
  // Mirrors GenerationRules.objectShapeFamilyById -- a rough silhouette tag per object, used by
  // pickDistractors below to prefer distractors that could be mistaken for a target at a glance
  // instead of a uniformly random pick from the room's remaining pool.
  objectShapeFamilyById: {
    // Shared
    book: 'FLAT_RECT', coffee_mug: 'VESSEL', laptop: 'DEVICE', potted_plant: 'ORNAMENT',
    // Kitchen
    kettle: 'VESSEL', frying_pan: 'TOOL', cooking_pot: 'VESSEL', toaster: 'DEVICE',
    dinner_plate: 'FLAT_RECT', mixing_bowl: 'VESSEL', cutting_board: 'FLAT_RECT',
    chef_knife: 'TOOL', whisk: 'TOOL', spatula: 'TOOL', colander: 'VESSEL', teapot: 'VESSEL',
    spice_jar: 'VESSEL', apple: 'FOOD', banana: 'FOOD', oven_mitt: 'SOFT', blender: 'DEVICE',
    // Bedroom
    pillow: 'SOFT', table_lamp: 'TALL_NARROW', alarm_clock: 'ROUND_SMALL',
    picture_frame: 'FLAT_RECT', mirror: 'FLAT_RECT', jewelry_box: 'ORNAMENT',
    candle: 'TALL_NARROW', teddy_bear: 'SOFT', folded_blanket: 'SOFT', slippers: 'SOFT',
    coat_hanger: 'TOOL', perfume_bottle: 'VESSEL', eyeglasses: 'ROUND_SMALL',
    tissue_box: 'FLAT_RECT', flower_vase: 'VESSEL',
    // Coffee shop
    espresso_machine: 'DEVICE', latte_cup: 'VESSEL', pastry_croissant: 'FOOD',
    coffee_bean_bag: 'SOFT', coffee_grinder: 'DEVICE', chalkboard_menu: 'FLAT_RECT',
    bar_stool: 'FURNITURE_SMALL', pendant_lamp: 'TALL_NARROW', milk_frother_pitcher: 'VESSEL',
    tip_jar: 'VESSEL', sugar_shaker: 'VESSEL', napkin_holder: 'FLAT_RECT',
    // Gaming room
    gaming_pc_tower: 'DEVICE', monitor: 'DEVICE', gaming_chair: 'FURNITURE_SMALL',
    headset: 'DEVICE', mechanical_keyboard: 'DEVICE', gaming_mouse: 'DEVICE',
    controller: 'DEVICE', desk_fan: 'DEVICE', led_strip: 'DEVICE', poster: 'FLAT_RECT',
    energy_drink_can: 'VESSEL', figurine_collectible: 'ORNAMENT', trophy: 'ORNAMENT',
    backpack: 'SOFT',
    // Office
    desk_lamp: 'TALL_NARROW', pen_cup: 'VESSEL', stapler: 'TOOL', binder: 'FLAT_RECT',
    award_plaque: 'FLAT_RECT', wall_clock: 'ROUND_SMALL', calendar: 'FLAT_RECT',
    office_chair: 'FURNITURE_SMALL', waste_bin: 'VESSEL',
    // Library
    bookend: 'ORNAMENT', globe: 'ROUND_SMALL', quill_pen: 'TOOL', inkwell: 'VESSEL',
    chess_set: 'FURNITURE_SMALL', wax_seal_stamp: 'TOOL', magnifying_glass: 'TOOL',
    hourglass: 'TALL_NARROW', leather_journal: 'FLAT_RECT', armchair: 'FURNITURE_SMALL',
    telescope: 'TALL_NARROW', wall_map: 'FLAT_RECT', reading_lamp: 'TALL_NARROW',
    // Garden
    flower_pot: 'ORNAMENT', garden_trowel: 'TOOL', gardening_gloves: 'SOFT',
    picnic_basket: 'VESSEL', sunhat: 'SOFT', watering_can: 'VESSEL', garden_gnome: 'ORNAMENT',
    bird_bath: 'FURNITURE_SMALL', wheelbarrow: 'FURNITURE_SMALL',
    // Space Station
    control_panel: 'DEVICE', communicator: 'DEVICE', star_chart: 'FLAT_RECT',
    oxygen_tank: 'VESSEL', food_pouch: 'SOFT', satellite_model: 'DEVICE',
    space_helmet: 'ROUND_SMALL', robot_companion: 'DEVICE', space_boots: 'SOFT',
    hydroponic_pod: 'ORNAMENT',
  },
};

const TIER_INDEX = { BEGINNER: 0, EASY: 1, MEDIUM: 2, HARD: 3, EXPERT: 4 };

// Deterministic small PRNG (mulberry32) -- independent of the client's Kotlin Random;
// only needs to be internally deterministic so daily/weekly seeds reproduce consistently
// within this mock server's lifetime, not bit-for-bit identical to the client.
function mulberry32(seed) {
  let state = seed | 0;
  return function random() {
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function shuffle(array, random) {
  const copy = array.slice();
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

// Daily/Weekly Challenge get a fixed, deliberately small object count instead of the tier-derived
// formula - a simpler, more approachable puzzle size. The memorize duration is pinned alongside
// it rather than left to recompute from the smaller object count: computeMemorizeTimeMs's
// per-object floor (see generateLevel) would otherwise scale *down* with fewer objects, quietly
// undoing this session's earlier evidence-based fix for these two modes. Both pinned values are
// that same formula's own output for the object counts these modes used before this reduction
// (~10 for Daily/MEDIUM, ~13 for Weekly/HARD, both with rotation active) - reused here as a
// floor, not invented fresh. Mirrors DifficultyEngine.kt - keep both in sync.
const DAILY_CHALLENGE_OBJECT_COUNT = 3;
const WEEKLY_CHALLENGE_OBJECT_COUNT = 5;
const DAILY_CHALLENGE_MEMORIZE_MS = 11500;
const WEEKLY_CHALLENGE_MEMORIZE_MS = 15000;

// Mirrors DifficultyEngine.computeConstraints from domain/generation/DifficultyEngine.kt.
function computeConstraints(tierIndex, streak, mode) {
  const effectiveTierIndex = mode === 'HARDCORE' ? Math.min(tierIndex + 1, 4) : tierIndex;
  const streakBonus = Math.log(streak + 1) * RULES.streakRampFactor;
  let objectCount;
  if (mode === 'DAILY_CHALLENGE') {
    objectCount = DAILY_CHALLENGE_OBJECT_COUNT;
  } else if (mode === 'WEEKLY_CHALLENGE') {
    objectCount = WEEKLY_CHALLENGE_OBJECT_COUNT;
  } else {
    objectCount = Math.min(
      Math.max(
        Math.round(RULES.baseObjectCount + effectiveTierIndex * RULES.objectCountPerTier + streakBonus),
        RULES.minObjectCount,
      ),
      RULES.maxObjectCount,
    );
  }
  let memorizeDurationMs;
  if (mode === 'ZEN') {
    memorizeDurationMs = RULES.zenMemorizeDurationMs;
  } else if (mode === 'DAILY_CHALLENGE') {
    memorizeDurationMs = DAILY_CHALLENGE_MEMORIZE_MS;
  } else if (mode === 'WEEKLY_CHALLENGE') {
    memorizeDurationMs = WEEKLY_CHALLENGE_MEMORIZE_MS;
  } else {
    memorizeDurationMs = Math.max(RULES.baseMemorizeDurationMs - effectiveTierIndex * RULES.memorizeDurationStepMs, RULES.minMemorizeDurationMs);
  }
  const rotationEnabled = mode !== 'PRACTICE' && effectiveTierIndex >= RULES.rotationUnlockTier;
  // No distractors for Daily/Weekly - "4 objects" / "7 objects" means exactly that many shown in
  // total, not 4/7 targets plus more distractors stacked on top.
  const distractorRatio = (mode === 'DAILY_CHALLENGE' || mode === 'WEEKLY_CHALLENGE') ? 0 : RULES.distractorRatio;

  return {
    objectCount,
    distractorRatio,
    memorizeDurationMs,
    rotationEnabled,
    rotationStepDegrees: RULES.rotationStepDegrees,
    themeComplexity: effectiveTierIndex,
  };
}

function timeLimitFor(mode, tierIndex) {
  if (mode === 'ZEN' || mode === 'PRACTICE') return null;
  if (mode === 'TIME_ATTACK') return RULES.timeAttackLimitMs;
  return Math.max(RULES.baseTimeLimitMs - tierIndex * RULES.timeLimitStepMs, RULES.timeAttackLimitMs);
}

// Mirrors LevelGenerator.assignSlots from domain/generation/LevelGenerator.kt: each object
// prefers a slot matching its furniture category (interior-design logic) instead of a pure
// random slot, falling back to any remaining slot once its preferred category is exhausted or
// absent from this room. Stays deterministic -- every random choice goes through `random` in a
// fixed order.
function assignSlots(objectIds, sceneType, slotCount, random) {
  const slotCategories = RULES.roomSlotCategoriesByScene[sceneType];
  const remaining = [...Array(slotCount).keys()];
  const byCategory = {};
  if (slotCategories) {
    remaining.forEach((index) => {
      const category = slotCategories[index];
      if (category === undefined) return;
      (byCategory[category] = byCategory[category] || []).push(index);
    });
  }

  function popRandom(list) {
    const i = Math.floor(random() * list.length);
    return list.splice(i, 1)[0];
  }

  return objectIds.map((objectId) => {
    const preferred = RULES.objectCategoryById[objectId];
    const bucket = preferred ? byCategory[preferred] : undefined;
    const chosen = bucket && bucket.length > 0 ? popRandom(bucket) : popRandom(remaining);

    const remainingPos = remaining.indexOf(chosen);
    if (remainingPos !== -1) remaining.splice(remainingPos, 1);
    const chosenCategory = slotCategories ? slotCategories[chosen] : undefined;
    if (chosenCategory && byCategory[chosenCategory]) {
      const bucketPos = byCategory[chosenCategory].indexOf(chosen);
      if (bucketPos !== -1) byCategory[chosenCategory].splice(bucketPos, 1);
    }
    return chosen;
  });
}

// Mirrors LevelGenerator.pickDistractors from domain/generation/LevelGenerator.kt: prefers
// distractors whose shape family matches a target's (so a distractor can genuinely be mistaken
// for the real thing at a glance), falling back to whatever's left once similar-shaped
// candidates run out.
function pickDistractors(targetIds, remainingPool, count, random) {
  if (count <= 0) return [];
  const targetFamilies = new Set(
    targetIds.map((id) => RULES.objectShapeFamilyById[id]).filter((family) => family !== undefined),
  );
  const similarShape = [];
  const other = [];
  shuffle(remainingPool, random).forEach((id) => {
    (targetFamilies.has(RULES.objectShapeFamilyById[id]) ? similarShape : other).push(id);
  });
  return similarShape.concat(other).slice(0, count);
}

// Mirrors MemorizeTimeCalculator.compute from domain/generation/MemorizeTimeCalculator.kt: a
// floor computed from the *actual* generated scene (object count, how many are rotated, how
// many distractors share a shape family with a target) rather than an abstract tier lookup, so
// modes like Daily/Weekly Challenge -- which hardcode a single tier regardless of scene content
// -- still get a fair memorize window for what actually got generated. See that file for the
// reasoning behind these constants; keep both in sync.
const MEMORIZE_BASE_MS_PER_OBJECT = 900;
const MEMORIZE_ROTATION_BONUS_MS_PER_OBJECT = 220;
const MEMORIZE_SIMILAR_SHAPE_BONUS_MS_PER_DISTRACTOR = 300;
const MEMORIZE_MIN_MS = 4000;
const MEMORIZE_MAX_MS = 18000;

function computeMemorizeTimeMs(objects) {
  if (objects.length === 0) return MEMORIZE_MIN_MS;

  const targetFamilies = new Set(
    objects
      .filter((obj) => !obj.isDistractor)
      .map((obj) => RULES.objectShapeFamilyById[obj.objectId])
      .filter((family) => family !== undefined),
  );
  const lookalikeDistractorCount = objects.filter(
    (obj) => obj.isDistractor && targetFamilies.has(RULES.objectShapeFamilyById[obj.objectId]),
  ).length;
  const rotatedCount = objects.filter((obj) => obj.rotationDegrees !== 0).length;

  const totalMs = objects.length * MEMORIZE_BASE_MS_PER_OBJECT
    + rotatedCount * MEMORIZE_ROTATION_BONUS_MS_PER_OBJECT
    + lookalikeDistractorCount * MEMORIZE_SIMILAR_SHAPE_BONUS_MS_PER_DISTRACTOR;

  return Math.min(Math.max(totalMs, MEMORIZE_MIN_MS), MEMORIZE_MAX_MS);
}

// Mirrors LevelGenerator.generate from domain/generation/LevelGenerator.kt. Which objects
// appear (and which are distractors) is a random subset of the room's object pool; where each
// one lands is decided by assignSlots above rather than an independent random shuffle.
function generateLevel(seed, mode, tierIndex, constraints) {
  const random = mulberry32(seed);
  const sceneType = RULES.scenePool[Math.floor(random() * RULES.scenePool.length)];
  const objectPool = RULES.objectPoolsByScene[sceneType];
  const slotCount = RULES.roomSlotCountByScene[sceneType];

  const distractorCount = Math.round(constraints.objectCount * constraints.distractorRatio);
  const totalToPlace = Math.min(constraints.objectCount + distractorCount, objectPool.length, slotCount);
  const distractorStartIndex = Math.max(totalToPlace - distractorCount, 0);

  const shuffledPool = shuffle(objectPool, random);
  const targetIds = shuffledPool.slice(0, distractorStartIndex);
  const chosenObjects = targetIds.concat(
    pickDistractors(targetIds, shuffledPool.slice(distractorStartIndex), totalToPlace - distractorStartIndex, random),
  );
  const chosenSlots = assignSlots(chosenObjects, sceneType, slotCount, random);

  const objects = chosenObjects.map((objectId, index) => {
    const rotationDegrees = constraints.rotationEnabled
      ? Math.floor(random() * (360 / constraints.rotationStepDegrees)) * constraints.rotationStepDegrees
      : 0;
    const scale = 0.85 + random() * 0.3;

    return {
      objectId,
      slotIndex: chosenSlots[index],
      rotationDegrees,
      scale,
      isDistractor: index >= distractorStartIndex,
    };
  });

  return {
    seed,
    sceneType,
    objects,
    timeLimitMs: timeLimitFor(mode, tierIndex),
    // Floor, not override: never shorter than what computeConstraints already decided, only
    // ever raised when the real generated scene demands more than that tier-based estimate.
    memorizeDurationMs: Math.max(constraints.memorizeDurationMs, computeMemorizeTimeMs(objects)),
  };
}

module.exports = { RULES, TIER_INDEX, computeConstraints, generateLevel };
