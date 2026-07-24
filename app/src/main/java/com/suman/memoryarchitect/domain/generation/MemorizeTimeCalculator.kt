package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.SceneObjectSpec

/**
 * Computes a memorize-phase duration from the *actual* generated scene content, rather than from
 * an abstract tier/progress lookup - the same object count "costs" more or less real cognitive
 * load depending on whether those objects are rotated and how many distractors could be mistaken
 * for a real target, and only [LevelGenerator] knows that after objects are actually chosen.
 *
 * Grounding for the constants below (informed estimates for a casual mobile audience doing a
 * real room scan, not lab-flash-presentation numbers for trained subjects):
 * - [BASE_MS_PER_OBJECT] is calibrated against the campaign's own already-validated anchors:
 *   [com.suman.memoryarchitect.domain.progression.LevelCampaignRules]'s level-100 floor is
 *   9.5s for 12 objects (~790ms/object) *before* accounting for rotation/order-mode load, and
 *   its level-1 ceiling is 11s for 3 objects (~3.7s/object) as a deliberately generous
 *   onboarding pace. 900ms sits just above that floor - a sustainable per-object pace for scenes
 *   that don't yet add rotation or lookalike distractors.
 * - [ROTATION_BONUS_MS_PER_OBJECT] and [SIMILAR_SHAPE_BONUS_MS_PER_DISTRACTOR] generalize the
 *   flat per-level rotation/order-mode bonus already validated for the Classic campaign into a
 *   per-object form that scales with how much of the scene actually carries that extra load,
 *   instead of a single flat top-up regardless of object count.
 *
 * This is a *floor*, not a replacement for tier/progress-based pacing - see [LevelGenerator],
 * which takes the larger of this and whatever the calling engine (DifficultyEngine /
 * LevelCampaignEngine) already computed, so an already-generous value (e.g. a deliberately long
 * early-campaign onboarding window) is never shortened by this calculation.
 */
object MemorizeTimeCalculator {
    private const val BASE_MS_PER_OBJECT = 900L
    private const val ROTATION_BONUS_MS_PER_OBJECT = 220L
    private const val SIMILAR_SHAPE_BONUS_MS_PER_DISTRACTOR = 300L
    private const val MIN_MEMORIZE_MS = 4_000L
    private const val MAX_MEMORIZE_MS = 18_000L

    fun compute(objects: List<SceneObjectSpec>, rules: GenerationRules = GenerationRules.Default): Long {
        if (objects.isEmpty()) return MIN_MEMORIZE_MS

        val targetFamilies = objects.filterNot { it.isDistractor }
            .mapNotNull { rules.objectShapeFamilyById[it.objectId] }
            .toSet()
        val lookalikeDistractorCount = objects.count { obj ->
            obj.isDistractor && rules.objectShapeFamilyById[obj.objectId] in targetFamilies
        }
        val rotatedCount = objects.count { it.rotationDegrees != 0 }

        val totalMs = objects.size * BASE_MS_PER_OBJECT +
            rotatedCount * ROTATION_BONUS_MS_PER_OBJECT +
            lookalikeDistractorCount * SIMILAR_SHAPE_BONUS_MS_PER_DISTRACTOR

        return totalMs.coerceIn(MIN_MEMORIZE_MS, MAX_MEMORIZE_MS)
    }
}
