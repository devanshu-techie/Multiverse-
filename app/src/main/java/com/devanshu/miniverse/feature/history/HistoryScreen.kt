package com.devanshu.miniverse.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.MvCoral
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvSky
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(repository: GameRepository, onBack: () -> Unit) {
    val matches by repository.recentMatches.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Match History", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        if (matches.isEmpty()) {
            Text("No matches yet - go play something fun! 🎮",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
        LazyColumn {
            items(matches) { m ->
                val def = GameRegistry.find(m.gameId)?.definition
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(def?.emoji ?: "🎮", fontSize = 26.sp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(def?.name ?: m.gameId, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(m.startedAt)),
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            when (m.status) { "COMPLETED" -> "✅ Done"; "DRAW" -> "🤝 Draw"; "RUNNING" -> "▶ Running"; else -> "✖ Left" },
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = when (m.status) {
                                "COMPLETED" -> MvMint; "DRAW" -> MvSky; "ABANDONED" -> MvCoral
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}
