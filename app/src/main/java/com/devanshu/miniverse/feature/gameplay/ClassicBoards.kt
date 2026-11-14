package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.games.connectfour.C4Action
import com.devanshu.miniverse.games.connectfour.C4State
import com.devanshu.miniverse.games.connectfour.COLS
import com.devanshu.miniverse.games.connectfour.ROWS
import com.devanshu.miniverse.games.rps.Hand
import com.devanshu.miniverse.games.rps.RPS_WINS_NEEDED
import com.devanshu.miniverse.games.rps.RpsAction
import com.devanshu.miniverse.games.rps.RpsMode
import com.devanshu.miniverse.games.rps.RpsState
import com.devanshu.miniverse.games.tictactoe.TttAction
import com.devanshu.miniverse.games.tictactoe.TttState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF1B1730)

/** Two player cards side by side; the one whose turn it is lights up. */
@Composable
private fun DuelHeader(players: List<GamePlayer>, turnIndex: Int, colors: List<Color>, badges: List<String>, scores: List<String>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        players.forEachIndexed { i, p ->
            val active = i == turnIndex
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors[i].copy(alpha = if (active) 1f else 0.28f),
                modifier = Modifier.weight(1f)
                    .border(if (active) 3.dp else 0.dp, if (active) Color.White else Color.Transparent, RoundedCornerShape(18.dp)),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                        Text(badges[i], color = colors[i], fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(p.name, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1,
                            color = if (active) Ink else MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (active) "▶ YOUR TURN" else scores[i],
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = if (active) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ====================== Tic Tac Toe ======================

private val tttLines = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

@Composable
internal fun TicTacToeBoard(state: TttState, vm: GameplayViewModel) {
    val colors = listOf(PlayerColors[0], PlayerColors[3])       // X is red, O is blue
    val current = state.players[state.turnIndex]
    val winLine = tttLines.firstOrNull { l ->
        val a = state.cells[l[0]]; a != null && l.all { state.cells[it] == a }
    }
    val gameOver = winLine != null || state.cells.all { it != null }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        DuelHeader(
            players = state.players, turnIndex = if (gameOver) -1 else state.turnIndex, colors = colors,
            badges = listOf("X", "O"), scores = listOf("plays X", "plays O"),
        )
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).padding(6.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xFF241E3D)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (r in 0..2) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0..2) {
                        val i = r * 3 + c
                        val owner = state.cells[i]
                        val seat = state.players.indexOfFirst { it.id == owner }
                        val winning = winLine?.contains(i) == true
                        val pop by animateFloatAsState(
                            if (owner != null) 1f else 0f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "markPop",
                        )
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (winning) Color(0xFFFFC93C).copy(alpha = 0.35f)
                                    else Color(0xFF3A3360)
                                )
                                .border(
                                    if (winning) 3.dp else 0.dp,
                                    if (winning) Color(0xFFFFC93C) else Color.Transparent,
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable(enabled = owner == null && !gameOver && current.id != BOT_ID) {
                                    Sfx.play(Sfx.TAP)
                                    vm.submit(TttAction(i), current.id)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (owner != null && seat >= 0) {
                                val col = colors[seat.coerceIn(0, 1)]
                                Canvas(Modifier.fillMaxSize(0.62f).graphicsLayer { scaleX = pop; scaleY = pop }) {
                                    val w = size.width
                                    val stroke = w * 0.2f
                                    if (seat == 0) {
                                        drawLine(col, Offset(0f, 0f), Offset(w, w), strokeWidth = stroke, cap = StrokeCap.Round)
                                        drawLine(col, Offset(w, 0f), Offset(0f, w), strokeWidth = stroke, cap = StrokeCap.Round)
                                    } else {
                                        drawCircle(col, radius = w / 2 - stroke / 2, style = Stroke(stroke))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            when {
                winLine != null -> "🏆 ${state.players.first { it.id == state.cells[winLine[0]] }.name} wins!"
                gameOver -> "🤝 It's a draw!"
                current.id == BOT_ID -> "🤖 Robot is thinking..."
                else -> "${current.name}, place your ${if (state.turnIndex == 0) "X" else "O"}"
            },
            fontWeight = FontWeight.Black, fontSize = 17.sp,
            color = if (gameOver) Color(0xFFFFC93C) else colors[state.turnIndex],
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

// ====================== Connect Four ======================

private fun c4WinCells(state: C4State): List<Int> {
    val g = state.grid
    fun at(r: Int, c: Int) = if (r in 0 until ROWS && c in 0 until COLS) g[r * COLS + c] else null
    for (r in 0 until ROWS) for (c in 0 until COLS) {
        val owner = at(r, c) ?: continue
        for ((dr, dc) in listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)) {
            if ((1..3).all { at(r + dr * it, c + dc * it) == owner }) {
                return (0..3).map { (r + dr * it) * COLS + (c + dc * it) }
            }
        }
    }
    return emptyList()
}

@Composable
internal fun ConnectFourBoard(state: C4State, vm: GameplayViewModel) {
    val colors = listOf(PlayerColors[0], PlayerColors[2])       // red vs yellow
    val current = state.players[state.turnIndex]
    val winCells = c4WinCells(state)
    val gameOver = winCells.isNotEmpty() || state.grid.all { it != null }

    // Find the disc that was just dropped so it can fall from the top.
    var previous by remember { mutableStateOf(state.grid) }
    var droppedIndex by remember { mutableIntStateOf(-1) }
    val fall = remember { Animatable(1f) }
    LaunchedEffect(state.grid) {
        val idx = state.grid.indices.firstOrNull { previous[it] == null && state.grid[it] != null } ?: -1
        previous = state.grid
        if (idx >= 0) {
            droppedIndex = idx
            fall.snapTo(0f)
            fall.animateTo(1f, tween(140 + (idx / COLS) * 70))
            Sfx.play(Sfx.DROP)
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        DuelHeader(
            players = state.players, turnIndex = if (gameOver) -1 else state.turnIndex, colors = colors,
            badges = listOf("●", "●"), scores = listOf("red discs", "yellow discs"),
        )
        // column arrows in the current player's colour
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            repeat(COLS) {
                Text(
                    "▼", color = if (gameOver) Color.Transparent else colors[state.turnIndex],
                    fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(COLS / ROWS.toFloat())
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3B6CF6), Color(0xFF1E3FB0))))
                .pointerInput(state, gameOver) {
                    detectTapGestures { tap ->
                        if (gameOver || current.id == BOT_ID) return@detectTapGestures
                        val col = (tap.x / (size.width / COLS.toFloat())).toInt().coerceIn(0, COLS - 1)
                        vm.submit(C4Action(col), current.id)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val cw = size.width / COLS
                val ch = size.height / ROWS
                val rad = minOf(cw, ch) * 0.4f
                for (r in 0 until ROWS) for (c in 0 until COLS) {
                    val i = r * COLS + c
                    val centre = Offset((c + 0.5f) * cw, (r + 0.5f) * ch)
                    // the hole
                    drawCircle(Color(0xFF12256E), radius = rad, center = centre)
                    drawCircle(Color(0xFF0A1440), radius = rad * 0.92f, center = Offset(centre.x, centre.y + rad * 0.06f))
                    val owner = state.grid[i] ?: continue
                    val seat = state.players.indexOfFirst { it.id == owner }.coerceIn(0, 1)
                    val col = colors[seat]
                    val y = if (i == droppedIndex) (-ch * 0.5f) + (centre.y + ch * 0.5f) * fall.value else centre.y
                    val dc = Offset(centre.x, y)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color.White, col, col.copy(red = col.red * 0.65f, green = col.green * 0.65f, blue = col.blue * 0.65f)),
                            center = Offset(dc.x - rad * 0.35f, dc.y - rad * 0.4f), radius = rad * 1.5f,
                        ),
                        radius = rad * 0.94f, center = dc,
                    )
                    drawCircle(Color(0x33000000), radius = rad * 0.62f, center = dc, style = Stroke(rad * 0.1f))
                    if (i in winCells) drawCircle(Color.White, radius = rad * 0.98f, center = dc, style = Stroke(rad * 0.2f))
                }
            }
        }
        Text(
            when {
                winCells.isNotEmpty() -> "🏆 ${state.players.first { it.id == state.grid[winCells[0]] }.name} connected four!"
                gameOver -> "🤝 Board full - it's a draw!"
                current.id == BOT_ID -> "🤖 Robot is thinking..."
                else -> "${current.name}: tap a column to drop your disc"
            },
            fontWeight = FontWeight.Black, fontSize = 16.sp,
            color = if (gameOver) Color(0xFFFFC93C) else colors[state.turnIndex],
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// ====================== Rock Paper Scissors ======================

private val handEmoji = mapOf(Hand.ROCK to "✊", Hand.PAPER to "✋", Hand.SCISSORS to "✌️")

@Composable
internal fun RockPaperScissorsBoard(state: RpsState, vm: GameplayViewModel) {
    val colors = listOf(PlayerColors[0], PlayerColors[3])
    val p0 = state.players[0]; val p1 = state.players[1]
    val vsBot = state.players.any { it.id == BOT_ID }
    val scope = rememberCoroutineScope()
    var shaking by remember { mutableStateOf(false) }
    var shakeFace by remember { mutableIntStateOf(0) }
    val matchOver = state.roundWins.values.any { it >= RPS_WINS_NEEDED }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        DuelHeader(
            players = state.players, turnIndex = -1, colors = colors, badges = listOf("1", "2"),
            scores = state.players.map { p -> "★".repeat(state.roundWins[p.id] ?: 0).ifEmpty { "no wins yet" } },
        )
        Text("First to $RPS_WINS_NEEDED wins  ·  Round ${state.round + 1}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        when {
            // ---- choose how to play ----
            state.mode == null -> {
                Text("How do you want to play?", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(14.dp))
                ModeCard("🎲", "Lucky Shake", "Nobody picks! Shake and the game deals both hands at random.", PlayerColors[2]) {
                    Sfx.play(Sfx.TAP); vm.submit(RpsAction.ChooseMode(RpsMode.LUCKY), p0.id)
                }
                Spacer(Modifier.height(12.dp))
                ModeCard(
                    "🤫", "Secret Pick",
                    if (vsBot) "You choose your hand. The robot's hand is a surprise!"
                    else "Each player chooses in secret, then pass the phone.",
                    PlayerColors[1],
                ) { Sfx.play(Sfx.TAP); vm.submit(RpsAction.ChooseMode(RpsMode.PICK), p0.id) }
            }

            // ---- round result ----
            state.picks.size == 2 && state.revealed != null -> {
                val (h0, h1) = state.revealed
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    RevealHand(handEmoji.getValue(h0), p0.name, colors[0])
                    Text("VS", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFFFC93C))
                    RevealHand(handEmoji.getValue(h1), p1.name, colors[1])
                }
                Spacer(Modifier.height(16.dp))
                Text(state.lastResult ?: "", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFFFC93C))
                Spacer(Modifier.height(18.dp))
                if (!matchOver) {
                    Button(onClick = { Sfx.play(Sfx.TAP); vm.submit(RpsAction.NextRound, p0.id) },
                        modifier = Modifier.height(52.dp)) {
                        Text("Next round ➡️", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    }
                }
            }

            // ---- lucky shake ----
            state.mode == RpsMode.LUCKY -> {
                val faces = listOf("✊", "✋", "✌️")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    RevealHand(if (shaking) faces[shakeFace % 3] else "❓", p0.name, colors[0])
                    Text("VS", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFFFFC93C))
                    RevealHand(if (shaking) faces[(shakeFace + 1) % 3] else "❓", p1.name, colors[1])
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    enabled = !shaking,
                    onClick = {
                        shaking = true
                        Sfx.play(Sfx.DICE)
                        scope.launch {
                            repeat(12) { shakeFace++; delay(80) }
                            shaking = false
                            vm.submit(RpsAction.Shake, p0.id)
                            Sfx.play(Sfx.POP)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.75f).height(60.dp),
                ) { Text(if (shaking) "Rock... Paper... Scissors..." else "🎲 SHAKE!", fontWeight = FontWeight.Black, fontSize = 19.sp) }
                Text("Everybody shout: rock, paper, scissors!", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }

            // ---- secret pick ----
            else -> {
                val picker = state.players[state.pickerIndex]
                if (picker.id == BOT_ID) {
                    Text("🤖 Robot is choosing...", fontWeight = FontWeight.Black, fontSize = 20.sp)
                } else {
                    if (!vsBot) Text("📱 Pass the phone to", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(picker.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = colors[state.pickerIndex])
                    Text(if (vsBot) "Choose your hand!" else "Pick in secret - no peeking! 🙈", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Hand.entries.forEach { hand ->
                            Surface(
                                shape = CircleShape, color = colors[state.pickerIndex],
                                modifier = Modifier.size(92.dp).clickable {
                                    Sfx.play(Sfx.TAP); vm.submit(RpsAction.Pick(hand), picker.id)
                                },
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(handEmoji.getValue(hand), fontSize = 44.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp), color = color,
        modifier = Modifier.fillMaxWidth(0.92f).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 44.sp)
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 19.sp, color = Ink)
                Text(text, fontSize = 13.sp, color = Ink.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun RevealHand(emoji: String, name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(color.copy(alpha = 0.3f))
                .border(4.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 58.sp) }
        Text(name, fontWeight = FontWeight.Black, color = color, modifier = Modifier.padding(top = 6.dp))
    }
}
