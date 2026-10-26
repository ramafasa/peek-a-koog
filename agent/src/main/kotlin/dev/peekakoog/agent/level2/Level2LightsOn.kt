package dev.peekakoog.agent.level2

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.Game

/**
 * Level 2: lights on. Exactly the Level 1 agent (same prompt, same tools); the only change is that we can
 * now see inside it: every LLM call and tool call is streamed, and the run is exported as an OpenTelemetry trace.
 */
object Level2LightsOn : Seeker {
    override val title = "Level 2 · Lights on"
    override val idea = "The same agent as Level 1, but now we can peek inside: every LLM call and tool call is streamed, " +
        "and the run is exported as an OpenTelemetry trace."
    override val showsThoughts = true
    override val traceDestination: String get() = Telemetry.description

    override suspend fun seek(game: Game, llm: Llm, onThought: (Thought) -> Unit): String {
        val agent = AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = systemPrompt(game),
            toolRegistry = ToolRegistry.builder().tools(SeekerTools(game)).build(),
            installFeatures = {
                watchThoughts(onThought)
                Telemetry.install(this)
            },
        )
        return try {
            agent.run("Find Kodee!")
        } finally {
            // Closing the agent also flushes its OpenTelemetry spans.
            agent.close()
        }
    }
}

/** The rules of the game, as this level explains them to the model. */
private fun systemPrompt(game: Game): String {
    val house = game.house
    val layout = house.rooms.joinToString("\n") { room ->
        val spots = house.spotsIn(room.id).joinToString(", ") { it.id }
        "- ${room.id} (doors to: ${house.doorsFrom(room.id).joinToString(", ")}): $spots"
    }
    return """
        You are playing hide-and-seek. Someone hid Kodee, the Kotlin mascot, in one of the hiding spots of this house:
        $layout
        You start in the ${game.currentRoomId}.
        Find Kodee. End your answer with a line `FOUND: <spot-id>`.
        Use your tools to search.
    """.trimIndent()
}
