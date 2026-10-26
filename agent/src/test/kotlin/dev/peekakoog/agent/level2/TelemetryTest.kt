package dev.peekakoog.agent.level2

import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.play

import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.testing.tools.getMockExecutor
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.Houses
import io.opentelemetry.kotlin.export.OperationResultCode
import io.opentelemetry.kotlin.tracing.data.SpanData
import io.opentelemetry.kotlin.tracing.export.SpanExporter
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetryTest {
    private val exported = mutableListOf<SpanData>()

    private val inMemory = object : SpanExporter {
        override suspend fun export(telemetry: List<SpanData>): OperationResultCode {
            exported += telemetry
            return OperationResultCode.Success
        }

        override suspend fun forceFlush() = OperationResultCode.Success
        override suspend fun shutdown() = OperationResultCode.Success
    }

    @AfterTest
    fun cleanUp() {
        Telemetry.testExporter = null
    }

    @Test
    fun `a level 2 game is exported as one trace when the agent closes`() = runBlocking {
        Telemetry.testExporter = inMemory
        val game = Game(Houses.cozy, hiddenSpotId = "coat-rack")
        val listen = SeekerTools(game).asTools().first { it.name == "listen" }
        val executor = getMockExecutor {
            mockLLMToolCall(listen, ToolFromCallable.Args(emptyMap())) onRequestEquals "Find Kodee!"
            mockLLMAnswer("FOUND: coat-rack") onRequestContains "You listen carefully"
        }

        play(Level2LightsOn, game, Llm(executor, AnthropicModels.Sonnet_5))

        val names = exported.map { it.name }
        assertTrue(names.any { it.startsWith("invoke_agent") }, "spans: $names")
        assertEquals(2, names.count { it.startsWith("chat ") }, "spans: $names")
        assertTrue("execute_tool listen" in names, "spans: $names")
        assertEquals(1, exported.map { it.spanContext.traceId }.distinct().size, "one game, one trace")
    }
}
