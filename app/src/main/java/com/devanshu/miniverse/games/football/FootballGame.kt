package com.devanshu.miniverse.games.football

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/**
 * Finger Football penalty shoot-out for two: one player SHOOTS, the other is
 * the GOALKEEPER. The shooter secretly picks one of 6 zones of the goal,
 * then the keeper picks where to dive. Same zone = saved, otherwise GOAL.
 * After each kick the roles swap. Five kicks each.
 */
enum class KickPhase { SHOOT, DIVE, SHOWN }

data class FootballState(
    val players: List<GamePlayer>,
    val kickIndex: Int,                  // 0..9; kick i is taken by player i % 2
    val phase: KickPhase,
    val pendingShot: Int?,               // hidden until the keeper has dived
    val scores: Map<String, Int>,
    val lastShot: Pair<Int, Int>?,       // (shot zone, dive zone) of the kick being shown
    val history: List<Boolean>,          // goal / no goal for every finished kick
)

sealed interface FootballAction
data class Shoot(val zone: Int) : FootballAction      // zone 0..5 (3 columns x 2 rows)
data class Dive(val zone: Int) : FootballAction
data object NextKick : FootballAction

const val SHOTS_EACH = 5

object FootballGame : MiniGame<FootballState, FootballAction> {
    override val definition = GameDefinition(
        id = "football", name = "Finger Football", emoji = "⚽", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Penalty shoot-out: one shoots, one is the goalkeeper!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        FootballState(players, 0, KickPhase.SHOOT, null, players.associate { it.id to 0 }, null, emptyList())

    fun shooter(s: FootballState): GamePlayer = s.players[s.kickIndex % 2]
    fun keeper(s: FootballState): GamePlayer = s.players[(s.kickIndex + 1) % 2]

    override fun validateAction(state: FootballState, action: FootballAction, playerId: String): ActionValidation {
        if (state.kickIndex >= SHOTS_EACH * 2) return ActionValidation.Invalid("Game over")
        return when (action) {
            is Shoot -> when {
                state.phase != KickPhase.SHOOT -> ActionValidation.Invalid("Not time to shoot")
                shooter(state).id != playerId -> ActionValidation.Invalid("Not your kick")
                action.zone !in 0..5 -> ActionValidation.Invalid("Bad zone")
                else -> ActionValidation.Valid
            }
            is Dive -> when {
                state.phase != KickPhase.DIVE -> ActionValidation.Invalid("Not time to dive")
                keeper(state).id != playerId -> ActionValidation.Invalid("You are not the keeper")
                action.zone !in 0..5 -> ActionValidation.Invalid("Bad zone")
                else -> ActionValidation.Valid
            }
            NextKick ->
                if (state.phase == KickPhase.SHOWN) ActionValidation.Valid
                else ActionValidation.Invalid("Kick not finished")
        }
    }

    override fun reduce(state: FootballState, action: FootballAction, playerId: String): FootballState = when (action) {
        is Shoot -> state.copy(phase = KickPhase.DIVE, pendingShot = action.zone)
        is Dive -> {
            val shot = state.pendingShot ?: 0
            val goal = shot != action.zone
            val shooterId = shooter(state).id
            state.copy(
                phase = KickPhase.SHOWN,
                pendingShot = null,
                lastShot = shot to action.zone,
                scores = if (goal) state.scores + (shooterId to (state.scores[shooterId] ?: 0) + 1) else state.scores,
                history = state.history + goal,
            )
        }
        NextKick -> state.copy(kickIndex = state.kickIndex + 1, phase = KickPhase.SHOOT, lastShot = null)
    }

    override fun getOutcome(state: FootballState): GameOutcome {
        if (state.kickIndex < SHOTS_EACH * 2) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
