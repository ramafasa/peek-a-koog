package dev.peekakoog.app

import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.SeekerTools
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test

/** Level 2 on screen, with a scripted LLM instead of a real one. */
@OptIn(ExperimentalTestApi::class)
class ObservableLevelTest {
    private val house = Houses.cozy
    private val plan = FloorPlan.cozy(house)
    private val outDir = File(System.getProperty("snapshots.dir", "build/snapshots")).apply { mkdirs() }

    private fun scriptedLlm(): Llm {
        val tools = SeekerTools(Game(house, "fridge")).asTools().associateBy { it.name }
        fun call(tool: String, vararg args: Pair<String, String>) = tools.getValue(tool).let { t ->
            t to ToolFromCallable.Args(args.associate { (name, value) -> t.callable.parameters.first { it.name == name } to value })
        }
        val executor = getMockExecutor {
            val (move, toKitchen) = call("moveTo", "roomId" to "kitchen")
            mockLLMToolCall(move, toKitchen) onRequestEquals "Find Kodee!"
            val (peek, fridge) = call("peek", "spotId" to "fridge")
            mockLLMToolCall(peek, fridge) onRequestContains "You are now in the Kitchen"
            mockLLMAnswer("Kodee was hiding in the fridge, brr!\nFOUND: fridge") onRequestContains "Found Kodee"
        }
        return Llm(executor, AnthropicModels.Sonnet_5)
    }

    @Test
    fun `level 2 shows what the agent is thinking`() = runDesktopComposeUiTest(width = 1400, height = 860) {
        setContent { PeekAKoogScreen(house, llm = scriptedLlm()) }

        onNodeWithText("Level 2 · Lights on").performClick()
        onNodeWithText("What Koog is thinking").assertExists()
        clickOnPlan(onNodeWithTag("house"), plan.layout("fridge").rect.center)
        onNodeWithText("Start seeking").performClick()

        waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText("Found it for real", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        onRoot().saveAs("6-level-2")
        onNodeWithText("decides: moveTo", substring = true).assertExists()
        onNodeWithText("decides: peek", substring = true).assertExists()
        onNodeWithText("Kodee was hiding in the fridge", substring = true).assertExists()
        onRoot().saveAs("6-level-2")
    }

    private fun clickOnPlan(node: SemanticsNodeInteraction, point: Offset) {
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
