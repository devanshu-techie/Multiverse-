package com.devanshu.miniverse.games.reaction

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Split-screen reaction duel, best of 5 rounds. The UI schedules the "GO"
 * moment from the per-round delay below (derived from the seed, so a replay
 * of the same seed gives the same delays). Tapping before GO loses the round.
 */
data class ReactionState(
    val players: List<GamePlayer>,
    val roundDelaysMs: List<Long>,       // 5 delays, 1200..4200 ms, from seed
    val round: Int,                      // 0-based
    val phase: RoundPhase,
    val roundWins: Map<String, Int>,
)

enum class RoundPhase { WAITING, ARMED, SHOWN, RESOLVED }

sealed interface ReactionAction {
    data object Arm : ReactionAction            // round starts, screen red
    data object Show : ReactionAction           // delay elapsed, screen green
    data class Tap(val tooEarly: Boolean) : ReactionAction
    data object NextRound : ReactionAction
}

const val TOTAL_ROUNDS = 5

object ReactionGame : MiniGame<ReactionState, ReactionAction> {
    override val definition = GameDefinition(
        id = "reaction", name = "Reaction Battle", emoji = "⚡", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Wait for green, tap first. Best of five.",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): ReactionState {
        val rng = Random(seed)
        return ReactionState(
            players = players,
            roundDelaysMs = List(TOTAL_ROUNDS) { 1200L + rng.nextLong(3000L) },
            round = 0,
            phase = RoundPhase.WAITING,
            roundWins = players.associate { it.id to 0 },
        )
    }

    override fun validateAction(state: ReactionState, action: ReactionAction, playerId: String): ActionValidation =
        when (action) {
            ReactionAction.Arm ->
                if (state.phase == RoundPhase.WAITING) ActionValidation.Valid
                else ActionValidation.Invalid("Round already armed")
            ReactionAction.Show ->
                if (state.phase == RoundPhase.ARMED) ActionValidation.Valid
                else ActionValidation.Invalid("Not armed")
            is ReactionAction.Tap ->
                if (state.phase == RoundPhase.ARMED || state.phase == RoundPhase.SHOWN) ActionValidation.Valid
                else ActionValidation.Invalid("No round running")
            ReactionAction.NextRound ->
                if (state.phase == RoundPhase.RESOLVED && state.round < TOTAL_ROUNDS - 1) ActionValidation.Valid
                else ActionValidation.Invalid("Nothing to advance")
        }

    override fun reduce(state: ReactionState, action: ReactionAction, playerId: String): ReactionState =
        when (action) {
            ReactionAction.Arm -> state.copy(phase = RoundPhase.ARMED)
            ReactionAction.Show -> state.copy(phase = RoundPhase.SHOWN)
            is ReactionAction.Tap -> {
                // Early tap: the OTHER player wins the round. On-time tap: tapper wins.
                val winner = if (action.tooEarly) {
                    state.players.first { it.id != playerId }.id
                } else playerId
                state.copy(
                    phase = RoundPhase.RESOLVED,
                    roundWins = state.roundWins + (winner to (state.roundWins[winner] ?: 0) + 1),
                )
            }
            ReactionAction.NextRound -> state.copy(round = state.round + 1, phase = RoundPhase.WAITING)
        }

    override fun getOutcome(state: ReactionState): GameOutcome {
        val needed = TOTAL_ROUNDS / 2 + 1
        state.roundWins.forEach { (id, wins) -> if (wins >= needed) return GameOutcome.Winner(id) }
        val played = state.roundWins.values.sum()
        return if (played >= TOTAL_ROUNDS && state.phase == RoundPhase.RESOLVED) {
            val best = state.roundWins.maxByOrNull { it.value }!!
            if (state.roundWins.count { it.value == best.value } > 1) GameOutcome.Draw
            else GameOutcome.Winner(best.key)
        } else GameOutcome.Incomplete
    }
}
