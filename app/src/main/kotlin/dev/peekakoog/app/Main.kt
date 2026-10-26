package dev.peekakoog.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.peekakoog.agent.Llm
import dev.peekakoog.agent.SeekResult
import dev.peekakoog.agent.Thought
import dev.peekakoog.agent.Seeker
import dev.peekakoog.agent.Seekers
import dev.peekakoog.agent.baseline.PlainKotlinBaseline
import dev.peekakoog.agent.play
import dev.peekakoog.engine.Game
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Houses
import dev.peekakoog.engine.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal const val START_ROOM = "hall"

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

/** One step for the detective to act out: something the engine saw, or something the agent thought. */
private sealed interface Beat {
    data class Event(val event: GameEvent) : Beat
    data class Mind(val thought: Thought) : Beat
}

/** Who is seeking Kodee: you, or one of the seekers in the agent module. */
private sealed interface Player {
    val title: String
    val idea: String

    data object Human : Player {
        override val title = "You · Human player"
        override val idea = "The engine hides Kodee. Click a room to walk there, click furniture to peek. " +
            "Every move tells you how warm you are. Every click is one of the agent's tools."
    }

    data class Agent(val seeker: Seeker) : Player {
        override val title = seeker.title
        override val idea = seeker.idea
    }
}

private val Players: List<Player> = listOf(Player.Human) + Seekers.map { Player.Agent(it) }

/**
 * The whole game screen. [random] decides where the engine hides Kodee in your games;
 * [llm] is what the agent levels think with (taken from the environment unless given).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PeekAKoogScreen(house: House, random: Random = Random.Default, llm: Llm = remember { Llm.fromEnvironment() }) {
    val plan = remember { FloorPlan.cozy(house) }
    val director = remember { Director(house, plan, START_ROOM) }
    val scope = rememberCoroutineScope()

    var player by remember { mutableStateOf<Player>(Player.Human) }
    var hovered by remember { mutableStateOf<Target?>(null) }

    // Agent games: you hide Kodee, the seeker searches.
    var hiddenSpotId by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SeekResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Human games: the engine hides Kodee, you search.
    var humanGame by remember { mutableStateOf<Game?>(null) }
    var revealed by remember { mutableStateOf(false) }

    // What the seeker did and thought but the detective hasn't acted out yet. A restart swaps in a fresh queue.
    var queue by remember { mutableStateOf(Channel<Beat>(Channel.UNLIMITED)) }
    var pending by remember { mutableIntStateOf(0) }
    // Engine events and agent thoughts already acted out, in order. This is what the audience has seen.
    val shown = remember { mutableStateListOf<GameEvent>() }
    val thoughts = remember { mutableStateListOf<Thought>() }

    val currentQueue = queue
    LaunchedEffect(currentQueue) {
        // Runs again for every new queue, after the previous one has stopped acting.
        director.reset()
        shown.clear()
        thoughts.clear()
        for (beat in currentQueue) {
            when (beat) {
                is Beat.Event -> {
                    shown += beat.event
                    director.play(beat.event)
                }
                is Beat.Mind -> {
                    thoughts += beat.thought
                    director.think(beat.thought)
                }
            }
            pending--
        }
    }

    val busy = pending > 0
    val movesSpent = shown.count { it is GameEvent.Moved || it is GameEvent.Peeked || (it is GameEvent.Listened && !it.free) }
    val peeked = shown.filterIsInstance<GameEvent.Peeked>().map { it.spotId }.toSet()

    val agentStarted = running || result != null || error != null
    val agentFinished = agentStarted && !running && !busy
    val game = humanGame
    val humanOver = game != null && game.status != Status.SEEKING && !busy
    val canAct = game != null && game.status == Status.SEEKING && !busy

    val kodeeSpotId = when (player) {
        Player.Human -> game?.hiddenSpotId?.takeIf { revealed || humanOver || director.foundSpotId != null }
        is Player.Agent -> hiddenSpotId
    }
    val kodeeMood = when {
        director.foundSpotId != null -> KodeeMood.SURPRISED
        agentFinished || humanOver -> KodeeMood.EXCITED
        else -> KodeeMood.NAUGHTY
    }

    fun clearBoard() = scope.launch {
        director.reset()
        shown.clear()
        thoughts.clear()
    }

    fun choose(candidate: Player) {
        if (busy || running) return
        player = candidate
        humanGame = null
        revealed = false
        hiddenSpotId = null
        result = null
        error = null
        clearBoard()
    }

    fun startAgent() {
        val spot = hiddenSpotId ?: return
        val seeker = (player as? Player.Agent)?.seeker ?: return
        result = null
        error = null
        running = true
        // Called from the agent's thread; hop to the UI thread, keeping the order of events and thoughts.
        fun enqueue(beat: Beat) = scope.launch(Dispatchers.Main) {
            pending++
            queue.send(beat)
        }
        scope.launch {
            director.reset()
            shown.clear()
            thoughts.clear()
            try {
                val agentGame = Game(house, spot, START_ROOM) { enqueue(Beat.Event(it)) }
                result = withContext(Dispatchers.IO) { play(seeker, agentGame, llm) { enqueue(Beat.Mind(it)) } }
            } catch (e: Exception) {
                error = e.message ?: e::class.simpleName
            } finally {
                running = false
            }
        }
    }

    /** Starts a game with a new hiding spot. Works mid-game too: what the old game still had queued is dropped. */
    fun startHumanGame() {
        queue.cancel()
        queue = Channel(Channel.UNLIMITED)
        pending = 0
        revealed = false
        // Clicks run on the UI thread, so events can go straight to the queue.
        humanGame = Game(house, house.spots.random(random).id, START_ROOM, freeHintAfterMove = true) { event ->
            pending++
            queue.trySend(Beat.Event(event))
        }
    }

    /** Your click, turned into the same engine calls the agent's tools make. */
    fun humanAct(target: Target) {
        val g = humanGame ?: return
        if (!canAct) return
        val roomId = when (target) {
            is Target.Spot -> house.spot(target.id)!!.roomId
            is Target.Room -> target.id
        }
        for (next in house.path(g.currentRoomId, roomId)) {
            g.move(next)
            if (g.status != Status.SEEKING) return
        }
        if (target is Target.Spot) g.peek(target.id)
    }

    fun humanTooltip(target: Target): String? {
        val g = humanGame ?: return null
        val roomId = when (target) {
            is Target.Spot -> house.spot(target.id)!!.roomId
            is Target.Room -> target.id
        }
        val steps = house.path(g.currentRoomId, roomId).size
        val roomName = house.room(roomId)!!.name.replaceFirstChar(Char::lowercase)
        return when (target) {
            is Target.Spot -> {
                val where = house.spot(target.id)!!.name.replaceFirstChar(Char::lowercase)
                if (steps == 0) "Peek $where · 1 move" else "Walk to the $roomName and peek $where · ${steps + 1} moves"
            }
            is Target.Room -> if (steps == 0) null else "Walk to the $roomName · ${moves(steps)}"
        }
    }

    val tooltip: String? = hovered?.let { target ->
        when (player) {
            Player.Human -> humanTooltip(target)
            is Player.Agent -> (target as? Target.Spot)?.let { spot ->
                if (spot.id == hiddenSpotId) "Kodee is hiding here 🤫"
                else "Kodee will hide ${house.spot(spot.id)!!.name.replaceFirstChar(Char::lowercase)} · click to hide"
            }
        }
    }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(2.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(KodeeLogo.image, contentDescription = "Kodee covering its eyes", modifier = Modifier.size(56.dp))
                Text("Peek-a-Koog", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                when (player) {
                    Player.Human -> when {
                        game == null -> "Press New game: the engine hides Kodee and you have to find it."
                        humanOver -> "Game over. Start a new one whenever you like."
                        else -> "Find Kodee! Click a room to walk (you get a hot/cold hint), click furniture to peek."
                    }
                    is Player.Agent -> when {
                        agentFinished -> "Game over. See the verdict, or play again."
                        agentStarted -> "Koog the detective is looking for Kodee…"
                        hiddenSpotId == null -> "Hover over the furniture and click to hide Kodee."
                        else -> "Kodee is hidden. Let Koog seek!"
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HouseView(
                house = house,
                plan = plan,
                director = director,
                kodeeSpotId = kodeeSpotId,
                kodeeMood = kodeeMood,
                peeked = peeked,
                previewSpotId = (hovered as? Target.Spot)?.id?.takeIf { player is Player.Agent && !agentStarted },
                hovered = hovered,
                tooltip = tooltip,
                interactive = when (player) {
                    Player.Human -> canAct
                    is Player.Agent -> !agentStarted
                },
                onHover = { target ->
                    // While hiding, only furniture can be pointed at.
                    hovered = if (player is Player.Agent) target as? Target.Spot else target
                },
                onClick = { target ->
                    when (player) {
                        Player.Human -> humanAct(target)
                        is Player.Agent -> (target as? Target.Spot)?.let { hiddenSpotId = it.id }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("house"),
            )
            Text(
                "Kodee by JetBrains s.r.o. is licensed under CC BY 4.0",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Who seeks?", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Players.forEach { candidate ->
                    FilterChip(
                        selected = candidate == player,
                        onClick = { choose(candidate) },
                        label = { Text(candidate.title) },
                    )
                }
            }
            Text(player.idea, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (val current = player) {
                Player.Human -> {
                    if (game == null || humanOver) {
                        Button(onClick = ::startHumanGame) { Text("New game") }
                    } else {
                        OutlinedButton(onClick = ::startHumanGame) { Text("🔄 Restart") }
                    }
                    if (game != null && !humanOver) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { humanGame?.listen() }, enabled = canAct) { Text("👂 Listen · 1 move") }
                            OutlinedButton(onClick = { revealed = !revealed }) {
                                Text(if (revealed) "🙈 Hide Kodee" else "👁 Show Kodee")
                            }
                        }
                    }
                    if (game != null) {
                        Text(
                            "Moves left: ${game.moveBudget - movesSpent} of ${game.moveBudget}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                is Player.Agent -> {
                    if (current.seeker.usesLlm) {
                        Text("LLM: ${llm.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    current.seeker.traceDestination?.let { destination ->
                        Text("Trace: $destination", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (agentFinished) {
                        OutlinedButton(onClick = { choose(current) }) { Text("Play again") }
                    } else {
                        Button(onClick = ::startAgent, enabled = hiddenSpotId != null && !agentStarted) {
                            Text(if (agentStarted) "Seeking…" else "Start seeking")
                        }
                    }
                }
            }

            if ((player as? Player.Agent)?.seeker?.showsThoughts == true) {
                ThoughtLog(thoughts, Modifier.weight(1.4f))
            }
            EventLog(house, shown, Modifier.weight(1f))
            if (humanOver && game != null) HumanVerdict(house, game, movesSpent)
            if (agentFinished) result?.let { Verdict(it) }
            error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun moves(n: Int) = if (n == 1) "1 move" else "$n moves"

@Composable
private fun HumanVerdict(house: House, game: Game, movesSpent: Int) {
    val where = house.spot(game.hiddenSpotId)!!.name.replaceFirstChar(Char::lowercase)
    // How many moves the plain-Kotlin baseline needs for the same spot.
    val baseline = remember(game) {
        Game(house, game.hiddenSpotId, START_ROOM).also { PlainKotlinBaseline.search(it) }.let { it.moveBudget - it.movesLeft }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (game.status == Status.FOUND) {
                Text("🎉 You found Kodee in ${moves(movesSpent)}!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            } else {
                Text("😵 Out of moves!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text("Kodee was $where.", fontSize = 13.sp)
            Text(
                "The no-AI baseline needs ${moves(baseline)} for this spot.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    is GameEvent.Listened -> if (event.free) "👂 hint: ${event.warmth} (free)" else "👂 listened: ${event.warmth}"
    is GameEvent.Claimed -> "📣 claimed ${event.spotId ?: "nothing"} (${if (event.correct) "correct" else "wrong"}, ${if (event.everPeekedThere) "looked there" else "never looked"})"
    is GameEvent.Rejected -> "⛔ ${event.action} rejected: ${event.reason}"
}
