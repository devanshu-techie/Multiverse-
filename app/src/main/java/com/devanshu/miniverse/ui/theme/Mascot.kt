package com.devanshu.miniverse.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Mini" - the MiniVerse mascot: a happy little planet wearing a crown, with
 * a ring and an orbiting moon. Drawn in code so it can blink, bob and spin.
 */
@Composable
fun MascotLogo(size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "mascot")
    val orbit by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "orbit",
    )
    val bob by t.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "bob",
    )
    // eyes are open most of the time and blink quickly
    val eyeOpen by t.animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 0; 1f at 2850; 0.1f at 2950; 1f at 3050; 1f at 3200
            },
        ),
        label = "blink",
    )

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val c = Offset(w / 2, w / 2 + bob * w * 0.02f)
        val r = w * 0.27f
        val ink = Color(0xFF1B1730)
        val ringColor = Color(0xFFFF8AC2)

        // back half of the ring
        rotate(-18f, c) {
            drawArc(ringColor, 180f, 180f, false, topLeft = Offset(c.x - r * 1.75f, c.y - r * 0.45f),
                size = Size(r * 3.5f, r * 0.9f), style = Stroke(w * 0.045f, cap = StrokeCap.Round))
        }
        // body
        drawCircle(ink, radius = r * 1.06f, center = c)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFFF3B0), Color(0xFFFFC93C), Color(0xFFFF9F1C)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.4f), radius = r * 1.6f,
            ),
            radius = r, center = c,
        )
        // front half of the ring
        rotate(-18f, c) {
            drawArc(ink, 0f, 180f, false, topLeft = Offset(c.x - r * 1.75f, c.y - r * 0.45f),
                size = Size(r * 3.5f, r * 0.9f), style = Stroke(w * 0.065f, cap = StrokeCap.Round))
            drawArc(ringColor, 0f, 180f, false, topLeft = Offset(c.x - r * 1.75f, c.y - r * 0.45f),
                size = Size(r * 3.5f, r * 0.9f), style = Stroke(w * 0.045f, cap = StrokeCap.Round))
        }
        // crown
        val top = c.y - r * 0.92f
        val crown = Path().apply {
            moveTo(c.x - r * 0.5f, top)
            lineTo(c.x - r * 0.62f, top - r * 0.55f)
            lineTo(c.x - r * 0.25f, top - r * 0.22f)
            lineTo(c.x, top - r * 0.7f)
            lineTo(c.x + r * 0.25f, top - r * 0.22f)
            lineTo(c.x + r * 0.62f, top - r * 0.55f)
            lineTo(c.x + r * 0.5f, top)
            close()
        }
        drawPath(crown, Color(0xFFFFE066))
        drawPath(crown, ink, style = Stroke(w * 0.02f))
        drawCircle(Color(0xFFFF5A6E), radius = r * 0.08f, center = Offset(c.x, top - r * 0.16f))

        // eyes
        val eyeY = c.y - r * 0.08f
        listOf(-1f, 1f).forEach { side ->
            val ex = c.x + side * r * 0.36f
            drawOval(Color.White, topLeft = Offset(ex - r * 0.2f, eyeY - r * 0.24f * eyeOpen),
                size = Size(r * 0.4f, r * 0.48f * eyeOpen))
            drawCircle(ink, radius = r * 0.11f * eyeOpen.coerceAtLeast(0.3f), center = Offset(ex + r * 0.03f, eyeY))
            drawCircle(Color.White, radius = r * 0.04f * eyeOpen, center = Offset(ex + r * 0.07f, eyeY - r * 0.05f))
            // rosy cheeks
            drawCircle(Color(0x66FF5A6E), radius = r * 0.13f, center = Offset(c.x + side * r * 0.6f, c.y + r * 0.28f))
        }
        // smile
        drawArc(ink, 20f, 140f, false, topLeft = Offset(c.x - r * 0.33f, c.y + r * 0.02f),
            size = Size(r * 0.66f, r * 0.5f), style = Stroke(w * 0.022f, cap = StrokeCap.Round))

        // orbiting moon
        val a = Math.toRadians(orbit.toDouble())
        val moon = Offset(c.x + cos(a).toFloat() * r * 1.75f, c.y + sin(a).toFloat() * r * 0.75f - r * 0.2f)
        drawCircle(ink, radius = w * 0.045f, center = moon)
        drawCircle(Color(0xFF4DB9F5), radius = w * 0.036f, center = moon)
    }
}
