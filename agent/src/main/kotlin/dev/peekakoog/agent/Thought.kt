package dev.peekakoog.agent

/** What the agent does between the engine's events: talking to the LLM and calling tools. */
sealed interface Thought {
    /** The agent sends the conversation to the LLM. */
    data class Asking(val call: Int, val model: String) : Thought

    /** The LLM answered: some text, maybe reasoning, maybe tool calls. */
    data class Replied(
        val call: Int,
        val text: String?,
        val reasoning: String?,
        val toolCalls: List<String>,
        val inputTokens: Int?,
        val outputTokens: Int?,
        val millis: Long,
    ) : Thought

    data class UsingTool(val tool: String, val args: String) : Thought
    data class ToolReturned(val tool: String, val result: String) : Thought
    data class ToolFailed(val tool: String, val message: String) : Thought
    data class Failed(val message: String) : Thought
}
