package dev.peekakoog.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.peekakoog.agent.Thought
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Warmth
import kotlinx.coroutines.delay

enum class Tone { NEUTRAL, GOOD, BAD, HOT, WARM, COLD, THOUGHT }

data class Bubble(val text: String, val tone: Tone = Tone.NEUTRAL)

/**
 * Turns engine events into the detective's choreography: walking through doorways, stopping at furniture,
 * peeking, listening. Events are played one by one, so the audience can follow even when the agent is fast.
 */
class Director(private val house: House, private val plan: FloorPlan, private val startRoomId: String) {
    val position = Animatable(plan.roomCenter(startRoomId), Offset.VectorConverter)
    var pose by mutableStateOf(Pose.IDLE)
        private set
    var facingLeft by mutableStateOf(false)
        private set
    var bubble by mutableStateOf<Bubble?>(null)
        private set
    var foundSpotId by mutableStateOf<String?>(null)
        private set

    private val _route = mutableStateListOf(plan.roomCenter(startRoomId))

    /** Waypoints the detective has really walked through: the footprints on the floor. */
    val route: List<Offset> get() = _route

    /** The waypoint the detective is walking to right now, leaving footprints on the way. */
    var walkingTo by mutableStateOf<Offset?>(null)
        private set

    private var roomId = startRoomId

    suspend fun reset() {
        position.snapTo(plan.roomCenter(startRoomId))
        roomId = startRoomId
        pose = Pose.IDLE
        facingLeft = false
        bubble = null
        foundSpotId = null
        _route.clear()
        _route += plan.roomCenter(startRoomId)
        walkingTo = null
    }

    suspend fun play(event: GameEvent) {
        when (event) {
            is GameEvent.Moved -> {
                bubble = null
                walk(plan.walk(event.from, event.to))
                roomId = event.to
            }
            is GameEvent.Looked -> {
                pose = Pose.IDLE
                bubble = Bubble("Let me look around…")
                repeat(2) {
                    facingLeft = !facingLeft
                    delay(350)
                }
                delay(300)
            }
            is GameEvent.Peeked -> {
                val spot = house.spot(event.spotId)!!
                bubble = null
                walk(listOf(plan.layout(event.spotId).stand))
                pose = Pose.PEEKING
                facingLeft = plan.layout(event.spotId).rect.center.x < position.value.x
                bubble = Bubble("${spot.name}?")
                delay(800)
                if (event.found) {
                    foundSpotId = event.spotId
                    pose = Pose.CELEBRATING
                    bubble = Bubble("Found Kodee! 🎉", Tone.GOOD)
                    delay(1500)
                } else {
                    bubble = Bubble("Nothing here.", Tone.BAD)
                    delay(700)
                    pose = Pose.IDLE
                }
            }
            is GameEvent.Listened -> {
                pose = Pose.LISTENING
                bubble = Bubble("Listening…")
                // A free hint after a move is quicker, so walking through rooms doesn't drag.
                delay(if (event.free) 250 else 700)
                bubble = when (event.warmth) {
                    Warmth.HOT -> Bubble("HOT! It's in this room 🔥", Tone.HOT)
                    Warmth.WARM -> Bubble("Warm… one door away", Tone.WARM)
                    Warmth.COLD -> Bubble("Cold ❄️", Tone.COLD)
                }
                delay(if (event.free) 800 else 1000)
                pose = Pose.IDLE
            }
            is GameEvent.Rejected -> {
                pose = Pose.CONFUSED
                bubble = Bubble("⛔ ${event.reason}", Tone.BAD)
                delay(1600)
                pose = Pose.IDLE
            }
            is GameEvent.Claimed -> {
                val claimed = event.spotId?.let(house::spot)
                if (claimed != null && !event.everPeekedThere) {
                    // The agent names a spot it never looked at: act out the confident march there.
                    // Nothing is checked on the way, so no ✗ or ✓ appears, and it leaves no footprints.
                    bubble = Bubble("I know where Kodee is!")
                    walkToRoom(claimed.roomId)
                    walk(listOf(plan.layout(claimed.id).stand), footprints = false)
                    facingLeft = plan.layout(claimed.id).rect.center.x < position.value.x
                }
                pose = if (event.spotId == null) Pose.CONFUSED else Pose.CELEBRATING
                val where = event.spotId?.let { house.spot(it)?.name?.replaceFirstChar(Char::lowercase) ?: it }
                bubble = Bubble(if (where == null) "I give up 🤷" else "Kodee is $where!")
                delay(1500)
            }
        }
    }

    /** Walks door by door along a shortest route to [target]. Only imagined walks use it, so no footprints. */
    private suspend fun walkToRoom(target: String) {
        house.path(roomId, target).forEach { next ->
            walk(plan.walk(roomId, next), footprints = false)
            roomId = next
        }
    }

    /** The agent thinking out loud: what the LLM writes between tool calls shows up as a thought bubble. */
    suspend fun think(thought: Thought) {
        when (thought) {
            is Thought.Asking -> {
                pose = Pose.IDLE
                bubble = Bubble("🤔 …", Tone.THOUGHT)
                delay(300)
            }
            is Thought.Replied -> thought.text?.let { text ->
                bubble = Bubble(text.lines().first { it.isNotBlank() }.take(MAX_THOUGHT), Tone.THOUGHT)
                delay(1400)
            }
            is Thought.ToolFailed -> {
                pose = Pose.CONFUSED
                bubble = Bubble("⚠️ ${thought.message.take(MAX_THOUGHT)}", Tone.BAD)
                delay(1400)
                pose = Pose.IDLE
            }
            else -> Unit
        }
    }

    private suspend fun walk(points: List<Offset>, footprints: Boolean = true) {
        pose = Pose.WALKING
        for (target in points) {
            val distance = (target - position.value).getDistance()
            if (distance < 0.5f) continue
            facingLeft = target.x < position.value.x - 0.5f || (facingLeft && target.x <= position.value.x)
            if (footprints) walkingTo = target
            position.animateTo(target, tween((distance / SPEED * 1000).toInt(), easing = LinearEasing))
            if (footprints) _route += target
            walkingTo = null
        }
        pose = Pose.IDLE
    }

    private companion object {
        /** House units per second. */
        const val SPEED = 110f
        const val MAX_THOUGHT = 90
    }
}
