package dev.peekakoog.agent.level1

import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Thought
import dev.peekakoog.agent.play
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Level1ToolsInTheDarkTest {
    @Test
    fun `plays, but stays in the dark`() = runBlocking {
        val game = Game(Houses.cozy, hiddenSpotId = "coat-rack")
        val executor = getMockExecutor { mockLLMAnswer("FOUND: coat-rack").asDefaultResponse }
        val thoughts = mutableListOf<Thought>()
        val result = play(Level1ToolsInTheDark, game, Llm(executor, AnthropicModels.Sonnet_5)) { thoughts += it }
        assertEquals("coat-rack", result.claim.spotId)
        assertTrue(thoughts.isEmpty())
    }

    @Test
    fun `gets no map of the house`() {
        val prompt = systemPrompt(Game(Houses.cozy, hiddenSpotId = "coat-rack"))
        val elsewhere = Houses.cozy.rooms.map { it.id } - "hall"
        assertTrue(elsewhere.none { it in prompt }, prompt)
        assertTrue(Houses.cozy.spots.none { it.id in prompt }, prompt)
    }
}
