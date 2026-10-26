package dev.peekakoog.agent.level2

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.Game

/**
 * Level 2: lights on. Exactly the Level 1 agent (same prompt without a map, same tools); the only change is that we can
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

/**
 * The rules of the game, as this level explains them to the model. There is no map: the house is new to
 * the seeker, so it has to look around and remember where it has been.
 */
internal fun systemPrompt(game: Game): String = """
    You are playing hide-and-seek in a house you have never seen. Someone hid Kodee, the Kotlin mascot,
    in one of its hiding spots. You start in the ${game.house.room(game.currentRoomId)!!.name.lowercase()} (${game.currentRoomId}).
    Find Kodee. End your answer with a line `FOUND: <spot-id>`.
    Use your tools to explore and search.
""".trimIndent()
