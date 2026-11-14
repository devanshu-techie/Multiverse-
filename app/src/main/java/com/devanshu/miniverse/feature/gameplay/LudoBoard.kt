package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.games.ludo.LUDO_HOME
import com.devanshu.miniverse.games.ludo.LUDO_HOME_COLUMNS
import com.devanshu.miniverse.games.ludo.LUDO_PATH
import com.devanshu.miniverse.games.ludo.LUDO_SAFE_CELLS
import com.devanshu.miniverse.games.ludo.LUDO_TOKENS
import com.devanshu.miniverse.games.ludo.LudoAction
import com.devanshu.miniverse.games.ludo.LudoGame
import com.devanshu.miniverse.games.ludo.LudoState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private val yardOrigins = listOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)          // (row, col) of each 6x6 yard
private val yardSpots = listOf(1.8f to 1.8f, 1.8f to 4.2f, 4.2f to 1.8f, 4.2f to 4.2f)
private val homeSpots = listOf(                                             // finished tokens, near the centre
    6.75f to 7.5f, 7.5f to 6.75f, 8.25f to 7.5f, 7.5f to 8.25f,             // (col, row) anchors per corner
)

@Composable
internal fun LudoBoard(state: LudoState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    val over = LudoGame.getOutcome(state) != GameOutcome.Incomplete
    val scope = rememberCoroutineScope()
    var rolling by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    var animToken by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }  // player, token, shown step
    var lift by remember { mutableFloatStateOf(0f) }

    val pulse by rememberInfiniteTransition(label = "ludoPulse").animateFloat(
        initialValue = 0.85f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "pulse",
    )

    fun colorOf(playerId: String): Color = PlayerColors[(state.corners[playerId] ?: 0) % 4]

    val movable = state.pendingRoll?.let { LudoGame.movableTokens(state, current.id, it) } ?: emptyList()

    fun roll() {
        if (rolling || animating || over || state.pendingRoll != null) return
        rolling = true
        Sfx.play(Sfx.DICE)
        scope.launch {
            delay(650)
            rolling = false
            vm.submit(LudoAction.Roll, current.id)
        }
    }

    // Walk the moved token square by square, with a hop and a hoof sound per step.
    LaunchedEffect(state.moveCount) {
        val m = state.lastMove
        if (m != null && state.moveCount > 0) {
            animating = true
            if (m.from == -1) {
                animToken = Triple(m.playerId, m.token, 0)
                lift = 1f; Sfx.play(Sfx.STEP); delay(140); lift = 0f; delay(120)
            } else {
                for (s in (m.from + 1)..m.to) {
                    animToken = Triple(m.playerId, m.token, s)
                    lift = 1f
                    Sfx.play(Sfx.STEP, rate = 1f + (s - m.from) * 0.04f, volume = 0.8f)
                    delay(95); lift = 0f; delay(85)
                }
            }
            if (m.captured > 0) { Sfx.play(Sfx.CAPTURE); delay(380) }
            if (m.to == LUDO_HOME) { Sfx.play(Sfx.COIN); delay(300) }
            animToken = null
            animating = false
        }
    }

    // One movable token only? Move it automatically so little kids are not stuck.
    LaunchedEffect(state.rollCount, state.pendingRoll, animating) {
        if (!animating && state.pendingRoll != null && current.id != BOT_ID && movable.size == 1) {
            delay(500)
            vm.submit(LudoAction.Move(movable.first()), current.id)
        }
    }

    // Robot player.
    LaunchedEffect(state.rollCount, state.moveCount, state.turnIndex, animating, rolling) {
        if (current.id == BOT_ID && !animating && !rolling && !over) {
            delay(750)
            val pending = state.pendingRoll
            if (pending == null) roll()
            else Bots.ludoToken(state, pending)?.let { vm.submit(LudoAction.Move(it), BOT_ID) }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Turn banner
        Surface(
            shape = RoundedCornerShape(16.dp), color = colorOf(current.id),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Text(
                when {
                    over -> "🏆 Game over!"
                    state.pendingRoll != null && current.id != BOT_ID -> "${current.name}: tap a glowing token"
                    else -> "${current.name}'s turn - roll the dice!"
                },
                color = Color(0xFF1B1730), fontWeight = FontWeight.Black, fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            )
        }

        // ---------------- The board ----------------
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(state, animating) {
                    detectTapGestures { tap ->
                        if (animating || state.pendingRoll == null || current.id == BOT_ID) return@detectTapGestures
                        val cell = size.width / 15f
                        val mine = state.tokens.getValue(current.id)
                        val hit = movable.minByOrNull { t ->
                            val c = tokenCenter(state, current.id, t, mine[t], cell)
                            val dx = c.x - tap.x; val dy = c.y - tap.y
                            sqrt(dx * dx + dy * dy)
                        }
                        if (hit != null) {
                            val c = tokenCenter(state, current.id, hit, mine[hit], cell)
                            val dx = c.x - tap.x; val dy = c.y - tap.y
                            if (sqrt(dx * dx + dy * dy) < cell * 1.6f) {
                                Sfx.play(Sfx.TAP)
                                vm.submit(LudoAction.Move(hit), current.id)
                            }
                        }
                    }
                },
            ) {
                val cell = size.width / 15f
                val line = Color(0xFF1B1730)
                fun cellRect(row: Int, col: Int, color: Color) {
                    drawRect(color, topLeft = Offset(col * cell, row * cell), size = Size(cell, cell))
                    drawRect(line.copy(alpha = 0.55f), topLeft = Offset(col * cell, row * cell), size = Size(cell, cell), style = Stroke(cell * 0.035f))
                }

                // board base
                drawRoundRect(Color(0xFFFFF8E7), size = size, cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f))

                // yards
                yardOrigins.forEachIndexed { k, (r0, c0) ->
                    val col = PlayerColors[k]
                    drawRoundRect(col, topLeft = Offset(c0 * cell, r0 * cell), size = Size(cell * 6, cell * 6),
                        cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f))
                    drawRoundRect(Color(0xFFFFF8E7), topLeft = Offset((c0 + 0.8f) * cell, (r0 + 0.8f) * cell),
                        size = Size(cell * 4.4f, cell * 4.4f), cornerRadius = CornerRadius(cell * 0.7f, cell * 0.7f))
                    yardSpots.forEach { (sx, sy) ->
                        drawCircle(col.copy(alpha = 0.45f), radius = cell * 0.62f,
                            center = Offset((c0 + sx) * cell, (r0 + sy) * cell))
                    }
                }

                // loop cells
                val starPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = cell * 0.8f
                    color = android.graphics.Color.argb(150, 27, 23, 48)
                    isAntiAlias = true
                }
                LUDO_PATH.forEachIndexed { i, (r, c) ->
                    val fill = when {
                        i % 13 == 0 -> PlayerColors[i / 13]                 // start cells
                        i in LUDO_SAFE_CELLS -> Color(0xFFE9E4F5)           // star cells
                        else -> Color.White
                    }
                    cellRect(r, c, fill)
                    if (i in LUDO_SAFE_CELLS) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "★", (c + 0.5f) * cell, (r + 0.5f) * cell + cell * 0.28f, starPaint,
                        )
                    }
                }
                // home columns
                LUDO_HOME_COLUMNS.forEachIndexed { k, cells ->
                    cells.forEach { (r, c) -> cellRect(r, c, PlayerColors[k]) }
                }
                // centre: four triangles pointing in
                val cx = 7.5f * cell; val cy = 7.5f * cell
                val corners = listOf(
                    Offset(6 * cell, 6 * cell), Offset(9 * cell, 6 * cell),
                    Offset(9 * cell, 9 * cell), Offset(6 * cell, 9 * cell),
                )
                // triangle k sits on the side its home column arrives from: 0 left, 1 top, 2 right, 3 bottom
                val sides = listOf(3 to 0, 0 to 1, 1 to 2, 2 to 3)
                sides.forEachIndexed { k, (a, b) ->
                    val path = Path().apply {
                        moveTo(corners[a].x, corners[a].y); lineTo(corners[b].x, corners[b].y)
                        lineTo(cx, cy); close()
                    }
                    drawPath(path, PlayerColors[k])
                    drawPath(path, line.copy(alpha = 0.55f), style = Stroke(cell * 0.035f))
                }
                drawRoundRect(line, size = size, cornerRadius = CornerRadius(cell * 0.6f, cell * 0.6f), style = Stroke(cell * 0.1f))

                // tokens: count how many share a square so they fan out instead of hiding each other
                val stacks = HashMap<String, Int>()
                state.players.forEach { p ->
                    val steps = state.tokens.getValue(p.id)
                    steps.forEachIndexed { t, realStep ->
                        val anim = animToken
                        val isAnim = anim != null && anim.first == p.id && anim.second == t
                        val step = if (isAnim) anim!!.third else realStep
                        var center = tokenCenter(state, p.id, t, step, cell)
                        if (step in 0..55) {
                            val key = "${center.x.toInt()}:${center.y.toInt()}"
                            val n = stacks[key] ?: 0
                            stacks[key] = n + 1
                            center = Offset(center.x + n * cell * 0.2f - (if (n > 0) cell * 0.1f else 0f), center.y - n * cell * 0.12f)
                        }
                        val canMove = !animating && p.id == current.id && state.pendingRoll != null && t in movable
                        drawPawn(
                            center = center,
                            radius = cell * 0.36f * (if (canMove) pulse else 1f),
                            color = colorOf(p.id),
                            highlight = canMove,
                            lift = if (isAnim) lift else 0f,
                        )
                    }
                }
            }
        }

        // ---------------- Scoreboard: who is winning ----------------
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.players.forEach { p ->
                val home = LudoGame.homeCount(state, p.id)
                val isTurn = p.id == current.id && !over
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorOf(p.id).copy(alpha = if (isTurn) 1f else 0.35f),
                    modifier = Modifier.weight(1f)
                        .border(if (isTurn) 2.dp else 0.dp, if (isTurn) Color.White else Color.Transparent, RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            (if (isTurn) "▶ " else "") + p.name, maxLines = 1,
                            fontWeight = FontWeight.Black, fontSize = 12.sp,
                            color = if (isTurn) Color(0xFF1B1730) else MaterialTheme.colorScheme.onSurface,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(LUDO_TOKENS) { i ->
                                Box(
                                    Modifier.size(9.dp).clip(CircleShape)
                                        .border(1.dp, Color(0xFF1B1730), CircleShape)
                                        .then(if (i < home) Modifier.clip(CircleShape) else Modifier),
                                ) {
                                    if (i < home) Canvas(Modifier.fillMaxSize()) { drawCircle(Color(0xFF1B1730)) }
                                }
                            }
                        }
                        Text("$home / $LUDO_TOKENS home", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (isTurn) Color(0xFF1B1730) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ---------------- Dice ----------------
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Dice3D(value = state.lastRoll, rolling = rolling)
            Spacer(Modifier.width(16.dp))
            Column {
                Button(
                    onClick = { roll() },
                    enabled = !rolling && !animating && !over && state.pendingRoll == null && current.id != BOT_ID,
                    colors = ButtonDefaults.buttonColors(containerColor = colorOf(current.id)),
                    modifier = Modifier.height(52.dp),
                ) {
                    Text("🎲 ROLL", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1730))
                }
                Text(
                    state.note ?: if (current.id == BOT_ID) "Robot is thinking..." else "Roll a 6 to bring a token out",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Centre of a token on the canvas for any step (yard, loop, home column or finished). */
private fun tokenCenter(state: LudoState, playerId: String, token: Int, step: Int, cell: Float): Offset {
    val corner = (state.corners[playerId] ?: 0) % 4
    return when {
        step < 0 -> {
            val (r0, c0) = yardOrigins[corner]
            val (sx, sy) = yardSpots[token % 4]
            Offset((c0 + sx) * cell, (r0 + sy) * cell)
        }
        step >= LUDO_HOME -> {
            val (hx, hy) = homeSpots[corner]
            val spread = (token - 1.5f) * cell * 0.3f
            if (corner % 2 == 0) Offset(hx * cell, hy * cell + spread) else Offset(hx * cell + spread, hy * cell)
        }
        else -> {
            val (r, c) = LudoGame.cellOf(state, playerId, step) ?: (7 to 7)
            Offset((c + 0.5f) * cell, (r + 0.5f) * cell)
        }
    }
}
