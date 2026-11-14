package com.devanshu.miniverse.feature.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.SettingsRepository
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.feature.gameplay.PlayerColorNames
import com.devanshu.miniverse.feature.gameplay.PlayerColors
import com.devanshu.miniverse.ui.theme.MvYellow
import kotlinx.coroutines.launch

/** Games that have a built-in robot opponent. */
private val botGames = setOf("tictactoe", "connectfour", "snakes", "ludo", "rps")
private const val BOT_NAME = "Robo 🤖"

/**
 * Who is playing? Player 1 is always the profile owner. Other seats are
 * filled from the saved friends list (names are typed once and remembered),
 * by typing a new name, or by the robot.
 */
@Composable
fun SetupScreen(
    repository: GameRepository,
    gameId: String,
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { SettingsRepository(context.applicationContext) }
    val def = GameRegistry.find(gameId)?.definition
    val maxP = def?.maxPlayers ?: 2
    val minP = (def?.minPlayers ?: 2).coerceAtLeast(1)
    val owner by repository.player.collectAsState(initial = null)
    val friends by store.friends.collectAsState(initial = emptyList())
    var others by remember { mutableStateOf(listOf<String>()) }      // names in seats 2..n
    var newName by remember { mutableStateOf("") }
    var starting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val count = 1 + others.size
    val canAdd = count < maxP
    val hasBot = BOT_NAME in others

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("${def?.emoji ?: ""} ${def?.name ?: "Game"}", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Text("Who's playing?", fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.padding(top = 6.dp))
        Text(
            if (minP == maxP) "This game needs $minP player${if (minP > 1) "s" else ""}."
            else "$minP to $maxP players. Pass the phone on your turn.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        // ---- seats ----
        SeatRow(1, owner?.name ?: "You", owner?.avatar ?: "🙂", "that's you!", PlayerColors[0], null)
        others.forEachIndexed { i, name ->
            SeatRow(
                i + 2, name, if (name == BOT_NAME) "🤖" else "🧒",
                if (name == BOT_NAME) "robot player" else "${PlayerColorNames[(i + 1) % 4]} player",
                PlayerColors[(i + 1) % 4],
            ) { others = others.filterIndexed { idx, _ -> idx != i } }
        }
        // empty seats
        for (seat in (count + 1)..maxP) {
            Surface(
                shape = RoundedCornerShape(18.dp), color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
            ) {
                Text(
                    "Player $seat  -  empty seat" + if (seat <= minP) " (needed)" else " (optional)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // ---- add a player ----
        if (canAdd) {
            Spacer(Modifier.height(12.dp))
            Text("＋ Add player ${count + 1}", fontWeight = FontWeight.Black, fontSize = 17.sp)
            val available = friends.filter { it !in others }
            if (available.isNotEmpty()) {
                Text("Saved friends - tap to add", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    available.forEach { friend ->
                        Surface(
                            shape = RoundedCornerShape(20.dp), color = PlayerColors[count % 4],
                            modifier = Modifier.clickable { Sfx.play(Sfx.TAP); others = others + friend },
                        ) {
                            Text("🧒 $friend", fontWeight = FontWeight.Black, color = Color(0xFF1B1730),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 14) newName = it },
                    label = { Text("New friend's name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = newName.trim().length >= 2 && newName.trim() !in others,
                    onClick = {
                        val clean = newName.trim()
                        others = others + clean
                        newName = ""
                        Sfx.play(Sfx.POP)
                        scope.launch { store.addFriend(clean) }     // remembered for next time
                    },
                    modifier = Modifier.padding(start = 8.dp).height(54.dp),
                ) { Text("Add", fontWeight = FontWeight.Black) }
            }
            if (gameId in botGames && !hasBot) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { Sfx.play(Sfx.TAP); others = others + BOT_NAME },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 30.sp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Play against the robot", fontWeight = FontWeight.Black)
                            Text("No friend nearby? Robo will play with you.", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = MvYellow.copy(alpha = 0.15f)) {
            Text("🪙 Win +20 · Draw +10 · Lose +5 MiniCoins (for your profile)",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        Button(
            enabled = !starting && def != null && owner != null && count in minP..maxP,
            onClick = {
                starting = true
                Sfx.play(Sfx.COIN)
                scope.launch {
                    val me = repository.currentPlayer() ?: return@launch
                    val players = buildList {
                        add(GamePlayer(me.playerId, me.name, seat = 0))
                        others.forEachIndexed { i, name ->
                            add(
                                if (name == BOT_NAME) GamePlayer(BOT_ID, "Robo", i + 1)
                                else GamePlayer("guest-${i + 2}", name, i + 1)
                            )
                        }
                    }
                    val (matchId, _) = repository.createMatch(gameId, players, me.playerId)
                    onMatchCreated(matchId)
                }
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
        ) {
            Text(
                when {
                    starting -> "Starting..."
                    count < minP -> "Add ${minP - count} more player${if (minP - count > 1) "s" else ""}"
                    else -> "🎉 START GAME"
                },
                fontSize = 18.sp, fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SeatRow(seat: Int, name: String, avatar: String, note: String, color: Color, onRemove: (() -> Unit)?) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(2.dp, color, RoundedCornerShape(18.dp)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Text(avatar, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Player $seat · $note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRemove != null) TextButton(onClick = onRemove) { Text("✕") }
        }
    }
}
