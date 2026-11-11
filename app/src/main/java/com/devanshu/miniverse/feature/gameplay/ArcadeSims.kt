package com.devanshu.miniverse.feature.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.games.airhockey.GOALS_TO_WIN
import com.devanshu.miniverse.games.airhockey.GoalScored
import com.devanshu.miniverse.games.airhockey.HockeyState
import com.devanshu.miniverse.games.arcade.ScoreState
import com.devanshu.miniverse.games.arcade.SubmitScore
import com.devanshu.miniverse.games.sumo.SumoRoundWon
import com.devanshu.miniverse.games.sumo.SumoState
import com.devanshu.miniverse.ui.theme.MvCoral
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvLemon
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.SeatColors
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

// ====================== Air Hockey (live, two fingers) ======================

@Composable
internal fun AirHockeyBoard(state: HockeyState, vm: GameplayViewModel) {
    val p0 = state.players[0]; val p1 = state.players[1]
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    var puck by remember { mutableStateOf(Offset.Zero) }
    var vel by remember { mutableStateOf(Offset.Zero) }
    var pad0 by remember { mutableStateOf(Offset.Zero) }   // bottom player
    var pad1 by remember { mutableStateOf(Offset.Zero) }   // top player
    var started by remember { mutableStateOf(false) }

    fun reset(towardTop: Boolean) {
        val w = size.width.toFloat(); val h = size.height.toFloat()
        puck = Offset(w / 2, h / 2)
        vel = Offset(0f, if (towardTop) -5f else 5f)
        pad0 = Offset(w / 2, h * 0.85f)
        pad1 = Offset(w / 2, h * 0.15f)
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "${p1.name} ${state.goals[p1.id]}  -  ${state.goals[p0.id]} ${p0.name}   (first to $GOALS_TO_WIN)",
            fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp),
        )
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(MvDark)
                .onSizeChanged { size = it; if (!started) { started = true; reset(true) } },
        ) {
            val r = size.width * 0.045f           // puck radius
            val pr = size.width * 0.075f          // paddle radius
            val goalHalf = size.width * 0.18f

            LaunchedEffect(size, state.goals) {
                var last = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (last != 0L) {
                            val w = size.width.toFloat(); val h = size.height.toFloat()
                            var p = puck + vel
                            var v = vel * 0.998f
                            if (p.x < r) { p = p.copy(x = r); v = v.copy(x = -v.x) }
                            if (p.x > w - r) { p = p.copy(x = w - r); v = v.copy(x = -v.x) }
                            val inGoalX = abs(p.x - w / 2) < goalHalf
                            if (p.y < r) {
                                if (inGoalX) { vm.submit(GoalScored(p0.id), p0.id); reset(false) }
                                else { p = p.copy(y = r); v = v.copy(y = -v.y) }
                            }
                            if (p.y > h - r) {
                                if (inGoalX) { vm.submit(GoalScored(p1.id), p1.id); reset(true) }
                                else { p = p.copy(y = h - r); v = v.copy(y = -v.y) }
                            }
                            // paddle collisions
                            for (pad in listOf(pad0, pad1)) {
                                val d = p - pad
                                val dist = sqrt(d.x * d.x + d.y * d.y)
                                if (dist < r + pr && dist > 1f) {
                                    val n = Offset(d.x / dist, d.y / dist)
                                    p = pad + Offset(n.x * (r + pr), n.y * (r + pr))
                                    val speed = maxOf(sqrt(v.x * v.x + v.y * v.y), 7f) * 1.05f
                                    v = Offset(n.x * speed, n.y * speed)
                                }
                            }
                            if (p != puck || v != vel) { puck = p; vel = v }
                        }
                        last = now
                    }
                }
            }
            // centre line + goals
            Canvas(Modifier.fillMaxSize()) {
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, this.size.height / 2),
                    Offset(this.size.width, this.size.height / 2), strokeWidth = 4f)
                drawLine(MvYellow, Offset(this.size.width / 2 - goalHalf, 6f),
                    Offset(this.size.width / 2 + goalHalf, 6f), strokeWidth = 12f)
                drawLine(MvYellow, Offset(this.size.width / 2 - goalHalf, this.size.height - 6f),
                    Offset(this.size.width / 2 + goalHalf, this.size.height - 6f), strokeWidth = 12f)
                drawCircle(Color.White, radius = r, center = puck)
                drawCircle(SeatColors[1], radius = pr, center = pad1)
                drawCircle(SeatColors[0], radius = pr, center = pad0)
            }
            // top half drag (player 2)
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter)
                    .pointerInput(size) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            pad1 = Offset(
                                change.position.x.coerceIn(0f, size.width.toFloat()),
                                change.position.y.coerceIn(0f, size.height * 0.5f),
                            )
                        }
                    },
            )
            // bottom half drag (player 1)
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                    .pointerInput(size) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            pad0 = Offset(
                                change.position.x.coerceIn(0f, size.width.toFloat()),
                                (size.height * 0.5f + change.position.y).coerceIn(size.height * 0.5f, size.height.toFloat()),
                            )
                        }
                    },
            )
        }
        Text("Drag your paddle. Defend the yellow goal!", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp))
    }
}

// ====================== Mini Sumo (tap battle) ======================

@Composable
internal fun SumoBoard(state: SumoState, vm: GameplayViewModel) {
    val p0 = state.players[0]; val p1 = state.players[1]
    var pos by remember { mutableFloatStateOf(0f) }      // -1 (p0 out) .. +1 (p1 out)
    var velocity by remember { mutableFloatStateOf(0f) }
    var roundLive by remember { mutableStateOf(true) }

    LaunchedEffect(state.roundWins) { pos = 0f; velocity = 0f; roundLive = true }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && roundLive) {
                    pos += velocity
                    velocity *= 0.92f
                    if (pos <= -1f) { roundLive = false; vm.submit(SumoRoundWon(p1.id), p1.id) }
                    if (pos >= 1f) { roundLive = false; vm.submit(SumoRoundWon(p0.id), p0.id) }
                }
                last = now
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("${p0.name} ${state.roundWins[p0.id]}  -  ${state.roundWins[p1.id]} ${p1.name}   (best of 3)",
            fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MvLemon),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.35f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(100.dp)).background(MvOrangeSoft),
            )
            Text("🤼", fontSize = 64.sp, modifier = Modifier.graphicsLayer {
                translationX = pos * 380f
            })
        }
        Row(Modifier.height(130.dp).padding(top = 8.dp)) {
            Surface(
                color = SeatColors[0], shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 4.dp)
                    .clickable(enabled = roundLive) { velocity += 0.028f },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${p0.name}\nTAP TAP TAP →", color = Color.White, fontWeight = FontWeight.Black,
                        fontSize = 16.sp)
                }
            }
            Surface(
                color = SeatColors[1], shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp)
                    .clickable(enabled = roundLive) { velocity -= 0.028f },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("← TAP TAP TAP\n${p1.name}", color = Color.White, fontWeight = FontWeight.Black,
                        fontSize = 16.sp)
                }
            }
        }
    }
}

private val MvOrangeSoft = Color(0xFFFFD9A8)

// ====================== Score-run harness (Pinball / Runner / Balloon) ======================

@Composable
internal fun ScoreRunHost(state: ScoreState, vm: GameplayViewModel) {
    if (state.runnerIndex >= state.players.size) return
    val runner = state.players[state.runnerIndex]
    var runStarted by remember(state.runnerIndex) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.players.forEachIndexed { i, p ->
                Text(
                    "${p.name}: ${state.scores[p.id]?.toString() ?: "-"}",
                    color = SeatColors[i], fontWeight = FontWeight.Bold,
                )
            }
        }
        if (!runStarted) {
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(MvPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(runner.name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = MvPurple)
                    Text("get ready for your run!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { runStarted = true }, modifier = Modifier.padding(top = 14.dp)) {
                        Text("GO!", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            val end: (Int) -> Unit = { score -> vm.submit(SubmitScore(score), runner.id) }
            Box(Modifier.weight(1f)) {
                when (state.gameId) {
                    "pinball" -> PinballSim(runner, onEnd = end)
                    "runner" -> RunnerSim(runner, onEnd = end)
                    else -> BalloonSim(runner, onEnd = end)
                }
            }
        }
    }
}

// ---------- Pinball ----------

@Composable
private fun PinballSim(runner: GamePlayer, onEnd: (Int) -> Unit) {
    var size by remember { mutableStateOf(IntSize(1, 1)) }
    var ball by remember { mutableStateOf(Offset(0.3f, 0.2f)) }  // normalized
    var vel by remember { mutableStateOf(Offset(0.004f, 0f)) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var flash by remember { mutableIntStateOf(-1) }
    val bumpers = listOf(Offset(0.25f, 0.3f), Offset(0.75f, 0.3f), Offset(0.5f, 0.52f))

    LaunchedEffect(Unit) {
        var last = 0L
        while (lives > 0) {
            withFrameNanos { now ->
                if (last != 0L) {
                    var v = vel + Offset(0f, 0.00022f)          // gravity
                    var b = ball + v
                    if (b.x < 0.04f) { b = b.copy(x = 0.04f); v = v.copy(x = -v.x * 0.9f) }
                    if (b.x > 0.96f) { b = b.copy(x = 0.96f); v = v.copy(x = -v.x * 0.9f) }
                    if (b.y < 0.04f) { b = b.copy(y = 0.04f); v = v.copy(y = -v.y * 0.9f) }
                    bumpers.forEachIndexed { i, bump ->
                        val d = b - bump
                        val dist = sqrt(d.x * d.x + d.y * d.y)
                        if (dist < 0.09f && dist > 0.001f) {
                            val n = Offset(d.x / dist, d.y / dist)
                            b = bump + Offset(n.x * 0.09f, n.y * 0.09f)
                            v = Offset(n.x * 0.009f, n.y * 0.009f)
                            score += 5; flash = i
                        }
                    }
                    if (b.y > 1.05f) {
                        lives -= 1
                        b = Offset(0.3f + 0.4f * Random.nextFloat(), 0.2f)
                        v = Offset(0.004f, 0f)
                    }
                    ball = b; vel = v
                }
                last = now
            }
        }
        onEnd(score)
    }

    Column(Modifier.fillMaxSize()) {
        Text("Score $score   ·   Balls ${"●".repeat(lives)}", fontWeight = FontWeight.Black)
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MvDark)
                .onSizeChanged { size = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width; val h = this.size.height
                bumpers.forEachIndexed { i, bump ->
                    drawCircle(
                        if (i == flash) MvYellow else MvPink,
                        radius = w * 0.07f, center = Offset(bump.x * w, bump.y * h),
                    )
                }
                drawCircle(Color.White, radius = w * 0.035f, center = Offset(ball.x * w, ball.y * h))
            }
        }
        Row(Modifier.height(86.dp).padding(top = 6.dp)) {
            Button(
                onClick = { if (ball.y > 0.6f) { vel = Offset(0.006f, -0.013f) } },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 4.dp),
            ) { Text("FLIP ↖", fontSize = 17.sp, fontWeight = FontWeight.Black) }
            Button(
                onClick = { if (ball.y > 0.6f) { vel = Offset(-0.006f, -0.013f) } },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 4.dp),
            ) { Text("↗ FLIP", fontSize = 17.sp, fontWeight = FontWeight.Black) }
        }
    }
}

// ---------- Crazy Runner ----------

@Composable
private fun RunnerSim(runner: GamePlayer, onEnd: (Int) -> Unit) {
    var playerY by remember { mutableFloatStateOf(0f) }      // 0 = ground, up is negative
    var vy by remember { mutableFloatStateOf(0f) }
    var obstacles by remember { mutableStateOf(listOf(1.3f, 2.1f)) }  // x positions (1 = right edge)
    var distance by remember { mutableIntStateOf(0) }
    var alive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var last = 0L
        var speed = 0.008f
        while (alive) {
            withFrameNanos { now ->
                if (last != 0L) {
                    vy += 0.0016f
                    playerY = (playerY + vy).coerceAtMost(0f)
                    if (playerY == 0f) vy = 0f
                    speed += 0.0000015f
                    obstacles = obstacles.map { it - speed }
                    if (obstacles.any { it < 0.02f }) {
                        obstacles = obstacles.filter { it >= 0.02f } +
                            (1.1f + Random.nextFloat() * 0.6f)
                    }
                    distance += 1
                    val hit = obstacles.any { it in 0.10f..0.20f } && playerY > -0.09f
                    if (hit) alive = false
                }
                last = now
            }
        }
        onEnd(distance / 10)
    }

    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(MvSky.copy(alpha = 0.25f))
            .pointerInput(Unit) { detectTapGestures { if (playerY == 0f) vy = -0.022f } },
    ) {
        Text("Distance ${distance / 10}", fontWeight = FontWeight.Black,
            modifier = Modifier.padding(10.dp))
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            val ground = h * 0.8f
            drawLine(MvDark, Offset(0f, ground), Offset(w, ground), strokeWidth = 6f)
            // player
            drawCircle(MvPurple, radius = h * 0.045f,
                center = Offset(w * 0.15f, ground + playerY * h - h * 0.045f))
            // obstacles
            obstacles.forEach { ox ->
                drawRoundRect(
                    MvMint,
                    topLeft = Offset(ox * w, ground - h * 0.08f),
                    size = androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.08f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                )
            }
        }
        Text("TAP TO JUMP", fontSize = 12.sp, color = MvDark.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

// ---------- Balloon Pop ----------

private data class Balloon(val id: Int, var x: Float, var y: Float, val speed: Float, val bomb: Boolean)

@Composable
private fun BalloonSim(runner: GamePlayer, onEnd: (Int) -> Unit) {
    var balloons by remember { mutableStateOf(listOf<Balloon>()) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var nextId by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        var last = 0L
        var elapsed = 0L
        var spawnAcc = 0L
        while (timeLeft > 0) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = now - last
                    elapsed += dt; spawnAcc += dt
                    timeLeft = (30 - elapsed / 1_000_000_000L).toInt().coerceAtLeast(0)
                    if (spawnAcc > 650_000_000L) {
                        spawnAcc = 0L
                        balloons = balloons + Balloon(
                            nextId++, 0.08f + Random.nextFloat() * 0.84f, 1.05f,
                            0.003f + Random.nextFloat() * 0.004f, Random.nextInt(6) == 0,
                        )
                    }
                    balloons = balloons.map { it.apply { y -= speed } }.filter { it.y > -0.1f }
                }
                last = now
            }
        }
        onEnd(score.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(MvSky.copy(alpha = 0.2f))) {
        Text("⏱ $timeLeft s   ·   Score $score", fontWeight = FontWeight.Black,
            modifier = Modifier.padding(10.dp))
        balloons.forEach { b ->
            Text(
                if (b.bomb) "💣" else "🎈",
                fontSize = 40.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = b.x * size.width
                        translationY = b.y * size.height
                    }
                    .clickable {
                        balloons = balloons.filter { it.id != b.id }
                        score += if (b.bomb) -2 else 1
                    },
            )
        }
    }
}
