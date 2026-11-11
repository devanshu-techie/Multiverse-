package com.devanshu.miniverse.games.ludo

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Mini Ludo: a compact race version. One token per player on a 24-cell ring.
 * Each player starts at an offset (seat * 6) and must travel 24 steps, then
 * land exactly on the finish. Landing on an opponent (not on a safe cell)
 * sends them back to start. Dice come from (seed, rollCount) - deterministic.
 */
const val RING = 24
const val TRACK = 26 // 24 steps around + 2 home steps; progress 0 = at start

data class LudoState(
    val players: List<GamePlayer>,
    val seed: Long,
    val progress: Map<String, Int>,  // 0..TRACK, TRACK = home
    val turnIndex: Int,
    val rollCount: Int,
    val lastRoll: Int?,
    val lastEvent: String?,          // "capture", "safe", "exact", null
)

data object LudoRoll

object LudoGame : MiniGame<LudoState, LudoRoll> {
    override val definition = GameDefinition(
        id = "ludo", name = "Mini Ludo", emoji = "🎲", category = "Board",
        minPlayers = 2, maxPlayers = 4,
        description = "Race your token around the board. Catch rivals!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        LudoState(players, seed, players.associate { it.id to 0 }, 0, 0, null, null)

    fun startCell(seat: Int) = (seat * (RING / 4)) % RING

    /** Ring cell for a player's progress, or null when home-stretch/finished. */
    fun ringCell(state: LudoState, playerId: String): Int? {
        val p = state.progress[playerId] ?: 0
        if (p >= RING) return null
        val seat = state.players.first { it.id == playerId }.seat
        return (startCell(seat) + p) % RING
    }

    fun isSafeCell(cell: Int) = cell % (RING / 4) == 0  // the 4 start cells are safe

    override fun validateAction(state: LudoState, action: LudoRoll, playerId: String): ActionValidation =
        when {
            getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
            state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
            else -> ActionValidation.Valid
        }

    override fun reduce(state: LudoState, action: LudoRoll, playerId: String): LudoState {
        val roll = 1 + Random(state.seed + state.rollCount).nextInt(6)
        val from = state.progress[playerId] ?: 0
        var target = from + roll
        var event: String? = null
        if (target > TRACK) { target = from; event = "exact" }   // must land exactly home

        var progress = state.progress + (playerId to target)
        if (target < RING && event == null) {
            val myCell = run {
                val seat = state.players.first { it.id == playerId }.seat
                (startCell(seat) + target) % RING
            }
            if (!isSafeCell(myCell)) {
                state.players.filter { it.id != playerId }.forEach { other ->
                    val oc = ringCell(state.copy(progress = progress), other.id)
                    if (oc == myCell) {
                        progress = progress + (other.id to 0)
                        event = "capture"
                    }
                }
            } else if (state.players.any { it.id != playerId && ringCell(state, it.id) == myCell }) {
                event = "safe"
            }
        }
        val extraTurn = roll == 6 && target != from
        return state.copy(
            progress = progress,
            turnIndex = if (target >= TRACK || extraTurn) state.turnIndex
            else (state.turnIndex + 1) % state.players.size,
            rollCount = state.rollCount + 1,
            lastRoll = roll,
            lastEvent = event,
        )
    }

    override fun getOutcome(state: LudoState): GameOutcome {
        val winner = state.progress.entries.firstOrNull { it.value >= TRACK }
        return if (winner != null) GameOutcome.Winner(winner.key) else GameOutcome.Incomplete
    }
}
