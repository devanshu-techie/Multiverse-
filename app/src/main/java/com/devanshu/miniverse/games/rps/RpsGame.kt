package com.devanshu.miniverse.games.rps

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Rock Paper Scissors, first to 2 round wins.
 *
 * Two ways to play, chosen on the first screen:
 *  - LUCKY: nobody picks. Everyone shouts "shake!" and the game deals both
 *    hands at random (from the match seed) - pure luck, great for little kids.
 *  - PICK: each player picks in secret (pass the phone). Against the robot,
 *    only you pick and the robot's hand is dealt at random.
 */
enum class Hand { ROCK, PAPER, SCISSORS }
enum class RpsMode { LUCKY, PICK }

data class RpsState(
    val players: List<GamePlayer>,
    val seed: Long,
    val mode: RpsMode?,                  // null until chosen
    val round: Int,                      // 0-based
    val pickerIndex: Int,                // whose secret pick we wait for (PICK mode)
    val picks: Map<String, Hand>,        // this round's hands
    val roundWins: Map<String, Int>,
    val lastResult: String?,
    val revealed: Pair<Hand, Hand>?,     // hands of player 0 and player 1 when a round is shown
)

sealed interface RpsAction {
    data class ChooseMode(val mode: RpsMode) : RpsAction
    data object Shake : RpsAction                    // LUCKY: deal both hands
    data class Pick(val hand: Hand) : RpsAction      // PICK: secret pick
    data object NextRound : RpsAction
}

const val RPS_WINS_NEEDED = 2

object RpsGame : MiniGame<RpsState, RpsAction> {
    override val definition = GameDefinition(
        id = "rps", name = "Rock Paper Scissors", emoji = "✊", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Lucky shake or secret picks - first to 2 wins!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        RpsState(players, seed, null, 0, 0, emptyMap(), players.associate { it.id to 0 }, null, null)

    override fun validateAction(state: RpsState, action: RpsAction, playerId: String): ActionValidation {
        if (getOutcome(state) != GameOutcome.Incomplete) return ActionValidation.Invalid("Game over")
        return when (action) {
            is RpsAction.ChooseMode ->
                if (state.mode == null) ActionValidation.Valid else ActionValidation.Invalid("Mode already set")
            RpsAction.Shake ->
                if (state.mode == RpsMode.LUCKY && state.picks.isEmpty()) ActionValidation.Valid
                else ActionValidation.Invalid("Cannot shake now")
            is RpsAction.Pick -> when {
                state.mode != RpsMode.PICK -> ActionValidation.Invalid("Not in pick mode")
                state.picks.size >= 2 -> ActionValidation.Invalid("Round done")
                state.players[state.pickerIndex].id != playerId -> ActionValidation.Invalid("Not your pick")
                else -> ActionValidation.Valid
            }
            RpsAction.NextRound ->
                if (state.picks.size == 2) ActionValidation.Valid
                else ActionValidation.Invalid("Round not finished")
        }
    }

    private fun beats(a: Hand, b: Hand) =
        (a == Hand.ROCK && b == Hand.SCISSORS) ||
            (a == Hand.PAPER && b == Hand.ROCK) ||
            (a == Hand.SCISSORS && b == Hand.PAPER)

    private fun resolve(state: RpsState, picks: Map<String, Hand>): RpsState {
        val (p0, p1) = state.players
        val h0 = picks.getValue(p0.id); val h1 = picks.getValue(p1.id)
        val winner = when {
            beats(h0, h1) -> p0
            beats(h1, h0) -> p1
            else -> null
        }
        return state.copy(
            picks = picks,
            roundWins = winner?.let { state.roundWins + (it.id to (state.roundWins[it.id] ?: 0) + 1) }
                ?: state.roundWins,
            lastResult = winner?.let { "${it.name} wins the round!" } ?: "It's a tie!",
            revealed = h0 to h1,
        )
    }

    override fun reduce(state: RpsState, action: RpsAction, playerId: String): RpsState = when (action) {
        is RpsAction.ChooseMode -> state.copy(mode = action.mode)
        RpsAction.Shake -> {
            val rng = Random(state.seed + state.round * 31L)
            val hands = Hand.entries
            resolve(
                state,
                mapOf(
                    state.players[0].id to hands[rng.nextInt(3)],
                    state.players[1].id to hands[rng.nextInt(3)],
                ),
            )
        }
        is RpsAction.Pick -> {
            val picks = state.picks + (playerId to action.hand)
            if (picks.size < 2) state.copy(picks = picks, pickerIndex = (state.pickerIndex + 1) % 2)
            else resolve(state, picks)
        }
        RpsAction.NextRound -> state.copy(
            round = state.round + 1, picks = emptyMap(), pickerIndex = 0, lastResult = null, revealed = null,
        )
    }

    override fun getOutcome(state: RpsState): GameOutcome {
        state.roundWins.forEach { (id, w) -> if (w >= RPS_WINS_NEEDED) return GameOutcome.Winner(id) }
        return GameOutcome.Incomplete
    }
}
