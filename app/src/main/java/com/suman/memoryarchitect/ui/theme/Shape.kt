package com.suman.memoryarchitect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Mirrors the old corner_radius_card/button/chip dimens (24/28/18dp). */
val MemoryArchitectShapes = Shapes(
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object MemoryArchitectRadii {
    val chip = 18.dp
    val card = 24.dp
    val button = 28.dp
}
