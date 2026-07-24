package com.suman.memoryarchitect.domain.hint

import com.suman.memoryarchitect.domain.model.HintReveal
import com.suman.memoryarchitect.domain.model.HintRules
import com.suman.memoryarchitect.domain.model.LevelSpec

/**
 * Pure hint logic: how many hints a level grants, and what a spent hint reveals for a chosen
 * object. Distractors have no correct slot by definition, so [revealFor] returns null for them -
 * callers must treat that as a silent no-op (the tap simply does nothing), never as a "this is a
 * distractor" tell, or a hint would leak scoring-relevant information for free.
 */
class HintEngine(private val rules: HintRules = HintRules.Default) {

    fun maxHintsForLevel(levelNumber: Int): Int = rules.hintsAllowedForLevel(levelNumber)

    fun revealFor(objectId: String, level: LevelSpec): HintReveal? =
        level.objects
            .firstOrNull { it.objectId == objectId && !it.isDistractor }
            ?.let { HintReveal(objectId = it.objectId, slotIndex = it.slotIndex, rotationDegrees = it.rotationDegrees) }
}
