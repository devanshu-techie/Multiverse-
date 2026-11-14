package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.audio.Sfx
import com.devanshu.miniverse.ui.theme.bouncyClick
import kotlinx.coroutines.delay
import kotlin.math.sin

private class Level(val name: String, val emoji: String, val color: Color, val words: List<String>)

private val scrambleLevels = listOf(
    Level("Baby", "🍼", Color(0xFFFF8AC2), "CAT DOG SUN BUS CUP HAT PIG BEE COW EGG FOX BAT CAR BED BOX ANT OWL TOY KEY PEN JAM HEN ZOO ICE MAP NET RUN RED BIG TOP".split(" ")),
    Level("Very Easy", "🐣", Color(0xFF2FD08A), "FISH BIRD FROG CAKE MOON STAR TREE BOOK BALL DUCK LION BEAR MILK RAIN SHIP KITE DRUM GOAT NOSE HAND LEAF DOOR BLUE PINK JUMP KING SNOW WOLF BOAT COIN".split(" ")),
    Level("Easy", "😀", Color(0xFF3FA9F5), "APPLE TIGER ROBOT CANDY HORSE MOUSE PIZZA TRAIN HAPPY WATER SMILE CLOUD GRAPE HOUSE PLANT ZEBRA SHEEP BREAD CHAIR MUSIC LEMON OCEAN RIVER STORM PANDA WHALE MAGIC DANCE HEART LIGHT".split(" ")),
    Level("Medium", "😎", Color(0xFFFFC93C), "PLANET ROCKET JUNGLE PIRATE WIZARD DRAGON CASTLE BANANA MONKEY PUZZLE SUMMER WINTER GARDEN FLOWER BRIDGE CAMERA PENCIL SCHOOL FRIEND ORANGE YELLOW PURPLE SILVER GOLDEN TURTLE RABBIT SPIDER GUITAR COOKIE ISLAND".split(" ")),
    Level("Hard", "🔥", Color(0xFFFF5A6E), "RAINBOW DOLPHIN PENGUIN GIRAFFE KITCHEN LIBRARY MONSTER POPCORN BALLOON CHICKEN ELEPHANT DINOSAUR SANDWICH UMBRELLA KANGAROO TREASURE MOUNTAIN COMPUTER BIRTHDAY HOSPITAL AIRPLANE SQUIRREL VOLCANO DIAMOND PYRAMID JOURNEY CAPTAIN".split(" ")),
)

private const val WORD_SECONDS = 15f
private const val START_LIVES = 3
private const val START_HINTS = 3

/**
 * Word Scramble run: pick a level, then unscramble words for as long as your
 * three hearts last. 15 seconds per word, a wrong word or a timeout costs a
 * heart, and a hint drops the next correct letter into its exact place.
 */
@Composable
internal fun ScrambleSim(onEnd: (Int) -> Unit) {
    var level by remember { mutableStateOf<Level?>(null) }
    var queue by remember { mutableStateOf(listOf<String>()) }
    var wordNo by remember { mutableIntStateOf(0) }
    var word by remember { mutableStateOf("") }
    var tiles by remember { mutableStateOf(listOf<Char>()) }
    var tileUsed by remember { mutableStateOf(listOf<Boolean>()) }
    var slots by remember { mutableStateOf(listOf<Int?>()) }          // tile index placed in each slot
    var locked by remember { mutableStateOf(setOf<Int>()) }           // slots fixed by a hint
    var lives by remember { mutableIntStateOf(START_LIVES) }
    var hints by remember { mutableIntStateOf(START_HINTS) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableFloatStateOf(WORD_SECONDS) }
    var flash by remember { mutableStateOf<String?>(null) }
    var ended by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val heartPop = remember { Animatable(1f) }

    fun loadNext() {
        val lv = level ?: return
        var q = queue
        if (q.isEmpty()) q = lv.words.shuffled()                 // endless: reshuffle and keep going
        val next = q.first()
        queue = q.drop(1)
        var mixed = next.toList().shuffled()
        if (mixed.joinToString("") == next) mixed = next.reversed().toList()
        word = next; tiles = mixed
        tileUsed = List(mixed.size) { false }
        slots = List(next.length) { null }
        locked = emptySet()
        timeLeft = WORD_SECONDS
        wordNo += 1
    }

    fun loseLife(reason: String) {
        lives -= 1
        flash = reason
        Sfx.play(Sfx.WRONG)
    }

    // 15-second clock for each word
    LaunchedEffect(wordNo, level) {
        if (level == null || ended) return@LaunchedEffect
        var lastTick = -1
        while (timeLeft > 0f && !ended) {
            delay(100)
            timeLeft -= 0.1f
            val whole = timeLeft.toInt()
            if (whole != lastTick && whole in 0..4) { lastTick = whole; Sfx.play(Sfx.TICK) }
        }
        if (!ended && timeLeft <= 0f) {
            loseLife("⏰ Time's up! It was $word")
            heartPop.snapTo(1.6f); heartPop.animateTo(1f, tween(400))
            delay(900)
            flash = null
            if (lives <= 0) { ended = true; onEnd(score) } else loadNext()
        }
    }

    // check the answer as soon as every slot is filled
    LaunchedEffect(slots) {
        if (level != null && slots.isNotEmpty() && slots.all { it != null } && !ended) {
            val guess = slots.map { tiles[it!!] }.joinToString("")
            if (guess == word) {
                score += 1
                flash = "✅ $word!"
                Sfx.play(Sfx.CORRECT)
                delay(700)
                flash = null
                loadNext()
            } else {
                loseLife("❌ Not $guess - try again!")
                heartPop.snapTo(1.6f)
                for (i in 0..8) { shake.snapTo(sin(i * 1.6f) * 18f); delay(35) }
                shake.snapTo(0f)
                heartPop.animateTo(1f, tween(300))
                if (lives <= 0) {
                    flash = "It was $word"
                    delay(1100)
                    ended = true; onEnd(score)
                } else {
                    // keep hinted letters, send the rest back
                    val keep = slots.mapIndexed { i, t -> if (i in locked) t else null }
                    tileUsed = tiles.indices.map { it in keep.filterNotNull() }
                    slots = keep
                    delay(500); flash = null
                }
            }
        }
    }

    val lv = level
    if (lv == null) {
        // ---------------- level picker ----------------
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pick your level", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("3 hearts · 15 seconds per word · 3 hints", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            scrambleLevels.forEach { l ->
                Surface(
                    shape = RoundedCornerShape(20.dp), color = l.color,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).bouncyClick {
                        Sfx.play(Sfx.POP)
                        level = l
                        queue = l.words.shuffled()
                    },
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(l.emoji, fontSize = 30.sp)
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(l.name.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B1730))
                            Text("${l.words.first().length}${if (l.name == "Hard") "-8" else ""} letter words", fontSize = 12.sp,
                                color = Color(0xFF1B1730).copy(alpha = 0.75f))
                        }
                    }
                }
            }
        }
        // first word loads once a level is chosen
        return
    }
    LaunchedEffect(lv) { if (wordNo == 0) loadNext() }

    Column(Modifier.fillMaxSize().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // hearts, level, score
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.graphicsLayer { scaleX = heartPop.value; scaleY = heartPop.value }) {
                repeat(START_LIVES) { i -> Text(if (i < lives) "❤️" else "🖤", fontSize = 26.sp) }
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(12.dp), color = lv.color) {
                Text("${lv.emoji} ${lv.name}", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF1B1730),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text("⭐ $score", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFFFFC93C))
        }
        Text("Word $wordNo", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp))

        // timer bar
        val frac = (timeLeft / WORD_SECONDS).coerceIn(0f, 1f)
        Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).height(14.dp).clip(RoundedCornerShape(7.dp))
                .background(Color(0x33FFFFFF)),
        ) {
            Box(
                Modifier.fillMaxWidth(frac.coerceAtLeast(0.01f)).height(14.dp).clip(RoundedCornerShape(7.dp))
                    .background(if (timeLeft < 5f) Color(0xFFE5484D) else if (timeLeft < 9f) Color(0xFFFFC93C) else Color(0xFF2FD08A)),
            )
        }
        Text("⏱ ${timeLeft.toInt().coerceAtLeast(0)} s", fontWeight = FontWeight.Black, fontSize = 16.sp,
            color = if (timeLeft < 5f) Color(0xFFE5484D) else Color.White)

        Spacer(Modifier.height(14.dp))
        // answer slots
        Row(
            Modifier.graphicsLayer { translationX = shake.value },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            slots.forEachIndexed { i, tileIndex ->
                val isLocked = i in locked
                Box(
                    Modifier.size(if (word.length > 6) 34.dp else 46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isLocked -> Color(0xFF2FD08A)
                                tileIndex != null -> Color.White
                                else -> Color(0x22FFFFFF)
                            }
                        )
                        .border(2.dp, if (isLocked) Color.White else Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                        .clickable(enabled = tileIndex != null && !isLocked) {
                            // take the letter back out
                            tileUsed = tileUsed.toMutableList().also { it[tileIndex!!] = false }
                            slots = slots.toMutableList().also { it[i] = null }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (tileIndex != null) {
                        Text("${tiles[tileIndex]}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF1B1730))
                    }
                }
            }
        }
        Text(flash ?: "Tap the letters in the right order", fontWeight = FontWeight.Black, fontSize = 15.sp,
            color = if (flash?.startsWith("✅") == true) Color(0xFF2FD08A) else if (flash != null) Color(0xFFFF8A8A) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 12.dp))

        // letter tiles
        Row(horizontalArrangement = Arrangement.spacedBy(if (word.length > 6) 5.dp else 8.dp)) {
            tiles.forEachIndexed { i, ch ->
                val used = tileUsed.getOrElse(i) { false }
                Box(
                    Modifier.size(if (word.length > 6) 36.dp else 50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (used) Color(0x22FFFFFF) else Color(0xFFFFC93C))
                        .clickable(enabled = !used && slots.any { it == null }) {
                            Sfx.play(Sfx.TAP)
                            val target = slots.indexOfFirst { it == null }
                            tileUsed = tileUsed.toMutableList().also { it[i] = true }
                            slots = slots.toMutableList().also { it[target] = i }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!used) Text("$ch", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF1B1730))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = hints > 0 && locked.size < word.length - 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FA9F5)),
                onClick = {
                    // drop the next correct letter into its exact place
                    val slot = (0 until word.length).firstOrNull { it !in locked } ?: return@Button
                    val wanted = word[slot]
                    var newSlots = slots.toMutableList()
                    var newUsed = tileUsed.toMutableList()
                    // free whatever sits in that slot
                    newSlots[slot]?.let { newUsed[it] = false; newSlots[slot] = null }
                    // find a free tile with the wanted letter, or pull one out of another open slot
                    var tile = tiles.indices.firstOrNull { tiles[it] == wanted && !newUsed[it] }
                    if (tile == null) {
                        val from = newSlots.indices.firstOrNull { s -> s !in locked && newSlots[s]?.let { tiles[it] == wanted } == true }
                        if (from != null) { tile = newSlots[from]; newSlots[from] = null }
                    }
                    if (tile != null) {
                        newUsed[tile] = true
                        newSlots[slot] = tile
                        locked = locked + slot
                        hints -= 1
                        Sfx.play(Sfx.SPARKLE)
                        tileUsed = newUsed; slots = newSlots
                    }
                },
            ) { Text("💡 Hint ($hints)", fontWeight = FontWeight.Black, color = Color(0xFF1B1730)) }
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x44FFFFFF)),
                onClick = {
                    val keep = slots.mapIndexed { i, t -> if (i in locked) t else null }
                    tileUsed = tiles.indices.map { it in keep.filterNotNull() }
                    slots = keep
                },
            ) { Text("↺ Clear", fontWeight = FontWeight.Black) }
        }
        Text("Green letters are hints - they are already in the right place.", fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 10.dp))
    }
}
