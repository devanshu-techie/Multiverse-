package com.devanshu.miniverse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvYellow

@Composable
fun HomeScreen(
    repository: GameRepository,
    onOpenLibrary: () -> Unit,
    onOpenWallet: () -> Unit,
    onPickGame: (String) -> Unit,
) {
    val player by repository.player.collectAsState(initial = null)
    val wallet by (player?.let { repository.wallet(it.playerId) }
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val matches by repository.recentMatches.collectAsState(initial = emptyList())

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player?.avatar ?: "🙂", fontSize = 34.sp)
                Spacer(Modifier.weight(0.05f))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Hi, ${player?.name ?: "player"}!", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Ready for a quick game?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp), color = MvDark,
                    modifier = Modifier.clickable(onClick = onOpenWallet),
                ) {
                    Text(
                        "🪙 ${wallet?.balance ?: 0} MC",
                        color = MvYellow, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Play now", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
        }
        items(GameRegistry.definitions) { def ->
            Card(
                onClick = { onPickGame(def.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(def.emoji, fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(def.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(def.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${def.minPlayers}-${def.maxPlayers}P", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            Text("Recent matches", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) {
                Text("No matches yet - play your first game!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        items(matches.take(10)) { m ->
            val def = GameRegistry.find(m.gameId)?.definition
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(def?.emoji ?: "🎮", fontSize = 20.sp)
                Text(def?.name ?: m.gameId, Modifier.weight(1f).padding(start = 10.dp), fontSize = 14.sp)
                Text(m.status, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = when (m.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.secondary
                        "ABANDONED" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
            }
        }
    }
}
