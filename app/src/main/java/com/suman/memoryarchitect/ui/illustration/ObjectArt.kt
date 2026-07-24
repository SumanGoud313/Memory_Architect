package com.suman.memoryarchitect.ui.illustration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.illustration.objects.registerBedroomObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerCoffeeShopObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerGamingRoomObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerGardenObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerKitchenObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerLibraryObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerOfficeObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerSharedObjects
import com.suman.memoryarchitect.ui.illustration.objects.registerSpaceStationObjects

/** The 5 reusable idle-animation categories every object in the registry picks from. */
enum class IdleAnimationKind { NONE, FLICKER, SWAY, PULSE, ROTATE, STEAM }

/**
 * A single object's illustration entry. [drawer] is a pure, static illustration (gradients,
 * shadow, highlight — no animation of its own); idle motion is layered on generically by
 * [IdleAnimatedObject] based on [idleAnimation], so no drawer ever duplicates animation logic.
 */
data class ObjectArt(
    val id: String,
    val labelRes: Int,
    val defaultSizeDp: Dp = 64.dp,
    val idleAnimation: IdleAnimationKind = IdleAnimationKind.NONE,
    val drawer: @Composable (Modifier) -> Unit,
)

/**
 * Data-driven objectId -> [ObjectArt] lookup, replacing the old `ObjectArtCatalog.kt` exhaustive
 * `when` blocks. Each room contributes its own `registerXxxObjects()` (see the
 * `ui/illustration/objects` package) — adding a future room never means editing a shared
 * function body, just adding one more small file and one more `register*()` call below.
 */
object ObjectArtRegistry {
    private val byId: Map<String, ObjectArt> by lazy {
        buildMap {
            registerSharedObjects()
            registerBedroomObjects()
            registerKitchenObjects()
            registerCoffeeShopObjects()
            registerGamingRoomObjects()
            registerOfficeObjects()
            registerLibraryObjects()
            registerGardenObjects()
            registerSpaceStationObjects()
        }
    }

    private val fallback = ObjectArt(
        id = "unknown",
        labelRes = R.string.object_unknown,
        drawer = { modifier -> UnknownObjectArt(modifier) },
    )

    /**
     * Returns the registered art for [objectId], or a visibly-generic fallback if none is
     * registered. A `GenerationRules` object id with no matching entry here is a real risk once
     * this scales past 4 rooms — callers should assert-fail in debug builds on a miss (see
     * [requireArt]) so it's caught in dev rather than silently rendering "unknown" in production.
     */
    fun get(objectId: String): ObjectArt = byId[objectId] ?: fallback

    fun requireArt(objectId: String): ObjectArt {
        val art = byId[objectId]
        check(art != null) { "No ObjectArt registered for objectId=\"$objectId\" — add it to the registry." }
        return art
    }
}

internal fun MutableMap<String, ObjectArt>.register(art: ObjectArt) {
    this[art.id] = art
}

/** Renders [art]'s illustration wrapped with its idle animation (transform and/or overlay). */
@Composable
fun IdleAnimatedObject(art: ObjectArt, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        art.drawer(Modifier.fillMaxSize().applyIdleAnimation(art.idleAnimation))
        if (art.idleAnimation == IdleAnimationKind.STEAM) {
            SteamEmitter(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-6).dp)
                    .align(Alignment.TopCenter),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun UnknownObjectArt(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawCircle(color = Color(0xFF6B6157))
    }
}
