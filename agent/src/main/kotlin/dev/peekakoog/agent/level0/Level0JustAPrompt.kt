package dev.peekakoog.agent.level0

import ai.koog.agents.core.agent.AIAgent
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.Game

/** Level 0: just a prompt. The model knows the house, but has no way to look anywhere. */
object Level0JustAPrompt : Seeker {
    override val title = "Level 0 · Just a prompt"
    override val idea = "No tools. The model can only talk about searching."

    override suspend fun seek(game: Game, llm: Llm, onThought: (Thought) -> Unit): String {
        val agent = AIAgent(
            promptExecutor = llm.executor,
            llmModel = llm.model,
            systemPrompt = systemPrompt(game),
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
    """.trimIndent()
}
