package com.devanshu.miniverse.feature.gameplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.data.SavedMatch
import com.devanshu.miniverse.data.SettingsRepository
import com.devanshu.miniverse.engine.ActionValidation
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import com.devanshu.miniverse.engine.MiniGame
import com.devanshu.miniverse.games.memory.MemState
import com.devanshu.miniverse.games.reaction.ReactionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GameplaySession(
    val matchId: String,
    val gameId: String,
    val players: List<GamePlayer>,
    val ownerId: String,
    val state: Any,
    val outcome: GameOutcome,
)

/**
 * Local GameCoordinator (roadmap 5.1), pass-and-play role. All inputs enter
 * through submit(); the engine decides everything; the result is saved in one
 * transaction before the UI navigates to the result screen.
 */
class GameplayViewModel(
    private val repository: GameRepository,
    private val matchId: String,
    private val store: SettingsRepository,
) : ViewModel() {

    private var game: MiniGame<Any, Any>? = null
    private val _session = MutableStateFlow<GameplaySession?>(null)
    val session: StateFlow<GameplaySession?> = _session.asStateFlow()
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()
    private var resultSaved = false
    private val _resumable = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val (match, rows) = repository.matchWithPlayers(matchId)
            if (match == null || rows.isEmpty()) return@launch
            @Suppress("UNCHECKED_CAST")
            val g = com.devanshu.miniverse.engine.GameRegistry.find(match.gameId) as? MiniGame<Any, Any> ?: return@launch
            game = g
            val players = rows.sortedBy { it.seat }.map { GamePlayer(it.playerId, it.playerName, it.seat) }
            val owner = rows.firstOrNull { !it.isLocalGuest }?.playerId ?: players.first().id
            // Continue a saved match if this is the one that was saved.
            val saved = store.savedMatch.first()
            val restored = if (saved != null && saved.matchId == matchId) g.decodeState(saved.stateJson) else null
            val state = restored ?: g.createInitialState(players, match.seed)
            _resumable.value = g.encodeState(state) != null
            _session.value = GameplaySession(matchId, match.gameId, players, owner, state, GameOutcome.Incomplete)
        }
    }

    fun submit(action: Any, playerId: String) {
        val g = game ?: return
        val s = _session.value ?: return
        if (s.outcome != GameOutcome.Incomplete) return
        if (g.validateAction(s.state, action, playerId) != ActionValidation.Valid) return
        val next = g.reduce(s.state, action, playerId)
        val outcome = g.getOutcome(next)
        _session.value = s.copy(state = next, outcome = outcome)
        if (outcome != GameOutcome.Incomplete) {
            finish(outcome, next, s)
        } else {
            // Checkpoint after every accepted action (only games that support it).
            g.encodeState(next)?.let { json ->
                viewModelScope.launch { store.saveMatch(SavedMatch(s.matchId, s.gameId, json)) }
            }
        }
    }

    private fun finish(outcome: GameOutcome, finalState: Any, s: GameplaySession) {
        if (resultSaved) return
        resultSaved = true
        viewModelScope.launch {
            repository.finishMatch(
                matchId = s.matchId,
                gameId = s.gameId,
                outcome = outcome,
                players = s.players,
                scores = scoresOf(finalState, s.players),
                ownerId = s.ownerId,
            )
            store.clearSavedMatch()
            _finished.value = true // save first, navigate/animate second
        }
    }

    /** Give up: the match is recorded as abandoned and cannot be continued. */
    fun quit() {
        if (resultSaved) return
        resultSaved = true
        viewModelScope.launch {
            store.clearSavedMatch()
            repository.abandonMatch(matchId)
        }
    }

    /** True when this game can be left now and continued later. */
    val resumable: StateFlow<Boolean> get() = _resumable

    private fun scoresOf(state: Any, players: List<GamePlayer>): Map<String, Int> = when (state) {
        is MemState -> state.scores
        is ReactionState -> state.roundWins
        is com.devanshu.miniverse.games.rps.RpsState -> state.roundWins
        is com.devanshu.miniverse.games.quickmath.MathState -> state.scores
        is com.devanshu.miniverse.games.whack.WhackState -> state.scores
        is com.devanshu.miniverse.games.snakes.SnlState -> state.positions.mapValues { it.value }
        is com.devanshu.miniverse.games.dotsboxes.DbState -> state.scores
        is com.devanshu.miniverse.games.ludo.LudoState ->
            players.associate { it.id to com.devanshu.miniverse.games.ludo.LudoGame.homeCount(state, it.id) }
        is com.devanshu.miniverse.games.tank.TankState -> state.hits
        is com.devanshu.miniverse.games.merge.MergeState -> mapOf(players.first().id to state.score)
        is com.devanshu.miniverse.games.scramble.ScrambleState -> mapOf(players.first().id to state.score)
        is com.devanshu.miniverse.games.simon.SimonState -> players.associate { it.id to state.length }
        is com.devanshu.miniverse.games.maze.MazeState -> state.moves
        is com.devanshu.miniverse.games.football.FootballState -> state.scores
        is com.devanshu.miniverse.games.airhockey.HockeyState -> state.goals
        is com.devanshu.miniverse.games.sumo.SumoState -> state.roundWins
        is com.devanshu.miniverse.games.arcade.ScoreState -> state.scores
        else -> players.associate { it.id to 0 }
    }

    class Factory(
        private val repo: GameRepository,
        private val matchId: String,
        private val store: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameplayViewModel(repo, matchId, store) as T
    }
}
