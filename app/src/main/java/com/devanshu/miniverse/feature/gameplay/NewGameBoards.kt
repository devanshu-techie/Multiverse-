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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.games.quickmath.Answer
import com.devanshu.miniverse.games.quickmath.MathState
import com.devanshu.miniverse.games.rps.Hand
import com.devanshu.miniverse.games.rps.RpsAction
import com.devanshu.miniverse.games.rps.RpsState
import com.devanshu.miniverse.games.snakes.LADDERS
import com.devanshu.miniverse.games.snakes.RollDice
import com.devanshu.miniverse.games.snakes.SNAKES
import com.devanshu.miniverse.games.snakes.SnlState
import com.devanshu.miniverse.games.whack.MOLES_PER_PLAYER
import com.devanshu.miniverse.games.whack.WhackAction
import com.devanshu.miniverse.games.whack.WhackGame
import com.devanshu.miniverse.games.whack.WhackState
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvLemon
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvOrange
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.SeatColors
import kotlinx.coroutines.delay

// ---------- Snakes & Ladders ----------

@Composable
internal fun SnakesBoard(state: SnlState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    Column(Modifier.fillMaxSize()) {
        Text(
            "${current.name}'s turn" + (state.lastRoll?.let { "  ·  🎲 $it" } ?: ""),
            fontWeight = FontWeight.Black, fontSize = 16.sp,
            color = SeatColors[state.turnIndex % SeatColors.size],
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (state.lastEvent == "snake") Text("🐍 Oh no, a snake!", color = MvPink, fontWeight = FontWeight.Bold)
        if (state.lastEvent == "ladder") Text("🪜 Yay, a ladder!", color = MvMint, fontWeight = FontWeight.Bold)
        if (state.lastEvent == "need exact") Text("Need the exact number to finish!", color = MvOrange, fontWeight = FontWeight.Bold)

        // 10x10 board, cell 100 top-left row order (boustrophedon)
        Column(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 6.dp)) {
            for (rowTop in 9 downTo 0) {
                Row(Modifier.weight(1f)) {
                    val cells = (rowTop * 10 + 1..rowTop * 10 + 10).let {
                        if (rowTop % 2 == 0) it.toList() else it.toList().reversed()
                    }
                    for (cell in cells) {
                        val here = state.players.filter { state.positions[it.id] == cell }
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .padding(0.5.dp)
                                .background(
                                    when {
                                        SNAKES.containsKey(cell) -> MvPink.copy(alpha = 0.45f)
                                        LADDERS.containsKey(cell) -> MvMint.copy(alpha = 0.5f)
                                        (cell / 10 + cell) % 2 == 0 -> MvLemon
                                        else -> Color.White
                                    },
                                    RoundedCornerShape(3.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                here.isNotEmpty() -> Row {
                                    here.forEach {
                                        Box(
                                            Modifier.size(9.dp).clip(CircleShape)
                                                .background(SeatColors[it.seat % SeatColors.size])
                                        )
                                    }
                                }
                                SNAKES.containsKey(cell) -> Text("🐍", fontSize = 9.sp)
                                LADDERS.containsKey(cell) -> Text("🪜", fontSize = 9.sp)
                                else -> Text("$cell", fontSize = 7.sp, color = MvDark.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { vm.submit(RollDice, current.id) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("🎲 Roll the dice!", fontSize = 17.sp, fontWeight = FontWeight.Black) }
    }
}

// ---------- Rock Paper Scissors ----------

@Composable
internal fun RpsBoard(state: RpsState, vm: GameplayViewModel) {
    val hands = listOf(Hand.ROCK to "✊", Hand.PAPER to "✋", Hand.SCISSORS to "✌️")
    Column(
        Modifier.fillMaxSize().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Round ${state.round + 1} of 3", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.roundWins[p.id] ?: 0}", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        if (state.picks.size < 2) {
            val picker = state.players[state.pickerIndex]
            Spacer(Modifier.height(16.dp))
            Text("📱 Pass the phone to", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(picker.name, fontSize = 26.sp, fontWeight = FontWeight.Black,
                color = SeatColors[state.pickerIndex])
            Text("Pick in secret - no peeking! 🙈", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                hands.forEach { (hand, emoji) ->
                    Surface(
                        shape = CircleShape, color = MvLemon,
                        modifier = Modifier.size(86.dp).clickable { vm.submit(RpsAction.Pick(hand), picker.id) },
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 40.sp) }
                    }
                }
            }
        } else {
            val (h0, h1) = state.revealed!!
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hands.first { it.first == h0 }.second, fontSize = 56.sp)
                    Text(state.players[0].name, fontWeight = FontWeight.Bold, color = SeatColors[0])
                }
                Text("VS", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MvOrange)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hands.first { it.first == h1 }.second, fontSize = 56.sp)
                    Text(state.players[1].name, fontWeight = FontWeight.Bold, color = SeatColors[1])
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(state.lastResult ?: "", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MvMint)
            Spacer(Modifier.height(18.dp))
            Button(onClick = { vm.submit(RpsAction.NextRound, state.players[0].id) }) {
                Text("Next round ➡️", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------- Quick Math ----------

@Composable
internal fun QuickMathBoard(state: MathState, vm: GameplayViewModel) {
    val turn = state.players[state.questionIndex % 2]
    val q = state.questions[state.questionIndex.coerceAtMost(state.questions.lastIndex)]
    Column(
        Modifier.fillMaxSize().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.scores[p.id] ?: 0}", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Question ${state.questionIndex + 1} of ${state.questions.size} — ${turn.name}",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        state.lastCorrect?.let {
            Text(if (it) "✅ Correct!" else "❌ Oops!", fontWeight = FontWeight.Black,
                color = if (it) MvMint else MvPink, fontSize = 16.sp)
        }
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = MvSky.copy(alpha = 0.18f)) {
            Text(q.text, fontSize = 34.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 22.dp))
        }
        Spacer(Modifier.height(22.dp))
        val optionColors = listOf(MvSky, MvMint, MvOrange, MvPink)
        q.options.forEachIndexed { i, opt ->
            Button(
                onClick = { vm.submit(Answer(i), turn.id) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = optionColors[i]),
                modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 5.dp).height(50.dp),
            ) { Text("$opt", fontSize = 19.sp, fontWeight = FontWeight.Black, color = MvDark) }
        }
    }
}

// ---------- Whack-a-Mole ----------

@Composable
internal fun WhackBoard(state: WhackState, vm: GameplayViewModel) {
    val current = WhackGame.currentPlayer(state)
    val moleNo = state.moleIndex % MOLES_PER_PLAYER + 1

    // Timing driver: pop the mole up, keep it up briefly, then hide and advance.
    LaunchedEffect(state.moleIndex, state.moleVisible) {
        if (!state.moleVisible) {
            delay(450)
            vm.submit(WhackAction.ShowMole, current.id)
        } else {
            delay(750)
            vm.submit(WhackAction.HideMole, current.id)
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${current.name} — mole $moleNo of $MOLES_PER_PLAYER",
            fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.scores[p.id] ?: 0}", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (r in 0..2) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0..2) {
                        val cell = r * 3 + c
                        val moleHere = state.moleVisible && !state.hitThisMole &&
                            state.moleCells[state.moleIndex.coerceAtMost(state.moleCells.lastIndex)] == cell
                        Surface(
                            shape = CircleShape,
                            color = if (moleHere) MvYellow else MvDark.copy(alpha = 0.82f),
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .clickable { vm.submit(WhackAction.Tap(cell), current.id) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (moleHere) "🐹" else "🕳️", fontSize = 34.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        Text("Tap the mole the moment it pops up!", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
