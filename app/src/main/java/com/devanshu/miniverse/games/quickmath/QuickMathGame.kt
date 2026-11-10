package com.devanshu.miniverse.games.quickmath

import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameDefinition
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import kotlin.random.Random

/**
 * Quick Math duel: 5 questions each, turns alternate. All questions and
 * option orders come from the seed, so the match is replayable.
 */
data class MathQuestion(val text: String, val options: List<Int>, val answerIndex: Int)

data class MathState(
    val players: List<GamePlayer>,
    val questions: List<MathQuestion>,   // 10 questions, q[i] belongs to player i % 2
    val questionIndex: Int,
    val scores: Map<String, Int>,
    val lastCorrect: Boolean?,
)

data class Answer(val optionIndex: Int)

const val QUESTIONS_PER_PLAYER = 5

object QuickMathGame : MiniGame<MathState, Answer> {
    override val definition = GameDefinition(
        id = "quickmath", name = "Quick Math", emoji = "➕", category = "Puzzle",
        minPlayers = 2, maxPlayers = 2,
        description = "5 sums each. Fast fingers, faster brains!",
    )

    override fun createInitialState(players: List<GamePlayer>, seed: Long): MathState {
        val rng = Random(seed)
        val questions = List(QUESTIONS_PER_PLAYER * 2) {
            val a = rng.nextInt(2, 20)
            val b = rng.nextInt(2, 20)
            val plus = rng.nextBoolean()
            val answer = if (plus) a + b else a * b
            val text = if (plus) "$a + $b = ?" else "$a × $b = ?"
            val wrong = mutableSetOf<Int>()
            while (wrong.size < 3) {
                val w = answer + rng.nextInt(-10, 11)
                if (w != answer && w > 0) wrong += w
            }
            val options = (wrong + answer).shuffled(rng)
            MathQuestion(text, options, options.indexOf(answer))
        }
        return MathState(players, questions, 0, players.associate { it.id to 0 }, null)
    }

    private fun turnPlayer(state: MathState) = state.players[state.questionIndex % 2]

    override fun validateAction(state: MathState, action: Answer, playerId: String): ActionValidation = when {
        state.questionIndex >= state.questions.size -> ActionValidation.Invalid("Game over")
        turnPlayer(state).id != playerId -> ActionValidation.Invalid("Not your question")
        action.optionIndex !in 0..3 -> ActionValidation.Invalid("Bad option")
        else -> ActionValidation.Valid
    }

    override fun reduce(state: MathState, action: Answer, playerId: String): MathState {
        val q = state.questions[state.questionIndex]
        val correct = action.optionIndex == q.answerIndex
        return state.copy(
            questionIndex = state.questionIndex + 1,
            scores = if (correct) state.scores + (playerId to (state.scores[playerId] ?: 0) + 1) else state.scores,
            lastCorrect = correct,
        )
    }

    override fun getOutcome(state: MathState): GameOutcome {
        if (state.questionIndex < state.questions.size) return GameOutcome.Incomplete
        val best = state.scores.maxByOrNull { it.value }!!
        return if (state.scores.count { it.value == best.value } > 1) GameOutcome.Draw
        else GameOutcome.Winner(best.key)
    }
}
