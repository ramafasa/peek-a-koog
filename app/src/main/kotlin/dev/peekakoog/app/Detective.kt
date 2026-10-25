package dev.peekakoog.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

enum class Pose { IDLE, WALKING, PEEKING, LISTENING, CELEBRATING, CONFUSED }

private val Coat = Color(0xFFC68E52)
private val CoatDark = Color(0xFF8A5A33)
private val Hat = Color(0xFF33334A)
private val KotlinPurple = Color(0xFF7F52FF)
private val Skin = Color(0xFFF5CBA7)
private val Ink = Color(0xFF2A2533)
private val Glass = Color(0x99BFE6FF)

/**
 * Koog, the detective. Drawn in a 100 × 100 box whose bottom centre is [feet]; [height] is the box size in pixels.
 * [phase] drives the walking cycle (radians).
 */
fun DrawScope.drawDetective(feet: Offset, height: Float, pose: Pose, facingLeft: Boolean, phase: Float) {
    val k = height / 100f
    val stroke = 7f * k
    fun p(x: Float, y: Float) = Offset(x * k, y * k)

    translate(feet.x - 50f * k, feet.y - 100f * k) {
        scale(if (facingLeft) -1f else 1f, 1f, pivot = p(50f, 50f)) {
            // Shadow.
            drawOval(Color.Black.copy(alpha = 0.22f), p(26f, 94f), Size(48f * k, 8f * k))

            // Legs.
            val swing = if (pose == Pose.WALKING) sin(phase) * 0.5f else 0f
            listOf(42f to swing, 58f to -swing).forEach { (x, angle) ->
                val hip = p(x, 74f)
                val foot = hip + Offset(sin(angle) * 22f * k, kotlin.math.cos(angle) * 22f * k)
                drawLine(Hat, hip, foot, stroke, cap = StrokeCap.Round)
                drawOval(Ink, foot + p(-5f, -2f), Size(10f * k, 5f * k))
            }

            // Trench coat.
            val coat = Path().apply {
                moveTo(36f * k, 40f * k)
                lineTo(64f * k, 40f * k)
                lineTo(72f * k, 80f * k)
                quadraticTo(50f * k, 84f * k, 28f * k, 80f * k)
                close()
            }
            drawPath(coat, Coat)
            drawPath(coat, CoatDark, style = Stroke(1.5f * k))
            drawLine(CoatDark, p(50f, 44f), p(50f, 81f), 1.5f * k)
            drawLine(CoatDark, p(32f, 62f), p(68f, 62f), 3.5f * k)
            drawRect(Ink, p(47f, 60f), Size(6f * k, 4f * k))

            // Left arm.
            val leftHand = if (pose == Pose.CELEBRATING) p(24f, 26f) else p(28f, 66f)
            drawLine(Coat, p(37f, 46f), leftHand, stroke, cap = StrokeCap.Round)
            drawCircle(Skin, 3.5f * k, leftHand)

            // Head.
            val head = p(50f, 28f)
            drawCircle(Skin, 14f * k, head)
            val eyeY = 29f
            if (pose == Pose.PEEKING) {
                drawCircle(Ink, 2f * k, p(46f, eyeY))
                drawCircle(Ink, 3.2f * k, p(56f, eyeY))
            } else {
                drawCircle(Ink, 2f * k, p(46f, eyeY))
                drawCircle(Ink, 2f * k, p(56f, eyeY))
            }
            when (pose) {
                Pose.CELEBRATING -> drawArc(Ink, 10f, 160f, false, p(45f, 31f), Size(10f * k, 6f * k), style = Stroke(1.6f * k))
                Pose.CONFUSED -> drawLine(Ink, p(46f, 36f), p(56f, 34f), 1.6f * k, cap = StrokeCap.Round)
                else -> drawLine(Ink, p(48f, 35.5f), p(55f, 35.5f), 1.6f * k, cap = StrokeCap.Round)
            }

            // Fedora with a Kotlin-purple band.
            drawOval(Hat, p(28f, 13f), Size(44f * k, 9f * k))
            drawRoundRect(Hat, p(37f, 1f), Size(26f * k, 16f * k), androidx.compose.ui.geometry.CornerRadius(5f * k))
            drawRect(KotlinPurple, p(37f, 11f), Size(26f * k, 4f * k))

            // Right arm and the magnifying glass.
            when (pose) {
                Pose.PEEKING -> {
                    val hand = p(70f, 40f)
                    drawLine(Coat, p(63f, 46f), hand, stroke, cap = StrokeCap.Round)
                    magnifier(hand, ring = p(82f, 28f), radius = 11f * k, k = k)
                }
                Pose.LISTENING -> {
                    val hand = p(66f, 24f)
                    drawLine(Coat, p(63f, 46f), hand, stroke, cap = StrokeCap.Round)
                    drawCircle(Skin, 3.5f * k, hand)
                    for (i in 1..3) {
                        drawArc(
                            KotlinPurple, -50f, 100f, false,
                            p(66f + i * 3f, 24f - i * 4f), Size(i * 8f * k, i * 8f * k),
                            style = Stroke(1.8f * k, cap = StrokeCap.Round),
                        )
                    }
                }
                Pose.CELEBRATING -> {
                    val hand = p(78f, 18f)
                    drawLine(Coat, p(63f, 46f), hand, stroke, cap = StrokeCap.Round)
                    magnifier(hand, ring = p(84f, 8f), radius = 7f * k, k = k)
                }
                else -> {
                    val hand = p(72f, 64f)
                    drawLine(Coat, p(63f, 46f), hand, stroke, cap = StrokeCap.Round)
                    magnifier(hand, ring = p(80f, 56f), radius = 7f * k, k = k)
                }
            }
        }
    }
}

private fun DrawScope.magnifier(hand: Offset, ring: Offset, radius: Float, k: Float) {
    drawLine(CoatDark, hand, ring, 3f * k, cap = StrokeCap.Round)
    drawCircle(Skin, 3.5f * k, hand)
    drawCircle(Glass, radius, ring)
    drawCircle(Ink, radius, ring, style = Stroke(2.5f * k))
    drawArc(Color.White, 200f, 60f, false, ring - Offset(radius * 0.6f, radius * 0.6f), Size(radius * 1.2f, radius * 1.2f), style = Stroke(1.5f * k))
}
