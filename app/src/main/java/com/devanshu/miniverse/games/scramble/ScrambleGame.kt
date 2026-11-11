package com.devanshu.miniverse.games.scramble

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/** Word Scramble, single player: 5 words, guess at least 3 to win. */
private val WORDS = listOf(
    "APPLE", "TIGER", "ROBOT", "CANDY", "MAGIC", "PLANET", "ROCKET", "JUNGLE",
    "BANANA", "PUZZLE", "WIZARD", "DRAGON", "CASTLE", "PIRATE", "GUITAR",
    "MONKEY", "SUNNY", "CLOUD", "RIVER", "HONEY", "ZEBRA", "PANDA", "CIRCUS",
    "GARDEN", "WINTER", "SILVER", "POCKET", "TURTLE", "BUBBLE", "COOKIE",
)

data class ScrambleState(
    val players: List<GamePlayer>,
    val words: List<String>,            // 5 chosen words
    val shuffles: List<String>,         // their scrambled forms
    val round: Int,
    val score: Int,
    val lastRight: Boolean?,
)

sealed interface ScrambleAction {
    data class Guess(val text: String) : ScrambleAction
    data object Skip : ScrambleAction
}

const val SCRAMBLE_ROUNDS = 5
const val SCRAMBLE_TO_WIN = 3

object ScrambleGame : MiniGame<ScrambleState, ScrambleAction> {
    override val definition = GameDefinition(
        id = "scramble", name = "Word Scramble", emoji = "🔤", category = "Puzzle",
        minPlayers = 1, maxPlayers = 1,
        description = "Unscramble 5 words - get 3 right to win!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): ScrambleState {
        val rng = Random(seed)
        val words = WORDS.shuffled(rng).take(SCRAMBLE_ROUNDS)
        val shuffles = words.map { w ->
            var s = w
            var guard = 0
            while (s == w && guard < 10) { s = w.toList().shuffled(rng).joinToString(""); guard++ }
            s
        }
        return ScrambleState(players, words, shuffles, 0, 0, null)
    }

    override fun validateAction(state: ScrambleState, action: ScrambleAction, playerId: String): ActionValidation =
        if (state.round >= SCRAMBLE_ROUNDS) ActionValidation.Invalid("Game over") else ActionValidation.Valid

    override fun reduce(state: ScrambleState, action: ScrambleAction, playerId: String): ScrambleState =
        when (action) {
            is ScrambleAction.Guess -> {
                val right = action.text.equals(state.words[state.round], ignoreCase = true)
                state.copy(
                    round = state.round + 1,
                    score = if (right) state.score + 1 else state.score,
                    lastRight = right,
                )
            }
            ScrambleAction.Skip -> state.copy(round = state.round + 1, lastRight = false)
        }

    override fun getOutcome(state: ScrambleState): GameOutcome = when {
        state.round < SCRAMBLE_ROUNDS -> GameOutcome.Incomplete
        state.score >= SCRAMBLE_TO_WIN -> GameOutcome.Winner(state.players.first().id)
        else -> GameOutcome.Lost(state.players.first().id)
    }
}
