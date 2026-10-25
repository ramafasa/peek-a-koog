package dev.peekakoog.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.peekakoog.engine.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val START_ROOM = "hall"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Peek-a-Koog",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) { PeekAKoogScreen(Houses.cozy) }
        }
    }
}

@Composable
private fun PeekAKoogScreen(house: House) {
    val llm = remember { Llm.fromEnvironment() }
    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf(Level.PROMPT_ONLY) }
    var hiddenSpotId by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SeekResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val events = remember { mutableStateListOf<GameEvent>() }

    val seekerRoomId = events.filterIsInstance<GameEvent.Moved>().lastOrNull()?.to ?: START_ROOM
    val peeked = events.filterIsInstance<GameEvent.Peeked>().map { it.spotId }.toSet()

    fun start() {
        val spot = hiddenSpotId ?: return
        events.clear()
        result = null
        error = null
        running = true
        scope.launch {
            try {
                val game = Game(house, spot, START_ROOM) { event ->
                    scope.launch(Dispatchers.Main) { events += event }
                }
                result = withContext(Dispatchers.IO) { seek(level, game, llm) }
            } catch (e: Exception) {
                error = e.message ?: e::class.simpleName
            } finally {
                running = false
            }
        }
    }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1.6f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🙈 Peek-a-Koog", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                if (hiddenSpotId == null) "Click a hiding spot to hide the toy 🧸" else "Toy hidden. Pick a level and let Koog seek!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HouseMap(
                house = house,
                seekerRoomId = seekerRoomId,
                hiddenSpotId = hiddenSpotId,
                peeked = peeked,
                onSpotClick = { if (!running) hiddenSpotId = it },
                modifier = Modifier.weight(1f),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Level", fontWeight = FontWeight.SemiBold)
            Level.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == level,
                    onClick = { if (!running) level = candidate },
                    label = { Text(candidate.title) },
                )
            }
            Text(level.idea, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("LLM: ${llm.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = ::start, enabled = hiddenSpotId != null && !running) {
                Text(if (running) "Seeking…" else "Start seeking")
            }
            EventLog(house, events, Modifier.weight(1f))
            result?.let { Verdict(it) }
            error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun HouseMap(
    house: House,
    seekerRoomId: String,
    hiddenSpotId: String?,
    peeked: Set<String>,
    onSpotClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = house.rooms.groupBy { it.row }.toSortedMap()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.values.forEach { roomsInRow ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                roomsInRow.sortedBy { it.col }.forEach { room ->
                    RoomCard(
                        room = room,
                        house = house,
                        hasSeeker = room.id == seekerRoomId,
                        hiddenSpotId = hiddenSpotId,
                        peeked = peeked,
                        onSpotClick = onSpotClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: Room,
    house: House,
    hasSeeker: Boolean,
    hiddenSpotId: String?,
    peeked: Set<String>,
    onSpotClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        border = if (hasSeeker) BorderStroke(2.dp, Color(0xFF7F52FF)) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text("${room.emoji} ${room.name}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (hasSeeker) Text("🕵️ Koog")
            }
            house.spotsIn(room.id).forEach { spot ->
                val marker = when {
                    spot.id in peeked && spot.id == hiddenSpotId -> " 🎉"
                    spot.id in peeked -> " ✗"
                    spot.id == hiddenSpotId -> " 🧸"
                    else -> ""
                }
                Text(
                    text = "${spot.emoji} ${spot.name}$marker",
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().clickable { onSpotClick(spot.id) }.padding(vertical = 2.dp),
                    color = if (spot.id in peeked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (house.doorsFrom(room.id).isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Doors: ${house.doorsFrom(room.id).joinToString { house.room(it)!!.name }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    is GameEvent.Peeked -> "🔍 peeked ${house.spot(event.spotId)!!.name.lowercase()}: ${if (event.found) "FOUND" else "nothing"}"
    is GameEvent.Listened -> "👂 listened: ${event.warmth}"
    is GameEvent.Claimed -> "📣 claimed ${event.spotId ?: "nothing"} (${if (event.correct) "correct" else "wrong"}, ${if (event.everPeekedThere) "looked there" else "never looked"})"
    is GameEvent.Rejected -> "⛔ ${event.action} rejected: ${event.reason}"
}
