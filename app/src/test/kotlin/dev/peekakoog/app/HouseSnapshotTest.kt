package dev.peekakoog.app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.use
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
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
        peeks: Map<String, Int> = emptyMap(),
        mood: KodeeMood = KodeeMood.NAUGHTY,
        canHide: Boolean = false,
    ) {
        ImageComposeScene(width = 1400, height = 800) {
            HouseView(
                house, plan, director,
                kodeeSpotId = hidden,
                kodeeMood = mood,
                peeks = peeks,
                modifier = Modifier.fillMaxSize(),
                previewSpotId = hovered.takeIf { canHide },
                hovered = hovered?.let { Target.Spot(it) },
                tooltip = hovered?.let { "Kodee will hide ${house.spot(it)!!.name.replaceFirstChar(Char::lowercase)} · click to hide" },
                interactive = canHide,
            )
        }.use { scene ->
            val image = scene.render()
            File(outDir, "$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Test
    fun `render the logo`() {
        val bitmap = KodeeLogo.image.asSkiaBitmap()
        File(outDir, "0-logo.png").writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    @Test
    fun `render key moments`() = runBlocking {
        snapshot("1-hover", Director(house, plan, "hall"), hovered = "under-bed", canHide = true)

        val peeking = Director(house, plan, "hall")
        peeking.position.snapTo(plan.layout("fridge").stand)
        snapshot("2-hidden", peeking, hidden = "behind-curtain", peeks = mapOf("oven" to 1))

        val found = Director(house, plan, "hall")
        found.position.snapTo(plan.layout("behind-curtain").stand + Offset(0f, 0f))
        snapshot("3-found", found, hidden = "behind-curtain", peeks = mapOf("behind-curtain" to 1, "wardrobe" to 1), mood = KodeeMood.SURPRISED)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `render footprints of a wasteful search`() = runDesktopComposeUiTest(width = 1400, height = 800) {
        val director = Director(house, plan, "hall")
        val wander = listOf(
            GameEvent.Moved("hall", "kitchen"), GameEvent.Peeked("fridge", found = false),
            GameEvent.Moved("kitchen", "hall"), GameEvent.Moved("hall", "kitchen"),
            GameEvent.Peeked("fridge", found = false), GameEvent.Moved("kitchen", "hall"),
            GameEvent.Moved("hall", "kids-room"), GameEvent.Moved("kids-room", "study"),
        )
        setContent {
            LaunchedEffect(Unit) { wander.forEach { director.play(it) } }
            HouseView(house, plan, director, kodeeSpotId = null, kodeeMood = KodeeMood.NAUGHTY,
                peeks = mapOf("fridge" to 2), modifier = Modifier.fillMaxSize())
        }
        mainClock.advanceTimeBy(30_000)
        val image = onRoot().captureToImage().asSkiaBitmap()
        File(outDir, "4-footprints.png").writeBytes(Image.makeFromBitmap(image).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
}
