package com.devanshu.miniverse.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.ui.theme.MvDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(hasPlayer: () -> GameRepository, onReady: (Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        val repo = hasPlayer()
        delay(600)
        val exists = repo.playerExists()
        onReady(exists)
    }
    Box(Modifier.fillMaxSize().background(MvDark), contentAlignment = Alignment.Center) {
        Text("MiniVerse", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
    }
}
