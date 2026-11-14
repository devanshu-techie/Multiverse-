package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.games.tank.BIG_RADIUS
import com.devanshu.miniverse.games.tank.Fire
import com.devanshu.miniverse.games.tank.GRAVITY
import com.devanshu.miniverse.games.tank.HITS_TO_WIN
import com.devanshu.miniverse.games.tank.HIT_RADIUS
import com.devanshu.miniverse.games.tank.TankGame
import com.devanshu.miniverse.games.tank.TankState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tank Battle scene. The shell's flight on screen uses the same physics as
 * the rules engine (angle, power, gravity, wind), so what you see is exactly
 * what counts. The engine is only told about the shot when the shell lands.
 */
@Composable
internal fun TankBoard(state: TankState, vm: GameplayViewModel) {
    val colors = listOf(PlayerColors[0], PlayerColors[3])
    val current = state.players[state.turnIndex]
    val over = TankGame.getOutcome(state) != GameOutcome.Incomplete
    val scope = rememberCoroutineScope()
    var angle by remember { mutableFloatStateOf(45f) }
    var power by remember { mutableFloatStateOf(25f) }
    var big by remember { mutableStateOf(false) }
    var firing by remember { mutableStateOf(false) }
    val flight = remember { Animatable(0f) }      // 0..1 of the shell's time in the air
    val boom = remember { Animatable(0f) }        // 0..1 explosion
    var boomAt by remember { mutableStateOf<Float?>(null) }   // world x of the last explosion (from shooter)
    var boomBig by remember { mutableStateOf(false) }
    var shotBy by remember { mutableStateOf(0) }

    val bigLeft = state.bigLeft[current.id] ?: 0
    if (bigLeft <= 0 && big) big = false

    // flight maths shared by the preview and the animation
    val rad = Math.toRadians(angle.toDouble())
    val vx = (power * cos(rad)).toFloat()
    val vy = (power * sin(rad)).toFloat()
    val airTime = 2f * vy / GRAVITY

    fun fire() {
        if (firing || over) return
        firing = true
        shotBy = state.turnIndex
        val shotAngle = angle; val shotPower = power; val shotBig = big
        val land = TankGame.landingX(shotAngle, shotPower, state.wind)
        Sfx.play(Sfx.CANNON)
        scope.launch {
            boom.snapTo(0f); boomAt = null
            flight.snapTo(0f)
            flight.animateTo(1f, tween((airTime * 330f).toInt().coerceIn(700, 2400), easing = LinearEasing))
            boomAt = land; boomBig = shotBig
            Sfx.play(Sfx.BLAST)
            boom.animateTo(1f, tween(550))
            vm.submit(Fire(shotAngle, shotPower, shotBig), current.id)
            flight.snapTo(0f)
            firing = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DuelHeader(
            players = state.players, turnIndex = if (over) -1 else state.turnIndex, colors = colors,
            badges = listOf("1", "2"),
            scores = state.players.map { "${state.hits[it.id] ?: 0} / $HITS_TO_WIN hits" },
        )
        Surface(shape = RoundedCornerShape(14.dp), color = if (state.lastHit == true) Color(0xFFFF5A6E) else Color(0x33FFFFFF)) {
            Text(
                when {
                    firing -> "💨 Shell in the air..."
                    state.lastHit == true -> "💥 DIRECT HIT!"
                    state.lastLanding != null ->
                        "Last shell landed ${abs(state.lastLanding - state.distance).toInt()} m " +
                            (if (state.lastLanding < state.distance) "short" else "too far")
                    else -> "${current.name}, aim and fire!"
                },
                fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }

        // ---------------- the battlefield ----------------
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1.35f).padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)),
        ) {
            val w = size.width; val h = size.height
            val ground = h * 0.8f
            val leftX = w * 0.09f; val rightX = w * 0.91f
            val span = rightX - leftX                       // pixels between the tanks
            val scale = span / state.distance                // pixels per metre

            // sky, sun, ground
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2B1B6B), Color(0xFF6A3DF5), Color(0xFFFF8AC2)), endY = ground))
            drawCircle(Color(0xFFFFE066), radius = w * 0.06f, center = Offset(w * 0.8f, h * 0.2f))
            drawRect(Color(0xFF2FB368), topLeft = Offset(0f, ground), size = Size(w, h - ground))
            drawRect(Color(0xFF1E8E4E), topLeft = Offset(0f, ground), size = Size(w, h * 0.03f))
            // hill between the tanks
            val hill = Path().apply {
                moveTo(w * 0.34f, ground)
                quadraticBezierTo(w * 0.5f, ground - h * 0.3f, w * 0.66f, ground)
                close()
            }
            drawPath(hill, Color(0xFF239A57))

            // wind flag (relative to the shooter's direction)
            val dirNow = if (state.turnIndex == 0) 1f else -1f
            val flagX = w * 0.5f; val flagTop = ground - h * 0.44f
            drawLine(Color.White, Offset(flagX, ground - h * 0.3f), Offset(flagX, flagTop), strokeWidth = 5f)
            val flagLen = state.wind * dirNow * w * 0.05f
            val flag = Path().apply {
                moveTo(flagX, flagTop); lineTo(flagX + flagLen, flagTop + h * 0.03f); lineTo(flagX, flagTop + h * 0.06f); close()
            }
            drawPath(flag, Color(0xFFFFC93C))

            // tanks
            fun tank(x: Float, color: Color, facing: Float, barrelAngle: Float, alive: Boolean) {
                val bodyW = w * 0.11f; val bodyH = h * 0.07f
                drawRoundRect(Color(0xFF1B1730), topLeft = Offset(x - bodyW / 2 - 4f, ground - bodyH - 4f),
                    size = Size(bodyW + 8f, bodyH + 8f), cornerRadius = CornerRadius(14f, 14f))
                drawRoundRect(color, topLeft = Offset(x - bodyW / 2, ground - bodyH), size = Size(bodyW, bodyH),
                    cornerRadius = CornerRadius(12f, 12f))
                // wheels
                for (i in -1..1) drawCircle(Color(0xFF1B1730), radius = bodyH * 0.28f, center = Offset(x + i * bodyW * 0.3f, ground - bodyH * 0.12f))
                // turret + barrel
                val turret = Offset(x, ground - bodyH)
                val a = Math.toRadians(barrelAngle.toDouble())
                val tip = Offset(turret.x + facing * cos(a).toFloat() * bodyW * 0.75f, turret.y - sin(a).toFloat() * bodyW * 0.75f)
                drawLine(Color(0xFF1B1730), turret, tip, strokeWidth = bodyH * 0.42f, cap = StrokeCap.Round)
                drawLine(color, turret, tip, strokeWidth = bodyH * 0.26f, cap = StrokeCap.Round)
                drawCircle(Color(0xFF1B1730), radius = bodyH * 0.5f, center = turret)
                drawCircle(if (alive) color else Color.Gray, radius = bodyH * 0.4f, center = turret)
            }
            tank(leftX, colors[0], 1f, if (state.turnIndex == 0) angle else 35f, true)
            tank(rightX, colors[1], -1f, if (state.turnIndex == 1) angle else 35f, true)

            val shooterX = if ((if (firing) shotBy else state.turnIndex) == 0) leftX else rightX
            val dir = if ((if (firing) shotBy else state.turnIndex) == 0) 1f else -1f
            fun shellAt(t: Float): Offset {
                val x = vx * t + 0.5f * state.wind * t * t
                val y = vy * t - 0.5f * GRAVITY * t * t
                return Offset(shooterX + dir * x * scale, ground - h * 0.07f - y * scale)
            }

            // aiming dots: the first part of the path, so kids can learn the arc
            if (!firing && !over) {
                for (i in 1..7) {
                    val p = shellAt(airTime * i / 28f)
                    drawCircle(Color.White.copy(alpha = 0.7f - i * 0.07f), radius = 5f, center = p)
                }
            }
            // the shell with a smoke trail
            if (firing && flight.value > 0f && flight.value < 1f) {
                val tNow = airTime * flight.value
                for (i in 1..6) {
                    val p = shellAt((tNow - i * airTime * 0.02f).coerceAtLeast(0f))
                    drawCircle(Color.White.copy(alpha = 0.35f - i * 0.05f), radius = 9f - i, center = p)
                }
                val p = shellAt(tNow)
                drawCircle(Color(0xFF1B1730), radius = if (big) 15f else 10f, center = p)
                drawCircle(if (big) Color(0xFFFF5A6E) else Color(0xFFFFC93C), radius = if (big) 11f else 7f, center = p)
            }
            // explosion where the shell landed
            boomAt?.let { land ->
                if (boom.value > 0f && boom.value < 1f) {
                    val sx = if (shotBy == 0) leftX else rightX
                    val d = if (shotBy == 0) 1f else -1f
                    val c = Offset(sx + d * land * scale, ground - h * 0.02f)
                    val r = (if (boomBig) BIG_RADIUS else HIT_RADIUS) * scale * (0.6f + boom.value * 1.4f)
                    drawCircle(Color(0xFFFF5A6E).copy(alpha = 1f - boom.value), radius = r, center = c)
                    drawCircle(Color(0xFFFFC93C).copy(alpha = 1f - boom.value), radius = r * 0.65f, center = c)
                    drawCircle(Color.White.copy(alpha = 1f - boom.value), radius = r * 0.3f, center = c)
                    for (i in 0 until 8) {
                        val ang = i * 0.785f
                        drawLine(Color(0xFFFFE066).copy(alpha = 1f - boom.value),
                            c, Offset(c.x + cos(ang) * r * 1.5f, c.y - abs(sin(ang)) * r * 1.5f), strokeWidth = 5f)
                    }
                }
            }
        }

        // ---------------- controls ----------------
        Text(
            "Distance ${state.distance.toInt()} m   ·   Wind " +
                (if (state.wind > 0) "pushes your shell forward " else if (state.wind < 0) "pushes your shell back " else "calm ") +
                (if (state.wind != 0f) "${abs(state.wind)}" else ""),
            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f),
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeaponChip("🚀 Rocket", "precise", !big, Modifier.weight(1f)) { big = false }
            WeaponChip("💣 Mega Bomb", "$bigLeft left · wide blast", big, Modifier.weight(1f), enabled = bigLeft > 0) { big = true }
        }
        Text("Angle ${angle.toInt()}°", fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        Slider(value = angle, onValueChange = { angle = it }, valueRange = 10f..80f, enabled = !firing,
            modifier = Modifier.fillMaxWidth(0.92f))
        Text("Power ${power.toInt()}", fontWeight = FontWeight.Black, color = Color.White)
        Slider(value = power, onValueChange = { power = it }, valueRange = 10f..40f, enabled = !firing,
            modifier = Modifier.fillMaxWidth(0.92f))
        Button(
            onClick = { fire() },
            enabled = !firing && !over,
            colors = ButtonDefaults.buttonColors(containerColor = colors[state.turnIndex.coerceIn(0, 1)]),
            modifier = Modifier.fillMaxWidth().height(58.dp),
        ) { Text("🔥 ${current.name}: FIRE!", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B1730)) }
    }
}

@Composable
private fun WeaponChip(title: String, note: String, selected: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFFFC93C) else Color(0x33FFFFFF),
        modifier = modifier.clickable(enabled = enabled) { Sfx.play(Sfx.TAP); onClick() },
    ) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp,
                color = if (selected) Color(0xFF1B1730) else if (enabled) Color.White else Color.Gray)
            Text(note, fontSize = 10.sp, color = if (selected) Color(0xFF1B1730) else Color.White.copy(alpha = 0.7f))
        }
    }
}
