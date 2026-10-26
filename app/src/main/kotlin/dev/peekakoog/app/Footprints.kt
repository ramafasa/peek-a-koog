package dev.peekakoog.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/** Brown the first time along a stretch, then warmer and warmer each time the detective walks it again. */
private val Lanes = listOf(Color(0xB33A2A20), Color(0xD9F57C00), Color(0xE6C62828))

private const val STRIDE = 5f
private const val LANE_GAP = 3.4f

/**
 * Footprints along the [Director]'s route, up to where the detective is now. Every time a stretch is
 * walked again, its footprints get a lane of their own next to the earlier ones, in a warmer colour,
 * so walking back and forth shows on the floor.
 */
internal fun DrawScope.drawFootprints(director: Director, proj: Projection) {
    val walked = mutableMapOf<Set<Pair<Int, Int>>, Int>()
    fun stretch(from: Offset, to: Offset, upTo: Offset = to) {
        val length = (to - from).getDistance()
        if (length < 0.5f) return
        val key = setOf(from.key(), to.key())
        val lane = walked.getOrDefault(key, 0)
        walked[key] = lane + 1
        drawLane(from, to, lane, until = (upTo - from).getDistance(), proj)
    }
    director.route.zipWithNext().forEach { (from, to) -> stretch(from, to) }
    director.walkingTo?.let { target -> stretch(director.route.last(), target, upTo = director.position.value) }
}

private fun DrawScope.drawLane(from: Offset, to: Offset, lane: Int, until: Float, proj: Projection) {
    val u = proj.u
    val length = (to - from).getDistance()
    val direction = (to - from) / length
    // Lanes stack on the same side of a stretch whichever way it is walked.
    val forwards = compareValuesBy(from, to, { it.x.roundToInt() }, { it.y.roundToInt() }) <= 0
    val normal = Offset(-direction.y, direction.x) * (if (forwards) 1f else -1f)
    val color = Lanes[lane.coerceAtMost(Lanes.lastIndex)]
    val degrees = atan2(direction.y, direction.x) * 180f / PI.toFloat() + 90f
    var travelled = STRIDE / 2
    var left = true
    while (travelled < until) {
        val foot = from + direction * travelled + normal * (LANE_GAP * lane + if (left) -0.9f else 0.9f)
        val center = proj.px(foot)
        rotate(degrees, center) {
            drawOval(Color.White.copy(alpha = 0.55f), center - Offset(0.95f * u, 1.45f * u), Size(1.9f * u, 2.9f * u))
            drawOval(color, center - Offset(0.7f * u, 1.2f * u), Size(1.4f * u, 2.4f * u))
        }
        left = !left
        travelled += STRIDE
    }
}

private fun Offset.key() = x.roundToInt() to y.roundToInt()
