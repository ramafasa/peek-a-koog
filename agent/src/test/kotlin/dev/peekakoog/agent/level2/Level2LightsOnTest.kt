package dev.peekakoog.agent.level2

import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Thought
import dev.peekakoog.agent.play

import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Level2LightsOnTest {
    @Test
    fun `streams the LLM calls and tool calls behind a game`() = runBlocking {
        val game = Game(Houses.cozy, hiddenSpotId = "coat-rack")
        val listen = SeekerTools(game).asTools().first { it.name == "listen" }
        val executor = getMockExecutor {
            mockLLMToolCall(listen, ToolFromCallable.Args(emptyMap())) onRequestEquals "Find Kodee!"
            mockLLMAnswer("HOT in the hall, so it must be the coats.\nFOUND: coat-rack") onRequestContains "You listen carefully"
        }
        val thoughts = mutableListOf<Thought>()

        val result = play(Level2LightsOn, game, Llm(executor, AnthropicModels.Sonnet_5)) { thoughts += it }

        assertEquals("coat-rack", result.claim.spotId)
        assertIs<GameEvent.Listened>(game.events.first())
        val kinds = thoughts.map { it::class.simpleName }
        assertEquals(listOf("Asking", "Replied", "UsingTool", "ToolReturned", "Asking", "Replied"), kinds)
        assertEquals(listOf("listen({})"), (thoughts[1] as Thought.Replied).toolCalls)
        assertTrue("HOT" in (thoughts[3] as Thought.ToolReturned).result)
        assertTrue("coats" in (thoughts[5] as Thought.Replied).text.orEmpty())
    }
}
