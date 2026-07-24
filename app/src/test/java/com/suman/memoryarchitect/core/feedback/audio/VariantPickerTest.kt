package com.suman.memoryarchitect.core.feedback.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class VariantPickerTest {

    @Test
    fun `single variant always returns index 0 without consuming randomness`() {
        val picker = VariantPicker(random = Random(0))

        repeat(5) { assertEquals(0, picker.next("sfx", variantCount = 1)) }
    }

    @Test
    fun `zero variants also returns 0 rather than throwing`() {
        val picker = VariantPicker(random = Random(0))

        assertEquals(0, picker.next("sfx", variantCount = 0))
    }

    @Test
    fun `never repeats the previous index for the same key across many draws`() {
        val picker = VariantPicker(random = Random(42))
        var previous = picker.next("object_pickup", variantCount = 3)

        repeat(200) {
            val next = picker.next("object_pickup", variantCount = 3)
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun `every variant index is eventually reachable, not just the fallback neighbor`() {
        val picker = VariantPicker(random = Random(7))
        val seen = mutableSetOf<Int>()

        repeat(200) { seen += picker.next("object_place", variantCount = 3) }

        assertEquals(setOf(0, 1, 2), seen)
    }

    @Test
    fun `different keys track independent history`() {
        val picker = VariantPicker(random = Random(1))
        picker.next("object_pickup", variantCount = 3)
        val pickupPrevious = picker.next("object_pickup", variantCount = 3)

        // Drawing for an unrelated key in between must not affect "object_pickup"'s own
        // no-repeat rule - only its own previous index should ever be excluded.
        picker.next("object_rotate", variantCount = 3)
        val pickupNext = picker.next("object_pickup", variantCount = 3)

        assertNotEquals(pickupPrevious, pickupNext)
    }
}
