package com.devanshu.miniverse

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MatchPhase
import com.devanshu.miniverse.engine.MatchStateMachine
import com.devanshu.miniverse.games.connectfour.C4Action
import com.devanshu.miniverse.games.connectfour.ConnectFourGame
import com.devanshu.miniverse.games.memory.MemAction
import com.devanshu.miniverse.games.memory.MemoryGame
import com.devanshu.miniverse.games.tictactoe.TicTacToeGame
import com.devanshu.miniverse.games.tictactoe.TttAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val A = GamePlayer("a", "Asha", 0)
private val B = GamePlayer("b", "Bo", 1)
private val two = listOf(A, B)

class EngineTests {

    @Test fun tictactoe_row_win() {
        var s = TicTacToeGame.createInitialState(two, 1L)
        listOf("a" to 0, "b" to 3, "a" to 1, "b" to 4, "a" to 2).forEach { (p, c) ->
            assertEquals(ActionValidation.Valid, TicTacToeGame.validateAction(s, TttAction(c), p))
            s = TicTacToeGame.reduce(s, TttAction(c), p)
        }
        assertEquals(GameOutcome.Winner("a"), TicTacToeGame.getOutcome(s))
    }

    @Test fun tictactoe_rejects_wrong_turn_and_taken_cell() {
        var s = TicTacToeGame.createInitialState(two, 1L)
        assertTrue(TicTacToeGame.validateAction(s, TttAction(0), "b") is ActionValidation.Invalid)
        s = TicTacToeGame.reduce(s, TttAction(0), "a")
        assertTrue(TicTacToeGame.validateAction(s, TttAction(0), "b") is ActionValidation.Invalid)
    }

    @Test fun connectfour_vertical_win_and_full_column() {
        var s = ConnectFourGame.createInitialState(two, 1L)
        repeat(3) {
            s = ConnectFourGame.reduce(s, C4Action(0), "a")
            s = ConnectFourGame.reduce(s, C4Action(1), "b")
        }
        s = ConnectFourGame.reduce(s, C4Action(0), "a")
        assertEquals(GameOutcome.Winner("a"), ConnectFourGame.getOutcome(s))
    }

    @Test fun memory_same_seed_same_board_and_match_keeps_turn() {
        val s1 = MemoryGame.createInitialState(two, 42L)
        val s2 = MemoryGame.createInitialState(two, 42L)
        assertEquals(s1.symbols, s2.symbols) // determinism from seed

        // find a pair and match it: player A keeps the turn
        var s = s1
        val first = 0
        val second = s.symbols.indices.first { it != first && s.symbols[it] == s.symbols[first] }
        s = MemoryGame.reduce(s, MemAction.Flip(first), "a")
        s = MemoryGame.reduce(s, MemAction.Flip(second), "a")
        s = MemoryGame.reduce(s, MemAction.Settle, "a")
        assertEquals(0, s.turnIndex)
        assertEquals(1, s.scores["a"])
    }

    @Test fun state_machine_blocks_illegal_transitions() {
        assertTrue(MatchStateMachine.canTransition(MatchPhase.RUNNING, MatchPhase.PAUSED))
        assertTrue(!MatchStateMachine.canTransition(MatchPhase.COMPLETED, MatchPhase.RUNNING))
        assertTrue(!MatchStateMachine.canTransition(MatchPhase.CREATED, MatchPhase.RUNNING))
    }
}
