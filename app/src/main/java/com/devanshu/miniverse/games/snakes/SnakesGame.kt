package com.devanshu.miniverse.games.snakes

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Snakes & Ladders, 2-4 players, cells 1..100 (0 = not started).
 * Dice value is derived from (seed, rollCount), so reduce() stays
 * deterministic: the same match replays identically from the seed.
 */
data class SnlState(
    val players: List<GamePlayer>,
    val seed: Long,
    val positions: Map<String, Int>,
    val turnIndex: Int,
    val rollCount: Int,
    val lastRoll: Int?,          // shown on the dice
    val lastEvent: String?,      // "snake", "ladder", "exact", null
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
        SnlState(players, seed, players.associate { it.id to 0 }, 0, 0, null, null)

    override fun validateAction(state: SnlState, action: RollDice, playerId: String): ActionValidation =
        when {
            getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
            state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: SnlState, action: RollDice, playerId: String): SnlState {
        val roll = 1 + Random(state.seed + state.rollCount).nextInt(6)
        val from = state.positions[playerId] ?: 0
        var target = from + roll
        var event: String? = null
        if (target > 100) { target = from; event = "need exact" }   // overshoot: stay
        LADDERS[target]?.let { target = it; event = "ladder" }
        SNAKES[target]?.let { target = it; event = "snake" }
        if (target == 100) event = "win"
        return state.copy(
            positions = state.positions + (playerId to target),
            turnIndex = if (target == 100) state.turnIndex else (state.turnIndex + 1) % state.players.size,
            rollCount = state.rollCount + 1,
            lastRoll = roll,
            lastEvent = event,
        )
    }

    override fun getOutcome(state: SnlState): GameOutcome {
        val winner = state.positions.entries.firstOrNull { it.value == 100 }
        return if (winner != null) GameOutcome.Winner(winner.key) else GameOutcome.Incomplete
    }
}
