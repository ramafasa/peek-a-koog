package dev.peekakoog.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.peekakoog.agent.Thought

/** "Peek" inside the agent: every LLM call and tool call, as Koog's event handlers report them. */
@Composable
fun ThoughtLog(thoughts: List<Thought>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(thoughts.size) { if (thoughts.isNotEmpty()) listState.animateScrollToItem(thoughts.lastIndex) }
    val totals = totals(thoughts)
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            PanelTitle("What Koog is thinking") { thoughtsAsText(thoughts) }
            totals?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (thoughts.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer {
                LazyColumn(state = listState) {
                    items(thoughts) { thought ->
                        describe(thought)?.let { (text, color) ->
                            Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color ?: Color.Unspecified)
                        }
                    }
                }
            }
        }
    }
}

private fun totals(thoughts: List<Thought>): String? {
    val replies = thoughts.filterIsInstance<Thought.Replied>()
    if (replies.isEmpty()) return null
    val input = replies.sumOf { it.inputTokens ?: 0 }
    val output = replies.sumOf { it.outputTokens ?: 0 }
    val seconds = replies.sumOf { it.millis } / 1000.0
    return "${replies.size} LLM calls · ${"%,d".format(input)} tokens in · ${"%,d".format(output)} out · ${"%.1f".format(seconds)} s"
}

/** Everything in the panel, with nothing shortened: what the Copy button copies. */
internal fun thoughtsAsText(thoughts: List<Thought>): String = buildString {
    appendLine("What Koog is thinking")
    totals(thoughts)?.let { appendLine(it) }
    thoughts.forEach { thought -> describe(thought, full = true)?.let { appendLine(it.first) } }
}.trimEnd()

private val Speech = Color(0xFFB9A4FF)
private val Problem = Color(0xFFFF8A80)
private val Quiet = Color(0xFF9E9AA8)

/** One line (or a few) per thought. On screen long texts are shortened; [full] keeps them whole. */
private fun describe(thought: Thought, full: Boolean = false): Pair<String, Color?>? {
    fun String.clip(max: Int) = if (full || length <= max) this else take(max) + "…"
    return when (thought) {
        is Thought.Asking -> "🧠 LLM call #${thought.call} → ${thought.model}" to Quiet
        is Thought.Replied -> buildString {
            thought.reasoning?.let { appendLine("🤔 ${it.clip(240)}") }
            thought.text?.let { appendLine("💬 ${it.clip(400)}") }
            thought.toolCalls.forEach { appendLine("➡ decides: $it") }
            append("   ⏱ ${"%.1f".format(thought.millis / 1000.0)} s")
            if (thought.inputTokens != null || thought.outputTokens != null) {
                append(" · ${thought.inputTokens ?: "?"} in / ${thought.outputTokens ?: "?"} out tokens")
            }
        } to Speech
        // The decision is already shown in the reply; the engine's answer is what matters here.
        is Thought.UsingTool -> null
        is Thought.ToolReturned -> "↩ ${thought.tool}: ${thought.result.clip(200)}" to null
        is Thought.ToolFailed -> "⚠ ${thought.tool} failed: ${thought.message.clip(200)}" to Problem
        is Thought.Failed -> "💥 agent failed: ${thought.message.clip(300)}" to Problem
    }
}
