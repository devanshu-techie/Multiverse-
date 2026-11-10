package com.devanshu.miniverse.engine

/** Game catalogue metadata. */
data class GameDefinition(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val description: String,
)

data class GamePlayer(val id: String, val name: String, val seat: Int)

sealed interface ActionValidation {
    data object Valid : ActionValidation
    data class Invalid(val reason: String) : ActionValidation
}

sealed interface GameOutcome {
    data object Incomplete : GameOutcome
    data class Winner(val playerId: String) : GameOutcome
    data object Draw : GameOutcome
}

/**
 * The core contract from the MiniVerse roadmap (section 6).
 * Rules only - no Android, no database, no UI. reduce() must be pure and
 * deterministic: all randomness comes from the seed passed at creation.
 */
interface MiniGame<S : Any, A : Any> {
    val definition: GameDefinition
    fun createInitialState(players: List<GamePlayer>, seed: Long): S
    fun validateAction(state: S, action: A, playerId: String): ActionValidation
    fun reduce(state: S, action: A, playerId: String): S
    fun getOutcome(state: S): GameOutcome
}
