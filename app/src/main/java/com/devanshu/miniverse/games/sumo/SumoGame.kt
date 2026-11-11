package com.devanshu.miniverse.games.sumo

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/** Mini Sumo tap battle: live push physics on screen; engine counts rounds (best of 3). */
data class SumoState(
    val players: List<GamePlayer>,
    val roundWins: Map<String, Int>,
)

data class SumoRoundWon(val winnerId: String)

const val SUMO_ROUNDS_TO_WIN = 2

object SumoGame : MiniGame<SumoState, SumoRoundWon> {
    override val definition = GameDefinition(
        id = "sumo", name = "Mini Sumo", emoji = "🤼", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Tap fast, push your rival out of the ring!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        SumoState(players, players.associate { it.id to 0 })

    override fun validateAction(state: SumoState, action: SumoRoundWon, playerId: String): ActionValidation =
        when {
            getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
            state.players.none { it.id == action.winnerId } -> ActionValidation.Invalid("Unknown player")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: SumoState, action: SumoRoundWon, playerId: String): SumoState =
        state.copy(roundWins = state.roundWins + (action.winnerId to (state.roundWins[action.winnerId] ?: 0) + 1))

    override fun getOutcome(state: SumoState): GameOutcome {
        state.roundWins.forEach { (id, w) -> if (w >= SUMO_ROUNDS_TO_WIN) return GameOutcome.Winner(id) }
        return GameOutcome.Incomplete
    }
}
