package com.devanshu.miniverse.games.connectfour

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val COLS = 7
const val ROWS = 6

@Serializable
data class C4State(
    val players: List<GamePlayer>,
    val grid: List<String?>, // ROWS*COLS, row 0 = top; index = row * COLS + col
    val turnIndex: Int,
)

data class C4Action(val column: Int)

object ConnectFourGame : MiniGame<C4State, C4Action> {
    override val definition = GameDefinition(
        id = "connectfour", name = "Connect Four", emoji = "🔴", category = "Board",
        minPlayers = 2, maxPlayers = 2,
        description = "Drop discs, connect four in any direction.",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        C4State(players, List(ROWS * COLS) { null }, 0)

    private fun landingRow(grid: List<String?>, col: Int): Int? {
        for (row in ROWS - 1 downTo 0) if (grid[row * COLS + col] == null) return row
        return null
    }

    override fun validateAction(state: C4State, action: C4Action, playerId: String): ActionValidation = when {
        state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
        action.column !in 0 until COLS -> ActionValidation.Invalid("Bad column")
        landingRow(state.grid, action.column) == null -> ActionValidation.Invalid("Column full")
        getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: C4State, action: C4Action, playerId: String): C4State {
        val row = landingRow(state.grid, action.column) ?: return state
        return state.copy(
            grid = state.grid.toMutableList().also { it[row * COLS + action.column] = playerId },
            turnIndex = (state.turnIndex + 1) % state.players.size,
        )
    }

    override fun getOutcome(state: C4State): GameOutcome {
        val g = state.grid
        fun at(r: Int, c: Int): String? =
            if (r in 0 until ROWS && c in 0 until COLS) g[r * COLS + c] else null
        val dirs = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val owner = at(r, c) ?: continue
            for ((dr, dc) in dirs) {
                if ((1..3).all { at(r + dr * it, c + dc * it) == owner }) {
                    return GameOutcome.Winner(owner)
                }
            }
        }
        return if (g.all { it != null }) GameOutcome.Draw else GameOutcome.Incomplete
    }

    override fun encodeState(state: C4State): String? = Json.encodeToString(C4State.serializer(), state)
    override fun decodeState(json: String): C4State? =
        runCatching { Json.decodeFromString(C4State.serializer(), json) }.getOrNull()
}
