package com.suman.memoryarchitect.domain.generation

import kotlin.random.Random

/**
 * Whether a given level renders its room mirrored left-right — pure presentation variety with
 * zero effect on scoring (placement is still checked by [com.suman.memoryarchitect.domain.model.SceneObjectSpec.slotIndex],
 * never by screen position). Deliberately its own tiny [Random] draw from [seed] rather than
 * reusing [LevelGenerator]'s internal one, so it never perturbs the existing object/slot/rotation
 * sequence for a seed that already shipped.
 */
object LevelMirrorPolicy {
    fun isMirrored(seed: Long): Boolean = Random(seed).nextBoolean()
}
