package com.devanshu.miniverse.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * MiniVerse FX kit: small reusable effects that give the app its
 * "futuristic animated" feel without a 3D engine.
 */

/** 3D-style press: the element squashes and tilts while touched, then springs back. */
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    val tilt by animateFloatAsState(
        targetValue = if (pressed) 6f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressTilt",
    )
    this
        .graphicsLayer {
            scaleX = scale; scaleY = scale
            rotationX = tilt
            cameraDistance = 12f * density
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Endless drifting gradient - the signature header background. */
@Composable
fun Modifier.animatedGradient(colors: List<Color>): Modifier {
    val t = rememberInfiniteTransition(label = "grad")
    val shift by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradShift",
    )
    return this.background(
        Brush.linearGradient(
            colors = colors,
            start = Offset(1200f * shift, 0f),
            end = Offset(1200f * shift + 900f, 1400f),
        )
    )
}

/** Gently floating element (idle hover), like objects in zero gravity. */
fun Modifier.floaty(amplitude: Dp = 4.dp, periodMs: Int = 2400): Modifier = composed {
    val t = rememberInfiniteTransition(label = "float")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "floatPhase",
    )
    val amp = amplitude
    this.graphicsLayer { translationY = sin(phase) * amp.toPx() }
}

/** Pulsing glow halo drawn behind content (fake neon light). */
fun Modifier.pulseGlow(color: Color): Modifier = composed {
    val t = rememberInfiniteTransition(label = "glow")
    val a by t.animateFloat(
        initialValue = 0.25f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    this.background(
        Brush.radialGradient(listOf(color.copy(alpha = a), Color.Transparent)),
    )
}

private class Particle(
    val x: Float, val delay: Float, val speed: Float,
    val color: Color, val size: Float, val sway: Float,
)

/** Confetti rain for the result screen. Draws for ~3 s then stays quiet. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        val palette = listOf(MvYellow, MvCoral, MvSky, MvMint, MvPink, Color(0xFF9D7BFF))
        List(90) {
            Particle(
                x = Random.nextFloat(), delay = Random.nextFloat() * 0.5f,
                speed = 0.55f + Random.nextFloat() * 0.7f,
                color = palette[it % palette.size],
                size = 8f + Random.nextFloat() * 12f,
                sway = 20f + Random.nextFloat() * 50f,
            )
        }
    }
    val t = rememberInfiniteTransition(label = "confetti")
    val time by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "confettiTime",
    )
    Canvas(modifier.fillMaxSize()) {
        particles.forEach { p ->
            val tt = (time - p.delay)
            if (tt > 0f) {
                val y = (tt * p.speed) % 1.1f
                val x = p.x * size.width + sin(y * 12f + p.x * 20f) * p.sway
                drawCircle(p.color, radius = p.size / 2, center = Offset(x, y * size.height))
            }
        }
    }
}

/** Starfield background dots for dark screens. */
@Composable
fun StarField(modifier: Modifier = Modifier) {
    val stars = remember {
        List(60) { Triple(Random.nextFloat(), Random.nextFloat(), 1f + Random.nextFloat() * 2.2f) }
    }
    val t = rememberInfiniteTransition(label = "stars")
    val tw by t.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "twinkle",
    )
    Canvas(modifier.fillMaxSize()) {
        stars.forEachIndexed { i, (sx, sy, r) ->
            drawCircle(
                Color.White.copy(alpha = if (i % 3 == 0) tw * 0.8f else 0.35f),
                radius = r, center = Offset(sx * size.width, sy * size.height),
            )
        }
    }
}
