package com.devanshu.miniverse.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val avatars = listOf("🦊", "🐼", "🦁", "🐙", "🦄", "🐸", "🤖", "👾")

@Composable
fun ProfileScreen(repository: GameRepository, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit) {
    val player by repository.player.collectAsState(initial = null)
    val wins by (player?.let { repository.wins(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val played by (player?.let { repository.played(it.playerId) } ?: flowOf(0)).collectAsState(initial = 0)
    val wallet by (player?.let { repository.wallet(it.playerId) } ?: flowOf(null)).collectAsState(initial = null)

    var editing by remember { mutableStateOf(false) }
    var name by remember(player?.name) { mutableStateOf(player?.name ?: "") }
    var avatar by remember(player?.avatar) { mutableStateOf(player?.avatar ?: avatars.first()) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().height(180.dp)
                .background(Brush.verticalGradient(listOf(MvPurple, MvPink))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(player?.avatar ?: "🙂", fontSize = 56.sp)
                Text(player?.name ?: "", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard("🎮", "$played", "Matches", MvSky, Modifier.weight(1f))
            StatCard("🏆", "$wins", "Wins", MvYellow, Modifier.weight(1f))
            StatCard("🪙", "${wallet?.balance ?: 0}", "MiniCoins", MvMint, Modifier.weight(1f))
        }
        if (editing) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { if (it.length <= 16) name = it },
                        label = { Text("Nickname") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        avatars.forEach { a ->
                            Surface(
                                shape = CircleShape,
                                color = if (a == avatar) MvPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp).clickable { avatar = a },
                            ) { Box(contentAlignment = Alignment.Center) { Text(a, fontSize = 19.sp) } }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = name.trim().length >= 2,
                        onClick = {
                            scope.launch {
                                player?.let { repository.updateProfile(it.playerId, name.trim(), avatar) }
                                editing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MenuRow("✏️  Edit profile") { editing = !editing }
        MenuRow("🕹️  Match history") { onOpenHistory() }
        MenuRow("⚙️  Settings") { onOpenSettings() }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.18f), modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
