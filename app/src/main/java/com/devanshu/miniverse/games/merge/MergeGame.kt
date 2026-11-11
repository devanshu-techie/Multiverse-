package com.devanshu.miniverse.games.merge

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Number Merge (2048-style) on a 4x4 grid. Two players take full games in
 * turn is overkill; instead this is a single-run challenge: the profile
 * player plays; reaching the 256 tile wins the match, running out of moves
 * before that is a draw (small reward). New tiles come from (seed, moveCount).
 */
enum class Dir { UP, DOWN, LEFT, RIGHT }

data class MergeState(
    val players: List<GamePlayer>,
    val seed: Long,
    val grid: List<Int>,     // 16 cells, 0 = empty
    val score: Int,
    val moveCount: Int,
)

data class Swipe(val dir: Dir)

const val MERGE_TARGET = 256

object MergeGame : MiniGame<MergeState, Swipe> {
    override val definition = GameDefinition(
        id = "merge", name = "Number Merge", emoji = "🔢", category = "Puzzle",
        minPlayers = 1, maxPlayers = 1,
        description = "Swipe to merge numbers. Reach 256 to win!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): MergeState {
        var s = MergeState(players, seed, List(16) { 0 }, 0, 0)
        s = spawn(s); s = spawn(s)
        return s
    }

    private fun spawn(s: MergeState): MergeState {
        val empty = s.grid.indices.filter { s.grid[it] == 0 }
        if (empty.isEmpty()) return s
        val rng = Random(s.seed + s.moveCount * 31 + s.grid.sum())
        val cell = empty[rng.nextInt(empty.size)]
        val value = if (rng.nextInt(10) == 0) 4 else 2
        return s.copy(grid = s.grid.toMutableList().also { it[cell] = value })
    }

    /** Merge one row toward its start; returns (newRow, gained). */
    private fun mergeLine(line: List<Int>): Pair<List<Int>, Int> {
        val vals = line.filter { it != 0 }.toMutableList()
        var gained = 0
        var i = 0
        val out = mutableListOf<Int>()
        while (i < vals.size) {
            if (i + 1 < vals.size && vals[i] == vals[i + 1]) {
                out.add(vals[i] * 2); gained += vals[i] * 2; i += 2
            } else { out.add(vals[i]); i += 1 }
        }
        while (out.size < 4) out.add(0)
        return out to gained
    }

    private fun lines(dir: Dir): List<List<Int>> = when (dir) {
        Dir.LEFT -> (0..3).map { r -> (0..3).map { c -> r * 4 + c } }
        Dir.RIGHT -> (0..3).map { r -> (3 downTo 0).map { c -> r * 4 + c } }
        Dir.UP -> (0..3).map { c -> (0..3).map { r -> r * 4 + c } }
        Dir.DOWN -> (0..3).map { c -> (3 downTo 0).map { r -> r * 4 + c } }
    }

    private fun apply(s: MergeState, dir: Dir): MergeState? {
        val grid = s.grid.toMutableList()
        var gained = 0
        var moved = false
        for (idxs in lines(dir)) {
            val (merged, g) = mergeLine(idxs.map { s.grid[it] })
            gained += g
            idxs.forEachIndexed { i, cell ->
                if (grid[cell] != merged[i]) moved = true
                grid[cell] = merged[i]
            }
        }
        if (!moved) return null
        return s.copy(grid = grid, score = s.score + gained, moveCount = s.moveCount + 1)
    }

    fun canMove(s: MergeState): Boolean = Dir.entries.any { apply(s, it) != null }

    override fun validateAction(state: MergeState, action: Swipe, playerId: String): ActionValidation = when {
        getOutcome(state) != GameOutcome.Incomplete -> ActionValidation.Invalid("Game over")
        state.players.first().id != playerId -> ActionValidation.Invalid("Owner plays this one")
        apply(state, action.dir) == null -> ActionValidation.Invalid("Nothing moves that way")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: MergeState, action: Swipe, playerId: String): MergeState {
        val next = apply(state, action.dir) ?: return state
        return spawn(next)
    }

    override fun getOutcome(state: MergeState): GameOutcome = when {
        state.grid.any { it >= MERGE_TARGET } -> GameOutcome.Winner(state.players.first().id)
        !canMove(state) -> GameOutcome.Lost(state.players.first().id)
        else -> GameOutcome.Incomplete
    }
}
