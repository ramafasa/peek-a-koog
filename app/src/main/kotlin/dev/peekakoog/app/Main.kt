package dev.peekakoog.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.peekakoog.agent.Level
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.SeekResult
import dev.peekakoog.agent.seek
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Houses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val START_ROOM = "hall"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Peek-a-Koog",
        icon = BitmapPainter(KodeeLogo.image),
        state = rememberWindowState(width = 1400.dp, height = 860.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) { PeekAKoogScreen(Houses.cozy) }
        }
    }
}

@Composable
private fun PeekAKoogScreen(house: House) {
    val llm = remember { Llm.fromEnvironment() }
    val plan = remember { FloorPlan.cozy(house) }
    val director = remember { Director(house, plan, START_ROOM) }
    val scope = rememberCoroutineScope()

    var level by remember { mutableStateOf(Level.PROMPT_ONLY) }
    var hiddenSpotId by remember { mutableStateOf<String?>(null) }
    var hoveredSpotId by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SeekResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Events the agent produced but the detective hasn't acted out yet.
    val queue = remember { Channel<GameEvent>(Channel.UNLIMITED) }
    var pending by remember { mutableIntStateOf(0) }
    // Events already acted out, in order. This is what the audience has seen.
    val shown = remember { mutableStateListOf<GameEvent>() }

    LaunchedEffect(Unit) {
        for (event in queue) {
            shown += event
            director.play(event)
            pending--
        }
    }

    val started = running || result != null || error != null
    val finished = started && !running && pending == 0
    val peeked = shown.filterIsInstance<GameEvent.Peeked>().map { it.spotId }.toSet()
    val kodeeMood = when {
        director.foundSpotId != null -> KodeeMood.SURPRISED
        finished -> KodeeMood.EXCITED
        else -> KodeeMood.NAUGHTY
    }

    fun start() {
        val spot = hiddenSpotId ?: return
        result = null
        error = null
        running = true
        scope.launch {
            director.reset()
            shown.clear()
            try {
                val game = Game(house, spot, START_ROOM) { event ->
                    // Called from the agent's thread; hop to the UI thread, keeping the order.
                    scope.launch(Dispatchers.Main) {
                        pending++
                        queue.send(event)
                    }
                }
                result = withContext(Dispatchers.IO) { seek(level, game, llm) }
            } catch (e: Exception) {
                error = e.message ?: e::class.simpleName
            } finally {
                running = false
            }
        }
    }

    fun playAgain() {
        scope.launch {
            director.reset()
            shown.clear()
            result = null
            error = null
            hiddenSpotId = null
        }
    }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(2.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(KodeeLogo.image, contentDescription = "Kodee covering its eyes", modifier = Modifier.size(56.dp))
                Text("Peek-a-Koog", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                when {
                    started -> "Koog the detective is looking for Kodee…"
                    hiddenSpotId == null -> "Hover over the furniture and click to hide Kodee."
                    else -> "Kodee is hidden. Pick a level and let Koog seek!"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HouseView(
                house = house,
                plan = plan,
                director = director,
                hiddenSpotId = hiddenSpotId,
                hoveredSpotId = hoveredSpotId,
                peeked = peeked,
                kodeeMood = kodeeMood,
                canHide = !started,
                onHover = { hoveredSpotId = it },
                onHide = { hiddenSpotId = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                "Kodee by JetBrains s.r.o. is licensed under CC BY 4.0",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Level", fontWeight = FontWeight.SemiBold)
            Level.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == level,
                    onClick = { if (!started) level = candidate },
                    label = { Text(candidate.title) },
                )
            }
            Text(level.idea, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("LLM: ${llm.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (finished) {
                OutlinedButton(onClick = ::playAgain) { Text("Play again") }
            } else {
                Button(onClick = ::start, enabled = hiddenSpotId != null && !started) {
                    Text(if (started) "Seeking…" else "Start seeking")
                }
            }
            EventLog(house, shown, Modifier.weight(1f))
            if (finished) result?.let { Verdict(it) }
            error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EventLog(house: House, events: List<GameEvent>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(events.size) { if (events.isNotEmpty()) listState.animateScrollToItem(events.lastIndex) }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("What the engine saw", fontWeight = FontWeight.SemiBold)
            if (events.isEmpty()) Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(state = listState) {
                items(events) { event ->
                    Text(describe(house, event), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Verdict(result: SeekResult) {
    val claim = result.claim
    val headline = when {
        claim.spotId == null -> "🤷 No answer"
        claim.correct && claim.everPeekedThere -> "✅ Found it for real"
        claim.correct -> "🍀 Lucky guess: it never looked there"
        else -> "👻 Boo! It made that up"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(headline, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Claimed: ${claim.spotId ?: "–"}", fontSize = 13.sp)
            SelectionContainer {
                Text(result.finalAnswer, fontSize = 13.sp, maxLines = 8, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun describe(house: House, event: GameEvent): String = when (event) {
    is GameEvent.Moved -> "🚶 ${event.from} → ${event.to}"
    is GameEvent.Looked -> "👀 looked around ${event.roomId}"
    is GameEvent.Peeked -> "🔍 peeked ${house.spot(event.spotId)!!.name.replaceFirstChar(Char::lowercase)}: ${if (event.found) "FOUND" else "nothing"}"
    is GameEvent.Listened -> "👂 listened: ${event.warmth}"
    is GameEvent.Claimed -> "📣 claimed ${event.spotId ?: "nothing"} (${if (event.correct) "correct" else "wrong"}, ${if (event.everPeekedThere) "looked there" else "never looked"})"
    is GameEvent.Rejected -> "⛔ ${event.action} rejected: ${event.reason}"
}
