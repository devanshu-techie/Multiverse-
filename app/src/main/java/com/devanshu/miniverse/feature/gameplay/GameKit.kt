package com.devanshu.miniverse.feature.gameplay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devanshu.miniverse.engine.BOT_ID
import com.devanshu.miniverse.games.connectfour.C4Action
import com.devanshu.miniverse.games.connectfour.C4State
import com.devanshu.miniverse.games.connectfour.COLS
import com.devanshu.miniverse.games.connectfour.ConnectFourGame
import com.devanshu.miniverse.games.ludo.LUDO_HOME
import com.devanshu.miniverse.games.ludo.LUDO_SAFE_CELLS
import com.devanshu.miniverse.games.ludo.LudoGame
import com.devanshu.miniverse.games.ludo.LudoState
import com.devanshu.miniverse.games.rps.Hand
import com.devanshu.miniverse.games.rps.RpsAction
import com.devanshu.miniverse.games.rps.RpsMode
import com.devanshu.miniverse.games.rps.RpsState
import com.devanshu.miniverse.games.tictactoe.TicTacToeGame
import com.devanshu.miniverse.games.tictactoe.TttAction
import com.devanshu.miniverse.games.tictactoe.TttState
import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameOutcome
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Player colours used on every board: red, green, yellow, blue (classic Ludo order). */
val PlayerColors = listOf(Color(0xFFFF5A6E), Color(0xFF2FD08A), Color(0xFFFFC93C), Color(0xFF3FA9F5))
val PlayerColorNames = listOf("Red", "Green", "Yellow", "Blue")

// ====================== 3D-look dice ======================

private val pipLayouts: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
    3 to listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f),
    4 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f),
    5 to listOf(0.27f to 0.27f, 0.73f to 0.27f, 0.5f to 0.5f, 0.27f to 0.73f, 0.73f to 0.73f),
    6 to listOf(0.28f to 0.24f, 0.72f to 0.24f, 0.28f to 0.5f, 0.72f to 0.5f, 0.28f to 0.76f, 0.72f to 0.76f),
)

/** A chunky dice that tumbles while [rolling] and then shows [value]. */
@Composable
fun Dice3D(value: Int?, rolling: Boolean, size: Dp = 68.dp, tint: Color = Color.White) {
    var face by remember { mutableIntStateOf(value ?: 1) }
    LaunchedEffect(rolling, value) {
        if (rolling) {
            while (true) { face = Random.nextInt(1, 7); delay(70) }
        } else if (value != null) face = value
    }
    val spin by animateFloatAsState(if (rolling) 540f else 0f, tween(if (rolling) 560 else 0), label = "diceSpin")
    val tilt by animateFloatAsState(if (rolling) 35f else 0f, tween(200), label = "diceTilt")

    Canvas(
        Modifier.size(size).graphicsLayer {
            rotationZ = spin
            rotationX = tilt
            rotationY = tilt
            cameraDistance = 10f * density
        },
    ) {
        val s = this.size.width
        val r = CornerRadius(s * 0.2f, s * 0.2f)
        // depth: darker slab offset down-right
        drawRoundRect(Color(0x66000000), topLeft = Offset(s * 0.06f, s * 0.09f), size = Size(s * 0.92f, s * 0.92f), cornerRadius = r)
        drawRoundRect(
            Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.82f)), Offset.Zero, Offset(s, s)),
            topLeft = Offset.Zero, size = Size(s * 0.92f, s * 0.92f), cornerRadius = r,
        )
        drawRoundRect(Color(0x33000000), topLeft = Offset.Zero, size = Size(s * 0.92f, s * 0.92f), cornerRadius = r, style = Stroke(s * 0.025f))
        pipLayouts[face]?.forEach { (px, py) ->
            drawCircle(Color(0xFF1B1730), radius = s * 0.075f, center = Offset(px * s * 0.92f, py * s * 0.92f))
        }
    }
}

// ====================== 3D-look pawn ======================

/** Draws a glossy game piece with a ground shadow. */
fun DrawScope.drawPawn(center: Offset, radius: Float, color: Color, highlight: Boolean = false, lift: Float = 0f) {
    // ground shadow (shrinks while the pawn is in the air)
    val shadowScale = 1f - lift * 0.35f
    drawOval(
        Color(0x55000000),
        topLeft = Offset(center.x - radius * shadowScale, center.y + radius * 0.55f),
        size = Size(radius * 2 * shadowScale, radius * 0.7f * shadowScale),
    )
    val c = Offset(center.x, center.y - lift * radius * 1.6f)
    if (highlight) drawCircle(Color.White.copy(alpha = 0.85f), radius = radius * 1.32f, center = c, style = Stroke(radius * 0.22f))
    drawCircle(Color(0xFF1B1730), radius = radius * 1.08f, center = c)                    // outline
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White, color, color.copy(red = color.red * 0.6f, green = color.green * 0.6f, blue = color.blue * 0.6f)),
            center = Offset(c.x - radius * 0.35f, c.y - radius * 0.4f), radius = radius * 1.5f,
        ),
        radius = radius, center = c,
    )
    drawCircle(Color.White.copy(alpha = 0.75f), radius = radius * 0.22f, center = Offset(c.x - radius * 0.35f, c.y - radius * 0.38f))
}

// ====================== Robot opponents ======================

object Bots {
    /**
     * Returns the robot's action when it is the robot's turn in a simple
     * turn-based game, otherwise null. (Ludo and Snakes & Ladders drive their
     * robot from the board so it waits for the animations.)
     */
    fun move(state: Any): Any? = when (state) {
        is TttState -> if (state.players[state.turnIndex].id == BOT_ID) ticTacToe(state) else null
        is C4State -> if (state.players[state.turnIndex].id == BOT_ID) connectFour(state) else null
        is RpsState ->
            if (state.mode == RpsMode.PICK && state.picks.size < 2 &&
                state.players[state.pickerIndex].id == BOT_ID
            ) RpsAction.Pick(Hand.entries[Random.nextInt(3)]) else null
        else -> null
    }

    private fun ticTacToe(s: TttState): TttAction? {
        if (TicTacToeGame.getOutcome(s) != GameOutcome.Incomplete) return null
        val free = s.cells.indices.filter { s.cells[it] == null }
        if (free.isEmpty()) return null
        val human = s.players.first { it.id != BOT_ID }
        // 1) win now  2) block  3) centre  4) corner  5) anything
        free.firstOrNull { TicTacToeGame.getOutcome(TicTacToeGame.reduce(s, TttAction(it), BOT_ID)) is GameOutcome.Winner }
            ?.let { return TttAction(it) }
        val asHuman = s.copy(turnIndex = s.players.indexOf(human))
        free.firstOrNull { TicTacToeGame.getOutcome(TicTacToeGame.reduce(asHuman, TttAction(it), human.id)) is GameOutcome.Winner }
            ?.let { return TttAction(it) }
        // A little randomness so kids can win sometimes.
        if (Random.nextInt(4) == 0) return TttAction(free.random())
        if (4 in free) return TttAction(4)
        listOf(0, 2, 6, 8).filter { it in free }.randomOrNull()?.let { return TttAction(it) }
        return TttAction(free.random())
    }

    private fun connectFour(s: C4State): C4Action? {
        if (ConnectFourGame.getOutcome(s) != GameOutcome.Incomplete) return null
        val cols = (0 until COLS).filter {
            ConnectFourGame.validateAction(s, C4Action(it), BOT_ID) == ActionValidation.Valid
        }
        if (cols.isEmpty()) return null
        val human = s.players.first { it.id != BOT_ID }
        cols.firstOrNull { ConnectFourGame.getOutcome(ConnectFourGame.reduce(s, C4Action(it), BOT_ID)) is GameOutcome.Winner }
            ?.let { return C4Action(it) }
        val asHuman = s.copy(turnIndex = s.players.indexOf(human))
        cols.firstOrNull { ConnectFourGame.getOutcome(ConnectFourGame.reduce(asHuman, C4Action(it), human.id)) is GameOutcome.Winner }
            ?.let { return C4Action(it) }
        if (Random.nextInt(3) == 0) return C4Action(cols.random())
        return C4Action(listOf(3, 2, 4, 1, 5, 0, 6).first { it in cols })
    }

    /** Ludo: capture > get home > leave the yard > advance the front runner. */
    fun ludoToken(state: LudoState, roll: Int): Int? {
        val movable = LudoGame.movableTokens(state, BOT_ID, roll)
        if (movable.isEmpty()) return null
        val mine = state.tokens.getValue(BOT_ID)
        fun landing(token: Int) = if (mine[token] == -1) 0 else mine[token] + roll
        movable.firstOrNull { t ->
            val loop = LudoGame.loopIndex(state, BOT_ID, landing(t))
            loop != null && loop !in LUDO_SAFE_CELLS && state.players.any { rival ->
                rival.id != BOT_ID && state.tokens.getValue(rival.id).any { LudoGame.loopIndex(state, rival.id, it) == loop }
            }
        }?.let { return it }
        movable.firstOrNull { landing(it) == LUDO_HOME }?.let { return it }
        movable.firstOrNull { mine[it] == -1 }?.let { return it }
        return movable.maxByOrNull { mine[it] }
    }
}
