package dev.peekakoog.agent.baseline

import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.Status
import dev.peekakoog.engine.Warmth

/**
 * A seeker without any AI: listen, rule out rooms, walk to the nearest room that is still possible.
 * The bar every agent level has to beat, and a way to rehearse the show without an API key.
 */
object PlainKotlinBaseline : Seeker {
    override val title = "Baseline · Plain Kotlin, no AI"
    override val idea = "A hand-written search. The bar every agent has to beat."
    override val usesLlm = false

    override suspend fun seek(game: Game, llm: Llm, onThought: (Thought) -> Unit): String = search(game)

    fun search(game: Game): String {
        val house = game.house
        val candidates = house.rooms.map { it.id }.toMutableSet()

        while (game.status == Status.SEEKING) {
            val here = game.currentRoomId
            if (candidates.size > 1) {
                game.listen()
                val warmth = (game.events.last() as? GameEvent.Listened)?.warmth ?: break
                candidates.retainAll { room ->
                    when (warmth) {
                        Warmth.HOT -> room == here
                        Warmth.WARM -> house.distance(here, room) == 1
                        Warmth.COLD -> house.distance(here, room) > 1
                    }
                }
            }
            if (here in candidates && candidates.size == 1) {
                house.spotsIn(here).forEach { spot ->
                    if (game.status == Status.SEEKING) game.peek(spot.id)
                }
                if (game.status == Status.FOUND) {
                    return "Found it by listening and checking every spot.\nFOUND: ${game.peekedSpots.last()}"
                }
                candidates -= here
            }
            val target = candidates.minByOrNull { house.distance(here, it) } ?: break
            val next = house.doorsFrom(here).minBy { house.distance(it, target) }
            if (game.status == Status.SEEKING) game.move(next)
        }
        return "I could not find it."
    }
}
