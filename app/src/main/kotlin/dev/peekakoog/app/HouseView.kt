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

private val Highlight = Color(0xFF7F52FF)
private val Ink = Color(0xFF2A2533)

/** Extra room around the plan, in house units: on top for speech bubbles, a little on the other sides. */
private const val MARGIN_TOP = 22f
private const val MARGIN = 4f
private const val DETECTIVE_HEIGHT = 32f
private const val KODEE_SIZE = 22f

/** Maps house units to pixels for a canvas of a given size. */
internal class Projection(canvas: Size, plan: FloorPlan) {
    val u = min(canvas.width / (plan.width + 2 * MARGIN), canvas.height / (plan.height + MARGIN_TOP + MARGIN))
    val origin = Offset(
        (canvas.width - plan.width * u) / 2,
        MARGIN_TOP * u + (canvas.height - (plan.height + MARGIN_TOP + MARGIN) * u) / 2,
    )

    fun px(p: Offset) = origin + p * u
    fun px(r: Rect) = Rect(px(r.topLeft), px(r.bottomRight))
    fun units(p: Offset) = (p - origin) / u
}

/** Something on the plan the pointer can point at. */
sealed interface Target {
    data class Spot(val id: String) : Target
    data class Room(val id: String) : Target
}

/**
 * The house with the detective and Kodee.
 *
 * @param kodeeSpotId where Kodee is drawn, or null to keep it invisible.
 * @param previewSpotId where a see-through Kodee previews a hiding spot.
 * @param tooltip text shown next to the [hovered] target.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HouseView(
    house: House,
    plan: FloorPlan,
    director: Director,
    kodeeSpotId: String?,
    kodeeMood: KodeeMood,
    peeked: Set<String>,
    modifier: Modifier = Modifier,
    previewSpotId: String? = null,
    hovered: Target? = null,
    tooltip: String? = null,
    interactive: Boolean = false,
    onHover: (Target?) -> Unit = {},
    onClick: (Target) -> Unit = {},
) {
    val textMeasurer = rememberTextMeasurer()
    val phase by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing)),
    )
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    fun targetAt(pointer: Offset): Target? {
        val point = Projection(canvasSize, plan).units(pointer)
        return plan.spotAt(point)?.let { Target.Spot(it) } ?: plan.roomAt(point)?.let { Target.Room(it) }
    }

    Canvas(
        modifier
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerHoverIcon(if (interactive && hovered != null) PointerIcon.Hand else PointerIcon.Default)
            .onPointerEvent(PointerEventType.Move) { if (interactive) onHover(targetAt(it.changes.first().position)) }
            .onPointerEvent(PointerEventType.Exit) { onHover(null) }
            .onPointerEvent(PointerEventType.Press) {
                if (interactive) targetAt(it.changes.first().position)?.let(onClick)
            },
    ) {
        val proj = Projection(size, plan)
        val u = proj.u
        val hoveredSpotId = (hovered as? Target.Spot)?.id?.takeIf { interactive }
        val hoveredRoomId = (hovered as? Target.Room)?.id?.takeIf { interactive }

        val px: (Rect) -> Rect = proj::px
        drawRoomShells(house, plan, px, u)
        hoveredRoomId?.let { roomId ->
            val r = proj.px(plan.roomRect(roomId))
            drawRect(Highlight.copy(alpha = 0.14f), r.topLeft, r.size)
        }
        plan.decor.filter { it.decor.onFloor }.forEach { drawDecor(it.decor, proj.px(it.rect), u) }
        drawWallCaps(house, plan, px, u, frontDoorRoomId = "hall")
        hoveredRoomId?.let { roomId ->
            val r = proj.px(plan.roomRect(roomId)).deflate(FloorPlan.WALL * u)
            drawRect(Highlight, r.topLeft, r.size, style = Stroke(0.8f * u))
        }

        val kodeePoppedOut = kodeeMood == KodeeMood.SURPRISED || kodeeMood == KodeeMood.EXCITED

        // Painter's order: things lower on screen are in front.
        val items = plan.spotLayouts.map { it.rect.bottom to it } + plan.decor.filterNot { it.decor.onFloor }.map { it.rect.bottom to it }
        items.sortedBy { it.first }.forEach { (_, item) ->
            when (item) {
                is SpotLayout -> {
                    // A hiding Kodee is drawn just before its furniture, so it peeks out from behind it.
                    if (item.spotId == kodeeSpotId && !kodeePoppedOut) drawKodee(item, plan, kodeeMood, proj, alpha = 1f)
                    if (item.spotId == previewSpotId && previewSpotId != kodeeSpotId) drawKodee(item, plan, KodeeMood.WELCOMING, proj, alpha = 0.7f)
                    val r = proj.px(item.rect)
                    if (item.spotId == hoveredSpotId) {
                        drawRoundRect(Highlight.copy(alpha = 0.35f), r.inflate(2f * u).topLeft, r.inflate(2f * u).size, CornerRadius(3f * u))
                    }
                    drawFurniture(item.furniture, r, u)
                }
                is DecorLayout -> drawDecor(item.decor, proj.px(item.rect), u)
            }
        }
        drawRoomLabels(house, plan, px, u, textMeasurer)

        peeked.forEach { spotId ->
            val r = proj.px(plan.layout(spotId).rect)
            val found = spotId == director.foundSpotId
            val center = r.topLeft + Offset(2f * u, 4f * u)
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

        val feet = proj.px(director.position.value) + Offset(0f, DETECTIVE_HEIGHT * u * 0.25f)
        drawDetective(feet, DETECTIVE_HEIGHT * u, director.pose, director.facingLeft, phase)
        // Once found, Kodee pops out in front of the detective.
        if (kodeePoppedOut) kodeeSpotId?.let { drawKodee(plan.layout(it), plan, kodeeMood, proj, alpha = 1f) }
        director.bubble?.let { drawBubble(it, anchor = feet - Offset(0f, DETECTIVE_HEIGHT * u), u, textMeasurer) }

        if (tooltip != null && hovered != null && interactive) {
            val anchor = when (hovered) {
                is Target.Spot -> proj.px(plan.layout(hovered.id).rect).let { Offset(it.center.x, it.top - 1f * u) }
                is Target.Room -> proj.px(plan.roomRect(hovered.id)).let { Offset(it.center.x, it.top + 24f * u) }
            }
            drawBubble(Bubble(tooltip), anchor = anchor, u, textMeasurer)
        }
    }
}

private fun DrawScope.drawKodee(layout: SpotLayout, plan: FloorPlan, mood: KodeeMood, proj: Projection, alpha: Float) {
    val u = proj.u
    val r = proj.px(layout.rect)
    val room = proj.px(plan.roomRect(plan.roomOf(layout.spotId)))
    // Kodee appears on the side of the furniture that faces the middle of the room.
    val towardsLeft = r.center.x > room.center.x
    fun beside(outwards: Float) = if (towardsLeft) r.left - outwards else r.right + outwards
    val (center, sizeUnits) = when (mood) {
        // Jumped out next to the hiding spot, a little bigger, so it never covers the detective.
        KodeeMood.SURPRISED, KodeeMood.EXCITED ->
            Offset(beside(KODEE_SIZE * 1.4f * u * 0.45f), r.top + r.height * 0.35f) to KODEE_SIZE * 1.4f
        // Peeking out from behind the furniture.
        else -> Offset(beside(KODEE_SIZE * u * 0.22f), r.top + r.height * 0.45f) to KODEE_SIZE
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
        Tone.THOUGHT -> Color(0xFFE8DEFF)
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
