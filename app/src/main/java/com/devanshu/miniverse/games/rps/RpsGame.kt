package com.devanshu.miniverse.games.rps

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/**
 * Rock Paper Scissors, best of 3. Pass-the-phone secret picks:
 * the UI shows a "hand over" screen between picks so choices stay hidden.
 * (On two phones this becomes commit-reveal per the roadmap.)
 */
enum class Hand { ROCK, PAPER, SCISSORS }

data class RpsState(
    val players: List<GamePlayer>,
    val round: Int,                      // 0-based, best of 3
    val pickerIndex: Int,                // whose secret pick we wait for
    val picks: Map<String, Hand>,        // this round's picks
    val roundWins: Map<String, Int>,
    val lastResult: String?,             // "Asha wins the round!" / "Tie!"
    val revealed: Pair<Hand, Hand>?,     // last round's hands for the UI
)

sealed interface RpsAction {
    data class Pick(val hand: Hand) : RpsAction
    data object NextRound : RpsAction
}

const val RPS_ROUNDS = 3

object RpsGame : MiniGame<RpsState, RpsAction> {
    override val definition = GameDefinition(
        id = "rps", name = "Rock Paper Scissors", emoji = "✊", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Secret picks, best of three. Paper beats rock!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        RpsState(players, 0, 0, emptyMap(), players.associate { it.id to 0 }, null, null)

    override fun validateAction(state: RpsState, action: RpsAction, playerId: String): ActionValidation =
        when (action) {
            is RpsAction.Pick -> when {
                getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
                state.picks.size >= 2 -> ActionValidation.Invalid("Round done")
                state.players[state.pickerIndex].id != playerId -> ActionValidation.Invalid("Not your pick")
                else -> ActionValidation.Valid
            }
            RpsAction.NextRound ->
                if (state.picks.size == 2) ActionValidation.Valid
                else ActionValidation.Invalid("Round not finished")
        }

    private fun beats(a: Hand, b: Hand) =
        (a == Hand.ROCK && b == Hand.SCISSORS) ||
        (a == Hand.PAPER && b == Hand.ROCK) ||
        (a == Hand.SCISSORS && b == Hand.PAPER)

    override fun reduce(state: RpsState, action: RpsAction, playerId: String): RpsState = when (action) {
        is RpsAction.Pick -> {
            val picks = state.picks + (playerId to action.hand)
            if (picks.size < 2) {
                state.copy(picks = picks, pickerIndex = (state.pickerIndex + 1) % 2)
            } else {
                val (p0, p1) = state.players
                val h0 = picks.getValue(p0.id); val h1 = picks.getValue(p1.id)
                val winner = when {
                    beats(h0, h1) -> p0
                    beats(h1, h0) -> p1
                    else -> null
                }
                state.copy(
                    picks = picks,
                    roundWins = winner?.let { state.roundWins + (it.id to (state.roundWins[it.id] ?: 0) + 1) }
                        ?: state.roundWins,
                    lastResult = winner?.let { "${it.name} wins the round!" } ?: "Tie!",
                    revealed = h0 to h1,
                )
            }
        }
        RpsAction.NextRound -> state.copy(
            round = state.round + 1, picks = emptyMap(), pickerIndex = 0, lastResult = null,
        )
    }

    override fun getOutcome(state: RpsState): GameOutcome {
        val needed = RPS_ROUNDS / 2 + 1
        state.roundWins.forEach { (id, w) -> if (w >= needed) return GameOutcome.Winner(id) }
        if (state.round >= RPS_ROUNDS && state.picks.size == 2) {
            val best = state.roundWins.maxByOrNull { it.value }!!
            return if (state.roundWins.count { it.value == best.value } > 1) GameOutcome.Draw
            else GameOutcome.Winner(best.key)
        }
        return GameOutcome.Incomplete
    }
}
