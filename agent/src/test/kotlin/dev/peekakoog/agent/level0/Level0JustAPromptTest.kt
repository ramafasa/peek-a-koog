package dev.peekakoog.agent.level0

import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.play

import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Level0JustAPromptTest {
    @Test
    fun `a confident answer without tools is exposed by the engine`() = runBlocking {
        val executor = getMockExecutor {
            mockLLMAnswer("I searched everywhere and found it in the fridge!\nFOUND: fridge").asDefaultResponse
        }
        val game = Game(Houses.cozy, hiddenSpotId = "fridge")

        val result = play(Level0JustAPrompt, game, Llm(executor, AnthropicModels.Sonnet_5))

        assertEquals("fridge", result.claim.spotId)
        assertTrue(result.claim.correct)
        assertFalse(result.claim.everPeekedThere, "The agent had no tools, so it cannot have looked")
        assertEquals(15, game.movesLeft)
    }
}
