package dev.peekakoog.agent

import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent

/**
 * One version of the seeker, i.e. one level of the talk.
 *
 * Every level lives in its own package (`baseline`, `level0`, `level1`, …) and imports nothing from the other
 * levels: only this harness ([Seeker], [Llm], [Thought], [play]) and the engine. When a level builds on the
 * previous one, it copies the code on purpose, so every level reads on its own and the diff between two levels
 * is exactly what changed.
 */
interface Seeker {
    val title: String
    val idea: String

    /** False for seekers that need no LLM at all. */
    val usesLlm: Boolean get() = true

    /** True when the seeker reports what it is thinking through `onThought`. */
    val showsThoughts: Boolean get() = false

    /** Where the seeker sends its traces, if it does. */
    val traceDestination: String? get() = null

    /** Searches the [game] and returns the final answer, which should end with a line `FOUND: <spot-id>`. */
    suspend fun seek(game: Game, llm: Llm, onThought: (Thought) -> Unit): String
}

data class SeekResult(val finalAnswer: String, val claim: GameEvent.Claimed)

/** The referee: lets [seeker] play one game, then has the engine judge where it says Kodee is. */
suspend fun play(seeker: Seeker, game: Game, llm: Llm, onThought: (Thought) -> Unit = {}): SeekResult {
    val answer = seeker.seek(game, llm, onThought)
    return SeekResult(answer, game.claim(parseClaim(answer)))
}

private val claimPattern = Regex("""FOUND:\s*`?([a-z-]+)`?""")

internal fun parseClaim(answer: String): String? = claimPattern.findAll(answer).lastOrNull()?.groupValues?.get(1)
