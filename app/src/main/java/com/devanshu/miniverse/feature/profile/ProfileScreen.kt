package com.devanshu.miniverse.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.SettingsRepository
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.feature.gameplay.PlayerColors
import com.devanshu.miniverse.ui.theme.CardPastels
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.floaty
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val avatars = listOf("🦊", "🐼", "🦁", "🐙", "🦄", "🐸", "🤖", "👾")

private fun rankTitle(wins: Int) = when {
    wins >= 50 -> "👑 Legend of MiniVerse"
    wins >= 20 -> "🥇 Champion"
    wins >= 5 -> "🥈 Rising Star"
    else -> "🥉 Rookie Explorer"
}

/**
 * Profile laid out like a social profile: avatar ring and counters on top,
 * a "stories" row of saved friends, and a grid of the games you have played.
 */
@Composable
fun ProfileScreen(repository: GameRepository, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsRepository(context.applicationContext) }
    val player by repository.player.collectAsState(initial = null)
    val wins by (player?.let { repository.wins(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val played by (player?.let { repository.played(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val wallet by (player?.let { repository.wallet(it.playerId) } ?: flowOf(null)).collectAsState(initial = null)
    val stats by (player?.let { repository.gameStats(it.playerId) } ?: flowOf(emptyList())).collectAsState(initial = emptyList())
    val friends by store.friends.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf(false) }
    var name by remember(player?.name) { mutableStateOf(player?.name ?: "") }
    var avatar by remember(player?.avatar) { mutableStateOf(player?.avatar ?: avatars.first()) }
    var addingFriend by remember { mutableStateOf(false) }
    var friendName by remember { mutableStateOf("") }
    var managing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        // handle bar
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("@${(player?.name ?: "player").lowercase().replace(" ", "_")}", fontWeight = FontWeight.Black,
                fontSize = 20.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("⚙️", fontSize = 22.sp) }
        }

        // avatar ring + counters
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(104.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(MvYellow, MvPink, Color(0xFF9D7BFF), MvSky, MvYellow))),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(92.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) { Text(player?.avatar ?: "🙂", fontSize = 52.sp, modifier = Modifier.floaty(3.dp, 2400)) }
            }
            Row(Modifier.weight(1f).padding(start = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Counter("$played", "Matches")
                Counter("$wins", "Wins")
                Counter("${wallet?.balance ?: 0}", "Coins")
            }
        }
        Text(player?.name ?: "", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(rankTitle(wins), color = MvYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        val favourite = stats.firstOrNull()?.let { GameRegistry.find(it.gameId)?.definition }
        Text(
            if (favourite != null) "Favourite game: ${favourite.emoji} ${favourite.name}" else "Play a game to fill your profile!",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { editing = !editing }, modifier = Modifier.weight(1f)) {
                Text(if (editing) "Close" else "Edit profile", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                Text("Match history", fontWeight = FontWeight.Black)
            }
        }

        if (editing) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { if (it.length <= 16) name = it },
                        label = { Text("Nickname") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        avatars.forEach { a ->
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (a == avatar) MvYellow else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { avatar = a },
                                contentAlignment = Alignment.Center,
                            ) { Text(a, fontSize = 18.sp) }
                        }
                    }
                    Button(
                        enabled = name.trim().length >= 2,
                        onClick = {
                            scope.launch {
                                player?.let { repository.updateProfile(it.playerId, name.trim(), avatar) }
                                editing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save", fontWeight = FontWeight.Black) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // friends, like a stories row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("My friends", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (friends.isNotEmpty()) {
                TextButton(onClick = { managing = !managing }) { Text(if (managing) "Done" else "Manage") }
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { addingFriend = !addingFriend }) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape).border(2.dp, MvYellow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("＋", fontSize = 26.sp, color = MvYellow) }
                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            friends.forEachIndexed { i, friend ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = managing) { scope.launch { store.removeFriend(friend) } },
                ) {
                    Box(
                        Modifier.size(62.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PlayerColors[i % 4], PlayerColors[(i + 1) % 4]))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (managing) "✕" else friend.take(1).uppercase(), fontSize = 24.sp,
                            fontWeight = FontWeight.Black, color = MvDark)
                    }
                    Text(friend, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (addingFriend) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                OutlinedTextField(
                    value = friendName, onValueChange = { if (it.length <= 14) friendName = it },
                    label = { Text("Friend's name") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = friendName.trim().length >= 2,
                    onClick = {
                        val clean = friendName.trim()
                        friendName = ""; addingFriend = false
                        scope.launch { store.addFriend(clean) }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("Save") }
            }
        }
        Text("Saved friends appear when you set up a game - no retyping names.", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // games grid, like a posts grid
        Spacer(Modifier.height(14.dp))
        Text("My games", fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        if (stats.isEmpty()) {
            Text("Games you play will show up here as tiles.", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        stats.chunked(3).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { colIndex, s ->
                    val def = GameRegistry.find(s.gameId)?.definition
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                            .background(CardPastels[(rowIndex * 3 + colIndex) % CardPastels.size]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(def?.emoji ?: "🎮", fontSize = 34.sp)
                            Text(def?.name ?: s.gameId, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MvDark,
                                maxLines = 1, textAlign = TextAlign.Center)
                            Text("▶ ${s.plays}   🏆 ${s.wins}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MvDark)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun Counter(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
