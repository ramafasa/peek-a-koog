package dev.peekakoog.agent.level2

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import dev.peekakoog.engine.Game

/** The seeker's hands, eyes and ears. Every tool goes through the engine, which enforces the rules. */
@LLMDescription("Tools for playing hide-and-seek in a house")
class SeekerTools(private val game: Game) : ToolSet {

    @Tool
    @LLMDescription("Look around the current room: lists its hiding spots and the doors to other rooms. Free.")
    fun lookAround(): String = game.look()

    @Tool
    @LLMDescription("Walk through a door to a neighbouring room. Costs one move.")
    fun moveTo(
        @LLMDescription("Id of the neighbouring room, e.g. 'kitchen'")
        roomId: String,
    ): String = game.move(roomId)

    @Tool
    @LLMDescription("Check one hiding spot in the current room. Costs one move.")
    fun peek(
        @LLMDescription("Id of a hiding spot in the current room, e.g. 'under-bed'")
        spotId: String,
    ): String = game.peek(spotId)

    @Tool
    @LLMDescription("Listen for Kodee: HOT means this room, WARM one door away, COLD further. Costs one move.")
    fun listen(): String = game.listen()
}
