package com.devanshu.miniverse.feature.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.games.connectfour.C4Action
import com.devanshu.miniverse.games.connectfour.C4State
import com.devanshu.miniverse.games.connectfour.COLS
import com.devanshu.miniverse.games.connectfour.ROWS
import com.devanshu.miniverse.games.memory.MemAction
import com.devanshu.miniverse.games.memory.MemState
import com.devanshu.miniverse.games.reaction.ReactionAction
import com.devanshu.miniverse.games.reaction.ReactionState
import com.devanshu.miniverse.games.reaction.RoundPhase
import com.devanshu.miniverse.games.reaction.TOTAL_ROUNDS
import com.devanshu.miniverse.games.tictactoe.TttAction
import com.devanshu.miniverse.games.tictactoe.TttState
import com.devanshu.miniverse.ui.theme.MvCoral
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.SeatColors
import kotlinx.coroutines.delay

@Composable
fun GameplayScreen(
    repository: GameRepository,
    matchId: String,
    onFinished: (String) -> Unit,
    onQuit: () -> Unit,
) {
    val vm: GameplayViewModel = viewModel(factory = GameplayViewModel.Factory(repository, matchId))
    val session by vm.session.collectAsState()
    val finished by vm.finished.collectAsState()

    LaunchedEffect(finished) { if (finished) onFinished(matchId) }

    val s = session ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...") }
        return
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.quit(); onQuit() }) { Text("✕ Quit") }
            Spacer(Modifier.weight(1f))
            s.players.forEachIndexed { i, p ->
                Surface(shape = RoundedCornerShape(14.dp), color = SeatColors[i % SeatColors.size]) {
                    Text(p.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
                Spacer(Modifier.size(6.dp))
            }
        }
        when (val state = s.state) {
            is TttState -> TttBoard(state) { cell ->
                vm.submit(TttAction(cell), state.players[state.turnIndex].id)
            }
            is C4State -> C4Board(state) { col ->
                vm.submit(C4Action(col), state.players[state.turnIndex].id)
            }
            is MemState -> MemoryBoard(state, vm)
            is ReactionState -> ReactionBoard(state, vm)
            is com.devanshu.miniverse.games.snakes.SnlState -> SnakesBoard(state, vm)
            is com.devanshu.miniverse.games.rps.RpsState -> RpsBoard(state, vm)
            is com.devanshu.miniverse.games.quickmath.MathState -> QuickMathBoard(state, vm)
            is com.devanshu.miniverse.games.whack.WhackState -> WhackBoard(state, vm)
        }
    }
}

// ---------- Tic Tac Toe ----------

@Composable
private fun TurnBanner(name: String, color: Color) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.15f)) {
            Text("$name's turn", color = color, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun TttBoard(state: TttState, onCell: (Int) -> Unit) {
    val marks = listOf("❌", "⭕")
    TurnBanner(state.players[state.turnIndex].name, SeatColors[state.turnIndex])
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (r in 0..2) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0..2) {
                    val i = r * 3 + c
                    val owner = state.cells[i]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clickable(enabled = owner == null) { onCell(i) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (owner != null) {
                                val seat = state.players.indexOfFirst { it.id == owner }
                                Text(marks[seat.coerceIn(0, 1)], fontSize = 40.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Connect Four ----------

@Composable
private fun C4Board(state: C4State, onColumn: (Int) -> Unit) {
    TurnBanner(state.players[state.turnIndex].name, SeatColors[state.turnIndex])
    Surface(
        shape = RoundedCornerShape(18.dp), color = MvDark,
        modifier = Modifier.fillMaxWidth().padding(4.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (r in 0 until ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in 0 until COLS) {
                        val owner = state.grid[r * COLS + c]
                        val seat = state.players.indexOfFirst { it.id == owner }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        owner == null -> Color(0xFF2E2A45)
                                        else -> SeatColors[seat.coerceIn(0, SeatColors.size - 1)]
                                    }
                                )
                                .clickable { onColumn(c) },
                        )
                    }
                }
            }
        }
    }
    Text(
        "Tap any column to drop your disc",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

// ---------- Memory Match ----------

@Composable
private fun MemoryBoard(state: MemState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    // Two cards face up: show them briefly, then settle (engine decides match or miss).
    LaunchedEffect(state.faceUp) {
        if (state.faceUp.size == 2) {
            delay(700)
            vm.submit(MemAction.Settle, current.id)
        }
    }
    TurnBanner(current.name, SeatColors[state.turnIndex])
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        state.players.forEachIndexed { i, p ->
            Text("${p.name}: ${state.scores[p.id] ?: 0}", fontWeight = FontWeight.Bold,
                color = SeatColors[i], fontSize = 14.sp)
        }
    }
    Column(
        Modifier.fillMaxWidth().aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (r in 0..3) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0..3) {
                    val i = r * 4 + c
                    val matched = state.matchedBy[i] != null
                    val revealed = matched || i in state.faceUp
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            matched -> MvMint.copy(alpha = 0.35f)
                            revealed -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.weight(1f).fillMaxSize()
                            .clickable(enabled = !revealed && state.faceUp.size < 2) {
                                vm.submit(MemAction.Flip(i), current.id)
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (revealed) Text(state.symbols[i], fontSize = 26.sp)
                            else Text("?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Reaction Battle (split screen) ----------

@Composable
private fun ReactionBoard(state: ReactionState, vm: GameplayViewModel) {
    val p0 = state.players[0]
    val p1 = state.players[1]

    // Round driver: arm the round, wait the seeded delay, then show GO.
    LaunchedEffect(state.round, state.phase) {
        when (state.phase) {
            RoundPhase.WAITING -> { delay(900); vm.submit(ReactionAction.Arm, p0.id) }
            RoundPhase.ARMED -> {
                delay(state.roundDelaysMs[state.round])
                vm.submit(ReactionAction.Show, p0.id)
            }
            RoundPhase.RESOLVED -> {
                if (state.round < TOTAL_ROUNDS - 1) { delay(1100); vm.submit(ReactionAction.NextRound, p0.id) }
            }
            else -> Unit
        }
    }

    val bg = when (state.phase) {
        RoundPhase.SHOWN -> MvMint
        RoundPhase.ARMED -> MvCoral
        else -> MvDark
    }
    val label = when (state.phase) {
        RoundPhase.WAITING -> "Round ${state.round + 1} of $TOTAL_ROUNDS"
        RoundPhase.ARMED -> "Wait for green..."
        RoundPhase.SHOWN -> "TAP!"
        RoundPhase.RESOLVED -> "${p0.name} ${state.roundWins[p0.id]} - ${state.roundWins[p1.id]} ${p1.name}"
    }

    fun tap(playerId: String) {
        if (state.phase == RoundPhase.ARMED) vm.submit(ReactionAction.Tap(tooEarly = true), playerId)
        else if (state.phase == RoundPhase.SHOWN) vm.submit(ReactionAction.Tap(tooEarly = false), playerId)
    }

    Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(bg)
                .clickable { tap(p1.id) },
            contentAlignment = Alignment.Center,
        ) {
            Text("${p1.name} — tap here", color = Color.White, fontWeight = FontWeight.Black,
                fontSize = 18.sp, modifier = Modifier.graphicsLayer { rotationZ = 180f })
        }
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Black, color = MvYellow,
                modifier = Modifier.background(MvDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(bg)
                .clickable { tap(p0.id) },
            contentAlignment = Alignment.Center,
        ) {
            Text("${p0.name} — tap here", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}
