package dev.peekakoog.agent.level2

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.message.MessagePart
import ai.koog.serialization.JSONElement
import ai.koog.serialization.JSONPrimitive
import dev.peekakoog.agent.Thought

/** Streams every LLM call and tool call of the agent to [onThought], using Koog's [EventHandler] feature. */
internal fun GraphAIAgent.FeatureContext.watchThoughts(onThought: (Thought) -> Unit) {
    var calls = 0
    var startedAt = 0L
    install(EventHandler) {
        onLLMCallStarting { event ->
            calls++
            startedAt = System.nanoTime()
            onThought(Thought.Asking(calls, event.model.id))
        }
        onLLMCallCompleted { event ->
            val parts = event.response?.parts.orEmpty()
            val meta = event.response?.metaInfo
            onThought(
                Thought.Replied(
                    call = calls,
                    text = parts.filterIsInstance<MessagePart.Text>().joinToString("\n") { it.text }.ifBlank { null },
                    reasoning = parts.filterIsInstance<MessagePart.Reasoning>()
                        .joinToString("\n") { (it.summary ?: it.content).joinToString("\n") }.ifBlank { null },
                    toolCalls = parts.filterIsInstance<MessagePart.Tool.Call>().map { "${it.tool}(${it.args})" },
                    inputTokens = meta?.inputTokensCount,
                    outputTokens = meta?.outputTokensCount,
                    millis = (System.nanoTime() - startedAt) / 1_000_000,
                ),
            )
        }
        onToolCallStarting { event -> onThought(Thought.UsingTool(event.toolName, event.toolArgs.toString())) }
        onToolCallCompleted { event -> onThought(Thought.ToolReturned(event.toolName, event.toolResult?.plain().orEmpty())) }
        onToolValidationFailed { event -> onThought(Thought.ToolFailed(event.toolName, event.message)) }
        onToolCallFailed { event -> onThought(Thought.ToolFailed(event.toolName, event.message)) }
        onAgentExecutionFailed { event -> onThought(Thought.Failed(event.error.message ?: event.error::class.simpleName.orEmpty())) }
    }
}

private fun JSONElement.plain(): String = if (this is JSONPrimitive && isString) content else toString()
