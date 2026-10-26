package dev.peekakoog.agent.level1

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.Game

/** Level 1: real tools and Koog's default strategy. It plays, but we can't see how. */
object Level1ToolsInTheDark : Seeker {
    override val title = "Level 1 · Tools, in the dark"
    override val idea = "Real tools, default strategy, no observability yet."

    override suspend fun seek(game: Game, llm: Llm, onThought: (Thought) -> Unit): String {
        val agent = AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = systemPrompt(game),
            toolRegistry = ToolRegistry.builder().tools(SeekerTools(game)).build(),
        )
        return try {
            agent.run("Find Kodee!")
        } finally {
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
