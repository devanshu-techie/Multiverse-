package com.devanshu.miniverse.games.dotsboxes

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame

/** Dots & Boxes on a 4x4 box grid (5x5 dots). Completing a box = point + extra turn. */
const val DB_SIZE = 4 // boxes per side

data class DbState(
    val players: List<GamePlayer>,
    val hEdges: List<Boolean>,       // (SIZE+1) * SIZE   : hIdx(r,c) = r*SIZE + c,  r in 0..SIZE
    val vEdges: List<Boolean>,       // SIZE * (SIZE+1)   : vIdx(r,c) = r*(SIZE+1)+c, c in 0..SIZE
    val boxOwner: List<String?>,     // SIZE*SIZE
    val scores: Map<String, Int>,
    val turnIndex: Int,
)

data class DrawEdge(val horizontal: Boolean, val index: Int)

fun hIdx(r: Int, c: Int) = r * DB_SIZE + c
fun vIdx(r: Int, c: Int) = r * (DB_SIZE + 1) + c

object DotsBoxesGame : MiniGame<DbState, DrawEdge> {
    override val definition = GameDefinition(
        id = "dotsboxes", name = "Dots & Boxes", emoji = "🔷", category = "Board",
        minPlayers = 2, maxPlayers = 2,
        description = "Draw lines, close boxes, steal extra turns!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long) = DbState(
        players,
        List((DB_SIZE + 1) * DB_SIZE) { false },
        List(DB_SIZE * (DB_SIZE + 1)) { false },
        List(DB_SIZE * DB_SIZE) { null },
        players.associate { it.id to 0 },
        0,
    )

    override fun validateAction(state: DbState, action: DrawEdge, playerId: String): ActionValidation {
        if (getOutcome(state) != GameOutcome.Incomplete) return ActionValidation.Invalid("Game over")
        if (state.players[state.turnIndex].id != playerId) return ActionValidation.Invalid("Not your turn")
        val list = if (action.horizontal) state.hEdges else state.vEdges
        if (action.index !in list.indices) return ActionValidation.Invalid("Bad edge")
        if (list[action.index]) return ActionValidation.Invalid("Edge taken")
        return ActionValidation.Valid
    }

    private fun boxComplete(h: List<Boolean>, v: List<Boolean>, r: Int, c: Int) =
        h[hIdx(r, c)] && h[hIdx(r + 1, c)] && v[vIdx(r, c)] && v[vIdx(r, c + 1)]

    override fun reduce(state: DbState, action: DrawEdge, playerId: String): DbState {
        val h = state.hEdges.toMutableList()
        val v = state.vEdges.toMutableList()
        if (action.horizontal) h[action.index] = true else v[action.index] = true
        val owner = state.boxOwner.toMutableList()
        var gained = 0
        for (r in 0 until DB_SIZE) for (c in 0 until DB_SIZE) {
            val i = r * DB_SIZE + c
            if (owner[i] == null && boxComplete(h, v, r, c)) { owner[i] = playerId; gained++ }
        }
        return state.copy(
            hEdges = h, vEdges = v, boxOwner = owner,
            scores = if (gained > 0) state.scores + (playerId to (state.scores[playerId] ?: 0) + gained) else state.scores,
            turnIndex = if (gained > 0) state.turnIndex else (state.turnIndex + 1) % state.players.size,
        )
    }

    override fun getOutcome(state: DbState): GameOutcome {
        if (state.boxOwner.any { it == null }) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
