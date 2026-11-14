package com.devanshu.miniverse.games.snakes

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Snakes & Ladders, 2-4 players, cells 1..100 (0 = waiting at the start).
 * Dice value is derived from (seed, rollCount), so reduce() stays
 * deterministic. Every roll records exactly what happened (lastMove) so the
 * board can animate it step by step and explain it.
 */
@Serializable
data class SnlMove(
    val playerId: String,
    val roll: Int,
    val from: Int,
    val landed: Int,        // square reached by walking (before any snake/ladder)
    val to: Int,            // final square
    val event: String?,     // "snake", "ladder", "overshoot", "win" or null
)

@Serializable
data class SnlState(
    val players: List<GamePlayer>,
    val seed: Long,
    val positions: Map<String, Int>,
    val turnIndex: Int,
    val rollCount: Int,
    val lastMove: SnlMove?,
)

data object RollDice

val LADDERS = mapOf(3 to 22, 8 to 30, 28 to 84, 58 to 77, 75 to 86, 80 to 99)
val SNAKES = mapOf(17 to 6, 52 to 29, 62 to 18, 88 to 36, 95 to 51, 97 to 63)

object SnakesGame : MiniGame<SnlState, RollDice> {
    override val definition = GameDefinition(
        id = "snakes", name = "Snakes & Ladders", emoji = "🐍", category = "Board",
        minPlayers = 2, maxPlayers = 4,
        description = "Roll, climb ladders, dodge snakes. First to 100 wins!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        SnlState(players, seed, players.associate { it.id to 0 }, 0, 0, null)

    override fun validateAction(state: SnlState, action: RollDice, playerId: String): ActionValidation =
        when {
            getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
            state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: SnlState, action: RollDice, playerId: String): SnlState {
        val roll = 1 + Random(state.seed + state.rollCount * 7919L).nextInt(6)
        val from = state.positions[playerId] ?: 0
        var landed = from + roll
        var event: String? = null
        if (landed > 100) { landed = from; event = "overshoot" }   // need the exact number
        var to = landed
        LADDERS[landed]?.let { to = it; event = "ladder" }
        SNAKES[landed]?.let { to = it; event = "snake" }
        if (to == 100) event = "win"
        // A six earns another roll (unless the game just ended).
        val again = roll == 6 && to != 100
        return state.copy(
            positions = state.positions + (playerId to to),
            turnIndex = if (to == 100 || again) state.turnIndex else (state.turnIndex + 1) % state.players.size,
            rollCount = state.rollCount + 1,
            lastMove = SnlMove(playerId, roll, from, landed, to, event),
        )
    }

    override fun getOutcome(state: SnlState): GameOutcome {
        val winner = state.positions.entries.firstOrNull { it.value == 100 }
        return if (winner != null) GameOutcome.Winner(winner.key) else GameOutcome.Incomplete
    }

    override fun encodeState(state: SnlState): String? = Json.encodeToString(SnlState.serializer(), state)
    override fun decodeState(json: String): SnlState? =
        runCatching { Json.decodeFromString(SnlState.serializer(), json) }.getOrNull()
}
