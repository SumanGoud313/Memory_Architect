package com.suman.memoryarchitect.domain.model

/**
 * What a spent hint reveals: exactly one target object's correct slot and rotation. Never
 * carries a placement - the player still has to drag (and rotate) the object there themselves,
 * a hint only shows where and which way, not does. [rotationDegrees] is always the object's
 * actual correct angle, including 0 for levels/objects where no rotation is needed - that's
 * still accurate information ("leave this one as-is"), not a value to omit.
 */
data class HintReveal(val objectId: String, val slotIndex: Int, val rotationDegrees: Int)
