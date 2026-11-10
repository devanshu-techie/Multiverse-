package com.devanshu.miniverse.feature.onboarding

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.devanshu.miniverse.ui.theme.MvPurple
import kotlinx.coroutines.launch

private val avatars = listOf("🦊", "🐼", "🦁", "🐙", "🦄", "🐸", "🤖", "👾")

@Composable
fun OnboardingScreen(repository: GameRepository, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(avatars.first()) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to MiniVerse", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MvPurple)
        Text("One app. Endless mini adventures.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 16) name = it },
            label = { Text("Your nickname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Text("Pick an avatar", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatars.take(4).forEach { AvatarChip(it, avatar == it) { avatar = it } }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            avatars.drop(4).forEach { AvatarChip(it, avatar == it) { avatar = it } }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            enabled = name.trim().length >= 2 && !saving,
            onClick = {
                saving = true
                scope.launch {
                    repository.createPlayer(name, avatar)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (saving) "Creating..." else "Start playing  (+100 MC)", fontSize = 16.sp) }
    }
}

@Composable
private fun AvatarChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MvPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(56.dp)
            .border(if (selected) 2.dp else 0.dp, if (selected) MvPurple else Color.Transparent, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { Text(emoji, fontSize = 26.sp) }
    }
}
