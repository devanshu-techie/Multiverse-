package com.devanshu.miniverse.games.tank

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tank Battle: artillery duel. Tanks sit on flat ground at seeded distance;
 * each turn the player picks angle + power, the engine computes the
 * parabolic landing point (with seeded wind). First to 3 hits wins.
 * Deterministic physics = closed-form; the UI draws the trajectory.
 */
data class TankState(
    val players: List<GamePlayer>,
    val seed: Long,
    val distance: Float,             // metres between tanks (40..70)
    val wind: Float,                 // m/s^2 sideways, per round
    val turnIndex: Int,
    val round: Int,
    val hits: Map<String, Int>,
    val lastLanding: Float?,         // landing x relative to shooter
    val lastHit: Boolean?,
)

data class Fire(val angleDeg: Float, val power: Float)  // angle 10..80, power 10..40

const val HIT_RADIUS = 3.5f
const val GRAVITY = 9.8f
const val HITS_TO_WIN = 3

object TankGame : MiniGame<TankState, Fire> {
    override val definition = GameDefinition(
        id = "tank", name = "Tank Battle", emoji = "🚜", category = "Arcade",
        minPlayers = 2, maxPlayers = 2,
        description = "Aim, set power, fire over the hill. 3 hits wins!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): TankState {
        val rng = Random(seed)
        return TankState(
            players, seed,
            distance = 40f + rng.nextInt(31),
            wind = (rng.nextInt(9) - 4) * 0.5f,
            turnIndex = 0, round = 0,
            hits = players.associate { it.id to 0 },
            lastLanding = null, lastHit = null,
        )
    }

    /** Landing x for a shot from x=0 toward +x. */
    fun landingX(angleDeg: Float, power: Float, wind: Float): Float {
        val a = Math.toRadians(angleDeg.toDouble())
        val vx = (power * cos(a)).toFloat()
        val vy = (power * sin(a)).toFloat()
        val t = 2f * vy / GRAVITY
        return vx * t + 0.5f * wind * t * t
    }

    override fun validateAction(state: TankState, action: Fire, playerId: String): ActionValidation = when {
        getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
        state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
        action.angleDeg !in 10f..80f -> ActionValidation.Invalid("Angle 10-80")
        action.power !in 10f..40f -> ActionValidation.Invalid("Power 10-40")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: TankState, action: Fire, playerId: String): TankState {
        val land = landingX(action.angleDeg, action.power, state.wind)
        val hit = kotlin.math.abs(land - state.distance) <= HIT_RADIUS
        val rng = Random(state.seed + state.round + 100)
        return state.copy(
            turnIndex = (state.turnIndex + 1) % state.players.size,
            round = state.round + 1,
            hits = if (hit) state.hits + (playerId to (state.hits[playerId] ?: 0) + 1) else state.hits,
            lastLanding = land,
            lastHit = hit,
            wind = (rng.nextInt(9) - 4) * 0.5f,   // new wind each shot, seeded
        )
    }

    override fun getOutcome(state: TankState): GameOutcome {
        state.hits.forEach { (id, h) -> if (h >= HITS_TO_WIN) return GameOutcome.Winner(id) }
        return GameOutcome.Incomplete
    }
}
