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

private val Outline = Color(0xFF3A3346)
private val Wood = Color(0xFFB98352)
private val WoodDark = Color(0xFF8A5A33)
private val White = Color(0xFFF8F6F2)
private val Metal = Color(0xFFB8C2CC)

/** Draws one piece of furniture, seen from above, inside [r] (pixels). [u] is pixels per house unit. */
fun DrawScope.drawFurniture(kind: Furniture, r: Rect, u: Float) {
    val line = Stroke(width = 0.8f * u)
    fun box(rect: Rect, color: Color, radius: Float = 2f) {
        drawRoundRect(color, rect.topLeft, rect.size, CornerRadius(radius * u))
        drawRoundRect(Outline, rect.topLeft, rect.size, CornerRadius(radius * u), style = line)
    }
    fun inset(dx: Float, dy: Float = dx) = Rect(r.left + dx * u, r.top + dy * u, r.right - dx * u, r.bottom - dy * u)

    when (kind) {
        Furniture.BED -> {
            box(r, WoodDark)
            box(inset(1.5f), Color(0xFF8E7CC3))
            box(Rect(r.left + 3 * u, r.top + 3 * u, r.right - 3 * u, r.top + 11 * u), White, 3f)
            drawLine(Color(0xFF6F5DAA), Offset(r.left + 1.5f * u, r.top + 16 * u), Offset(r.right - 1.5f * u, r.top + 16 * u), 1.2f * u)
        }
        Furniture.BUNK_BED -> {
            box(r, WoodDark)
            box(inset(1.5f), Color(0xFFFFB35C))
            box(Rect(r.left + 3 * u, r.top + 3 * u, r.right - 3 * u, r.top + 10 * u), White, 3f)
            // Ladder on the side.
            val x = r.right + 1.2f * u
            drawLine(WoodDark, Offset(x, r.top + 14 * u), Offset(x, r.bottom - 4 * u), 0.9f * u)
            for (i in 0..4) {
                val y = r.top + (16 + i * 5) * u
                drawLine(WoodDark, Offset(r.right - 0.5f * u, y), Offset(x + 1.5f * u, y), 0.7f * u)
            }
        }
        Furniture.WARDROBE -> {
            box(r, Wood)
            drawLine(Outline, Offset(r.center.x, r.top), Offset(r.center.x, r.bottom), 0.6f * u)
            drawCircle(Outline, 0.8f * u, Offset(r.center.x - 2 * u, r.center.y))
            drawCircle(Outline, 0.8f * u, Offset(r.center.x + 2 * u, r.center.y))
        }
        Furniture.CURTAIN -> {
            drawLine(Color(0xFF9FD3E6), Offset(r.right, r.top - 2 * u), Offset(r.right, r.bottom + 2 * u), 1.6f * u)
            val folds = 5
            val step = r.height / folds
            for (i in 0 until folds) {
                val top = r.top + i * step
                drawRoundRect(
                    if (i % 2 == 0) Color(0xFFE05D7A) else Color(0xFFC94A67),
                    Offset(r.left, top), Size(r.width, step), CornerRadius(1.5f * u),
                )
            }
            drawRoundRect(Outline, r.topLeft, r.size, CornerRadius(1.5f * u), style = line)
        }
        Furniture.TOY_BOX -> {
            box(r, Color(0xFF4FB0E8))
            box(inset(1.5f), Color(0xFF6CC3F2))
            drawStar(r.center, 3.5f * u, Color(0xFFFFD34D))
        }
        Furniture.TENT -> {
            val path = Path().apply {
                moveTo(r.center.x, r.top)
                lineTo(r.right, r.bottom)
                lineTo(r.left, r.bottom)
                close()
            }
            drawPath(path, Color(0xFFFF8A65))
            val stripe = Path().apply {
                moveTo(r.center.x, r.top)
                lineTo(r.center.x + r.width * 0.18f, r.bottom)
                lineTo(r.center.x - r.width * 0.18f, r.bottom)
                close()
            }
            drawPath(stripe, Color(0xFFFFE082))
            drawPath(path, Outline, style = line)
        }
        Furniture.BATHTUB -> {
            box(r, White, 6f)
            box(inset(2.5f), Color(0xFFA9DDF0), 5f)
            drawCircle(Metal, 1.2f * u, Offset(r.right - 4 * u, r.center.y))
        }
        Furniture.SINK -> {
            box(r, White)
            drawOval(Color(0xFFA9DDF0), Offset(r.left + 5 * u, r.top + 3 * u), Size(r.width - 10 * u, r.height - 6 * u))
            drawCircle(Metal, 1f * u, Offset(r.center.x, r.top + 2 * u))
        }
        Furniture.LAUNDRY -> {
            drawCircle(Color(0xFFD9B98C), r.width / 2, r.center)
            for (i in -2..2) {
                drawLine(WoodDark, Offset(r.center.x + i * 2.5f * u, r.top + 2 * u), Offset(r.center.x + i * 2.5f * u, r.bottom - 2 * u), 0.5f * u)
            }
            drawCircle(Color(0xFF7EC8E3), r.width / 4, r.center + Offset(-1.5f * u, -1 * u))
            drawCircle(Outline, r.width / 2, r.center, style = line)
        }
        Furniture.SOFA -> {
            box(r, Color(0xFF5C8D89), 4f)
            box(Rect(r.left + 4 * u, r.top + 4 * u, r.right - 4 * u, r.bottom - 2 * u), Color(0xFF79B0AB), 2f)
            drawLine(Outline, Offset(r.center.x, r.top + 4 * u), Offset(r.center.x, r.bottom - 2 * u), 0.5f * u)
        }
        Furniture.TV -> {
            box(r, Color(0xFF6B5B4B))
            box(Rect(r.left + 4 * u, r.top + 1 * u, r.right - 4 * u, r.top + 3.5f * u), Color(0xFF1E1E26), 1f)
        }
        Furniture.BOOKSHELF -> {
            box(r, Wood)
            val colors = listOf(0xFFE05D7A, 0xFF4FB0E8, 0xFFFFD34D, 0xFF7F52FF, 0xFF66BB6A)
            val bookH = (r.height - 4 * u) / 8
            for (i in 0 until 8) {
                drawRect(
                    Color(colors[i % colors.size]),
                    Offset(r.left + 1.5f * u + (i % 2) * u, r.top + 2 * u + i * bookH),
                    Size(r.width - 3.5f * u, bookH * 0.8f),
                )
            }
        }
        Furniture.COAT_RACK -> {
            drawCircle(WoodDark, 1.5f * u, r.center)
            val coats = listOf(0xFFC68E52, 0xFF5C6BC0, 0xFFE05D7A)
            coats.forEachIndexed { i, c ->
                val angle = Math.toRadians(90.0 + i * 120.0)
                val p = r.center + Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat()) * (4.5f * u)
                drawCircle(Color(c), 3.2f * u, p)
                drawCircle(Outline, 3.2f * u, p, style = line)
            }
        }
        Furniture.SHOE_CABINET -> {
            box(r, Wood)
            for (i in 1..2) {
                val x = r.left + r.width * i / 3
                drawLine(Outline, Offset(x, r.top), Offset(x, r.bottom), 0.5f * u)
            }
        }
        Furniture.FRIDGE -> {
            box(r, White)
            drawLine(Outline, Offset(r.left, r.top + r.height * 0.35f), Offset(r.right, r.top + r.height * 0.35f), 0.5f * u)
            drawLine(Metal, Offset(r.right - 2 * u, r.top + 2 * u), Offset(r.right - 2 * u, r.top + 5 * u), 0.8f * u, cap = StrokeCap.Round)
            drawLine(Metal, Offset(r.right - 2 * u, r.top + 9 * u), Offset(r.right - 2 * u, r.top + 14 * u), 0.8f * u, cap = StrokeCap.Round)
        }
        Furniture.OVEN -> {
            box(r, Color(0xFF9AA4AE))
            val q = Offset(r.width / 4, r.height / 4)
            listOf(Offset(-1f, -1f), Offset(1f, -1f), Offset(-1f, 1f), Offset(1f, 1f)).forEach { d ->
                val c = r.center + Offset(d.x * q.x, d.y * q.y)
                drawCircle(Color(0xFF3A3A44), 2.2f * u, c)
                drawCircle(Color(0xFFFF7043), 2.2f * u, c, style = Stroke(0.5f * u))
            }
        }
        Furniture.PANTRY -> {
            box(r, Wood)
            for (row in 0..2) {
                val y = r.top + (row + 0.5f) * r.height / 3
                for (col in 0..2) {
                    drawCircle(
                        Color(listOf(0xFFE53935, 0xFFFFB300, 0xFF43A047)[(row + col) % 3]),
                        1.6f * u,
                        Offset(r.left + (col + 0.5f) * r.width / 3, y),
                    )
                }
            }
        }
        Furniture.TABLE -> {
            val chair = Size(6 * u, 4 * u)
            listOf(0.25f, 0.75f).forEach { fx ->
                val x = r.left + r.width * fx - chair.width / 2
                drawRoundRect(WoodDark, Offset(x, r.top - 3 * u), chair, CornerRadius(u))
                drawRoundRect(WoodDark, Offset(x, r.bottom - 1 * u), chair, CornerRadius(u))
            }
            box(inset(0f, 1f), Wood, 3f)
            drawCircle(White, 2.5f * u, r.center)
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val rr = if (i % 2 == 0) radius else radius * 0.45f
        val p = center + Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat()) * rr
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color)
}
