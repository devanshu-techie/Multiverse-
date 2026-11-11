package com.devanshu.miniverse.games.airhockey

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/**
 * Air Hockey: the puck physics run live on the Canvas (same phone, two
 * fingers); the ENGINE only referees the score, exactly like the roadmap's
 * split of simulation vs result. First to 3 goals wins.
 */
data class HockeyState(
    val players: List<GamePlayer>,
    val goals: Map<String, Int>,
)

data class GoalScored(val scorerId: String)

const val GOALS_TO_WIN = 3

object AirHockeyGame : MiniGame<HockeyState, GoalScored> {
    override val definition = GameDefinition(
        id = "airhockey", name = "Air Hockey", emoji = "🏒", category = "Arcade",
        minPlayers = 2, maxPlayers = 2,
        description = "Two fingers, one puck. First to 3 goals!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        HockeyState(players, players.associate { it.id to 0 })

    override fun validateAction(state: HockeyState, action: GoalScored, playerId: String): ActionValidation =
        when {
            getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
            state.players.none { it.id == action.scorerId } -> ActionValidation.Invalid("Unknown scorer")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: HockeyState, action: GoalScored, playerId: String): HockeyState =
        state.copy(goals = state.goals + (action.scorerId to (state.goals[action.scorerId] ?: 0) + 1))

    override fun getOutcome(state: HockeyState): GameOutcome {
        state.goals.forEach { (id, g) -> if (g >= GOALS_TO_WIN) return GameOutcome.Winner(id) }
        return GameOutcome.Incomplete
    }
}
