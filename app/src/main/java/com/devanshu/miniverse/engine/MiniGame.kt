package com.devanshu.miniverse.engine

import kotlinx.serialization.Serializable

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

@Serializable
data class GamePlayer(val id: String, val name: String, val seat: Int)

/** Id used for the built-in robot opponent. */
const val BOT_ID = "bot"

sealed interface ActionValidation {
    data object Valid : ActionValidation
    data class Invalid(val reason: String) : ActionValidation
}

sealed interface GameOutcome {
    data object Incomplete : GameOutcome
    data class Winner(val playerId: String) : GameOutcome
    data object Draw : GameOutcome
    /** Single-player challenge failed (counts as a LOSS result). */
    data class Lost(val playerId: String) : GameOutcome
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

    /**
     * Optional save/restore. Games that return a string here can be continued
     * after the app is closed; others simply restart.
     */
    fun encodeState(state: S): String? = null
    fun decodeState(json: String): S? = null
}
