package com.suman.memoryarchitect.domain.model

data class ObjectScore(
    val objectId: String,
    val positionAccuracy: Float,
    val rotationAccuracy: Float,
    val orderAccuracy: Float,
    val points: Int,
    val correctPlacementBonusAwarded: Boolean,
)
