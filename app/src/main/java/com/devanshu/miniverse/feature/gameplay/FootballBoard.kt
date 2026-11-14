package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.games.football.Dive
import com.devanshu.miniverse.games.football.FootballGame
import com.devanshu.miniverse.games.football.FootballState
import com.devanshu.miniverse.games.football.KickPhase
import com.devanshu.miniverse.games.football.NextKick
import com.devanshu.miniverse.games.football.SHOTS_EACH
import com.devanshu.miniverse.games.football.Shoot
import kotlinx.coroutines.delay

/** Centre of a goal zone as a fraction of the scene (3 columns x 2 rows). */
private fun zoneCenter(zone: Int): Offset = Offset(0.22f + 0.28f * (zone % 3), 0.17f + 0.2f * (zone / 3))

@Composable
internal fun FootballBoard(state: FootballState, vm: GameplayViewModel) {
    val colors = listOf(PlayerColors[0], PlayerColors[3])
    val over = state.kickIndex >= SHOTS_EACH * 2
    val shooter = if (over) state.players[0] else FootballGame.shooter(state)
    val keeper = if (over) state.players[1] else FootballGame.keeper(state)
    val active = if (state.phase == KickPhase.DIVE) keeper else shooter
    var box by remember { mutableStateOf(IntSize(1, 1)) }
    val fly = remember { Animatable(0f) }          // 0..1 ball flight / keeper dive

    // Play the kick: ball flies, keeper dives, then the next kick is set up.
    LaunchedEffect(state.kickIndex, state.phase) {
        if (state.phase == KickPhase.SHOWN) {
            val (shot, dive) = state.lastShot ?: (0 to 0)
            fly.snapTo(0f)
            Sfx.play(Sfx.KICK)
            fly.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            Sfx.play(if (shot != dive) Sfx.HORN else Sfx.WHISTLE)
            delay(1700)
            vm.submit(NextKick, shooter.id)
        } else fly.snapTo(0f)
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        DuelHeader(
            players = state.players,
            turnIndex = if (over) -1 else state.players.indexOf(active),
            colors = colors, badges = listOf("1", "2"),
            scores = state.players.map { "${state.scores[it.id] ?: 0} goals" },
        )
        // kick history: 5 lights per player
        state.players.forEachIndexed { pi, p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(p.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors[pi])
                repeat(SHOTS_EACH) { k ->
                    val kick = k * 2 + pi
                    val result = state.history.getOrNull(kick)
                    Box(
                        Modifier.size(14.dp).clip(CircleShape).background(
                            when (result) { true -> Color(0xFF2FD08A); false -> Color(0xFFE5484D); null -> Color(0x33FFFFFF) }
                        )
                    )
                }
            }
        }

        // instruction banner
        Surface(
            shape = RoundedCornerShape(16.dp), color = colors[state.players.indexOf(active).coerceIn(0, 1)],
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            val shot = state.lastShot
            Text(
                when {
                    over -> "Full time!"
                    state.phase == KickPhase.SHOWN && shot != null ->
                        if (shot.first != shot.second) "⚽ GOOOAL! ${shooter.name} scores!" else "🧤 SAVED by ${keeper.name}!"
                    state.phase == KickPhase.SHOOT && shooter.id == BOT_ID -> "🤖 Robo is taking the kick..."
                    state.phase == KickPhase.SHOOT -> "⚽ ${shooter.name}: tap the goal where you want to SHOOT (keep it secret!)"
                    keeper.id == BOT_ID -> "🤖 Robo the keeper is guessing..."
                    else -> "🧤 Pass the phone! ${keeper.name}: tap where to DIVE"
                },
                color = Color(0xFF1B1730), fontWeight = FontWeight.Black, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp),
            )
        }

        // the pitch
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.95f).clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E8E4E), Color(0xFF2FB368), Color(0xFF1E8E4E))))
                .onSizeChanged { box = it }
                .pointerInput(state.phase, state.kickIndex) {
                    detectTapGestures { tap ->
                        if (over || active.id == BOT_ID || state.phase == KickPhase.SHOWN) return@detectTapGestures
                        val fx = tap.x / size.width; val fy = tap.y / size.height
                        if (fy > 0.5f || fx < 0.06f || fx > 0.94f) return@detectTapGestures
                        val col = ((fx - 0.08f) / 0.28f).toInt().coerceIn(0, 2)
                        val row = if (fy < 0.27f) 0 else 1
                        val zone = row * 3 + col
                        Sfx.play(Sfx.TAP)
                        if (state.phase == KickPhase.SHOOT) vm.submit(Shoot(zone), shooter.id)
                        else vm.submit(Dive(zone), keeper.id)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                // mown stripes
                for (i in 0 until 6) if (i % 2 == 0) drawRect(Color(0x14FFFFFF), topLeft = Offset(0f, h * i / 6f), size = Size(w, h / 6f))
                // goal net
                val gl = w * 0.08f; val gt = h * 0.06f; val gw = w * 0.84f; val gh = h * 0.41f
                drawRect(Color(0x33000000), topLeft = Offset(gl, gt), size = Size(gw, gh))
                for (i in 1 until 12) drawLine(Color(0x55FFFFFF), Offset(gl + gw * i / 12f, gt), Offset(gl + gw * i / 12f, gt + gh), 2f)
                for (i in 1 until 6) drawLine(Color(0x55FFFFFF), Offset(gl, gt + gh * i / 6f), Offset(gl + gw, gt + gh * i / 6f), 2f)
                // zone guides (only while someone is choosing)
                if (state.phase != KickPhase.SHOWN && !over) {
                    for (z in 0..5) {
                        val c = zoneCenter(z)
                        drawCircle(Color.White.copy(alpha = 0.28f), radius = w * 0.075f, center = Offset(c.x * w, c.y * h), style = Stroke(5f))
                    }
                }
                // posts and crossbar
                drawRoundRect(Color.White, topLeft = Offset(gl - 10f, gt - 10f), size = Size(gw + 20f, gh + 10f),
                    cornerRadius = CornerRadius(8f, 8f), style = Stroke(16f))
                // penalty box + spot
                drawRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(w * 0.02f, gt + gh), size = Size(w * 0.96f, h * 0.3f), style = Stroke(5f))
                drawCircle(Color.White, radius = 9f, center = Offset(w * 0.5f, h * 0.84f))
            }
            // keeper
            val shot = state.lastShot
            val diveTo = if (state.phase == KickPhase.SHOWN && shot != null) zoneCenter(shot.second) else Offset(0.5f, 0.3f)
            val keeperPos = lerpOffset(Offset(0.5f, 0.3f), diveTo, fly.value)
            Text(
                "🧤", fontSize = 54.sp,
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                    translationX = keeperPos.x * box.width - 34.dp.toPx()
                    translationY = keeperPos.y * box.height - 36.dp.toPx()
                    rotationZ = (diveTo.x - 0.5f) * 140f * fly.value
                },
            )
            // ball
            val ballTo = if (state.phase == KickPhase.SHOWN && shot != null) zoneCenter(shot.first) else Offset(0.5f, 0.84f)
            val ballPos = lerpOffset(Offset(0.5f, 0.84f), ballTo, fly.value)
            Text(
                "⚽", fontSize = 40.sp,
                modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                    translationX = ballPos.x * box.width - 24.dp.toPx()
                    translationY = ballPos.y * box.height - 26.dp.toPx()
                    val shrink = 1f - fly.value * 0.35f        // gets smaller as it flies away
                    scaleX = shrink; scaleY = shrink
                    rotationZ = fly.value * 540f
                },
            )
        }
        Text(
            "Kick ${(state.kickIndex / 2 + 1).coerceAtMost(SHOTS_EACH)} of $SHOTS_EACH  ·  roles swap after every kick",
            fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 8.dp),
        )
    }
}
