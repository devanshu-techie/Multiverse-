package com.devanshu.miniverse.games.tictactoe

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

data class TttState(
    val players: List<GamePlayer>,
    val cells: List<String?>, // 9 cells holding playerId or null
    val turnIndex: Int,
)

data class TttAction(val cell: Int)

object TicTacToeGame : MiniGame<TttState, TttAction> {
    override val definition = GameDefinition(
        id = "tictactoe", name = "Tic Tac Toe", emoji = "❌", category = "Board",
        minPlayers = 2, maxPlayers = 2,
        description = "Three in a row wins. The classic.",
    )

    private val lines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6),
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        TttState(players, List(9) { null }, 0)

    override fun validateAction(state: TttState, action: TttAction, playerId: String): ActionValidation = when {
        state.players[state.turnIndex].id != playerId -> ActionValidation.Invalid("Not your turn")
        action.cell !in 0..8 -> ActionValidation.Invalid("Bad cell")
        state.cells[action.cell] != null -> ActionValidation.Invalid("Cell taken")
        getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: TttState, action: TttAction, playerId: String): TttState =
        state.copy(
            cells = state.cells.toMutableList().also { it[action.cell] = playerId },
            turnIndex = (state.turnIndex + 1) % state.players.size,
        )

    override fun getOutcome(state: TttState): GameOutcome {
        for (line in lines) {
            val a = state.cells[line[0]]
            if (a != null && line.all { state.cells[it] == a }) return GameOutcome.Winner(a)
        }
        return if (state.cells.all { it != null }) GameOutcome.Draw else GameOutcome.Incomplete
    }
}
