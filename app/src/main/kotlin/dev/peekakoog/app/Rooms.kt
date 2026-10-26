package dev.peekakoog.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import dev.peekakoog.engine.House

private enum class FloorMaterial { PLANKS, DARK_PLANKS, CARPET, TILES, CHECKER, STONE, GRASS }

private enum class WallPattern { STRIPES, DOTS, TILES, PLAIN, SUBWAY }

private data class RoomStyle(val floor: FloorMaterial, val floorColor: Color, val wall: WallPattern, val wallColor: Color)

private val Styles = mapOf(
    "garage" to RoomStyle(FloorMaterial.STONE, Color(0xFFC4C6C8), WallPattern.PLAIN, Color(0xFFAEB6BF)),
    "study" to RoomStyle(FloorMaterial.CARPET, Color(0xFFD9BFA6), WallPattern.STRIPES, Color(0xFF9FC3A8)),
    "laundry-room" to RoomStyle(FloorMaterial.TILES, Color(0xFFE9E3F3), WallPattern.TILES, Color(0xFFCFE3D9)),
    "bedroom" to RoomStyle(FloorMaterial.PLANKS, Color(0xFFE2BF8E), WallPattern.STRIPES, Color(0xFFCDB8EC)),
    "kids-room" to RoomStyle(FloorMaterial.CARPET, Color(0xFFBFE3D0), WallPattern.DOTS, Color(0xFFFFE08A)),
    "bathroom" to RoomStyle(FloorMaterial.TILES, Color(0xFFE6F3F7), WallPattern.TILES, Color(0xFFB9E1EE)),
    "living-room" to RoomStyle(FloorMaterial.DARK_PLANKS, Color(0xFFC99A66), WallPattern.PLAIN, Color(0xFFB9D3B0)),
    "hall" to RoomStyle(FloorMaterial.STONE, Color(0xFFD9D3CA), WallPattern.PLAIN, Color(0xFFE5D8C3)),
    "kitchen" to RoomStyle(FloorMaterial.CHECKER, Color(0xFFF3EBDD), WallPattern.SUBWAY, Color(0xFFF7F5F0)),
    "front-yard" to RoomStyle(FloorMaterial.GRASS, Color(0xFF9BCB6E), WallPattern.PLAIN, Color.Transparent),
)

private val WallCap = Color(0xFF2E2838)
private val Baseboard = Color(0xFF8A6A50)
private val Threshold = Color(0xFFC9A37A)
private val Doorway = Color(0xFF4A3F55)

/** Floors, back-wall faces and doorways. Furniture is drawn on top of this. Outdoors there are no walls. */
internal fun DrawScope.drawRoomShells(house: House, plan: FloorPlan, px: (Rect) -> Rect, u: Float) {
    house.rooms.forEach { room ->
        val style = Styles.getValue(room.id)
        val r = px(plan.roomRect(room))
        if (room.id in plan.outdoors) {
            clipRect(r.left, r.top, r.right, r.bottom) { drawFloor(style, r, u) }
            return@forEach
        }
        val wallFace = Rect(r.left, r.top, r.right, r.top + FloorPlan.WALL_FACE * u)
        val floor = Rect(r.left, wallFace.bottom, r.right, r.bottom)
        clipRect(floor.left, floor.top, floor.right, floor.bottom) { drawFloor(style, floor, u) }
        clipRect(wallFace.left, wallFace.top, wallFace.right, wallFace.bottom) { drawWallFace(style, wallFace, u) }
        // Baseboard and a soft shadow where the wall meets the floor.
        drawRect(Color(0x1A000000), Offset(floor.left, floor.top), Size(floor.width, 2.5f * u))
        drawRect(Baseboard, Offset(wallFace.left, wallFace.bottom - 1f * u), Size(wallFace.width, 1f * u))
    }

    // Doorways in the back walls of the lower rooms.
    doors(house).filterNot { (a, b) -> plan.isVerticalDoor(a, b) }.forEach { (a, b) ->
        val c = px(Rect(plan.door(a, b), Size.Zero)).topLeft
        val half = FloorPlan.DOOR / 2 * u
        val face = FloorPlan.WALL_FACE * u
        drawRect(Palette.WoodLight, Offset(c.x - half - 1.2f * u, c.y), Size(2 * half + 2.4f * u, face))
        drawRect(Doorway, Offset(c.x - half, c.y), Size(2 * half, face))
        drawRect(Color(0x33FFFFFF), Offset(c.x - half, c.y + face - 3f * u), Size(2 * half, 3f * u))
    }
}

/** The tops of the walls, with gaps for the doorways. The [frontDoor] (inside to outside) also gets an open door. */
internal fun DrawScope.drawWallCaps(house: House, plan: FloorPlan, px: (Rect) -> Rect, u: Float, frontDoor: Pair<String, String>) {
    val wall = FloorPlan.WALL * u
    house.rooms.filterNot { it.id in plan.outdoors }.forEach { room ->
        val r = px(plan.roomRect(room))
        drawRect(WallCap, r.topLeft, r.size, style = androidx.compose.ui.graphics.drawscope.Stroke(wall))
    }
    val half = FloorPlan.DOOR / 2 * u
    doors(house).forEach { (a, b) ->
        val c = px(Rect(plan.door(a, b), Size.Zero)).topLeft
        val size = if (plan.isVerticalDoor(a, b)) Size(wall + 1f, 2 * half) else Size(2 * half, wall + 1f)
        drawRect(Threshold, c - Offset(size.width / 2, size.height / 2), size)
    }
    // The front door stands open, swung out into the yard.
    val (inside, outside) = frontDoor
    val c = px(Rect(plan.door(inside, outside), Size.Zero)).topLeft
    val outwards = if (plan.roomRect(outside).left >= plan.roomRect(inside).right) 1f else -1f
    val hinge = Offset(c.x + outwards * wall / 2, c.y - half)
    val leaf = Size(2 * half * 0.9f, 2.8f * u)
    val leafLeft = if (outwards > 0) hinge.x else hinge.x - leaf.width
    drawRoundRect(Palette.WoodDark, Offset(leafLeft, hinge.y - leaf.height), leaf, CornerRadius(0.5f * u))
    drawRoundRect(Palette.Wood, Offset(leafLeft, hinge.y - leaf.height + 0.3f * u), Size(leaf.width, leaf.height * 0.5f), CornerRadius(0.4f * u))
    drawCircle(Palette.Yellow, 0.6f * u, Offset(hinge.x + outwards * (leaf.width - 1.6f * u), hinge.y - leaf.height / 2))
}

internal fun DrawScope.drawRoomLabels(house: House, plan: FloorPlan, px: (Rect) -> Rect, u: Float, textMeasurer: TextMeasurer) {
    house.rooms.forEach { room ->
        val r = px(plan.roomRect(room))
        val label = textMeasurer.measure(
            room.name,
            TextStyle(fontSize = (3.4f * u).toSp(), fontWeight = FontWeight.Bold, color = Palette.Outline),
        )
        val pad = Offset(1.6f * u, 0.6f * u)
        val size = Size(label.size.width + 2 * pad.x, label.size.height + 2 * pad.y)
        val topLeft = Offset(r.right - size.width - 3f * u, r.bottom - size.height - 2.5f * u)
        drawRoundRect(Color.White.copy(alpha = 0.85f), topLeft, size, CornerRadius(size.height / 2))
        drawText(label, topLeft = topLeft + pad)
    }
}

private fun doors(house: House): List<Pair<String, String>> =
    house.rooms.flatMap { room -> house.doorsFrom(room.id).filter { it > room.id }.map { room.id to it } }

private fun DrawScope.drawFloor(style: RoomStyle, r: Rect, u: Float) {
    drawRect(style.floorColor, r.topLeft, r.size)
    val seam = style.floorColor.darker(0.12f)
    when (style.floor) {
        FloorMaterial.PLANKS, FloorMaterial.DARK_PLANKS -> {
            val plank = 5f * u
            var row = 0
            var y = r.top
            while (y < r.bottom) {
                drawLine(seam, Offset(r.left, y), Offset(r.right, y), 0.35f * u)
                var x = r.left + (row % 3) * 9f * u
                while (x < r.right) {
                    drawLine(seam, Offset(x, y), Offset(x, y + plank), 0.35f * u)
                    x += 26f * u
                }
                if (row % 2 == 1) drawRect(Color.White.copy(alpha = 0.06f), Offset(r.left, y), Size(r.width, plank))
                y += plank
                row++
            }
        }
        FloorMaterial.CARPET -> {
            var y = r.top + 2f * u
            var row = 0
            while (y < r.bottom) {
                var x = r.left + 2f * u + (row % 2) * 2f * u
                while (x < r.right) {
                    drawCircle(seam.copy(alpha = 0.35f), 0.35f * u, Offset(x, y))
                    x += 4f * u
                }
                y += 4f * u
                row++
            }
        }
        FloorMaterial.TILES -> grid(r, 6f * u, Color.White, 0.5f * u)
        FloorMaterial.STONE -> grid(r, 13f * u, seam, 0.5f * u)
        FloorMaterial.GRASS -> {
            val blade = style.floorColor.darker(0.18f)
            var y = r.top + 3f * u
            var row = 0
            while (y < r.bottom) {
                var x = r.left + 2f * u + (row % 2) * 3.5f * u
                while (x < r.right) {
                    drawLine(blade, Offset(x, y), Offset(x - 0.6f * u, y - 1.4f * u), 0.4f * u)
                    drawLine(blade, Offset(x, y), Offset(x + 0.6f * u, y - 1.5f * u), 0.4f * u)
                    x += 7f * u
                }
                y += 4.5f * u
                row++
            }
        }
        FloorMaterial.CHECKER -> {
            val tile = 8f * u
            val other = Color(0xFFD9826B)
            var y = r.top
            var row = 0
            while (y < r.bottom) {
                var x = r.left
                var col = 0
                while (x < r.right) {
                    if ((row + col) % 2 == 0) drawRect(other, Offset(x, y), Size(tile, tile))
                    x += tile
                    col++
                }
                y += tile
                row++
            }
        }
    }
}

private fun DrawScope.drawWallFace(style: RoomStyle, r: Rect, u: Float) {
    drawRect(style.wallColor, r.topLeft, r.size)
    val accent = style.wallColor.darker(0.1f)
    when (style.wall) {
        WallPattern.STRIPES -> {
            var x = r.left + 2f * u
            while (x < r.right) {
                drawRect(accent, Offset(x, r.top), Size(1.2f * u, r.height))
                x += 5f * u
            }
        }
        WallPattern.DOTS -> {
            var y = r.top + 2.5f * u
            var row = 0
            while (y < r.bottom - 1f * u) {
                var x = r.left + 3f * u + (row % 2) * 3f * u
                while (x < r.right) {
                    drawCircle(Color.White.copy(alpha = 0.7f), 0.6f * u, Offset(x, y))
                    x += 6f * u
                }
                y += 3.5f * u
                row++
            }
        }
        WallPattern.TILES -> grid(r, 4f * u, Color.White, 0.4f * u)
        WallPattern.SUBWAY -> {
            val bh = 2.5f * u
            var y = r.top
            var row = 0
            while (y < r.bottom) {
                drawLine(accent, Offset(r.left, y), Offset(r.right, y), 0.3f * u)
                var x = r.left + (row % 2) * 2.5f * u
                while (x < r.right) {
                    drawLine(accent, Offset(x, y), Offset(x, y + bh), 0.3f * u)
                    x += 5f * u
                }
                y += bh
                row++
            }
        }
        WallPattern.PLAIN -> drawRect(accent, Offset(r.left, r.top + r.height * 0.62f), Size(r.width, 0.6f * u))
    }
}

private fun DrawScope.grid(r: Rect, step: Float, color: Color, width: Float) {
    var x = r.left
    while (x <= r.right) {
        drawLine(color, Offset(x, r.top), Offset(x, r.bottom), width)
        x += step
    }
    var y = r.top
    while (y <= r.bottom) {
        drawLine(color, Offset(r.left, y), Offset(r.right, y), width)
        y += step
    }
}

private fun Color.darker(amount: Float) = Color(red * (1 - amount), green * (1 - amount), blue * (1 - amount), alpha)
