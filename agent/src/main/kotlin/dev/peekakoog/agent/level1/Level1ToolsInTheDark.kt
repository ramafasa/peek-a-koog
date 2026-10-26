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
    override val idea = "Real tools, no map, default strategy, no observability yet."

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
