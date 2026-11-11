package com.devanshu.miniverse.games.simon

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Simon Says duel: one shared colour sequence from the seed. Players take
 * alternating turns repeating an ever-longer prefix. First mistake loses.
 * The UI drives the "showing" phase; the engine owns correctness.
 */
data class SimonState(
    val players: List<GamePlayer>,
    val sequence: List<Int>,        // 0..3, pre-generated
    val length: Int,                // current prefix length to repeat
    val turnIndex: Int,
    val inputPos: Int,              // progress inside the current attempt
    val showing: Boolean,           // UI replays the sequence
    val loser: String?,
)

sealed interface SimonAction {
    data object DoneShowing : SimonAction
    data class Press(val color: Int) : SimonAction
}

object SimonGame : MiniGame<SimonState, SimonAction> {
    override val definition = GameDefinition(
        id = "simon", name = "Simon Says", emoji = "🚦", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Watch the colours, repeat the pattern. One slip = out!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): SimonState {
        val rng = Random(seed)
        return SimonState(players, List(40) { rng.nextInt(4) }, 1, 0, 0, true, null)
    }

    override fun validateAction(state: SimonState, action: SimonAction, playerId: String): ActionValidation =
        when {
            state.loser != null -> ActionValidation.Invalid("Game over")
            action is SimonAction.DoneShowing ->
                if (state.showing) ActionValidation.Valid else ActionValidation.Invalid("Not showing")
            state.showing -> ActionValidation.Invalid("Watch first")
            state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: SimonState, action: SimonAction, playerId: String): SimonState = when (action) {
        SimonAction.DoneShowing -> state.copy(showing = false, inputPos = 0)
        is SimonAction.Press -> {
            if (action.color != state.sequence[state.inputPos]) {
                state.copy(loser = playerId)
            } else if (state.inputPos + 1 < state.length) {
                state.copy(inputPos = state.inputPos + 1)
            } else {
                // Prefix completed: other player's turn, sequence grows every 2 turns
                val nextTurn = (state.turnIndex + 1) % state.players.size
                state.copy(
                    turnIndex = nextTurn,
                    length = if (nextTurn == 0) state.length + 1 else state.length,
                    inputPos = 0,
                    showing = true,
                )
            }
        }
    }

    override fun getOutcome(state: SimonState): GameOutcome =
        state.loser?.let { l -> GameOutcome.Winner(state.players.first { it.id != l }.id) }
            ?: GameOutcome.Incomplete
}
