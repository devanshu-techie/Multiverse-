package com.devanshu.miniverse.games.football

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Finger Football penalty duel: 5 flicks each. The goal has 6 zones
 * (left/centre/right x low/high). The keeper dives to a seeded zone each
 * shot; same zone = save. UI turns a flick gesture into the zone.
 */
data class FootballState(
    val players: List<GamePlayer>,
    val seed: Long,
    val shotIndex: Int,              // 0..9, shot i belongs to player i % 2
    val scores: Map<String, Int>,
    val lastShot: Pair<Int, Int>?,   // shot zone, keeper zone
)

data class Shoot(val zone: Int)      // 0..5

const val SHOTS_EACH = 5

object FootballGame : MiniGame<FootballState, Shoot> {
    override val definition = GameDefinition(
        id = "football", name = "Finger Football", emoji = "⚽", category = "Party",
        minPlayers = 2, maxPlayers = 2,
        description = "Flick penalties past the keeper. 5 shots each!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        FootballState(players, seed, 0, players.associate { it.id to 0 }, null)

    private fun shooter(s: FootballState) = s.players[s.shotIndex % 2]

    fun keeperZone(s: FootballState): Int = Random(s.seed + s.shotIndex * 7).nextInt(6)

    override fun validateAction(state: FootballState, action: Shoot, playerId: String): ActionValidation = when {
        state.shotIndex >= SHOTS_EACH * 2 -> ActionValidation.Invalid("Game over")
        shooter(state).id != playerId -> ActionValidation.Invalid("Not your shot")
        action.zone !in 0..5 -> ActionValidation.Invalid("Bad zone")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: FootballState, action: Shoot, playerId: String): FootballState {
        val keeper = keeperZone(state)
        val goal = action.zone != keeper
        return state.copy(
            shotIndex = state.shotIndex + 1,
            scores = if (goal) state.scores + (playerId to (state.scores[playerId] ?: 0) + 1) else state.scores,
            lastShot = action.zone to keeper,
        )
    }

    override fun getOutcome(state: FootballState): GameOutcome {
        if (state.shotIndex < SHOTS_EACH * 2) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
