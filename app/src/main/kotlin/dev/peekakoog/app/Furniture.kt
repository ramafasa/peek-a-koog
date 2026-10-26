package dev.peekakoog.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal object Palette {
    val Outline = Color(0xFF3A3346)
    val Shadow = Color(0x2E000000)
    val Wood = Color(0xFFB98352)
    val WoodLight = Color(0xFFD4A574)
    val WoodDark = Color(0xFF8A5A33)
    val White = Color(0xFFF8F6F2)
    val WhiteShade = Color(0xFFDCD8D0)
    val Metal = Color(0xFFB8C2CC)
    val MetalDark = Color(0xFF8E99A4)
    val Glass = Color(0xFFBFE3F2)
    val Water = Color(0xFF9FD6EE)
    val Dark = Color(0xFF2B2734)
    val Red = Color(0xFFE05252)
    val Orange = Color(0xFFFF9A4D)
    val Yellow = Color(0xFFFFD34D)
    val Green = Color(0xFF5DAE5B)
    val Blue = Color(0xFF4F8FE0)
    val Purple = Color(0xFF7F52FF)
    val Pink = Color(0xFFE77FA0)
}

/**
 * Draws in a piece of furniture's own coordinates: (0, 0) is the top-left of its rect, in house units.
 * Furniture is drawn in a 3/4 view: a top surface and, below it, a front face.
 */
internal class Sketch(private val ds: DrawScope, private val r: Rect, val u: Float) {
    val w = r.width / u
    val h = r.height / u
    private val line = Stroke(width = 0.45f * u)

    fun o(x: Float, y: Float) = Offset(r.left + x * u, r.top + y * u)

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Color, radius: Float = 0.8f, outline: Boolean = true) {
        ds.drawRoundRect(color, o(x, y), Size(w * u, h * u), CornerRadius(radius * u))
        if (outline) ds.drawRoundRect(Palette.Outline, o(x, y), Size(w * u, h * u), CornerRadius(radius * u), style = line)
    }

    fun oval(x: Float, y: Float, w: Float, h: Float, color: Color, outline: Boolean = true) {
        ds.drawOval(color, o(x, y), Size(w * u, h * u))
        if (outline) ds.drawOval(Palette.Outline, o(x, y), Size(w * u, h * u), style = line)
    }

    fun circle(cx: Float, cy: Float, radius: Float, color: Color, outline: Boolean = true) {
        ds.drawCircle(color, radius * u, o(cx, cy))
        if (outline) ds.drawCircle(Palette.Outline, radius * u, o(cx, cy), style = line)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = Palette.Outline, width: Float = 0.45f) =
        ds.drawLine(color, o(x1, y1), o(x2, y2), width * u, cap = StrokeCap.Round)

    /** A closed polygon from x, y pairs. */
    fun poly(color: Color, vararg xy: Float, outline: Boolean = true) {
        val path = Path().apply {
            moveTo(o(xy[0], xy[1]).x, o(xy[0], xy[1]).y)
            for (i in 2 until xy.size step 2) lineTo(o(xy[i], xy[i + 1]).x, o(xy[i], xy[i + 1]).y)
            close()
        }
        ds.drawPath(path, color)
        if (outline) ds.drawPath(path, Palette.Outline, style = line)
    }

    fun shadow(x: Float = 0f, y: Float = h - 2.5f, width: Float = w, height: Float = 4f) =
        ds.drawOval(Palette.Shadow, o(x + 0.8f, y), Size(width * u, height * u))

    /** A box seen from the top-front: a top face of height h - [front], then a front face. */
    fun block(x: Float, y: Float, w: Float, h: Float, front: Float, top: Color, frontColor: Color, radius: Float = 0.8f) {
        rect(x, y + h - front, w, front, frontColor, radius)
        rect(x, y, w, h - front, top, radius)
    }
}

fun DrawScope.drawFurniture(kind: Furniture, r: Rect, u: Float) = with(Sketch(this, r, u)) {
    when (kind) {
        Furniture.BED -> bed()
        Furniture.BUNK_BED -> bunkBed()
        Furniture.WARDROBE -> wardrobe()
        Furniture.CURTAIN -> curtain()
        Furniture.TOY_BOX -> toyBox()
        Furniture.TENT -> tent()
        Furniture.BATHTUB -> bathtub()
        Furniture.SINK -> sink()
        Furniture.LAUNDRY -> laundry()
        Furniture.SOFA -> sofa()
        Furniture.TV -> tv()
        Furniture.BOOKSHELF -> bookshelf()
        Furniture.COAT_RACK -> coatRack()
        Furniture.SHOE_CABINET -> shoeCabinet()
        Furniture.FRIDGE -> fridge()
        Furniture.OVEN -> oven()
        Furniture.PANTRY -> pantry()
        Furniture.TABLE -> table()
        Furniture.CAR -> car()
        Furniture.WORKBENCH -> workbench()
        Furniture.BOXES -> boxes()
        Furniture.DESK -> desk()
        Furniture.ARMCHAIR -> armchair()
        Furniture.FILING_CABINET -> filingCabinet()
        Furniture.WASHER -> washer()
        Furniture.DRYING_RACK -> dryingRack()
        Furniture.LINEN_CUPBOARD -> linenCupboard()
    }
}

fun DrawScope.drawDecor(kind: Decor, r: Rect, u: Float) = with(Sketch(this, r, u)) {
    when (kind) {
        Decor.RUG -> {
            rect(0f, 0f, w, h, Color(0xFFC9705A), radius = 1.5f)
            rect(1.5f, 1.5f, w - 3f, h - 3f, Color(0xFFE9C9A3), radius = 1f, outline = false)
            rect(3f, 3f, w - 6f, h - 6f, Color(0xFFC9705A), radius = 1f, outline = false)
            poly(Color(0xFFE9C9A3), w / 2, 4.5f, w / 2 + 8f, h / 2, w / 2, h - 4.5f, w / 2 - 8f, h / 2, outline = false)
        }
        Decor.ROUND_RUG -> {
            oval(0f, 0f, w, h, Color(0xFF6CC3F2))
            oval(3f, 2.5f, w - 6f, h - 5f, Palette.Yellow, outline = false)
            oval(6f, 5f, w - 12f, h - 10f, Color(0xFFFF8A65), outline = false)
            oval(9f, 7.5f, w - 18f, h - 15f, Palette.Green, outline = false)
        }
        Decor.RUNNER -> {
            rect(0f, 0f, w, h, Color(0xFF5C6BC0), radius = 1f)
            for (y in 3..(h.toInt() - 3) step 4) line(1.5f, y.toFloat(), w - 1.5f, y.toFloat(), Color(0xFFE9C9A3), 0.8f)
        }
        Decor.BATH_MAT -> {
            rect(0f, 0f, w, h, Color(0xFF8FD3C9), radius = 2.5f)
            for (x in 2..(w.toInt() - 2) step 2) line(x.toFloat(), 1.5f, x.toFloat(), h - 1.5f, Color(0xFF6FBFB3), 0.5f)
        }
        Decor.OIL_STAIN -> {
            oval(0f, 0f, w, h, Color(0x553A3346), outline = false)
            oval(w * 0.55f, h * 0.3f, w * 0.35f, h * 0.6f, Color(0x443A3346), outline = false)
        }
        Decor.DOORMAT -> {
            rect(0f, 0f, w, h, Color(0xFF9C6B3E), radius = 1f)
            for (x in 2..(w.toInt() - 2) step 2) line(x.toFloat(), 1.2f, x.toFloat(), h - 1.2f, Color(0xFF7A512B), 0.5f)
        }
        Decor.NIGHTSTAND -> {
            shadow()
            block(0f, 4f, w, h - 4f, front = 5f, top = Palette.WoodLight, frontColor = Palette.Wood)
            line(1.5f, h - 2.5f, w - 1.5f, h - 2.5f, width = 0.4f)
            circle(w / 2, h - 1.4f, 0.5f, Palette.WoodDark, outline = false)
            // Lamp with a warm glow.
            circle(w / 2, 3f, 4.5f, Color(0x44FFE082), outline = false)
            line(w / 2, 2f, w / 2, 6f, Palette.MetalDark, 0.6f)
            poly(Color(0xFFFFE082), w / 2 - 3f, 3f, w / 2 + 3f, 3f, w / 2 + 2f, -1f, w / 2 - 2f, -1f)
        }
        Decor.TOILET -> {
            shadow(2f, h - 3f, w - 4f)
            rect(1f, 0f, w - 2f, 6f, Palette.White, radius = 1.2f)
            rect(w / 2 - 1.5f, 0.6f, 3f, 1f, Palette.Metal, outline = false)
            rect(w / 2 - 3f, 12f, 6f, h - 13f, Palette.WhiteShade, radius = 1.5f)
            oval(0.5f, 5f, w - 1f, 10f, Palette.White)
            oval(2.5f, 6.5f, w - 5f, 7f, Palette.Water)
        }
        Decor.PLANT -> {
            shadow(1f, h - 2.5f, w - 2f)
            poly(Color(0xFFD9774B), 1.5f, h - 6f, w - 1.5f, h - 6f, w - 2.5f, h, 2.5f, h)
            listOf(w / 2 to 2.5f, 2.5f to 6f, w - 2.5f to 6f, w / 2 - 1.5f to 7.5f, w / 2 + 2f to 5f).forEach { (x, y) ->
                circle(x, y, 2.8f, Palette.Green)
            }
        }
        Decor.BALL -> {
            shadow(0.5f, h - 2f, w - 1f, 3f)
            circle(w / 2, h / 2, w / 2, Palette.White)
            poly(Palette.Red, w / 2, 0.3f, w / 2 + 1.8f, h / 2, w / 2, h - 0.3f, w / 2 - 1.8f, h / 2, outline = false)
            circle(w / 2, h / 2, w / 2, Color.Transparent)
        }
        Decor.BLOCKS -> {
            block(0f, h - 5f, 5f, 5f, front = 3f, top = Color(0xFFFFE082), frontColor = Palette.Yellow)
            block(5.5f, h - 5f, 5f, 5f, front = 3f, top = Color(0xFF8FB8F0), frontColor = Palette.Blue)
            block(2.5f, h - 9.5f, 5f, 5f, front = 3f, top = Color(0xFFF08A8A), frontColor = Palette.Red)
        }
        Decor.COUNTER_SINK -> {
            block(0f, 0f, w, h, front = h - 7f, top = Color(0xFFE3E0DA), frontColor = Palette.White)
            oval(2.5f, 1.2f, w - 5f, 4.5f, Palette.Metal)
            oval(3.5f, 1.8f, w - 7f, 3.2f, Palette.Water, outline = false)
            line(w / 2, 0.2f, w / 2, 1.6f, Palette.MetalDark, 0.8f)
            line(w / 2, h - (h - 7f) + 1f, w / 2, h - 1f, width = 0.4f)
            circle(w / 2 - 1.5f, 12f, 0.5f, Palette.MetalDark, outline = false)
            circle(w / 2 + 1.5f, 12f, 0.5f, Palette.MetalDark, outline = false)
        }
        Decor.POSTER -> {
            rect(0f, 0f, w, h, Color(0xFF3949AB), radius = 0.5f)
            val cx = w / 2
            val cy = h / 2
            poly(
                Palette.Yellow,
                cx, cy - 3.5f, cx + 1f, cy - 1f, cx + 3.5f, cy - 1f, cx + 1.5f, cy + 0.7f,
                cx + 2.3f, cy + 3.2f, cx, cy + 1.7f, cx - 2.3f, cy + 3.2f, cx - 1.5f, cy + 0.7f,
                cx - 3.5f, cy - 1f, cx - 1f, cy - 1f,
                outline = false,
            )
            circle(2f, 2f, 0.5f, Color.White, outline = false)
            circle(w - 2.5f, h - 2f, 0.4f, Color.White, outline = false)
        }
        Decor.PICTURE -> {
            rect(0f, 0f, w, h, Palette.WoodDark, radius = 0.3f)
            rect(1f, 1f, w - 2f, h - 2f, Color(0xFFBDE3F5), radius = 0f, outline = false)
            circle(w - 3f, 3f, 1.2f, Palette.Yellow, outline = false)
            poly(Palette.Green, 1f, h - 1f, 4f, 3.5f, 6.5f, h - 1f, outline = false)
            poly(Color(0xFF3E8E3C), 4.5f, h - 1f, 7f, 4.5f, w - 1f, h - 1f, outline = false)
        }
        Decor.CLOCK -> {
            circle(w / 2, h / 2, w / 2, Palette.White)
            line(w / 2, h / 2, w / 2, 1.5f, width = 0.5f)
            line(w / 2, h / 2, w - 2f, h / 2, width = 0.5f)
        }
        Decor.TOWEL -> {
            line(-0.5f, 0.8f, w + 0.5f, 0.8f, Palette.MetalDark, 0.7f)
            rect(0.5f, 0.8f, w - 1f, h - 1f, Color(0xFFFFB3A7), radius = 0.5f)
            line(0.5f, h - 3f, w - 0.5f, h - 3f, Color.White, 0.6f)
        }
        Decor.KITCHEN_WINDOW -> {
            rect(0f, 0f, w, h, Palette.White, radius = 0.3f)
            rect(0.8f, 0.6f, w - 1.6f, h - 1.2f, Palette.Glass, radius = 0f, outline = false)
            line(w / 2, 0.6f, w / 2, h - 0.6f, Palette.White, 0.6f)
        }
    }
}

private fun Sketch.bed() {
    shadow()
    rect(0f, 0f, w, 9f, Palette.WoodDark, radius = 2f)
    rect(1.5f, 1.5f, w - 3f, 5f, Palette.Wood, radius = 1.2f, outline = false)
    block(0f, 6f, w, h - 6f, front = 4f, top = Palette.Wood, frontColor = Palette.WoodDark)
    rect(1.5f, 7f, w - 3f, h - 12.5f, Palette.White, radius = 1.2f)
    val pillow = (w - 8f) / 2
    rect(3f, 8.5f, pillow, 6f, Color.White, radius = 2.2f)
    rect(5f + pillow, 8.5f, pillow, 6f, Color.White, radius = 2.2f)
    rect(1.5f, 17f, w - 3f, h - 21.5f, Color(0xFF8E7CC3), radius = 1f)
    rect(1.5f, 17f, w - 3f, 4f, Color(0xFFB6A8E0), radius = 1f)
    line(4f, 27f, w - 4f, 27f, Color(0xFF6F5DAA), 0.4f)
}

private fun Sketch.bunkBed() {
    shadow()
    // Lower bunk.
    block(0f, 30f, w, h - 30f, front = 5f, top = Palette.Wood, frontColor = Palette.WoodDark)
    rect(2f, 31f, w - 4f, h - 37f, Color(0xFF6CC3F2), radius = 1f)
    // Gap under the top bunk.
    rect(2.5f, 24f, w - 5f, 7f, Color(0xFF5A4A66), radius = 0.5f, outline = false)
    // Top bunk.
    block(0f, 4f, w, 22f, front = 4f, top = Palette.Wood, frontColor = Palette.WoodDark)
    rect(2f, 5f, w - 4f, 15.5f, Color(0xFFFFB35C), radius = 1f)
    rect(3.5f, 5.8f, w - 7f, 4.5f, Color.White, radius = 2f)
    // Posts.
    rect(0f, 0f, 2.5f, h - 0.5f, Palette.WoodDark, radius = 0.6f)
    rect(w - 2.5f, 0f, 2.5f, h - 0.5f, Palette.WoodDark, radius = 0.6f)
    // Ladder.
    line(w - 8f, 22f, w - 8f, h, Palette.WoodDark, 0.9f)
    line(w - 4f, 22f, w - 4f, h, Palette.WoodDark, 0.9f)
    for (y in 26..(h.toInt() - 2) step 5) line(w - 8f, y.toFloat(), w - 4f, y.toFloat(), Palette.WoodDark, 0.8f)
}

private fun Sketch.wardrobe() {
    shadow()
    block(0f, 1f, w, h - 1.5f, front = h - 5f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(-0.5f, 0f, w + 1f, 2.2f, Palette.WoodDark, radius = 0.5f)
    line(w / 2, 5f, w / 2, h - 1.5f, width = 0.5f)
    rect(2f, 7f, w / 2 - 3.5f, h - 11f, Color(0xFFC4915F), radius = 0.5f, outline = false)
    rect(w / 2 + 1.5f, 7f, w / 2 - 3.5f, h - 11f, Color(0xFFC4915F), radius = 0.5f, outline = false)
    rect(w / 2 - 1.8f, h / 2, 0.8f, 4f, Palette.MetalDark, radius = 0.3f, outline = false)
    rect(w / 2 + 1f, h / 2, 0.8f, 4f, Palette.MetalDark, radius = 0.3f, outline = false)
    rect(1f, h - 1.5f, 1.5f, 1.5f, Palette.WoodDark, radius = 0.2f, outline = false)
    rect(w - 2.5f, h - 1.5f, 1.5f, 1.5f, Palette.WoodDark, radius = 0.2f, outline = false)
}

private fun Sketch.curtain() {
    // Window on the back wall.
    rect(3.5f, 1.5f, w - 7f, 10f, Palette.White, radius = 0.4f)
    rect(4.5f, 2.3f, w - 9f, 8.4f, Palette.Glass, radius = 0f, outline = false)
    line(w / 2, 2.3f, w / 2, 10.7f, Palette.White, 0.7f)
    line(4.5f, 6.5f, w - 4.5f, 6.5f, Palette.White, 0.7f)
    rect(2.5f, 11.3f, w - 5f, 1.5f, Palette.White, radius = 0.3f)
    // Floor-length drapes.
    shadow(0f, h - 2f, 7f, 3f)
    shadow(w - 7f, h - 2f, 7f, 3f)
    val drape = Color(0xFFE05D7A)
    val fold = Color(0xFFB8435F)
    poly(drape, 0f, 1f, 6.5f, 1f, 7.5f, h, 0f, h)
    poly(drape, w - 6.5f, 1f, w, 1f, w, h, w - 7.5f, h)
    listOf(2.2f, 4.4f).forEach { x ->
        line(x, 2f, x + 0.3f, h - 0.5f, fold, 0.5f)
        line(w - x, 2f, w - x - 0.3f, h - 0.5f, fold, 0.5f)
    }
    line(-1f, 1f, w + 1f, 1f, Palette.WoodDark, 1f)
}

private fun Sketch.toyBox() {
    shadow()
    rect(0f, 3f, w, h - 10f, Color(0xFF5A4A66), radius = 0.8f)
    circle(w * 0.3f, 4.5f, 3f, Palette.Red)
    rect(w * 0.55f, 1.5f, 5f, 5f, Palette.Yellow, radius = 0.6f)
    circle(w * 0.8f, 3.5f, 2f, Palette.Green)
    rect(0f, h - 7.5f, w, 7.5f, Color(0xFF4F8FE0), radius = 0.8f)
    line(0f, h - 7.5f, w, h - 7.5f, Color(0xFF3A6FB8), 0.8f)
    val cx = w / 2
    val cy = h - 3.6f
    poly(
        Palette.Yellow,
        cx, cy - 2.6f, cx + 0.8f, cy - 0.8f, cx + 2.6f, cy - 0.8f, cx + 1.1f, cy + 0.5f,
        cx + 1.7f, cy + 2.4f, cx, cy + 1.3f, cx - 1.7f, cy + 2.4f, cx - 1.1f, cy + 0.5f,
        cx - 2.6f, cy - 0.8f, cx - 0.8f, cy - 0.8f,
        outline = false,
    )
}

private fun Sketch.tent() {
    shadow(1f, h - 3f, w - 2f, 5f)
    val top = 4f
    poly(Color(0xFFFF8A65), w / 2, top, w, h, 0f, h)
    poly(Color(0xFFFFE082), w / 2, top, w / 2 + w * 0.17f, h, w / 2 - w * 0.17f, h, outline = false)
    poly(Color(0xFFFFE082), w / 2, top, w * 0.92f, h, w * 0.80f, h, outline = false)
    poly(Color(0xFFFFE082), w / 2, top, w * 0.20f, h, w * 0.08f, h, outline = false)
    poly(Color.Transparent, w / 2, top, w, h, 0f, h)
    // Entrance.
    poly(Color(0xFF5A4A66), w / 2, h * 0.5f, w / 2 + 5f, h, w / 2 - 5f, h)
    // Poles and a flag.
    line(w / 2 - 2.5f, 0f, w / 2 + 1.5f, top + 3f, Palette.WoodDark, 0.8f)
    line(w / 2 + 2.5f, 0f, w / 2 - 1.5f, top + 3f, Palette.WoodDark, 0.8f)
    poly(Palette.Purple, w / 2 + 2.5f, 0f, w / 2 + 6.5f, 1.2f, w / 2 + 2.9f, 2.5f, outline = false)
}

private fun Sketch.bathtub() {
    shadow()
    // Tap on the wall.
    rect(w / 2 - 2f, 0.5f, 4f, 2f, Palette.Metal, radius = 0.6f)
    line(w / 2, 2.5f, w / 2, 6f, Palette.MetalDark, 1f)
    block(0f, 4f, w, h - 4f, front = 7f, top = Palette.White, frontColor = Palette.WhiteShade, radius = 3f)
    rect(2.5f, 6f, w - 5f, h - 15f, Palette.Water, radius = 3f)
    // Foam and a rubber duck.
    listOf(5f to 9f, 8f to 11f, 6.5f to 13f).forEach { (x, y) -> circle(x, y, 1.6f, Color.White, outline = false) }
    val dx = w * 0.68f
    oval(dx - 3f, 8.5f, 6f, 4f, Palette.Yellow)
    circle(dx + 2f, 8f, 2f, Palette.Yellow)
    poly(Palette.Orange, dx + 3.6f, 7.6f, dx + 5.6f, 8.2f, dx + 3.6f, 8.9f, outline = false)
    circle(dx + 2.4f, 7.4f, 0.4f, Palette.Dark, outline = false)
}

private fun Sketch.sink() {
    // Mirror on the wall.
    rect(3f, 0.8f, w - 6f, 10f, Palette.MetalDark, radius = 1.5f)
    rect(4f, 1.8f, w - 8f, 8f, Color(0xFFDDF1F8), radius = 1f, outline = false)
    line(6f, 8f, 9f, 3.5f, Color.White, 0.8f)
    shadow()
    block(0f, 12f, w, h - 12f, front = h - 18f, top = Color(0xFFE3E0DA), frontColor = Palette.WoodLight)
    oval(w / 2 - 6f, 12.6f, 12f, 4.6f, Palette.White)
    oval(w / 2 - 4.5f, 13.4f, 9f, 3f, Palette.Water, outline = false)
    line(w / 2, 11.5f, w / 2, 13.4f, Palette.MetalDark, 0.9f)
    line(w / 2, 19f, w / 2, h - 1f, width = 0.4f)
    circle(w / 2 - 1.5f, 23f, 0.5f, Palette.MetalDark, outline = false)
    circle(w / 2 + 1.5f, 23f, 0.5f, Palette.MetalDark, outline = false)
}

private fun Sketch.laundry() {
    shadow()
    listOf(Triple(4f, 4.5f, Palette.Blue), Triple(8f, 3.5f, Palette.Pink), Triple(11.5f, 4.5f, Palette.Yellow)).forEach { (x, y, c) ->
        circle(x, y, 2.6f, c)
    }
    oval(0f, 4f, w, 4f, Color(0xFFB08A5B))
    poly(Color(0xFFD9B98C), 0f, 6f, w, 6f, w - 2f, h, 2f, h)
    for (y in 8..(h.toInt() - 1) step 2) line(1f, y.toFloat(), w - 1f, y.toFloat(), Color(0xFFB08A5B), 0.4f)
    for (x in 3..(w.toInt() - 3) step 3) line(x.toFloat(), 6.5f, x.toFloat(), h - 0.5f, Color(0xFFB08A5B), 0.4f)
}

private fun Sketch.sofa() {
    // Seen from behind, facing the TV.
    shadow()
    val fabric = Color(0xFF4F8C87)
    val fabricLight = Color(0xFF79B0AB)
    rect(4f, 0f, w - 8f, 8f, fabricLight, radius = 1.5f)
    line(w / 2, 0.5f, w / 2, 7.5f, width = 0.4f)
    rect(0f, 1f, 5f, h - 2f, fabric, radius = 2f)
    rect(w - 5f, 1f, 5f, h - 2f, fabric, radius = 2f)
    rect(3.5f, 6f, w - 7f, h - 7.5f, fabric, radius = 2.5f)
    line(6f, 9f, w - 6f, 9f, fabricLight, 0.5f)
    rect(3f, h - 1.5f, 2f, 1.5f, Palette.WoodDark, radius = 0.3f, outline = false)
    rect(w - 5f, h - 1.5f, 2f, 1.5f, Palette.WoodDark, radius = 0.3f, outline = false)
}

private fun Sketch.tv() {
    shadow()
    rect(1f, 0f, w - 2f, 16f, Palette.Dark, radius = 1f)
    rect(2f, 1f, w - 4f, 14f, Color(0xFF1E2A44), radius = 0.4f, outline = false)
    // A Kotlin logo on screen.
    val cx = w / 2
    poly(Palette.Purple, cx - 5f, 3f, cx + 5f, 3f, cx - 5f, 13f, outline = false)
    poly(Palette.Orange, cx + 5f, 13f, cx - 5f, 13f, cx, 8f, outline = false)
    line(3f, 14f, 8f, 2f, Color(0x33FFFFFF), 1.5f)
    rect(w / 2 - 2f, 16f, 4f, 2f, Palette.Dark, radius = 0.3f, outline = false)
    block(0f, 17.5f, w, h - 17.5f, front = 7f, top = Palette.WoodLight, frontColor = Palette.Wood)
    line(w / 2, h - 7f, w / 2, h, width = 0.4f)
    circle(w / 2 - 2f, h - 3.5f, 0.5f, Palette.WoodDark, outline = false)
    circle(w / 2 + 2f, h - 3.5f, 0.5f, Palette.WoodDark, outline = false)
}

private fun Sketch.bookshelf() {
    shadow()
    block(0f, 0f, w, h - 0.5f, front = h - 3.5f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(1.5f, 4.5f, w - 3f, h - 7f, Color(0xFF6E4A2B), radius = 0.3f, outline = false)
    val colors = listOf(Palette.Red, Palette.Blue, Palette.Yellow, Palette.Purple, Palette.Green, Palette.Orange, Palette.Pink)
    val shelves = 4
    val shelfH = (h - 7f) / shelves
    for (s in 0 until shelves) {
        val bottom = 4.5f + (s + 1) * shelfH
        var x = 2f
        var i = s * 3
        while (x < w - 3.5f) {
            val bw = 1.6f + (i % 3) * 0.4f
            val bh = shelfH - 1.2f - (i % 4) * 0.5f
            rect(x, bottom - bh, bw, bh, colors[i % colors.size], radius = 0.2f, outline = false)
            x += bw + 0.25f
            i++
        }
        rect(1f, bottom - 0.2f, w - 2f, 1f, Palette.WoodLight, radius = 0f, outline = false)
    }
}

private fun Sketch.coatRack() {
    shadow(w / 2 - 6f, h - 3f, 12f)
    oval(w / 2 - 5f, h - 3f, 10f, 3f, Palette.WoodDark)
    line(w / 2, 3f, w / 2, h - 1.5f, Palette.WoodDark, 1.4f)
    // Coats on hooks.
    poly(Color(0xFF5C6BC0), w / 2 - 1f, 6f, w / 2 - 3f, 6f, 1f, 22f, w / 2 - 1f, 23f)
    poly(Palette.Red, w / 2 + 1f, 7f, w / 2 + 3f, 7f, w - 1f, 21f, w / 2 + 1f, 22f)
    rect(w / 2 - 4f, 8f, 2f, 10f, Palette.Yellow, radius = 0.5f)
    // A hat on top.
    oval(w / 2 - 4.5f, 2.2f, 9f, 2.4f, Palette.Dark, outline = false)
    rect(w / 2 - 2.8f, -1f, 5.6f, 4f, Palette.Dark, radius = 1.2f, outline = false)
}

private fun Sketch.shoeCabinet() {
    shadow()
    block(0f, 6f, w, h - 6f, front = 9f, top = Palette.WoodLight, frontColor = Palette.Wood)
    line(1f, h - 4.5f, w - 1f, h - 4.5f, width = 0.4f)
    rect(w / 2 - 2f, h - 7.5f, 4f, 0.8f, Palette.MetalDark, radius = 0.3f, outline = false)
    rect(w / 2 - 2f, h - 3f, 4f, 0.8f, Palette.MetalDark, radius = 0.3f, outline = false)
    // Shoes on top.
    oval(3f, 4.5f, 4.5f, 2.6f, Palette.Red)
    oval(7f, 4.5f, 4.5f, 2.6f, Palette.Red)
    oval(14f, 3.5f, 3.5f, 4.5f, Palette.WoodDark)
    oval(18f, 3.5f, 3.5f, 4.5f, Palette.WoodDark)
}

private fun Sketch.fridge() {
    shadow()
    block(0f, 0f, w, h - 0.5f, front = h - 4f, top = Palette.WhiteShade, frontColor = Palette.White, radius = 1.2f)
    val split = 3.5f + (h - 4f) * 0.35f
    line(0.5f, split, w - 0.5f, split, width = 0.5f)
    rect(1.5f, 5.5f, 0.9f, split - 7f, Palette.MetalDark, radius = 0.4f, outline = false)
    rect(1.5f, split + 1.5f, 0.9f, 8f, Palette.MetalDark, radius = 0.4f, outline = false)
    rect(w - 6f, split + 3f, 4f, 4f, Palette.Yellow, radius = 0.2f, outline = false)
    circle(w - 4f, split + 10f, 0.9f, Palette.Red, outline = false)
    circle(w - 7f, split + 12f, 0.9f, Palette.Blue, outline = false)
}

private fun Sketch.oven() {
    shadow()
    // Extractor hood on the wall.
    poly(Palette.Metal, 3f, 0f, w - 3f, 0f, w, 4f, 0f, 4f)
    block(0f, 6f, w, h - 6f, front = h - 14f, top = Palette.Dark, frontColor = Palette.Metal)
    listOf(4f to 7.8f, w - 4f to 7.8f, 4f to 11.6f, w - 4f to 11.6f).forEach { (x, y) ->
        circle(x, y, 1.6f, Color(0xFF45404F), outline = false)
        circle(x, y, 1.6f, Color(0xFFFF7043).copy(alpha = 0.8f), outline = false)
        circle(x, y, 0.9f, Color(0xFF45404F), outline = false)
    }
    for (i in 0..3) circle(2.5f + i * (w - 5f) / 3, 15.5f, 0.6f, Palette.Dark, outline = false)
    rect(1.5f, 17f, w - 3f, h - 18.5f, Palette.Dark, radius = 0.6f)
    rect(2.5f, 18.5f, w - 5f, h - 21f, Color(0xFF5B3A2E), radius = 0.4f, outline = false)
    line(3f, 17.8f, w - 3f, 17.8f, Palette.MetalDark, 0.6f)
}

private fun Sketch.pantry() {
    shadow()
    block(0f, 0f, w, h - 0.5f, front = h - 3.5f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(1.5f, 4.5f, w - 3f, h - 7f, Color(0xFF6E4A2B), radius = 0.3f, outline = false)
    val shelfH = (h - 7f) / 3
    val contents = listOf(Palette.Red, Palette.Yellow, Palette.Green, Palette.Orange)
    for (s in 0 until 3) {
        val bottom = 4.5f + (s + 1) * shelfH
        for (j in 0 until 4) {
            val x = 2.5f + j * (w - 5f) / 4
            val c = contents[(s + j) % contents.size]
            if ((s + j) % 2 == 0) {
                // Jar with a lid.
                rect(x, bottom - 5.5f, 3f, 5f, Palette.Glass, radius = 0.6f)
                rect(x + 0.4f, bottom - 3.8f, 2.2f, 3.2f, c, radius = 0.4f, outline = false)
                rect(x - 0.2f, bottom - 6.3f, 3.4f, 1.2f, Palette.Red, radius = 0.3f, outline = false)
            } else {
                // Tin can.
                rect(x, bottom - 4.5f, 3f, 4.3f, c, radius = 0.3f)
                line(x, bottom - 3f, x + 3f, bottom - 3f, Color.White, 0.5f)
            }
        }
        rect(1f, bottom - 0.2f, w - 2f, 1f, Palette.WoodLight, radius = 0f, outline = false)
    }
}

private fun Sketch.table() {
    shadow(-2f, h - 1f, w + 4f, 5f)
    // Chairs behind the table.
    listOf(0.25f, 0.75f).forEach { fx ->
        rect(w * fx - 3.5f, -4f, 7f, 7f, Palette.WoodDark, radius = 1f)
        rect(w * fx - 2.5f, -3f, 5f, 2f, Palette.Wood, radius = 0.5f, outline = false)
    }
    block(0f, 1f, w, h - 1f, front = 4f, top = Palette.WoodLight, frontColor = Palette.Wood, radius = 1.2f)
    line(2f, 5f, w - 2f, 5f, Palette.Wood, 0.3f)
    line(2f, 9f, w - 2f, 9f, Palette.Wood, 0.3f)
    // Plates and a fruit bowl.
    circle(w * 0.2f, h / 2 - 1f, 2.4f, Palette.White)
    circle(w * 0.8f, h / 2 - 1f, 2.4f, Palette.White)
    oval(w / 2 - 4f, h / 2 - 3.5f, 8f, 5f, Color(0xFFE3E0DA))
    circle(w / 2 - 1.6f, h / 2 - 1.6f, 1.4f, Palette.Red, outline = false)
    circle(w / 2 + 1.2f, h / 2 - 1.8f, 1.4f, Palette.Orange, outline = false)
    circle(w / 2, h / 2 - 3f, 1.3f, Palette.Green, outline = false)
    // Chairs in front of the table.
    listOf(0.25f, 0.75f).forEach { fx ->
        rect(w * fx - 3.5f, h - 2f, 7f, 5.5f, Palette.WoodDark, radius = 1f)
        rect(w * fx - 3.5f, h + 2.5f, 7f, 1.5f, Palette.Wood, radius = 0.4f)
    }
}

/** Unused space under or inside furniture. */
private val Gap = Color(0xFF5A4A66)

private fun Sketch.car() {
    // Seen from above, bonnet towards the front of the garage.
    shadow(-1f, h - 4f, w + 2f, 6f)
    listOf(9f, h - 17f).forEach { y ->
        rect(-1.5f, y, 4f, 9f, Palette.Dark, radius = 1f)
        rect(w - 2.5f, y, 4f, 9f, Palette.Dark, radius = 1f)
    }
    rect(1f, 0f, w - 2f, h, Palette.Red, radius = 6f)
    rect(4f, 1f, 5f, 1.5f, Color(0xFFFFB3A7), radius = 0.5f, outline = false)
    rect(w - 9f, 1f, 5f, 1.5f, Color(0xFFFFB3A7), radius = 0.5f, outline = false)
    rect(4f, 6f, w - 8f, 6f, Palette.Glass, radius = 2f)
    rect(4f, 12f, w - 8f, 17f, Color(0xFFC94444), radius = 2f)
    rect(4f, 29f, w - 8f, 8f, Palette.Glass, radius = 2f)
    line(6f, 35f, 10f, 31f, Color.White, 0.6f)
    rect(-1.5f, 28f, 3f, 2f, Palette.Red, radius = 0.6f)
    rect(w - 1.5f, 28f, 3f, 2f, Palette.Red, radius = 0.6f)
    line(w / 2, 40f, w / 2, h - 5f, Color(0xFFC94444), 0.5f)
    circle(6f, h - 3f, 1.6f, Palette.Yellow)
    circle(w - 6f, h - 3f, 1.6f, Palette.Yellow)
}

private fun Sketch.workbench() {
    // Pegboard with tools on the wall.
    rect(2f, 0.5f, w - 4f, 11f, Color(0xFFD9B98C), radius = 0.4f)
    for (x in 4..(w.toInt() - 4) step 3) for (y in 2..10 step 3) circle(x.toFloat(), y.toFloat(), 0.25f, Color(0xFFB08A5B), outline = false)
    line(7f, 3f, 7f, 10f, Palette.WoodDark, 0.9f)
    rect(5f, 2f, 4f, 2f, Palette.MetalDark, radius = 0.3f)
    line(13f, 3f, 15f, 10f, Palette.Metal, 1.1f)
    circle(13f, 2.6f, 1.3f, Palette.Metal)
    poly(Palette.Metal, 22f, 3f, w - 6f, 3f, w - 6f, 6f, 22f, 9f)
    rect(19.5f, 2.5f, 3f, 5f, Palette.Red, radius = 0.6f)
    // Bench, open underneath.
    shadow()
    rect(1f, 21f, w - 2f, h - 21.5f, Gap, radius = 0.3f, outline = false)
    block(0f, 12f, w, 9f, front = 3f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(0.5f, 21f, 2.5f, h - 21f, Palette.WoodDark, radius = 0.4f)
    rect(w - 3f, 21f, 2.5f, h - 21f, Palette.WoodDark, radius = 0.4f)
    rect(w - 9f, 11f, 6f, 4f, Palette.Blue, radius = 0.5f)
    rect(5f, h - 5f, 9f, 4f, Palette.Red, radius = 0.5f)
}

private fun Sketch.boxes() {
    shadow()
    val card = Color(0xFFD4A574)
    val cardDark = Color(0xFFB98352)
    val tape = Color(0xFFE9D5B5)
    block(0f, 7f, 13f, h - 7f, front = 7f, top = card, frontColor = cardDark)
    block(12f, 9f, w - 12f, h - 9f, front = 6f, top = card, frontColor = cardDark)
    line(6.5f, 7.5f, 6.5f, h - 7.5f, tape, 1f)
    line(12f + (w - 12f) / 2, 9.5f, 12f + (w - 12f) / 2, h - 6.5f, tape, 1f)
    block(3f, 0f, 11f, 10f, front = 5f, top = card, frontColor = cardDark)
    line(8.5f, 0.5f, 8.5f, 4.5f, tape, 1f)
    // Flaps of the open box on the right.
    poly(card, 12.5f, 9f, 15f, 6f, 18f, 9f)
}

private fun Sketch.desk() {
    // Monitor showing some Kotlin, and a lamp.
    rect(w / 2 - 7f, 1f, 14f, 10f, Palette.Dark, radius = 0.8f)
    rect(w / 2 - 6f, 2f, 12f, 8f, Color(0xFF1E2A44), radius = 0.3f, outline = false)
    line(w / 2 - 4f, 4f, w / 2 + 1f, 4f, Palette.Purple, 0.6f)
    line(w / 2 - 3f, 6f, w / 2 + 4f, 6f, Palette.Orange, 0.6f)
    line(w / 2 - 3f, 8f, w / 2 + 2f, 8f, Palette.Green, 0.6f)
    rect(w / 2 - 1f, 11f, 2f, 1.5f, Palette.Dark, radius = 0.2f, outline = false)
    circle(4f, 10f, 4f, Color(0x44FFE082), outline = false)
    line(4f, 14f, 4f, 9f, Palette.MetalDark, 0.6f)
    poly(Color(0xFFFFE082), 1.5f, 10f, 6.5f, 10f, 5.5f, 7f, 2.5f, 7f)
    // Desk top, open underneath.
    shadow()
    rect(1f, 21f, w - 2f, h - 21.5f, Gap, radius = 0.3f, outline = false)
    block(0f, 12f, w, 9f, front = 2.5f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(0.5f, 21f, 2.5f, h - 21f, Palette.WoodDark, radius = 0.4f)
    rect(w - 3f, 21f, 2.5f, h - 21f, Palette.WoodDark, radius = 0.4f)
    rect(w / 2 - 6f, 14f, 12f, 3f, Palette.WhiteShade, radius = 0.4f)
    circle(w - 5f, 15.5f, 1.6f, Palette.Red)
}

private fun Sketch.armchair() {
    shadow()
    val fabric = Color(0xFFC0583F)
    val light = Color(0xFFE07A5F)
    rect(2f, 0f, w - 4f, 10f, fabric, radius = 2.5f)
    rect(0f, 5f, 5f, h - 6f, fabric, radius = 2f)
    rect(w - 5f, 5f, 5f, h - 6f, fabric, radius = 2f)
    rect(4f, 8f, w - 8f, h - 10f, light, radius = 2f)
    line(5f, h - 4.5f, w - 5f, h - 4.5f, fabric, 0.5f)
    rect(2f, h - 1.5f, 2f, 1.5f, Palette.WoodDark, radius = 0.3f, outline = false)
    rect(w - 4f, h - 1.5f, 2f, 1.5f, Palette.WoodDark, radius = 0.3f, outline = false)
}

private fun Sketch.filingCabinet() {
    shadow()
    // Papers on top.
    block(0f, 1f, w, h - 1.5f, front = h - 5f, top = Palette.MetalDark, frontColor = Palette.Metal)
    rect(2f, 0f, 8f, 3.5f, Palette.White, radius = 0.3f)
    rect(3f, -0.8f, 8f, 3.5f, Palette.White, radius = 0.3f)
    val top = 4.5f
    val drawer = (h - 1.5f - top) / 3
    for (i in 0 until 3) {
        val y = top + i * drawer
        rect(1.2f, y + 0.8f, w - 2.4f, drawer - 1.6f, Color(0xFFC9D2DA), radius = 0.4f)
        rect(w / 2 - 2f, y + 2f, 4f, 1f, Palette.Dark, radius = 0.3f, outline = false)
        rect(w / 2 - 1.5f, y + 3.6f, 3f, 1.6f, Palette.White, radius = 0.2f, outline = false)
    }
}

private fun Sketch.washer() {
    shadow()
    block(0f, 0f, w, h - 0.5f, front = h - 4f, top = Palette.WhiteShade, frontColor = Palette.White, radius = 1.2f)
    rect(1.5f, 5f, w - 3f, 3.5f, Palette.WhiteShade, radius = 0.4f, outline = false)
    rect(2.5f, 5.9f, 5f, 1.6f, Palette.Glass, radius = 0.3f, outline = false)
    circle(w - 4f, 6.7f, 1.1f, Palette.MetalDark, outline = false)
    val cy = 18.5f
    circle(w / 2, cy, w * 0.36f, Palette.MetalDark)
    circle(w / 2, cy, w * 0.28f, Palette.Glass)
    circle(w / 2 - 1.5f, cy + 1f, 1.6f, Palette.Blue, outline = false)
    circle(w / 2 + 1.6f, cy - 0.8f, 1.3f, Palette.Pink, outline = false)
    line(w / 2 - 3f, cy - 2.5f, w / 2 - 1f, cy - 4f, Color.White, 0.6f)
}

private fun Sketch.dryingRack() {
    shadow(1f, h - 3f, w - 2f, 4f)
    line(1f, h, 4f, 3f, Palette.MetalDark, 0.8f)
    line(w - 1f, h, w - 4f, 3f, Palette.MetalDark, 0.8f)
    line(3f, 3f, w - 3f, 3f, Palette.Metal, 0.9f)
    line(2f, 9f, w - 2f, 9f, Palette.Metal, 0.7f)
    // A shirt, socks and a towel.
    poly(Palette.Blue, 7f, 3f, 14f, 3f, 16f, 6f, 14f, 7f, 14f, 15f, 7f, 15f, 7f, 7f, 5f, 6f)
    rect(17f, 3f, 2.5f, 6f, Palette.Yellow, radius = 0.6f)
    rect(20.5f, 3f, 2.5f, 6f, Palette.Yellow, radius = 0.6f)
    rect(24f, 3f, 7f, 13f, Palette.Pink, radius = 0.5f)
    line(24f, 13.5f, 31f, 13.5f, Color.White, 0.6f)
}

private fun Sketch.linenCupboard() {
    shadow()
    block(0f, 0f, w, h - 0.5f, front = h - 3.5f, top = Palette.WoodLight, frontColor = Palette.Wood)
    rect(1.5f, 4.5f, w - 3f, h - 7f, Color(0xFF6E4A2B), radius = 0.3f, outline = false)
    val shelf = (h - 7f) / 3
    val towels = listOf(Color.White, Color(0xFF8FD3C9), Color(0xFFFFB3A7), Palette.Yellow)
    for (s in 0 until 3) {
        val bottom = 4.5f + (s + 1) * shelf
        for (k in 0 until 3) {
            rect(2.5f, bottom - 2.4f * (k + 1), w - 5f, 2.2f, towels[(s + k) % towels.size], radius = 0.8f)
        }
        rect(1f, bottom - 0.2f, w - 2f, 1f, Palette.WoodLight, radius = 0f, outline = false)
    }
}
