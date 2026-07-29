package com.suman.memoryarchitect.core.common

import com.suman.memoryarchitect.domain.model.LiveEvent

/** Maps [LiveEventCatalog][com.suman.memoryarchitect.domain.progression.LiveEventCatalog]'s raw
 * string ids to display copy - a `when` with a fallback (rather than an exhaustive one over an
 * enum, since [LiveEvent.id] is a plain String) so a tenth template someone adds later without
 * also touching this file still shows *something* readable instead of failing to compile. */
fun LiveEvent.toDisplayName(): String = when (id) {
    "NEW_YEAR" -> "New Year"
    "VALENTINES_DAY" -> "Valentine's Day"
    "HOLI" -> "Holi"
    "SUMMER" -> "Summer"
    "INDEPENDENCE_DAY" -> "Independence Day"
    "HALLOWEEN" -> "Halloween"
    "DIWALI" -> "Diwali"
    "CHRISTMAS" -> "Christmas"
    "ANNIVERSARY" -> "Anniversary"
    else -> id.split("_").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}
