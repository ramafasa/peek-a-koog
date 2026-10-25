package dev.peekakoog.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.peekakoog.engine.House
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt

private val FloorColors = mapOf(
    "bedroom" to Color(0xFFEADCF8),
    "kids-room" to Color(0xFFFFEBC2),
    "bathroom" to Color(0xFFD5EEF5),
    "living-room" to Color(0xFFF4E3D0),
    "hall" to Color(0xFFE7E2DA),
    "kitchen" to Color(0xFFDDEFD5),
)
private val WallColor = Color(0xFF2E2838)
private val Doorway = Color(0xFFC9A37A)
private val Highlight = Color(0xFF7F52FF)
private val Ink = Color(0xFF2A2533)

/** Extra room around the plan, in house units: on top for speech bubbles, a little on the other sides. */
private const val MARGIN_TOP = 22f
private const val MARGIN = 4f
private const val DETECTIVE_HEIGHT = 32f
private const val KODEE_SIZE = 22f

/** Maps house units to pixels for a canvas of a given size. */
private class Projection(canvas: Size, plan: FloorPlan) {
    val u = min(canvas.width / (plan.width + 2 * MARGIN), canvas.height / (plan.height + MARGIN_TOP + MARGIN))
    val origin = Offset(
        (canvas.width - plan.width * u) / 2,
        MARGIN_TOP * u + (canvas.height - (plan.height + MARGIN_TOP + MARGIN) * u) / 2,
    )

    fun px(p: Offset) = origin + p * u
    fun px(r: Rect) = Rect(px(r.topLeft), px(r.bottomRight))
    fun units(p: Offset) = (p - origin) / u
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HouseView(
    house: House,
    plan: FloorPlan,
    director: Director,
    hiddenSpotId: String?,
    hoveredSpotId: String?,
    peeked: Set<String>,
    kodeeMood: KodeeMood,
    canHide: Boolean,
    onHover: (String?) -> Unit,
    onHide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val phase by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing)),
    )
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    fun spotUnder(pointer: Offset) = plan.spotAt(Projection(canvasSize, plan).units(pointer))

    Canvas(
        modifier
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerHoverIcon(if (canHide && hoveredSpotId != null) PointerIcon.Hand else PointerIcon.Default)
            .onPointerEvent(PointerEventType.Move) { onHover(spotUnder(it.changes.first().position)) }
            .onPointerEvent(PointerEventType.Exit) { onHover(null) }
            .onPointerEvent(PointerEventType.Press) {
                if (canHide) spotUnder(it.changes.first().position)?.let(onHide)
            },
    ) {
        val proj = Projection(size, plan)
        val u = proj.u

        drawRooms(house, plan, proj, textMeasurer)

        plan.spotLayouts.forEach { layout ->
            val r = proj.px(layout.rect)
            if (layout.spotId == hoveredSpotId && canHide) {
                drawRoundRect(Highlight.copy(alpha = 0.25f), r.inflate(2.5f * u).topLeft, r.inflate(2.5f * u).size, CornerRadius(3f * u))
            }
            drawFurniture(layout.furniture, r, u)
        }

        peeked.forEach { spotId ->
            val r = proj.px(plan.layout(spotId).rect)
            val found = spotId == director.foundSpotId
            val center = r.topLeft + Offset(1f * u, 1f * u)
            drawCircle(if (found) Color(0xFF43A047) else Color(0xFFE53935), 3f * u, center)
            drawCircle(Color.White, 3f * u, center, style = Stroke(0.6f * u))
            val d = 1.3f * u
            if (found) {
                drawLine(Color.White, center + Offset(-d, 0f), center + Offset(-0.2f * u, d), 0.8f * u)
                drawLine(Color.White, center + Offset(-0.2f * u, d), center + Offset(d * 1.2f, -d), 0.8f * u)
            } else {
                drawLine(Color.White, center + Offset(-d, -d), center + Offset(d, d), 0.8f * u)
                drawLine(Color.White, center + Offset(-d, d), center + Offset(d, -d), 0.8f * u)
            }
        }

        val kodeePoppedOut = kodeeMood == KodeeMood.SURPRISED || kodeeMood == KodeeMood.EXCITED
        if (!kodeePoppedOut) hiddenSpotId?.let { drawKodee(plan.layout(it).rect, kodeeMood, proj, alpha = 1f) }
        if (canHide && hoveredSpotId != null && hoveredSpotId != hiddenSpotId) {
            drawKodee(plan.layout(hoveredSpotId).rect, KodeeMood.WELCOMING, proj, alpha = 0.6f)
        }

        val feet = proj.px(director.position.value) + Offset(0f, DETECTIVE_HEIGHT * u * 0.25f)
        drawDetective(feet, DETECTIVE_HEIGHT * u, director.pose, director.facingLeft, phase)
        // Once found, Kodee pops out in front of the detective.
        if (kodeePoppedOut) hiddenSpotId?.let { drawKodee(plan.layout(it).rect, kodeeMood, proj, alpha = 1f) }
        director.bubble?.let { drawBubble(it, anchor = feet - Offset(0f, DETECTIVE_HEIGHT * u), u, textMeasurer) }

        if (canHide && hoveredSpotId != null) {
            val spot = house.spot(hoveredSpotId)!!
            val text = if (hoveredSpotId == hiddenSpotId) "Kodee is hiding here 🤫"
            else "Kodee will hide ${spot.name.replaceFirstChar(Char::lowercase)} · click to hide"
            val r = proj.px(plan.layout(hoveredSpotId).rect)
            drawBubble(Bubble(text), anchor = Offset(r.center.x, r.top - 1f * u), u, textMeasurer)
        }
    }
}

private fun DrawScope.drawRooms(house: House, plan: FloorPlan, proj: Projection, textMeasurer: TextMeasurer) {
    val u = proj.u
    house.rooms.forEach { room ->
        val r = proj.px(plan.roomRect(room))
        drawRect(FloorColors[room.id] ?: Color(0xFFEDEDED), r.topLeft, r.size)
        val label = textMeasurer.measure(
            room.name.uppercase(),
            TextStyle(fontSize = (2.6f * u).toSp(), fontWeight = FontWeight.Bold, color = Ink.copy(alpha = 0.45f)),
        )
        drawText(label, topLeft = Offset(r.left + 3f * u, r.bottom - 3f * u - label.size.height))
    }
    // Walls: every room outline, so shared walls overlap into one.
    house.rooms.forEach { room ->
        val r = proj.px(plan.roomRect(room))
        drawRect(WallColor, r.topLeft, r.size, style = Stroke(FloorPlan.WALL * u))
    }
    // Doorways cut through the walls.
    house.rooms.forEach { room ->
        house.doorsFrom(room.id).filter { it > room.id }.forEach { other ->
            val c = proj.px(plan.door(room.id, other))
            val half = FloorPlan.DOOR / 2 * u
            val depth = FloorPlan.WALL * u
            val size = if (plan.isVerticalDoor(room.id, other)) Size(depth + 1f, half * 2) else Size(half * 2, depth + 1f)
            drawRect(Doorway, c - Offset(size.width / 2, size.height / 2), size)
        }
    }
}

private fun DrawScope.drawKodee(spotRect: Rect, mood: KodeeMood, proj: Projection, alpha: Float) {
    val u = proj.u
    val r = proj.px(spotRect)
    val (center, sizeUnits) = when (mood) {
        // Popped out of the hiding spot, a little bigger.
        KodeeMood.SURPRISED, KodeeMood.EXCITED -> Offset(r.center.x, r.top - 4f * u) to KODEE_SIZE * 1.4f
        // Peeking out from a corner of the furniture.
        else -> Offset(r.right - 2f * u, r.top + 2f * u) to KODEE_SIZE
    }
    val side = (sizeUnits * u).roundToInt()
    val image = mood.image
    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((center.x - side / 2f).roundToInt(), (center.y - side / 2f).roundToInt()),
        dstSize = IntSize(side, side),
        alpha = alpha,
    )
}

private fun DrawScope.drawBubble(bubble: Bubble, anchor: Offset, u: Float, textMeasurer: TextMeasurer) {
    val background = when (bubble.tone) {
        Tone.NEUTRAL -> Color.White
        Tone.GOOD -> Color(0xFFC8F7C5)
        Tone.BAD -> Color(0xFFFFD6D6)
        Tone.HOT -> Color(0xFFFFB199)
        Tone.WARM -> Color(0xFFFFE29A)
        Tone.COLD -> Color(0xFFCDE8FF)
    }
    val text = textMeasurer.measure(
        bubble.text,
        TextStyle(fontSize = (4.2f * u).toSp(), color = Ink, textAlign = TextAlign.Center),
        constraints = Constraints(maxWidth = (90f * u).roundToInt()),
    )
    val pad = 2f * u
    val w = text.size.width + 2 * pad
    val h = text.size.height + 2 * pad
    val left = (anchor.x - w / 2).coerceIn(0f, size.width - w)
    val top = (anchor.y - h - 2.5f * u).coerceAtLeast(0f)
    val tail = Path().apply {
        moveTo(anchor.x - 1.5f * u, top + h - 0.5f)
        lineTo(anchor.x + 1.5f * u, top + h - 0.5f)
        lineTo(anchor.x, top + h + 2f * u)
        close()
    }
    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(left + 0.6f * u, top + 0.8f * u), Size(w, h), CornerRadius(2.5f * u))
    drawRoundRect(background, Offset(left, top), Size(w, h), CornerRadius(2.5f * u))
    drawPath(tail, background)
    drawText(text, topLeft = Offset(left + pad, top + pad))
}
