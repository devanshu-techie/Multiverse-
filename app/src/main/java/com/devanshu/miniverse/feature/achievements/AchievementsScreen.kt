package com.devanshu.miniverse.feature.achievements

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvYellow
import kotlinx.coroutines.flow.flowOf

private data class Achievement(val emoji: String, val title: String, val detail: String, val unlocked: Boolean)

/**
 * Achievements derived live from the database (no extra table needed yet):
 * wins, matches played, coins and game variety.
 */
@Composable
fun AchievementsScreen(repository: GameRepository) {
    val player by repository.player.collectAsState(initial = null)
    val wins by (player?.let { repository.wins(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val played by (player?.let { repository.played(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val wallet by (player?.let { repository.wallet(it.playerId) } ?: flowOf(null)).collectAsState(initial = null)
    val variety by repository.distinctGamesFinished().collectAsState(initial = 0)
    val coins = wallet?.balance ?: 0

    val achievements = listOf(
        Achievement("🎉", "First Steps", "Play your first match", played >= 1),
        Achievement("🏆", "First Victory", "Win a match", wins >= 1),
        Achievement("🔥", "On a Roll", "Win 5 matches", wins >= 5),
        Achievement("👑", "Champion", "Win 20 matches", wins >= 20),
        Achievement("🎮", "Explorer", "Finish matches in 3 different games", variety >= 3),
        Achievement("🌟", "Collector", "Finish matches in all 8 games", variety >= 8),
        Achievement("🪙", "Saver", "Hold 200 MiniCoins", coins >= 200),
        Achievement("💰", "Rich Kid", "Hold 500 MiniCoins", coins >= 500),
        Achievement("🎯", "Regular", "Play 10 matches", played >= 10),
        Achievement("🚀", "Marathoner", "Play 50 matches", played >= 50),
    )
    val done = achievements.count { it.unlocked }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Achievements", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text("$done of ${achievements.size} unlocked", color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        LazyColumn {
            items(achievements) { a ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (a.unlocked) MvMint.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (a.unlocked) a.emoji else "🔒", fontSize = 28.sp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(a.title, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(a.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (a.unlocked) Text("✓", color = MvYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
