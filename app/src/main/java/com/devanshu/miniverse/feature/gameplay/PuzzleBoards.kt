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
import com.devanshu.miniverse.games.ludo.LudoRoll
import com.devanshu.miniverse.games.ludo.LudoState
import com.devanshu.miniverse.games.ludo.RING
import com.devanshu.miniverse.games.ludo.TRACK
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
import com.devanshu.miniverse.games.football.FootballState
import com.devanshu.miniverse.games.football.SHOTS_EACH
import com.devanshu.miniverse.games.football.Shoot
import com.devanshu.miniverse.games.tank.Fire
import com.devanshu.miniverse.games.tank.HITS_TO_WIN
import com.devanshu.miniverse.games.tank.TankGame
import com.devanshu.miniverse.games.tank.TankState
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

// ---------- Mini Ludo (ring race) ----------

@Composable
internal fun LudoBoard(state: LudoState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    // 7x7 border ring = 24 cells, clockwise from top-left
    val ring = buildList {
        for (c in 0..6) add(0 to c)
        for (r in 1..6) add(r to 6)
        for (c in 5 downTo 0) add(6 to c)
        for (r in 5 downTo 1) add(r to 0)
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${current.name}'s turn" + (state.lastRoll?.let { "  ·  🎲 $it" } ?: ""),
            fontWeight = FontWeight.Black, fontSize = 16.sp,
            color = SeatColors[state.turnIndex % SeatColors.size],
            modifier = Modifier.padding(vertical = 6.dp),
        )
        when (state.lastEvent) {
            "capture" -> Text("💥 Sent them back to start!", color = MvCoral, fontWeight = FontWeight.Bold)
            "exact" -> Text("Need the exact number to finish!", color = MvOrange, fontWeight = FontWeight.Bold)
            "safe" -> Text("Safe square - no capture here.", color = MvMint, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().aspectRatio(1f).padding(6.dp)) {
            for (r in 0..6) {
                Row(Modifier.weight(1f)) {
                    for (c in 0..6) {
                        val cell = ring.indexOf(r to c)
                        Box(Modifier.weight(1f).fillMaxSize().padding(1.dp), contentAlignment = Alignment.Center) {
                            if (cell >= 0) {
                                val isStart = LudoGame.isSafeCell(cell)
                                val startSeat = cell / (RING / 4)
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isStart) SeatColors[startSeat % 4].copy(alpha = 0.35f) else MvLemon
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row {
                                        state.players.forEach { p ->
                                            if (LudoGame.ringCell(state, p.id) == cell) {
                                                Box(
                                                    Modifier.size(13.dp).clip(CircleShape)
                                                        .background(SeatColors[p.seat % 4])
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (r == 3 && c == 3) Text("🏠", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            state.players.forEach { p ->
                val prog = state.progress[p.id] ?: 0
                Surface(shape = RoundedCornerShape(10.dp), color = SeatColors[p.seat % 4].copy(alpha = 0.2f)) {
                    Text(
                        "${p.name}: " + if (prog >= TRACK) "🏠 home!" else "$prog/$TRACK",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Button(
            onClick = { vm.submit(LudoRoll, current.id) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) { Text("🎲 Roll the dice!", fontWeight = FontWeight.Black, fontSize = 16.sp) }
    }
}

// ---------- Tank Battle (artillery) ----------

@Composable
internal fun TankBoard(state: TankState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    var angle by remember { mutableFloatStateOf(45f) }
    var power by remember { mutableFloatStateOf(25f) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${current.name} aims  ·  first to $HITS_TO_WIN hits", fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.hits[p.id]} 💥", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Distance ${state.distance.toInt()} m   ·   Wind " +
                (if (state.wind > 0) "→ " else if (state.wind < 0) "← " else "") + abs(state.wind),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.lastLanding?.let {
            Text(
                if (state.lastHit == true) "💥 DIRECT HIT!"
                else "Landed at ${it.toInt()} m (" + (if (it < state.distance) "short" else "long") + ")",
                fontWeight = FontWeight.Black,
                color = if (state.lastHit == true) MvCoral else MvSky,
            )
        }
        // Simple field drawing
        Box(
            Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp)).background(MvSky.copy(alpha = 0.2f)),
        ) {
            Text("🚜", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp))
            Text("🚜", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp))
            Text("⛰️", fontSize = 34.sp, modifier = Modifier.align(Alignment.BottomCenter))
        }
        Text("Angle: ${angle.toInt()}°", fontWeight = FontWeight.Bold)
        Slider(value = angle, onValueChange = { angle = it }, valueRange = 10f..80f,
            modifier = Modifier.fillMaxWidth(0.9f))
        Text("Power: ${power.toInt()}", fontWeight = FontWeight.Bold)
        Slider(value = power, onValueChange = { power = it }, valueRange = 10f..40f,
            modifier = Modifier.fillMaxWidth(0.9f))
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { vm.submit(Fire(angle, power), current.id) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("🔥 FIRE!", fontWeight = FontWeight.Black, fontSize = 17.sp) }
    }
}

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

// ---------- Finger Football (penalty flick) ----------

@Composable
internal fun FootballBoard(state: FootballState, vm: GameplayViewModel) {
    val shooter = state.players[state.shotIndex % 2]
    val shotNo = state.shotIndex / 2 + 1
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${shooter.name} shoots  ·  shot $shotNo of $SHOTS_EACH",
            fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.scores[p.id]} ⚽", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        state.lastShot?.let { (shot, keeper) ->
            Text(
                if (shot != keeper) "GOOOAL! 🥅" else "Saved by the keeper! 🧤",
                fontWeight = FontWeight.Black, fontSize = 17.sp,
                color = if (shot != keeper) MvMint else MvCoral,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("Pick your corner - the keeper guesses too!", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        // Goal: 2 rows x 3 zones
        Column(
            Modifier.fillMaxWidth().aspectRatio(1.5f).padding(10.dp)
                .clip(RoundedCornerShape(14.dp)).background(MvMint.copy(alpha = 0.25f)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0..1) {
                Row(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..2) {
                        val zone = row * 3 + col
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.weight(1f).fillMaxSize()
                                .clickable { vm.submit(Shoot(zone), shooter.id) },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (row == 0) "🥅" else "⚽", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
        Text("Top row = high shots, bottom row = low shots", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
