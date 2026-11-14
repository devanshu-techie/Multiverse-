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
