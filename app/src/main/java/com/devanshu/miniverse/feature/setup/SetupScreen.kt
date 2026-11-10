package com.devanshu.miniverse.feature.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.GameRegistry
import kotlinx.coroutines.launch

/**
 * Pass-and-play setup (mode = LOCAL). Seat 0 is the profile owner; seat 1 is a
 * guest seat with no wallet (decision D5). Online and two-phone modes plug in
 * here later via the MultiplayerTransport from the roadmap.
 */
@Composable
fun SetupScreen(
    repository: GameRepository,
    gameId: String,
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
) {
    val def = GameRegistry.find(gameId)?.definition
    var guestName by remember { mutableStateOf("Player 2") }
    var starting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text(def?.name ?: "Game", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${def?.emoji ?: ""}  ${def?.description ?: ""}", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Mode: Pass & Play (same phone)", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Winner +20 MC · Draw +10 MC · Loser +5 MC (profile player only)",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = guestName,
            onValueChange = { if (it.length <= 16) guestName = it },
            label = { Text("Player 2 name (guest)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            enabled = !starting && def != null && guestName.trim().length >= 2,
            onClick = {
                starting = true
                scope.launch {
                    val owner = repository.currentPlayer() ?: return@launch
                    val players = listOf(
                        GamePlayer(owner.playerId, owner.name, seat = 0),
                        GamePlayer("guest-2", guestName.trim(), seat = 1),
                    )
                    val (matchId, _) = repository.createMatch(gameId, players, owner.playerId)
                    onMatchCreated(matchId)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (starting) "Starting..." else "Start match", fontSize = 16.sp) }
    }
}
