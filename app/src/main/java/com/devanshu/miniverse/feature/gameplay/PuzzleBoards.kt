package com.devanshu.miniverse.feature.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.games.ludo.LudoGame
import com.devanshu.miniverse.games.ludo.LudoState
import com.devanshu.miniverse.games.maze.MZ
import com.devanshu.miniverse.games.maze.MazeGame
import com.devanshu.miniverse.games.maze.MazeMove
import com.devanshu.miniverse.games.maze.MazeState
import com.devanshu.miniverse.games.maze.MoveDir
import com.devanshu.miniverse.games.merge.Dir
import com.devanshu.miniverse.games.merge.MergeState
import com.devanshu.miniverse.games.merge.Swipe
import com.devanshu.miniverse.games.scramble.SCRAMBLE_ROUNDS
import com.devanshu.miniverse.games.scramble.ScrambleAction
import com.devanshu.miniverse.games.scramble.ScrambleState
import com.devanshu.miniverse.games.simon.SimonAction
import com.devanshu.miniverse.games.simon.SimonState
import com.devanshu.miniverse.ui.theme.MvCoral
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvLemon
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvOrange
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.SeatColors
import kotlinx.coroutines.delay
import kotlin.math.abs

// ---------- Number Merge (2048) ----------

@Composable
internal fun MergeBoard(state: MergeState, vm: GameplayViewModel) {
    val owner = state.players.first()
    val tileColors = mapOf(
        2 to MvLemon, 4 to MvYellow, 8 to MvOrange, 16 to MvPink, 32 to MvCoral,
        64 to MvSky, 128 to MvMint, 256 to MvPurple, 512 to MvPurple,
    )
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Score ${state.score}  ·  reach 256!", fontWeight = FontWeight.Black, fontSize = 17.sp,
            modifier = Modifier.padding(vertical = 8.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp)).background(MvDark.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    var totalX = 0f; var totalY = 0f
                    detectDragGestures(
                        onDragStart = { totalX = 0f; totalY = 0f },
                        onDragEnd = {
                            val dir = if (abs(totalX) > abs(totalY)) {
                                if (totalX > 0) Dir.RIGHT else Dir.LEFT
                            } else {
                                if (totalY > 0) Dir.DOWN else Dir.UP
                            }
                            if (abs(totalX) + abs(totalY) > 60f) vm.submit(Swipe(dir), owner.id)
                        },
                    ) { change, drag ->
                        change.consume(); totalX += drag.x; totalY += drag.y
                    }
                },
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0..3) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0..3) {
                            val v = state.grid[r * 4 + c]
                            Box(
                                Modifier.weight(1f).fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (v == 0) Color(0xFF3A3654) else (tileColors[v] ?: MvPurple)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v > 0) Text("$v", fontWeight = FontWeight.Black,
                                    fontSize = if (v >= 128) 20.sp else 24.sp,
                                    color = if (v >= 64) Color.White else MvDark)
                            }
                        }
                    }
                }
            }
        }
        Text("Swipe the board to slide tiles", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(6.dp))
    }
}

// ---------- Word Scramble ----------

@Composable
internal fun ScrambleBoard(state: ScrambleState, vm: GameplayViewModel) {
    val owner = state.players.first()
    if (state.round >= state.words.size) return
    val scrambled = state.shuffles[state.round]
    var picked by remember(state.round) { mutableStateOf(listOf<Int>()) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Word ${state.round + 1} of $SCRAMBLE_ROUNDS  ·  solved ${state.score}",
            fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
        state.lastRight?.let {
            Text(if (it) "✅ Correct!" else "❌ That wasn't it!",
                color = if (it) MvMint else MvCoral, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(16.dp))
        // answer being built
        Surface(shape = RoundedCornerShape(14.dp), color = MvSky.copy(alpha = 0.15f)) {
            Text(
                picked.map { scrambled[it] }.joinToString("").ifEmpty { "· ".repeat(scrambled.length) },
                fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            scrambled.forEachIndexed { i, ch ->
                val used = i in picked
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (used) MvDark.copy(alpha = 0.15f) else MvYellow,
                    modifier = Modifier.size(44.dp).clickable(enabled = !used) { picked = picked + i },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!used) Text("$ch", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { picked = emptyList() }) { Text("Clear") }
            Button(
                enabled = picked.size == scrambled.length,
                onClick = {
                    vm.submit(ScrambleAction.Guess(picked.map { scrambled[it] }.joinToString("")), owner.id)
                },
            ) { Text("Submit", fontWeight = FontWeight.Black) }
            TextButton(onClick = { vm.submit(ScrambleAction.Skip, owner.id) }) { Text("Skip") }
        }
    }
}

// ---------- Simon Says ----------

@Composable
internal fun SimonBoard(state: SimonState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    val padColors = listOf(MvCoral, MvMint, MvYellow, MvSky)
    var litPad by remember { mutableIntStateOf(-1) }

    // Replay the sequence, then hand control to the player
    LaunchedEffect(state.showing, state.length, state.turnIndex) {
        if (state.showing) {
            delay(700)
            for (i in 0 until state.length) {
                litPad = state.sequence[i]; delay(550)
                litPad = -1; delay(180)
            }
            vm.submit(SimonAction.DoneShowing, current.id)
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Round of ${state.length}  ·  ${current.name}", fontWeight = FontWeight.Black,
            fontSize = 17.sp, color = SeatColors[state.turnIndex], modifier = Modifier.padding(vertical = 8.dp))
        Text(
            if (state.showing) "👀 Watch the pattern..." else "Your turn - repeat it! (${state.inputPos}/${state.length})",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth().aspectRatio(1f).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (r in 0..1) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (c in 0..1) {
                        val pad = r * 2 + c
                        val lit = litPad == pad
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (lit) padColors[pad] else padColors[pad].copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .clickable(enabled = !state.showing) {
                                    vm.submit(SimonAction.Press(pad), current.id)
                                },
                        ) {}
                    }
                }
            }
        }
    }
}

// ---------- Maze Escape ----------

@Composable
internal fun MazeBoard(state: MazeState, vm: GameplayViewModel) {
    val runner = state.players[state.runnerIndex]
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${runner.name} is escaping  ·  moves ${state.moves[runner.id] ?: 0}",
            fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
        if (state.finished.isNotEmpty() && state.finished.size < state.players.size) {
            Text("${state.players.first { it.id in state.finished }.name} escaped in " +
                "${state.moves[state.finished.first()]} moves - beat that!",
                fontSize = 12.sp, color = MvOrange, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)) {
            for (r in 0 until MZ) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until MZ) {
                        val cell = r * MZ + c
                        val walls = state.walls[cell]
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .background(
                                    when {
                                        cell == state.pos -> MvPurple
                                        cell == MazeGame.exitCell() -> MvMint
                                        else -> MvLemon
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            // draw walls as thin edges (bit 0=N, 1=E, 2=S, 3=W)
                            if (walls and 1 != 0) Box(Modifier.fillMaxWidth().height(2.dp).background(MvDark).align(Alignment.TopCenter))
                            if (walls and 4 != 0) Box(Modifier.fillMaxWidth().height(2.dp).background(MvDark).align(Alignment.BottomCenter))
                            if (walls and 2 != 0) Box(Modifier.fillMaxHeight().width(2.dp).background(MvDark).align(Alignment.CenterEnd))
                            if (walls and 8 != 0) Box(Modifier.fillMaxHeight().width(2.dp).background(MvDark).align(Alignment.CenterStart))
                            if (cell == state.pos) Text("🏃", fontSize = 13.sp)
                            if (cell == MazeGame.exitCell()) Text("🚪", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        // D-pad
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
            DirBtn("⬆️") { vm.submit(MazeMove(MoveDir.N), runner.id) }
            Row {
                DirBtn("⬅️") { vm.submit(MazeMove(MoveDir.W), runner.id) }
                Spacer(Modifier.size(52.dp))
                DirBtn("➡️") { vm.submit(MazeMove(MoveDir.E), runner.id) }
            }
            DirBtn("⬇️") { vm.submit(MazeMove(MoveDir.S), runner.id) }
        }
    }
}

@Composable
private fun DirBtn(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp), color = MvSky,
        modifier = Modifier.padding(3.dp).size(52.dp).clickable(onClick = onClick),
    ) { Box(contentAlignment = Alignment.Center) { Text(label, fontSize = 22.sp) } }
}

