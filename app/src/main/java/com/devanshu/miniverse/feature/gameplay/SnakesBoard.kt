package com.devanshu.miniverse.feature.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.games.snakes.LADDERS
import com.devanshu.miniverse.games.snakes.RollDice
import com.devanshu.miniverse.games.snakes.SNAKES
import com.devanshu.miniverse.games.snakes.SnakesGame
import com.devanshu.miniverse.games.snakes.SnlState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.sqrt

/** Centre of a square in cell units (x right, y down). Square 0 waits just left of square 1. */
private fun squareCenter(n: Int): Offset {
    if (n <= 0) return Offset(0.5f, 9.5f)
    val r = (n - 1) / 10
    val i = (n - 1) % 10
    val c = if (r % 2 == 0) i else 9 - i
    return Offset(c + 0.5f, 9 - r + 0.5f)
}

private val snakeColors = listOf(Color(0xFF39B54A), Color(0xFFE5484D), Color(0xFF8E5CF7), Color(0xFFFF8A3D), Color(0xFF14B8A6), Color(0xFFD9469A))

@Composable
internal fun SnakesBoard(state: SnlState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    val over = SnakesGame.getOutcome(state) != GameOutcome.Incomplete
    val scope = rememberCoroutineScope()
    var rolling by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    var moverId by remember { mutableStateOf<String?>(null) }
    var moverCenter by remember { mutableStateOf<Offset?>(null) }
    var lift by remember { mutableFloatStateOf(0f) }
    var banner by remember { mutableStateOf("Roll the dice to start!") }
    var bannerColor by remember { mutableStateOf(Color(0xFF3FA9F5)) }
    var flashSquare by remember { mutableStateOf<Int?>(null) }

    fun colorOf(playerId: String) = PlayerColors[state.players.indexOfFirst { it.id == playerId }.coerceAtLeast(0) % 4]
    fun nameOf(playerId: String) = state.players.firstOrNull { it.id == playerId }?.name ?: "Player"

    fun roll() {
        if (rolling || animating || over) return
        rolling = true
        Sfx.play(Sfx.DICE)
        scope.launch {
            delay(650)
            rolling = false
            vm.submit(RollDice, current.id)
        }
    }

    // Play back exactly what the engine decided: walk, then slide or climb.
    LaunchedEffect(state.rollCount) {
        val m = state.lastMove
        if (m != null && state.rollCount > 0) {
            animating = true
            moverId = m.playerId
            val who = nameOf(m.playerId)
            banner = "$who rolled a ${m.roll}"
            bannerColor = colorOf(m.playerId)
            if (m.event == "overshoot") {
                banner = "$who rolled ${m.roll} - needs exactly ${100 - m.from} to finish!"
                delay(900)
            } else {
                for (s in (m.from + 1)..m.landed) {
                    moverCenter = squareCenter(s)
                    lift = 1f
                    Sfx.play(Sfx.STEP, rate = 1f + (s - m.from) * 0.05f, volume = 0.8f)
                    delay(105); lift = 0f; delay(95)
                }
                when (m.event) {
                    "snake" -> {
                        flashSquare = m.landed
                        banner = "🐍 CHOMP! Snake on ${m.landed} bit $who - slide down to ${m.to}!"
                        bannerColor = Color(0xFFE5484D)
                        Sfx.play(Sfx.SNAKE)
                        delay(450)
                        val a = squareCenter(m.landed); val b = squareCenter(m.to)
                        for (i in 1..24) {
                            val t = i / 24f
                            // wiggle on the way down, like sliding along the snake
                            moverCenter = Offset(a.x + (b.x - a.x) * t + sin(t * 12f) * 0.18f, a.y + (b.y - a.y) * t)
                            delay(32)
                        }
                        delay(500)
                    }
                    "ladder" -> {
                        flashSquare = m.landed
                        banner = "🪜 WOOHOO! Ladder on ${m.landed} - $who climbs up to ${m.to}!"
                        bannerColor = Color(0xFF2FD08A)
                        Sfx.play(Sfx.LADDER)
                        delay(300)
                        val a = squareCenter(m.landed); val b = squareCenter(m.to)
                        for (i in 1..20) {
                            val t = i / 20f
                            moverCenter = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
                            lift = if (i % 4 < 2) 0.5f else 0f
                            delay(34)
                        }
                        lift = 0f
                        delay(450)
                    }
                    "win" -> {
                        banner = "🏆 $who reached 100 and WINS!"
                        bannerColor = Color(0xFFFFC93C)
                    }
                    else -> banner = "$who moved from ${m.from} to ${m.to}" + if (m.roll == 6) " - a six, roll again!" else ""
                }
            }
            flashSquare = null
            moverCenter = null
            moverId = null
            animating = false
        }
    }

    // Robot player waits for the animation to finish.
    LaunchedEffect(state.rollCount, state.turnIndex, animating, rolling) {
        if (current.id == BOT_ID && !animating && !rolling && !over) {
            delay(800)
            roll()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Big clear message of what just happened
        Surface(
            shape = RoundedCornerShape(16.dp), color = bannerColor,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Text(
                banner, color = Color(0xFF1B1730), fontWeight = FontWeight.Black, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }

        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                val cell = size.width / 10f
                val ink = Color(0xFF1B1730)
                val numPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = cell * 0.27f
                    color = android.graphics.Color.argb(170, 27, 23, 48)
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                // squares
                for (n in 1..100) {
                    val c = squareCenter(n)
                    val even = (c.x.toInt() + c.y.toInt()) % 2 == 0
                    val fill = when {
                        n == 100 -> Color(0xFFFFC93C)
                        SNAKES.containsKey(n) -> Color(0xFFFFD9DC)
                        LADDERS.containsKey(n) -> Color(0xFFD4F7E6)
                        even -> Color(0xFFFFF4D6)
                        else -> Color(0xFFE5F3FF)
                    }
                    drawRect(fill, topLeft = Offset((c.x - 0.5f) * cell, (c.y - 0.5f) * cell), size = Size(cell, cell))
                    drawRect(ink.copy(alpha = 0.18f), topLeft = Offset((c.x - 0.5f) * cell, (c.y - 0.5f) * cell),
                        size = Size(cell, cell), style = Stroke(cell * 0.02f))
                    drawContext.canvas.nativeCanvas.drawText(
                        if (n == 100) "🏁" else "$n", (c.x - 0.44f) * cell, (c.y - 0.16f) * cell, numPaint,
                    )
                }

                // ladders: two rails and rungs
                LADDERS.forEach { (from, to) ->
                    val a = squareCenter(from) * cell
                    val b = squareCenter(to) * cell
                    val dx = b.x - a.x; val dy = b.y - a.y
                    val len = sqrt(dx * dx + dy * dy)
                    val nx = -dy / len * cell * 0.17f; val ny = dx / len * cell * 0.17f
                    val wood = Color(0xFF9A5B1E)
                    drawLine(wood, Offset(a.x + nx, a.y + ny), Offset(b.x + nx, b.y + ny), strokeWidth = cell * 0.09f, cap = StrokeCap.Round)
                    drawLine(wood, Offset(a.x - nx, a.y - ny), Offset(b.x - nx, b.y - ny), strokeWidth = cell * 0.09f, cap = StrokeCap.Round)
                    val rungs = (len / (cell * 0.42f)).toInt().coerceAtLeast(2)
                    for (i in 1 until rungs) {
                        val t = i / rungs.toFloat()
                        val px = a.x + dx * t; val py = a.y + dy * t
                        drawLine(Color(0xFFE0A050), Offset(px + nx, py + ny), Offset(px - nx, py - ny), strokeWidth = cell * 0.07f, cap = StrokeCap.Round)
                    }
                }

                // snakes: wavy body from head (high square) to tail (low square)
                SNAKES.entries.forEachIndexed { index, (head, tail) ->
                    val a = squareCenter(head) * cell
                    val b = squareCenter(tail) * cell
                    val dx = b.x - a.x; val dy = b.y - a.y
                    val len = sqrt(dx * dx + dy * dy)
                    val nx = -dy / len; val ny = dx / len
                    val body = snakeColors[index % snakeColors.size]
                    val path = Path()
                    val steps = 40
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val wave = sin(t * 3.1416f * 4f) * cell * 0.22f * (1f - t * 0.5f)
                        val px = a.x + dx * t + nx * wave
                        val py = a.y + dy * t + ny * wave
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(path, ink, style = Stroke(cell * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, body, style = Stroke(cell * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(cell * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // head with eyes and tongue
                    drawCircle(ink, radius = cell * 0.25f, center = a)
                    drawCircle(body, radius = cell * 0.21f, center = a)
                    drawCircle(Color.White, radius = cell * 0.07f, center = Offset(a.x - cell * 0.08f, a.y - cell * 0.05f))
                    drawCircle(Color.White, radius = cell * 0.07f, center = Offset(a.x + cell * 0.08f, a.y - cell * 0.05f))
                    drawCircle(ink, radius = cell * 0.035f, center = Offset(a.x - cell * 0.08f, a.y - cell * 0.05f))
                    drawCircle(ink, radius = cell * 0.035f, center = Offset(a.x + cell * 0.08f, a.y - cell * 0.05f))
                    drawLine(Color(0xFFE5484D), Offset(a.x, a.y + cell * 0.1f), Offset(a.x, a.y + cell * 0.3f), strokeWidth = cell * 0.05f, cap = StrokeCap.Round)
                }

                // flash the square where a snake or ladder was hit
                flashSquare?.let { n ->
                    val c = squareCenter(n) * cell
                    drawCircle(Color.White, radius = cell * 0.62f, center = c, style = Stroke(cell * 0.12f))
                    drawCircle(bannerColor, radius = cell * 0.5f, center = c, style = Stroke(cell * 0.1f))
                }

                drawRoundRect(ink, size = size, cornerRadius = CornerRadius(cell * 0.2f, cell * 0.2f), style = Stroke(cell * 0.08f))

                // tokens (fan out when they share a square)
                val stacks = HashMap<Int, Int>()
                state.players.forEachIndexed { i, p ->
                    val moving = p.id == moverId && moverCenter != null
                    val square = state.positions[p.id] ?: 0
                    var center = (if (moving) moverCenter!! else squareCenter(square)) * cell
                    if (!moving) {
                        val n = stacks[square] ?: 0
                        stacks[square] = n + 1
                        center = Offset(center.x + (n - 0.5f) * cell * 0.24f * (if (n > 0 || state.players.size > 1) 1f else 0f) * (if (n == 0) 0f else 1f), center.y - n * cell * 0.1f)
                    }
                    drawPawn(
                        center = center, radius = cell * 0.27f, color = PlayerColors[i % 4],
                        highlight = p.id == current.id && !animating && !over,
                        lift = if (moving) lift else 0f,
                    )
                }
            }
        }

        // Players and where they stand (leader gets the crown)
        val lead = state.positions.values.maxOrNull() ?: 0
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.players.forEachIndexed { i, p ->
                val square = state.positions[p.id] ?: 0
                val isTurn = p.id == current.id && !over
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PlayerColors[i % 4].copy(alpha = if (isTurn) 1f else 0.35f),
                    modifier = Modifier.weight(1f)
                        .border(if (isTurn) 2.dp else 0.dp, if (isTurn) Color.White else Color.Transparent, RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            (if (square == lead && lead > 0) "👑 " else "") + p.name, maxLines = 1,
                            fontWeight = FontWeight.Black, fontSize = 12.sp,
                            color = if (isTurn) Color(0xFF1B1730) else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (square == 0) "at start" else "square $square", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (isTurn) Color(0xFF1B1730) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Dice3D(value = state.lastMove?.roll, rolling = rolling)
            Spacer(Modifier.width(16.dp))
            Column {
                Button(
                    onClick = { roll() },
                    enabled = !rolling && !animating && !over && current.id != BOT_ID,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayerColors[state.turnIndex % 4],
                    ),
                    modifier = Modifier.height(52.dp),
                ) {
                    Text("🎲 ROLL", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1730))
                }
                Text(
                    if (current.id == BOT_ID) "Robot's turn..." else "${current.name}, it's your turn!",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
