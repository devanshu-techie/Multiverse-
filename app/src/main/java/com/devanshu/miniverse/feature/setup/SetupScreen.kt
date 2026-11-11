package com.devanshu.miniverse.feature.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.SeatColors
import kotlinx.coroutines.launch

/**
 * Pass-and-play setup. Seat 0 is the profile owner; other seats are guest
 * seats with no wallet (decision D5). Games like Snakes & Ladders allow up
 * to 4 players on one phone.
 */
@Composable
fun SetupScreen(
    repository: GameRepository,
    gameId: String,
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
) {
    val def = GameRegistry.find(gameId)?.definition
    val maxP = def?.maxPlayers ?: 2
    val minP = (def?.minPlayers ?: 2).coerceAtLeast(1)
    var playerCount by remember { mutableIntStateOf(minP) }
    val guestNames = remember { mutableStateOf(listOf("Player 2", "Player 3", "Player 4")) }
    var starting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("${def?.emoji ?: ""} ${def?.name ?: "Game"}", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(def?.description ?: "", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Mode: Pass & Play (same phone)", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Winner +20 MC · Draw +10 MC · Loser +5 MC (your profile only)",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (maxP > minP) {
            Spacer(Modifier.height(18.dp))
            Text("How many players?", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (minP..maxP).forEach { n ->
                    Surface(
                        shape = CircleShape,
                        color = if (playerCount == n) MvPurple else Color.White,
                        modifier = Modifier.size(46.dp).clickable { playerCount = n },
                    ) {
                        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$n", fontWeight = FontWeight.Black,
                                color = if (playerCount == n) Color.White else MvPurple, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        (1 until playerCount).forEach { seat ->
            OutlinedTextField(
                value = guestNames.value[seat - 1],
                onValueChange = { new ->
                    if (new.length <= 16) {
                        guestNames.value = guestNames.value.toMutableList().also { it[seat - 1] = new }
                    }
                },
                label = { Text("Player ${seat + 1} name (guest)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !starting && def != null &&
                guestNames.value.take(playerCount - 1).all { it.trim().length >= 2 },
            onClick = {
                starting = true
                scope.launch {
                    val owner = repository.currentPlayer() ?: return@launch
                    val players = buildList {
                        add(GamePlayer(owner.playerId, owner.name, seat = 0))
                        (1 until playerCount).forEach { seat ->
                            add(GamePlayer("guest-${seat + 1}", guestNames.value[seat - 1].trim(), seat))
                        }
                    }
                    val (matchId, _) = repository.createMatch(gameId, players, owner.playerId)
                    onMatchCreated(matchId)
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(if (starting) "Starting..." else "🎉 Start match", fontSize = 16.sp, fontWeight = FontWeight.Black) }
    }
}
