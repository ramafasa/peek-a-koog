package dev.peekakoog.agent

import dev.peekakoog.agent.baseline.PlainKotlinBaseline
import dev.peekakoog.agent.level0.Level0JustAPrompt
import dev.peekakoog.agent.level1.Level1ToolsInTheDark
import dev.peekakoog.agent.level2.Level2LightsOn

/**
 * All seekers, in the order of the talk. Each level fixes the failure the previous one showed on stage;
 * tag the commit that finishes a level as `level-N`.
 *
 * Next levels, in the order of the talk:
 * - STATEFUL – beliefs as structured output, validated tools, heatmap
 * - GRAPH    – custom strategy graph, move budget, history compression
 * - GUARDED  – the engine, not the model, decides who won
 * - MATURE   – evals, long-term memory, persistence, model race
 */
val Seekers: List<Seeker> = listOf(
    PlainKotlinBaseline,
    Level0JustAPrompt,
    Level1ToolsInTheDark,
    Level2LightsOn,
)
