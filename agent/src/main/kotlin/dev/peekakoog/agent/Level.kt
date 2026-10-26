package dev.peekakoog.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent

/**
 * Every level of the talk is one version of the seeker. Each level fixes the failure
 * the previous one showed on stage. Tag the commit that finishes a level as `level-N`.
 */
enum class Level(val title: String, val idea: String) {
    BASELINE(
        title = "Baseline · Plain Kotlin, no AI",
        idea = "A hand-written search. The bar every agent has to beat.",
    ),
    PROMPT_ONLY(
        title = "Level 0 · Just a prompt",
        idea = "No tools. The model can only talk about searching.",
    ),
    TOOLS(
        title = "Level 1 · Tools, in the dark",
        idea = "Real tools, default strategy, no observability yet.",
    ),
    OBSERVABLE(
        title = "Level 2 · Lights on",
        idea = "The same agent as Level 1, but now we can peek inside: every LLM call and tool call is streamed, " +
            "and the run is exported as an OpenTelemetry trace.",
    ),
    // Next levels, in the order of the talk:
    // STATEFUL     – beliefs as structured output, validated tools, heatmap
    // GRAPH        – custom strategy graph, move budget, history compression
    // GUARDED      – the engine, not the model, decides who won
    // MATURE       – evals, long-term memory, persistence, model race
}

data class SeekResult(val finalAnswer: String, val claim: GameEvent.Claimed)

/** Does this level let us see what the agent is thinking? */
val Level.isObservable: Boolean get() = this >= Level.OBSERVABLE

/**
 * Runs one game with the seeker of the given level.
 * From [Level.OBSERVABLE] on, [onThought] receives every LLM call and tool call as it happens.
 */
suspend fun seek(level: Level, game: Game, llm: Llm, onThought: (Thought) -> Unit = {}): SeekResult {
    if (level == Level.BASELINE) {
        val answer = baselineSeek(game)
        return SeekResult(answer, game.claim(parseClaim(answer)))
    }
    val agent = when (level) {
        Level.BASELINE -> error("Handled above")
        Level.PROMPT_ONLY -> AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = basePrompt(game),
        )
        Level.TOOLS -> AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = basePrompt(game) + "\nUse your tools to search.",
            toolRegistry = ToolRegistry.builder().tools(SeekerTools(game)).build(),
        )
        // Exactly the Level 1 agent. The only change: we can now see inside it.
        Level.OBSERVABLE -> AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = basePrompt(game) + "\nUse your tools to search.",
            toolRegistry = ToolRegistry.builder().tools(SeekerTools(game)).build(),
            installFeatures = {
                watchThoughts(onThought)
                Telemetry.install(this)
            },
        )
    }
    val answer = try {
        agent.run("Find Kodee!")
    } finally {
        // Closing the agent also flushes its OpenTelemetry spans.
        agent.close()
    }
    return SeekResult(answer, game.claim(parseClaim(answer)))
}

private fun basePrompt(game: Game): String {
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
    """.trimIndent()
}

private val claimPattern = Regex("""FOUND:\s*`?([a-z-]+)`?""")

internal fun parseClaim(answer: String): String? = claimPattern.findAll(answer).lastOrNull()?.groupValues?.get(1)
