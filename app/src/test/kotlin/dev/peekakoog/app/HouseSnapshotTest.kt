package dev.peekakoog.app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.use
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/** Renders the house in a few key moments to PNG files in build/snapshots, to eyeball the art without a screen. */
class HouseSnapshotTest {
    private val house = Houses.cozy
    private val plan = FloorPlan.cozy(house)
    private val outDir = File(System.getProperty("snapshots.dir", "build/snapshots")).apply { mkdirs() }

    private fun snapshot(
        name: String,
        director: Director,
        hidden: String? = null,
        hovered: String? = null,
        peeked: Set<String> = emptySet(),
        mood: KodeeMood = KodeeMood.NAUGHTY,
        canHide: Boolean = false,
    ) {
        ImageComposeScene(width = 1400, height = 800) {
            HouseView(house, plan, director, hidden, hovered, peeked, mood, canHide, {}, {}, Modifier.fillMaxSize())
        }.use { scene ->
            val image = scene.render()
            File(outDir, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test
    fun `render key moments`() = runBlocking {
        snapshot("1-hover", Director(house, plan, "hall"), hovered = "under-bed", canHide = true)

        val peeking = Director(house, plan, "hall")
        peeking.position.snapTo(plan.layout("fridge").stand)
        snapshot("2-hidden", peeking, hidden = "behind-curtain", peeked = setOf("oven"))

        val found = Director(house, plan, "hall")
        found.position.snapTo(plan.layout("behind-curtain").stand + Offset(0f, 0f))
        snapshot("3-found", found, hidden = "behind-curtain", peeked = setOf("behind-curtain", "wardrobe"), mood = KodeeMood.SURPRISED)
    }
}
