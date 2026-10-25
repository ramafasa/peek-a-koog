package dev.peekakoog.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.peekakoog.engine.GameEvent
import dev.peekakoog.engine.House
import dev.peekakoog.engine.Warmth
import kotlinx.coroutines.delay

enum class Tone { NEUTRAL, GOOD, BAD, HOT, WARM, COLD }

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

    suspend fun reset() {
        position.snapTo(plan.roomCenter(startRoomId))
        pose = Pose.IDLE
        facingLeft = false
        bubble = null
        foundSpotId = null
    }

    suspend fun play(event: GameEvent) {
        when (event) {
            is GameEvent.Moved -> {
                bubble = null
                walk(listOf(plan.roomCenter(event.from), plan.door(event.from, event.to), plan.roomCenter(event.to)))
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
                delay(700)
                bubble = when (event.warmth) {
                    Warmth.HOT -> Bubble("HOT! It's in this room 🔥", Tone.HOT)
                    Warmth.WARM -> Bubble("Warm… one door away", Tone.WARM)
                    Warmth.COLD -> Bubble("Cold ❄️", Tone.COLD)
                }
                delay(1000)
                pose = Pose.IDLE
            }
            is GameEvent.Rejected -> {
                pose = Pose.CONFUSED
                bubble = Bubble("⛔ ${event.reason}", Tone.BAD)
                delay(1600)
                pose = Pose.IDLE
            }
            is GameEvent.Claimed -> {
                pose = if (event.spotId == null) Pose.CONFUSED else Pose.CELEBRATING
                val where = event.spotId?.let { house.spot(it)?.name?.replaceFirstChar(Char::lowercase) ?: it }
                bubble = Bubble(if (where == null) "I give up 🤷" else "Kodee is $where!")
                delay(1500)
            }
        }
    }

    private suspend fun walk(points: List<Offset>) {
        pose = Pose.WALKING
        for (target in points) {
            val distance = (target - position.value).getDistance()
            if (distance < 0.5f) continue
            facingLeft = target.x < position.value.x - 0.5f || (facingLeft && target.x <= position.value.x)
            position.animateTo(target, tween((distance / SPEED * 1000).toInt(), easing = LinearEasing))
        }
        pose = Pose.IDLE
    }

    private companion object {
        /** House units per second. */
        const val SPEED = 110f
    }
}
