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
    PROMPT_ONLY(
        title = "Level 0 · Just a prompt",
        idea = "No tools. The model can only talk about searching.",
    ),
    TOOLS(
        title = "Level 1 · Tools, in the dark",
        idea = "Real tools, default strategy, no observability yet.",
    ),
    // Next levels, in the order of the talk:
    // OBSERVABLE   – event handlers, tracing, OpenTelemetry
    // STATEFUL     – beliefs as structured output, validated tools, heatmap
    // GRAPH        – custom strategy graph, move budget, history compression
    // GUARDED      – the engine, not the model, decides who won
    // MATURE       – evals, long-term memory, persistence, model race
}

data class SeekResult(val finalAnswer: String, val claim: GameEvent.Claimed)

/** Runs one game with the seeker of the given level. */
suspend fun seek(level: Level, game: Game, llm: Llm): SeekResult {
    val agent = when (level) {
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
    }
    val answer = agent.run("Find the toy!")
    return SeekResult(answer, game.claim(parseClaim(answer)))
}

private fun basePrompt(game: Game): String {
    val house = game.house
    val layout = house.rooms.joinToString("\n") { room ->
        val spots = house.spotsIn(room.id).joinToString(", ") { it.id }
        "- ${room.id} (doors to: ${house.doorsFrom(room.id).joinToString(", ")}): $spots"
    }
    return """
        You are playing hide-and-seek. Someone hid a toy in one of the hiding spots of this house:
        $layout
        You start in the ${game.currentRoomId}.
        Find the toy. End your answer with a line `FOUND: <spot-id>`.
    """.trimIndent()
}

private val claimPattern = Regex("""FOUND:\s*`?([a-z-]+)`?""")

internal fun parseClaim(answer: String): String? = claimPattern.findAll(answer).lastOrNull()?.groupValues?.get(1)
