package com.devanshu.miniverse.games.maze

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Maze Escape race: both players solve the SAME seeded maze one after the
 * other; fewer moves wins. Maze is generated with DFS backtracking.
 * Cell walls: bit 0 = N, 1 = E, 2 = S, 3 = W (wall present).
 */
const val MZ = 9 // maze side

enum class MoveDir(val dx: Int, val dy: Int, val bit: Int, val opposite: Int) {
    N(0, -1, 0, 2), E(1, 0, 1, 3), S(0, 1, 2, 0), W(-1, 0, 3, 1)
}

data class MazeState(
    val players: List<GamePlayer>,
    val walls: List<Int>,            // MZ*MZ cells, 4-bit wall mask
    val runnerIndex: Int,            // 0 then 1
    val pos: Int,                    // current runner's cell
    val moves: Map<String, Int>,     // move counts
    val finished: Set<String>,
)

data class MazeMove(val dir: MoveDir)

object MazeGame : MiniGame<MazeState, MazeMove> {
    override val definition = GameDefinition(
        id = "maze", name = "Maze Escape", emoji = "🌀", category = "Puzzle",
        minPlayers = 2, maxPlayers = 2,
        description = "Same maze, two racers. Escape in fewer moves!",
    )

    private fun generate(seed: Long): List<Int> {
        val walls = IntArray(MZ * MZ) { 0b1111 }
        val visited = BooleanArray(MZ * MZ)
        val rng = Random(seed)
        val stack = ArrayDeque<Int>()
        visited[0] = true; stack.addLast(0)
        while (stack.isNotEmpty()) {
            val cell = stack.last()
            val x = cell % MZ; val y = cell / MZ
            val options = MoveDir.entries.filter { d ->
                val nx = x + d.dx; val ny = y + d.dy
                nx in 0 until MZ && ny in 0 until MZ && !visited[ny * MZ + nx]
            }
            if (options.isEmpty()) { stack.removeLast(); continue }
            val d = options[rng.nextInt(options.size)]
            val next = (y + d.dy) * MZ + (x + d.dx)
            walls[cell] = walls[cell] and (1 shl d.bit).inv()
            walls[next] = walls[next] and (1 shl d.opposite).inv()
            visited[next] = true
            stack.addLast(next)
        }
        return walls.toList()
    }

    override fun createInitialState(players: List<GamePlayer>, seed: Long) =
        MazeState(players, generate(seed), 0, 0, players.associate { it.id to 0 }, emptySet())

    fun exitCell() = MZ * MZ - 1

    private fun runner(state: MazeState) = state.players[state.runnerIndex]

    override fun validateAction(state: MazeState, action: MazeMove, playerId: String): ActionValidation {
        if (getOutcome(state) != GameOutcome.Incomplete) return ActionValidation.Invalid("Game over")
        if (runner(state).id != playerId) return ActionValidation.Invalid("Not your run")
        val x = state.pos % MZ; val y = state.pos / MZ
        val nx = x + action.dir.dx; val ny = y + action.dir.dy
        return when {
            nx !in 0 until MZ || ny !in 0 until MZ -> ActionValidation.Invalid("Edge")
            state.walls[state.pos] and (1 shl action.dir.bit) != 0 -> ActionValidation.Invalid("Wall")
            else -> ActionValidation.Valid
        }
    }

    override fun reduce(state: MazeState, action: MazeMove, playerId: String): MazeState {
        val x = state.pos % MZ; val y = state.pos / MZ
        val next = (y + action.dir.dy) * MZ + (x + action.dir.dx)
        val moves = state.moves + (playerId to (state.moves[playerId] ?: 0) + 1)
        return if (next == exitCell()) {
            val finished = state.finished + playerId
            state.copy(
                pos = 0,
                moves = moves,
                finished = finished,
                runnerIndex = (state.runnerIndex + 1).coerceAtMost(state.players.size - 1),
            )
        } else state.copy(pos = next, moves = moves)
    }

    override fun getOutcome(state: MazeState): GameOutcome {
        if (state.finished.size < state.players.size) return GameOutcome.Incomplete
        val best = state.moves.minByOrNull { it.value }!!
        return if (state.moves.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
