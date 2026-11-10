package com.devanshu.miniverse.feature.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.MatchPlayerEntity
import com.devanshu.miniverse.data.RewardPolicy
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvYellow

/** Reads the SAVED result from Room (never recomputes - roadmap rule). */
@Composable
fun ResultScreen(repository: GameRepository, matchId: String, onHome: () -> Unit) {
    var rows by remember { mutableStateOf<List<MatchPlayerEntity>>(emptyList()) }
    var gameName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(matchId) {
        val (match, players) = repository.matchWithPlayers(matchId)
        rows = players.sortedBy { it.seat }
        status = match?.status ?: ""
        gameName = match?.let { GameRegistry.find(it.gameId)?.definition?.name } ?: ""
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (status == "DRAW") "🤝" else "🏆", fontSize = 56.sp)
        Text(
            when (status) {
                "DRAW" -> "It's a draw!"
                "ABANDONED" -> "Match abandoned"
                else -> "${rows.firstOrNull { it.result == "WIN" }?.playerName ?: "Someone"} wins!"
            },
            fontSize = 26.sp, fontWeight = FontWeight.Black,
        )
        Text(gameName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                rows.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.playerName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("score ${p.score}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 10.dp))
                        if (!p.isLocalGuest && status != "ABANDONED") {
                            val coins = when (p.result) {
                                "WIN" -> RewardPolicy.WIN
                                "DRAW" -> RewardPolicy.DRAW
                                else -> RewardPolicy.LOSS
                            }
                            Text("+$coins MC", fontWeight = FontWeight.Black, color = MvMint)
                        } else {
                            Text(p.result, fontWeight = FontWeight.Bold,
                                color = if (p.result == "WIN") MvYellow else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Back to Home", fontSize = 16.sp)
        }
    }
}
