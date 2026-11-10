package com.devanshu.miniverse.games.memory

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/** 4x4 pair-matching. A match keeps your turn (roadmap game catalogue). */
data class MemState(
    val players: List<GamePlayer>,
    val symbols: List<String>,           // 16 cards, shuffled from the seed
    val matchedBy: List<String?>,        // playerId that matched each card
    val faceUp: List<Int>,               // 0, 1 or 2 currently revealed indices
    val scores: Map<String, Int>,
    val turnIndex: Int,
)

sealed interface MemAction {
    data class Flip(val card: Int) : MemAction
    data object Settle : MemAction // resolve the two face-up cards
}

object MemoryGame : MiniGame<MemState, MemAction> {
    override val definition = GameDefinition(
        id = "memory", name = "Memory Match", emoji = "🧠", category = "Puzzle",
        minPlayers = 2, maxPlayers = 2,
        description = "Flip pairs. A match earns a point and another turn.",
    )

    private val pool = listOf("🍎", "🚀", "🐸", "🌙", "🎲", "🎧", "🌵", "🍩")

    override fun createInitialState(players: List<GamePlayer>, seed: Long): MemState {
        val cards = (pool + pool).shuffled(Random(seed)) // randomness only from the seed
        return MemState(players, cards, List(16) { null }, emptyList(), players.associate { it.id to 0 }, 0)
    }

    override fun validateAction(state: MemState, action: MemAction, playerId: String): ActionValidation {
        if (state.players[state.turnIndex].id != playerId) return ActionValidation.Invalid("Not your turn")
        return when (action) {
            is MemAction.Flip -> when {
                state.faceUp.size >= 2 -> ActionValidation.Invalid("Settle first")
                action.card !in 0..15 -> ActionValidation.Invalid("Bad card")
                state.matchedBy[action.card] != null -> ActionValidation.Invalid("Already matched")
                action.card in state.faceUp -> ActionValidation.Invalid("Already face up")
                else -> ActionValidation.Valid
            }
            MemAction.Settle ->
                if (state.faceUp.size == 2) ActionValidation.Valid
                else ActionValidation.Invalid("Need two cards up")
        }
    }

    override fun reduce(state: MemState, action: MemAction, playerId: String): MemState = when (action) {
        is MemAction.Flip -> state.copy(faceUp = state.faceUp + action.card)
        MemAction.Settle -> {
            val (a, b) = state.faceUp
            if (state.symbols[a] == state.symbols[b]) {
                state.copy(
                    matchedBy = state.matchedBy.toMutableList().also { it[a] = playerId; it[b] = playerId },
                    faceUp = emptyList(),
                    scores = state.scores + (playerId to (state.scores[playerId] ?: 0) + 1),
                    // match found: same player goes again
                )
            } else {
                state.copy(faceUp = emptyList(), turnIndex = (state.turnIndex + 1) % state.players.size)
            }
        }
    }

    override fun getOutcome(state: MemState): GameOutcome {
        if (state.matchedBy.any { it == null }) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value } ?: return GameOutcome.Draw
        val tied = state.scores.count { it.value == best.value }
        return if (tied > 1) GameOutcome.Draw else GameOutcome.Winner(best.key)
    }
}
