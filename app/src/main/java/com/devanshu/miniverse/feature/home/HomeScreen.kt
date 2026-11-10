package com.devanshu.miniverse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.CardPastels
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.categoryColor
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    repository: GameRepository,
    onOpenLibrary: () -> Unit,
    onOpenWallet: () -> Unit,
    onPickGame: (String) -> Unit,
) {
    val player by repository.player.collectAsState(initial = null)
    val wallet by (player?.let { repository.wallet(it.playerId) } ?: flowOf(null)).collectAsState(initial = null)
    val matches by repository.recentMatches.collectAsState(initial = emptyList())
    val defs = GameRegistry.definitions

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        // Playful gradient header
        item {
            Box(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(MvPurple, MvPink, MvSky)),
                        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    )
                    .padding(20.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(player?.avatar ?: "🙂", fontSize = 40.sp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Hi, ${player?.name ?: "player"}! 👋",
                                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("What shall we play today?",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp), color = MvDark,
                            modifier = Modifier.clickable(onClick = onOpenWallet),
                        ) {
                            Text("🪙 ${wallet?.balance ?: 0}",
                                color = MvYellow, fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // Featured strip
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(defs.take(5)) { i, def ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.clickable { onPickGame(def.id) },
                            ) {
                                Column(
                                    Modifier.width(92.dp).padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(def.emoji, fontSize = 32.sp)
                                    Text(def.name, color = Color.White, fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("All games", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text("See all ›", color = MvPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onOpenLibrary))
            }
        }
        itemsIndexed(defs) { i, def ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardPastels[i % CardPastels.size],
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clickable { onPickGame(def.id) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Text(def.emoji, fontSize = 28.sp) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(def.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MvDark)
                        Text(def.description, fontSize = 12.sp, color = MvDark.copy(alpha = 0.6f), maxLines = 1)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = categoryColor(def.category)) {
                        Text("${def.minPlayers}-${def.maxPlayers}P", fontSize = 11.sp,
                            fontWeight = FontWeight.Black, color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
        item {
            Text("Recent matches", fontWeight = FontWeight.Black, fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            if (matches.isEmpty()) {
                Text("No matches yet - tap a game above to start! 🚀",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        items(matches.take(5)) { m ->
            val def = GameRegistry.find(m.gameId)?.definition
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(def?.emoji ?: "🎮", fontSize = 20.sp)
                Text(def?.name ?: m.gameId, Modifier.weight(1f).padding(start = 10.dp), fontSize = 14.sp)
                Text(
                    when (m.status) { "COMPLETED" -> "✅"; "DRAW" -> "🤝"; "RUNNING" -> "▶️"; else -> "✖️" },
                    fontSize = 14.sp,
                )
            }
        }
    }
}
