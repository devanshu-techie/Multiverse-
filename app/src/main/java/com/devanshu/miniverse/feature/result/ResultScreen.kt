package com.devanshu.miniverse.feature.result

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.audio.Voice
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.MatchPlayerEntity
import com.devanshu.miniverse.data.RewardPolicy
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.feature.gameplay.PlayerColors
import com.devanshu.miniverse.feature.gameplay.Series
import com.devanshu.miniverse.feature.gameplay.SeriesStore
import com.devanshu.miniverse.ui.theme.ConfettiOverlay
import com.devanshu.miniverse.ui.theme.MascotLogo
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.StarField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/** A cheering character for each game's winner ceremony. */
private val gameCharacter = mapOf(
    "ludo" to "🐴", "snakes" to "🐍", "tictactoe" to "🤖", "connectfour" to "🦸", "dotsboxes" to "🧙",
    "rps" to "🥷", "reaction" to "⚡", "football" to "⚽", "sumo" to "🤼", "airhockey" to "🏒",
    "tank" to "🪖", "whack" to "🐹", "pinball" to "🪩", "runner" to "🏃", "balloon" to "🎈",
    "arballoon" to "📸", "tilt" to "🛸", "memory" to "🧠", "quickmath" to "🧮", "merge" to "🔢",
    "scramble" to "🔤", "simon" to "🚦", "maze" to "🌀",
)

/**
 * The winner ceremony. Reads the SAVED result from the database (never
 * recomputes), builds the suspense with a drumroll, then announces the
 * winner like a boxing-ring announcer - on screen and out loud.
 */
@Composable
fun ResultScreen(
    repository: GameRepository,
    matchId: String,
    onHome: () -> Unit,
    onNextRound: (String) -> Unit = {},
    onPlayAgain: (String) -> Unit = {},
) {
    var rows by remember { mutableStateOf<List<MatchPlayerEntity>>(emptyList()) }
    var gameId by remember { mutableStateOf("") }
    var gameName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var series by remember { mutableStateOf<Series?>(null) }
    var phase by remember { mutableIntStateOf(0) }            // 0 = drumroll, 1 = announced
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(0f) }

    val winnerRow = rows.firstOrNull { it.result == "WIN" }
    val soloLoss = status == "COMPLETED" && winnerRow == null
    val seriesOn = series != null
    val seriesDecided = series?.decided == true
    val seriesChampion = series?.takeIf { it.decided }?.leaderId?.let { id -> rows.firstOrNull { it.playerId == id } }

    LaunchedEffect(matchId) {
        val (match, players) = repository.matchWithPlayers(matchId)
        rows = players.sortedBy { it.seat }
        status = match?.status ?: ""
        gameId = match?.gameId ?: ""
        gameName = match?.let { GameRegistry.find(it.gameId)?.definition?.name } ?: ""
        val winnerId = players.firstOrNull { it.result == "WIN" }?.playerId
        series = if (match != null && match.status != "ABANDONED") SeriesStore.record(matchId, match.gameId, winnerId) else null

        // Suspense, then the big reveal.
        if (match?.status == "ABANDONED") { phase = 1; pop.snapTo(1f); return@LaunchedEffect }
        Sfx.play(Sfx.DRUMROLL)
        delay(1700)
        phase = 1
        val s = series
        val champion = s?.takeIf { it.decided }?.leaderId?.let { id -> players.firstOrNull { it.playerId == id } }
        val winner = players.firstOrNull { it.result == "WIN" }
        when {
            champion != null -> { Sfx.play(Sfx.CHAMPION); Voice.say("Ladies and gentlemen! The champion of $gameName is ${champion.playerName}! Congratulations ${champion.playerName}!") }
            s != null && !s.decided && winner != null -> { Sfx.play(Sfx.WIN); Voice.say("${winner.playerName} wins round ${s.roundsPlayed}!") }
            winner != null -> { Sfx.play(Sfx.CHAMPION); Voice.say("And the winner is ${winner.playerName}! Congratulations ${winner.playerName}!") }
            match?.status == "DRAW" || (s != null && s.decided) -> { Sfx.play(Sfx.WIN); Voice.say("It's a tie! Well played everyone!") }
            else -> { Sfx.play(Sfx.LOSE); Voice.say("Good try ${players.firstOrNull()?.playerName ?: ""}! Play again and beat it!") }
        }
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
    }

    val t = rememberInfiniteTransition(label = "ceremony")
    val sweep by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    val jump by t.animateFloat(0f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "jump")

    val headlineName = seriesChampion?.playerName ?: winnerRow?.playerName
    val celebrate = phase == 1 && headlineName != null && status != "ABANDONED"

    Box(Modifier.fillMaxSize()) {
        StarField()
        // Spotlights sweeping over the ring
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            listOf(-1f, 1f).forEach { side ->
                val topX = w / 2 + side * w * 0.42f
                val aim = w / 2 + side * sweep * w * (if (phase == 0) 0.35f else 0.08f)
                val beam = Path().apply {
                    moveTo(topX, 0f); lineTo(aim - w * 0.2f, h * 0.5f); lineTo(aim + w * 0.2f, h * 0.5f); close()
                }
                drawPath(beam, Color.White.copy(alpha = if (phase == 0) 0.10f else 0.06f))
            }
            // boxing-ring ropes
            val ropeColors = listOf(Color(0xFFFF5A6E), Color.White, Color(0xFF3FA9F5))
            ropeColors.forEachIndexed { i, c ->
                val y = h * (0.105f + i * 0.022f)
                drawLine(c.copy(alpha = 0.85f), Offset(w * 0.04f, y), Offset(w * 0.96f, y), strokeWidth = 7f, cap = StrokeCap.Round)
            }
            drawLine(Color(0xFFFFC93C), Offset(w * 0.04f, h * 0.085f), Offset(w * 0.04f, h * 0.17f), strokeWidth = 16f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFC93C), Offset(w * 0.96f, h * 0.085f), Offset(w * 0.96f, h * 0.17f), strokeWidth = 16f, cap = StrokeCap.Round)
        }
        if (celebrate) ConfettiOverlay()

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(150.dp))
            if (phase == 0) {
                // ---------- suspense ----------
                Text("🎤", fontSize = 64.sp, modifier = Modifier.graphicsLayer { rotationZ = sweep * 12f })
                Text("AND THE WINNER IS...", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MvYellow,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text("🥁 🥁 🥁", fontSize = 30.sp, modifier = Modifier.padding(top = 10.dp).graphicsLayer {
                    translationY = abs(sin(sweep * 9f)) * -14f
                })
            } else {
                // ---------- the announcement ----------
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Text("🎉", fontSize = 44.sp, modifier = Modifier.graphicsLayer {
                        rotationZ = -25f + jump * 20f; scaleX = pop.value; scaleY = pop.value
                    })
                    Box(contentAlignment = Alignment.BottomCenter) {
                        MascotLogo(
                            size = 150.dp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pop.value; scaleY = pop.value
                                translationY = if (celebrate) -jump * 26f else 0f
                                rotationY = (1f - pop.value) * 360f
                                cameraDistance = 14f * density
                            },
                        )
                        Text(
                            when {
                                status == "DRAW" && seriesChampion == null -> "🤝"
                                soloLoss -> "💪"
                                else -> gameCharacter[gameId] ?: "🏆"
                            },
                            fontSize = 46.sp,
                            modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer {
                                translationX = 40f; translationY = -(1f - jump) * 22f
                                scaleX = pop.value; scaleY = pop.value
                            },
                        )
                    }
                    Text("🎊", fontSize = 44.sp, modifier = Modifier.graphicsLayer {
                        rotationZ = 25f - jump * 20f; scaleX = pop.value; scaleY = pop.value
                    })
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        status == "ABANDONED" -> "Match abandoned"
                        seriesChampion != null -> "🏆 SERIES CHAMPION 🏆"
                        seriesOn && !seriesDecided && winnerRow != null -> "ROUND ${series?.roundsPlayed} WINNER"
                        winnerRow != null -> "🏆 WINNER 🏆"
                        soloLoss -> "SO CLOSE!"
                        else -> "IT'S A TIE!"
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.Black, color = MvYellow, letterSpacing = 3.sp,
                )
                Text(
                    headlineName ?: when {
                        soloLoss -> "Nice try, ${rows.firstOrNull()?.playerName ?: ""}!"
                        status == "ABANDONED" -> ""
                        else -> "Everybody wins!"
                    },
                    fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center,
                    lineHeight = 46.sp,
                    modifier = Modifier.graphicsLayer { scaleX = 0.6f + pop.value * 0.4f; scaleY = 0.6f + pop.value * 0.4f },
                )
                Text(
                    if (celebrate) "Congratulations! 👑  ·  $gameName" else gameName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                )

                // ---------- series scoreboard ----------
                series?.let { s ->
                    Spacer(Modifier.height(14.dp))
                    Surface(shape = RoundedCornerShape(18.dp), color = MvYellow.copy(alpha = 0.16f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (s.decided) "Series finished - best of ${s.bestOf}" else "Round ${s.roundsPlayed} of ${s.bestOf} done",
                                fontWeight = FontWeight.Black, fontSize = 13.sp, color = MvYellow,
                            )
                            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                rows.forEachIndexed { i, p ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(p.playerName, fontWeight = FontWeight.Black, color = PlayerColors[i % 4], fontSize = 14.sp)
                                        val w = s.wins[p.playerId] ?: 0
                                        Text("⭐".repeat(w).ifEmpty { "·" }, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------- scores and coins ----------
                Spacer(Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        rows.forEachIndexed { i, p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).background(PlayerColors[i % 4], RoundedCornerShape(6.dp)))
                                Text(p.playerName, Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Black)
                                Text("score ${p.score}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 10.dp))
                                if (!p.isLocalGuest && status != "ABANDONED") {
                                    val coins = when (p.result) {
                                        "WIN" -> RewardPolicy.WIN
                                        "DRAW" -> RewardPolicy.DRAW
                                        else -> RewardPolicy.LOSS
                                    }
                                    Text("+$coins 🪙", fontWeight = FontWeight.Black, color = MvMint)
                                } else {
                                    Text(
                                        when (p.result) { "WIN" -> "WIN 👑"; "DRAW" -> "DRAW"; "LOSS" -> "LOSS"; else -> p.result },
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.result == "WIN") MvYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- what next ----------
                Spacer(Modifier.height(18.dp))
                val s = series
                if (s != null && !s.decided) {
                    Button(
                        onClick = {
                            Sfx.play(Sfx.COIN)
                            scope.launch {
                                val (nextId, _) = repository.createMatch(s.gameId, s.players, s.ownerId)
                                onNextRound(nextId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (s.nextIsFinal) Color(0xFFFF5A6E) else MvYellow),
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(30.dp)),
                    ) {
                        Text(
                            if (s.nextIsFinal) "🔥 FINAL ROUND - PLAY!" else "▶ ROUND ${s.nextRound} - PLAY!",
                            fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1730),
                        )
                    }
                } else {
                    Button(
                        onClick = { SeriesStore.clear(); onPlayAgain(gameId) },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                    ) { Text("↺ PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Black) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { SeriesStore.clear(); onHome() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("🏠 Home", fontSize = 16.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
