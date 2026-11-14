package com.devanshu.miniverse.games.arcade

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/*
 * Generic score-challenge core (roadmap family "score challenge"). The live
 * action runs in the game's own Compose playfield; the engine receives only
 * the final run score, so rewards and history flow through the exact same
 * pipeline as board games. Solo mode: beat the target to win.
 */

data class ScoreState(
    val gameId: String,
    val players: List<GamePlayer>,
    val seed: Long,
    val scores: Map<String, Int>,       // only submitted runs appear here
    val runnerIndex: Int,               // whose run is live
)

data class SubmitScore(val score: Int)

class ScoreChallengeGame(
    override val definition: GameDefinition,
    private val soloTarget: Int,        // 1-player mode: reach this to win
) : MiniGame<ScoreState, SubmitScore> {

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        ScoreState(definition.id, players, seed, emptyMap(), 0)

    override fun validateAction(state: ScoreState, action: SubmitScore, playerId: String): ActionValidation =
        when {
            state.runnerIndex >= state.players.size -> ActionValidation.Invalid("All runs done")
            state.players[state.runnerIndex].id != playerId -> ActionValidation.Invalid("Not your run")
            action.score < 0 -> ActionValidation.Invalid("Bad score")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: ScoreState, action: SubmitScore, playerId: String): ScoreState = state.copy(
        scores = state.scores + (playerId to action.score),
        runnerIndex = state.runnerIndex + 1,
    )

    override fun getOutcome(state: ScoreState): GameOutcome {
        if (state.runnerIndex < state.players.size) return GameOutcome.Incomplete
        if (state.players.size == 1) {
            val p = state.players.first()
            return if ((state.scores[p.id] ?: 0) >= soloTarget) GameOutcome.Winner(p.id)
            else GameOutcome.Lost(p.id)
        }
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }

    val target: Int get() = soloTarget
}

val PinballGame = ScoreChallengeGame(
    GameDefinition("pinball", "Pinball Master", "🪩", "Arcade", 1, 2,
        "Bounce the ball off bumpers. 3 balls, big score!"),
    soloTarget = 60,
)
val RunnerGame = ScoreChallengeGame(
    GameDefinition("runner", "Crazy Runner", "🏃", "Arcade", 1, 2,
        "Jump the cactuses, run as far as you can!"),
    soloTarget = 300,
)
val ArBalloonGame = ScoreChallengeGame(
    GameDefinition("arballoon", "AR Balloon Hunt", "📸", "Motion", 1, 2,
        "Balloons float in YOUR room! Pop them through the camera."),
    soloTarget = 15,
)
val TiltGame = ScoreChallengeGame(
    GameDefinition("tilt", "Tilt & Catch", "📱", "Motion", 1, 2,
        "Tilt your phone to roll the ball and grab the stars!"),
    soloTarget = 12,
)
val BalloonGame = ScoreChallengeGame(
    GameDefinition("balloon", "Balloon Pop", "🎈", "Arcade", 1, 2,
        "Pop balloons before they float away. 30 seconds!"),
    soloTarget = 15,
)
