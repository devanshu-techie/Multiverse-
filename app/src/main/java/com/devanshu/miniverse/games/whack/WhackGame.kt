package com.devanshu.miniverse.games.whack

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Whack-a-Mole: each player gets 15 moles on a 3x3 field; most hits wins.
 * Mole positions come from the seed; the UI drives timing with NextMole,
 * and a tap on the mole cell scores.
 */
data class WhackState(
    val players: List<GamePlayer>,
    val moleCells: List<Int>,        // 30 pre-rolled cells (15 per player)
    val moleIndex: Int,              // which mole is active overall (0..29)
    val moleVisible: Boolean,
    val hitThisMole: Boolean,
    val scores: Map<String, Int>,
)

sealed interface WhackAction {
    data object ShowMole : WhackAction
    data class Tap(val cell: Int) : WhackAction
    data object HideMole : WhackAction   // mole escaped or was hit; advance
}

const val MOLES_PER_PLAYER = 15

object WhackGame : MiniGame<WhackState, WhackAction> {
    override val definition = GameDefinition(
        id = "whack", name = "Whack-a-Mole", emoji = "🔨", category = "Arcade",
        minPlayers = 2, maxPlayers = 2,
        description = "Bonk the mole before it hides. 15 moles each!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): WhackState {
        val rng = Random(seed)
        return WhackState(
            players = players,
            moleCells = List(MOLES_PER_PLAYER * 2) { rng.nextInt(9) },
            moleIndex = 0, moleVisible = false, hitThisMole = false,
            scores = players.associate { it.id to 0 },
        )
    }

    fun currentPlayer(state: WhackState): GamePlayer =
        state.players[if (state.moleIndex < MOLES_PER_PLAYER) 0 else 1]

    override fun validateAction(state: WhackState, action: WhackAction, playerId: String): ActionValidation {
        if (state.moleIndex >= state.moleCells.size) return ActionValidation.Invalid("Game over")
        return when (action) {
            WhackAction.ShowMole ->
                if (!state.moleVisible) ActionValidation.Valid else ActionValidation.Invalid("Already up")
            is WhackAction.Tap -> when {
                !state.moleVisible -> ActionValidation.Invalid("No mole")
                currentPlayer(state).id != playerId -> ActionValidation.Invalid("Not your run")
                else -> ActionValidation.Valid
            }
            WhackAction.HideMole ->
                if (state.moleVisible) ActionValidation.Valid else ActionValidation.Invalid("Nothing up")
        }
    }

    override fun reduce(state: WhackState, action: WhackAction, playerId: String): WhackState = when (action) {
        WhackAction.ShowMole -> state.copy(moleVisible = true, hitThisMole = false)
        is WhackAction.Tap ->
            if (!state.hitThisMole && action.cell == state.moleCells[state.moleIndex]) {
                state.copy(
                    hitThisMole = true,
                    scores = state.scores + (playerId to (state.scores[playerId] ?: 0) + 1),
                )
            } else state
        WhackAction.HideMole -> state.copy(moleVisible = false, moleIndex = state.moleIndex + 1)
    }

    override fun getOutcome(state: WhackState): GameOutcome {
        if (state.moleIndex < state.moleCells.size) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
