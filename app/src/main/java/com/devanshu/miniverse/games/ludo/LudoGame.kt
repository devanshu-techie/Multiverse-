package com.devanshu.miniverse.games.ludo

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Full Ludo on the classic 15x15 cross board.
 *
 * Each player has 4 tokens. A token's "step" is:
 *   -1        in the yard (needs a 6 to come out)
 *   0..50     on the shared 52-cell loop, counted from the player's start cell
 *   51..55    in the player's coloured home column
 *   56        home (finished)
 *
 * Rules: a 6 brings a token out and gives another roll; landing on a rival
 * (not on a safe star/start cell) sends it back to the yard and gives another
 * roll; reaching home gives another roll; exact roll needed to get home.
 * Dice come from (seed, rollCount), so a match replays identically.
 */
const val LUDO_TOKENS = 4
const val LUDO_HOME = 56
const val LUDO_LOOP = 52

/** Loop cell index -> board (row, col), clockwise, index 0 = corner-0 start cell. */
val LUDO_PATH: List<Pair<Int, Int>> = buildList {
    for (c in 1..5) add(6 to c)
    for (r in 5 downTo 0) add(r to 6)
    add(0 to 7); add(0 to 8)
    for (r in 1..5) add(r to 8)
    for (c in 9..14) add(6 to c)
    add(7 to 14); add(8 to 14)
    for (c in 13 downTo 9) add(8 to c)
    for (r in 9..14) add(r to 8)
    add(14 to 7); add(14 to 6)
    for (r in 13 downTo 9) add(r to 6)
    for (c in 5 downTo 0) add(8 to c)
    add(7 to 0); add(6 to 0)
}

/** Home-column cells (steps 51..55) for each corner. */
val LUDO_HOME_COLUMNS: List<List<Pair<Int, Int>>> = listOf(
    (1..5).map { 7 to it },             // corner 0: from the left
    (1..5).map { it to 7 },             // corner 1: from the top
    (13 downTo 9).map { 7 to it },      // corner 2: from the right
    (13 downTo 9).map { it to 7 },      // corner 3: from the bottom
)

val LUDO_SAFE_CELLS = setOf(0, 8, 13, 21, 26, 34, 39, 47)

@Serializable
data class LudoMove(
    val playerId: String,
    val token: Int,
    val from: Int,
    val to: Int,
    val captured: Int,          // how many rival tokens were sent home
)

@Serializable
data class LudoState(
    val players: List<GamePlayer>,
    val seed: Long,
    val corners: Map<String, Int>,           // playerId -> corner 0..3 (colour + start)
    val tokens: Map<String, List<Int>>,      // playerId -> 4 steps
    val turnIndex: Int,
    val rollCount: Int,
    val pendingRoll: Int?,                   // rolled, waiting for a token choice
    val lastRoll: Int?,
    val lastMove: LudoMove?,
    val moveCount: Int,
    val note: String?,
)

sealed interface LudoAction {
    data object Roll : LudoAction
    data class Move(val token: Int) : LudoAction
}

object LudoGame : MiniGame<LudoState, LudoAction> {
    override val definition = GameDefinition(
        id = "ludo", name = "Ludo", emoji = "🎲", category = "Board",
        minPlayers = 2, maxPlayers = 4,
        description = "The real thing: 4 tokens each, race them all home!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): LudoState {
        // Two players sit opposite each other; three or four fill corners in order.
        val cornerOrder = if (players.size == 2) listOf(0, 2) else listOf(0, 1, 2, 3)
        return LudoState(
            players = players, seed = seed,
            corners = players.mapIndexed { i, p -> p.id to cornerOrder[i] }.toMap(),
            tokens = players.associate { it.id to List(LUDO_TOKENS) { -1 } },
            turnIndex = 0, rollCount = 0, pendingRoll = null, lastRoll = null,
            lastMove = null, moveCount = 0, note = null,
        )
    }

    /** Loop index (0..51) for a token on the loop, else null. */
    fun loopIndex(state: LudoState, playerId: String, step: Int): Int? {
        if (step !in 0..50) return null
        val corner = state.corners[playerId] ?: 0
        return (corner * 13 + step) % LUDO_LOOP
    }

    /** Board cell for any on-board step (loop or home column), null for yard/home. */
    fun cellOf(state: LudoState, playerId: String, step: Int): Pair<Int, Int>? {
        val corner = state.corners[playerId] ?: 0
        return when (step) {
            in 0..50 -> LUDO_PATH[(corner * 13 + step) % LUDO_LOOP]
            in 51..55 -> LUDO_HOME_COLUMNS[corner][step - 51]
            else -> null
        }
    }

    fun movableTokens(state: LudoState, playerId: String, roll: Int): List<Int> {
        val mine = state.tokens[playerId] ?: return emptyList()
        return mine.indices.filter { i ->
            val s = mine[i]
            when {
                s == LUDO_HOME -> false
                s == -1 -> roll == 6
                else -> s + roll <= LUDO_HOME
            }
        }
    }

    override fun validateAction(state: LudoState, action: LudoAction, playerId: String): ActionValidation {
        if (getOutcome(state) != GameOutcome.Incomplete) return ActionValidation.Invalid("Game over")
        if (state.players[state.turnIndex].id != playerId) return ActionValidation.Invalid("Not your turn")
        return when (action) {
            LudoAction.Roll ->
                if (state.pendingRoll == null) ActionValidation.Valid
                else ActionValidation.Invalid("Pick a token first")
            is LudoAction.Move -> {
                val roll = state.pendingRoll ?: return ActionValidation.Invalid("Roll first")
                if (action.token in movableTokens(state, playerId, roll)) ActionValidation.Valid
                else ActionValidation.Invalid("That token cannot move")
            }
        }
    }

    private fun nextTurn(state: LudoState) = (state.turnIndex + 1) % state.players.size

    override fun reduce(state: LudoState, action: LudoAction, playerId: String): LudoState = when (action) {
        LudoAction.Roll -> {
            val roll = 1 + Random(state.seed + state.rollCount * 7919L).nextInt(6)
            val movable = movableTokens(state, playerId, roll)
            if (movable.isEmpty()) {
                state.copy(
                    rollCount = state.rollCount + 1, lastRoll = roll, pendingRoll = null,
                    turnIndex = nextTurn(state),
                    note = if (roll == 6) "No token can use that 6" else "No move - next player",
                )
            } else {
                state.copy(
                    rollCount = state.rollCount + 1, lastRoll = roll, pendingRoll = roll,
                    note = if (roll == 6) "A six! Bring a token out or move one" else null,
                )
            }
        }
        is LudoAction.Move -> {
            val roll = state.pendingRoll ?: return state
            val mine = state.tokens.getValue(playerId)
            val from = mine[action.token]
            val to = if (from == -1) 0 else from + roll
            var tokens = state.tokens + (playerId to mine.toMutableList().also { it[action.token] = to })

            // Capture: rivals standing on the same loop cell, unless it is a safe cell.
            var captured = 0
            val myLoop = loopIndex(state, playerId, to)
            if (myLoop != null && myLoop !in LUDO_SAFE_CELLS) {
                state.players.filter { it.id != playerId }.forEach { rival ->
                    val theirs = tokens.getValue(rival.id)
                    val updated = theirs.map { s ->
                        if (loopIndex(state, rival.id, s) == myLoop) { captured++; -1 } else s
                    }
                    tokens = tokens + (rival.id to updated)
                }
            }
            val reachedHome = to == LUDO_HOME
            val again = roll == 6 || captured > 0 || reachedHome
            state.copy(
                tokens = tokens,
                pendingRoll = null,
                turnIndex = if (again) state.turnIndex else nextTurn(state),
                lastMove = LudoMove(playerId, action.token, from, to, captured),
                moveCount = state.moveCount + 1,
                note = when {
                    captured > 0 -> "Captured! Roll again"
                    reachedHome -> "Token home! Roll again"
                    roll == 6 -> "Six - roll again"
                    else -> null
                },
            )
        }
    }

    fun homeCount(state: LudoState, playerId: String): Int =
        state.tokens[playerId]?.count { it == LUDO_HOME } ?: 0

    override fun getOutcome(state: LudoState): GameOutcome {
        val winner = state.players.firstOrNull { homeCount(state, it.id) == LUDO_TOKENS }
        return if (winner != null) GameOutcome.Winner(winner.id) else GameOutcome.Incomplete
    }

    override fun encodeState(state: LudoState): String? = Json.encodeToString(LudoState.serializer(), state)
    override fun decodeState(json: String): LudoState? =
        runCatching { Json.decodeFromString(LudoState.serializer(), json) }.getOrNull()
}
