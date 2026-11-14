package com.devanshu.miniverse.feature.extras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.ConfettiOverlay
import com.devanshu.miniverse.ui.theme.MvCoral
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvOrange
import com.devanshu.miniverse.ui.theme.MvPink
import com.devanshu.miniverse.ui.theme.MvSky
import com.devanshu.miniverse.ui.theme.MvYellow
import com.devanshu.miniverse.ui.theme.StarField
import com.devanshu.miniverse.ui.theme.animatedGradient
import com.devanshu.miniverse.ui.theme.bouncyClick
import com.devanshu.miniverse.ui.theme.categoryColor
import com.devanshu.miniverse.ui.theme.floaty
import com.devanshu.miniverse.ui.theme.pulseGlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}

// ====================== Game details ======================

private val howTo: Map<String, List<String>> = mapOf(
    "tictactoe" to listOf("Take turns placing your mark.", "Get three in a row - across, down or diagonal.", "Block your friend before they do!"),
    "connectfour" to listOf("Tap a column to drop your disc.", "Connect four discs in any direction.", "Watch out for sneaky diagonals."),
    "snakes" to listOf("Roll the dice and move forward.", "Ladders lift you up, snakes slide you down.", "Land exactly on 100 to win."),
    "ludo" to listOf("Roll to race your token around the ring.", "Land on a rival to send them back to start.", "Coloured corner squares are safe. A 6 gives another roll."),
    "dotsboxes" to listOf("Draw one line between two dots.", "Close a box to score and play again.", "Most boxes wins."),
    "rps" to listOf("Pick rock, paper or scissors in secret.", "Pass the phone - no peeking!", "Best of three rounds wins."),
    "reaction" to listOf("Wait while the screen is red.", "Tap the moment it turns green.", "Tap too early and the round goes to your rival."),
    "football" to listOf("Choose a corner of the goal.", "The keeper dives to one zone each shot.", "Five shots each - most goals wins."),
    "sumo" to listOf("Tap your side as fast as you can.", "Push your rival out of the ring.", "Win two rounds."),
    "airhockey" to listOf("Drag your paddle with one finger.", "Hit the puck into the yellow goal.", "First to 3 goals wins."),
    "tank" to listOf("Set your angle and power.", "Check the wind before you fire.", "Three hits wins the battle."),
    "whack" to listOf("A mole pops out of a hole.", "Bonk it before it hides.", "15 moles each - most hits wins."),
    "pinball" to listOf("The ball falls with gravity.", "Tap FLIP when the ball is low to launch it up.", "Hit bumpers for points. You get 3 balls."),
    "runner" to listOf("Your runner never stops.", "Tap anywhere to jump over obstacles.", "Run as far as you can."),
    "balloon" to listOf("Balloons float up the screen.", "Pop as many as you can in 30 seconds.", "Bombs take away 2 points!"),
    "arballoon" to listOf("Allow the camera so balloons appear in your room.", "Pop balloons (+1) and golden stars (+3).", "Avoid the bombs (-2). You have 30 seconds."),
    "tilt" to listOf("Hold your phone flat.", "Tilt it to roll the glowing ball.", "Roll over stars to collect them. 30 seconds!"),
    "memory" to listOf("Flip two cards.", "A matching pair scores and gives another turn.", "Most pairs wins."),
    "quickmath" to listOf("Read the sum.", "Tap the right answer from four choices.", "Five questions each."),
    "merge" to listOf("Swipe to slide all the tiles.", "Two tiles with the same number merge.", "Make the 256 tile to win."),
    "scramble" to listOf("The letters are mixed up.", "Tap them in the right order.", "Solve 3 of 5 words to win."),
    "simon" to listOf("Watch the pads light up.", "Repeat the pattern in the same order.", "One mistake and you are out!"),
    "maze" to listOf("Use the arrows to walk through the maze.", "Reach the door in as few moves as you can.", "Both players get the same maze."),
)

@Composable
fun GameDetailsScreen(
    repository: GameRepository,
    gameId: String,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val def = GameRegistry.find(gameId)?.definition
    val player by repository.player.collectAsState(initial = null)
    val stats by (player?.let { repository.gameStats(it.playerId) } ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val mine = stats.firstOrNull { it.gameId == gameId }
    val accent = categoryColor(def?.category ?: "")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth().height(250.dp)
                .animatedGradient(listOf(Color(0xFF2A1466), accent, Color(0xFF2A1466))),
            contentAlignment = Alignment.Center,
        ) {
            StarField()
            Box(Modifier.size(180.dp).pulseGlow(Color.White), contentAlignment = Alignment.Center) {
                Text(def?.emoji ?: "🎮", fontSize = 96.sp, modifier = Modifier.floaty(8.dp, 2600))
            }
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                Text("← Back", color = Color.White)
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(def?.name ?: "Game", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(def?.description ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(def?.category ?: "", accent)
                Chip(
                    if (def?.minPlayers == def?.maxPlayers) "${def?.minPlayers} player"
                    else "${def?.minPlayers}-${def?.maxPlayers} players",
                    MvSky,
                )
                Chip("Played ${mine?.plays ?: 0}", MvMint)
                Chip("Won ${mine?.wins ?: 0}", MvYellow)
            }
            Spacer(Modifier.height(20.dp))
            Text("How to play", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            (howTo[gameId] ?: listOf("Follow the hints on screen and have fun!")).forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(accent),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Black) }
                    Text(step, modifier = Modifier.padding(start = 12.dp), fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = MvYellow.copy(alpha = 0.15f)) {
                Text(
                    "🪙 Win +20 · Draw +10 · Lose +5 MiniCoins",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text("▶  PLAY", fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Chip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MvDark,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

// ====================== Daily spin wheel ======================

private val wheelPrizes = listOf(5, 10, 15, 20, 25, 30, 40, 50)
private val wheelColors = listOf(MvCoral, MvYellow, MvMint, MvSky, MvPink, MvOrange, Color(0xFF9D7BFF), MvMint)

@Composable
fun DailySpinScreen(repository: GameRepository, onBack: () -> Unit) {
    val player by repository.player.collectAsState(initial = null)
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val key = "daily:${player?.playerId}:$today"
    val claimFlow = remember(key) { repository.claimCount(key) }
    val claimed by claimFlow.collectAsState(initial = 0)
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        StarField()
        if (won != null) ConfettiOverlay()
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) { TopBar("Daily Spin", onBack) }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    won != null -> "You won $won MiniCoins! 🎉"
                    claimed > 0 -> "Come back tomorrow for another spin!"
                    else -> "One free spin every day!"
                },
                fontWeight = FontWeight.Black, fontSize = 18.sp, color = MvYellow, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Text("▼", fontSize = 30.sp, color = MvCoral)
            Box(Modifier.size(300.dp).pulseGlow(MvPink), contentAlignment = Alignment.Center) {
                Box(Modifier.size(280.dp).graphicsLayer { rotationZ = rotation.value }) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sweep = 360f / wheelPrizes.size
                        wheelPrizes.indices.forEach { i ->
                            drawArc(
                                color = wheelColors[i],
                                startAngle = -90f + i * sweep, sweepAngle = sweep, useCenter = true,
                                topLeft = Offset.Zero, size = Size(size.width, size.height),
                            )
                        }
                        drawCircle(MvDark, radius = size.width * 0.09f)
                    }
                    wheelPrizes.forEachIndexed { i, prize ->
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                rotationZ = i * 45f + 22.5f
                            },
                        ) {
                            Text(
                                "$prize", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MvDark,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 26.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                enabled = !spinning && claimed == 0 && won == null && player != null,
                onClick = {
                    spinning = true
                    val index = Random.nextInt(wheelPrizes.size)
                    val target = 360f * 6 - (index * 45f + 22.5f)
                    scope.launch {
                        rotation.snapTo(0f)
                        rotation.animateTo(target, tween(4200, easing = FastOutSlowInEasing))
                        val p = player
                        if (p != null) {
                            val granted = repository.claimBonus(
                                p.playerId, key, "daily spin", wheelPrizes[index].toLong(),
                            )
                            if (granted) won = wheelPrizes[index]
                        }
                        spinning = false
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
            ) {
                Text(if (spinning) "Spinning..." else "🎡 SPIN!", fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ====================== Shop ======================

private data class ShopItem(val id: String, val emoji: String, val name: String, val price: Long)

private val shopItems = listOf(
    ShopItem("owl", "🦉", "Wise Owl", 60), ShopItem("tiger", "🐯", "Tiger", 80),
    ShopItem("alien", "👽", "Alien", 100), ShopItem("dino", "🦖", "Dino", 120),
    ShopItem("dragon", "🐲", "Dragon", 150), ShopItem("ninja", "🥷", "Ninja", 180),
    ShopItem("wizard", "🧙", "Wizard", 200), ShopItem("hero", "🦸", "Super Hero", 250),
    ShopItem("rocket", "🚀", "Rocket", 300), ShopItem("crown", "👑", "Royal Crown", 400),
)

@Composable
fun ShopScreen(repository: GameRepository, onBack: () -> Unit) {
    val player by repository.player.collectAsState(initial = null)
    val wallet by (player?.let { repository.wallet(it.playerId) } ?: flowOf(null)).collectAsState(initial = null)
    val owned by (player?.let { repository.ownedShopItems(it.playerId) } ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { TopBar("Avatar Shop", onBack) }
            Surface(shape = RoundedCornerShape(18.dp), color = MvDark) {
                Text("🪙 ${wallet?.balance ?: 0}", color = MvYellow, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Text(message ?: "Spend MiniCoins on a new look. Coins come only from playing!",
            fontSize = 13.sp, color = if (message != null) MvYellow else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shopItems) { item ->
                val isOwned = "shop:${item.id}" in owned
                val equipped = player?.avatar == item.emoji
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (equipped) MvMint.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().bouncyClick {
                        val p = player ?: return@bouncyClick
                        scope.launch {
                            if (isOwned) {
                                repository.updateProfile(p.playerId, p.name, item.emoji)
                                message = "${item.name} equipped!"
                            } else if (repository.purchase(p.playerId, item.id, item.price)) {
                                repository.updateProfile(p.playerId, p.name, item.emoji)
                                message = "You bought ${item.name}! 🎉"
                            } else {
                                message = "Not enough coins for ${item.name} - play more games!"
                            }
                        }
                    },
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.emoji, fontSize = 52.sp, modifier = Modifier.floaty(3.dp, 2000 + item.price.toInt() * 3))
                        Text(item.name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when { equipped -> MvMint; isOwned -> MvSky; else -> MvYellow },
                        ) {
                            Text(
                                when { equipped -> "IN USE"; isOwned -> "USE"; else -> "🪙 ${item.price}" },
                                fontWeight = FontWeight.Black, fontSize = 13.sp, color = MvDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ====================== Stats ======================

@Composable
fun StatsScreen(repository: GameRepository, onBack: () -> Unit) {
    val player by repository.player.collectAsState(initial = null)
    val stats by (player?.let { repository.gameStats(it.playerId) } ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())
    val totalPlays = stats.sumOf { it.plays }
    val totalWins = stats.sumOf { it.wins }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TopBar("My Stats", onBack)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("🎮", "$totalPlays", "Matches", MvSky, Modifier.weight(1f))
            StatTile("🏆", "$totalWins", "Wins", MvYellow, Modifier.weight(1f))
            StatTile(
                "📈", if (totalPlays == 0) "-" else "${totalWins * 100 / totalPlays}%",
                "Win rate", MvMint, Modifier.weight(1f),
            )
        }
        if (stats.isEmpty()) {
            Text("Play a few games and your stats will appear here!",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn {
            items(stats) { s ->
                val def = GameRegistry.find(s.gameId)?.definition
                val rate = if (s.plays == 0) 0f else s.wins.toFloat() / s.plays
                Surface(
                    shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(def?.emoji ?: "🎮", fontSize = 26.sp)
                            Text(def?.name ?: s.gameId, fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f).padding(start = 10.dp))
                            Text("${s.wins} / ${s.plays} won", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(rate.coerceIn(0.02f, 1f)).height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(categoryColor(def?.category ?: "")),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.2f), modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
