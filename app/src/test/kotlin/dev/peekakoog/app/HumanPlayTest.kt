package dev.peekakoog.app

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.random.Random
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HumanPlayTest {
    private companion object {
        const val BUDGET = Game.DEFAULT_MOVE_BUDGET
    }

    private val house = Houses.cozy
    private val plan = FloorPlan.cozy(house)
    private val outDir = File(System.getProperty("snapshots.dir", "build/snapshots")).apply { mkdirs() }

    @Test
    fun `you can listen, walk to the furniture and find Kodee`() = runDesktopComposeUiTest(width = 1400, height = 860) {
        val seed = 7
        val hidden = house.spots.random(Random(seed))
        setContent { PeekAKoogScreen(house, Random(seed)) }

        onNodeWithText("New game").performClick()
        onNodeWithText("👂 Listen · 1 move").performClick()
        mainClock.advanceTimeBy(3_000)
        onNodeWithText("Moves left: ${BUDGET - 1} of $BUDGET").assertExists()
        onNodeWithText("👂 Listen · 1 move").assertIsEnabled()

        onNodeWithText("👁 Show Kodee").performClick()
        waitForIdle()
        onRoot().saveAs("4-human-revealed")

        val steps = house.path(START_ROOM, hidden.roomId).size
        clickOnPlan(onNodeWithTag("house"), plan.layout(hidden.id).rect.center)
        mainClock.advanceTimeBy(20_000)
        onNodeWithText("You found Kodee in ${steps + 2} moves", substring = true).assertExists()
        onNodeWithText("New game").assertExists()
        onRoot().saveAs("5-human-found")
    }

    @Test
    fun `restart in the middle of a walk starts a clean game`() = runDesktopComposeUiTest(width = 1400, height = 860) {
        setContent { PeekAKoogScreen(house, Random(3)) }
        onNodeWithText("New game").performClick()
        mainClock.advanceTimeBy(1_000)

        clickOnPlan(onNodeWithTag("house"), plan.layout("under-bed").rect.center)
        mainClock.advanceTimeBy(800)
        onNodeWithText("Moves left: $BUDGET of $BUDGET").assertDoesNotExist()

        onNodeWithText("🔄 Restart").performClick()
        mainClock.advanceTimeBy(5_000)
        onNodeWithText("Moves left: $BUDGET of $BUDGET").assertExists()
        onNodeWithText("Nothing yet.").assertExists()
        onNodeWithText("👂 Listen · 1 move").assertIsEnabled()
    }

    private fun clickOnPlan(node: SemanticsNodeInteraction, point: androidx.compose.ui.geometry.Offset) {
        val size = node.fetchSemanticsNode().size
        val target = Projection(Size(size.width.toFloat(), size.height.toFloat()), plan).px(point)
        node.performMouseInput {
            moveTo(target)
            press()
            release()
        }
    }

    private fun SemanticsNodeInteraction.saveAs(name: String) {
        val bitmap = captureToImage().asSkiaBitmap()
        File(outDir, "$name.png").writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
}
