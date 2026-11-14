package com.devanshu.miniverse.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.ui.theme.MascotLogo
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.StarField
import com.devanshu.miniverse.ui.theme.pulseGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Animated launch: starfield, a logo that flips in with a 3D spin, glowing halo. */
@Composable
fun SplashScreen(hasPlayer: () -> GameRepository, onReady: (Boolean) -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val spin = remember { Animatable(-180f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val repo = hasPlayer()
        launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch { spin.animateTo(0f, tween(900)) }
        launch { delay(500); textAlpha.animateTo(1f, tween(600)) }
        delay(1700)
        onReady(repo.playerExists())
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0E0B1E), Color(0xFF2A1466), Color(0xFF0E0B1E)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        StarField()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(190.dp).pulseGlow(MvPink), contentAlignment = Alignment.Center) {
                MascotLogo(
                    size = 170.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale.value; scaleY = scale.value
                        rotationY = spin.value
                        cameraDistance = 14f * density
                    },
                )
            }
            Text(
                "MiniVerse", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
            )
            Text(
                "ONE APP. ENDLESS MINI ADVENTURES.", color = MvYellow, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 6.dp).graphicsLayer { alpha = textAlpha.value },
            )
            Text(
                "Powered by Devanshu", color = MvSky, fontSize = 11.sp,
                modifier = Modifier.padding(top = 26.dp).graphicsLayer { alpha = textAlpha.value },
            )
        }
    }
}
