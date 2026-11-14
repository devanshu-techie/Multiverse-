package com.devanshu.miniverse.feature.gameplay

import com.devanshu.miniverse.engine.GamePlayer

/**
 * A series of rounds (best of 3 or 5) of the same game with the same players.
 * Kept in memory for the current play session; every round is still saved as
 * its own match with its own coins.
 */
data class Series(
    val gameId: String,
    val players: List<GamePlayer>,
    val ownerId: String,
    val bestOf: Int,
    val wins: Map<String, Int> = emptyMap(),
    val roundsPlayed: Int = 0,
    val countedMatches: Set<String> = emptySet(),
) {
    val needed: Int get() = bestOf / 2 + 1
    val leaderId: String? get() = wins.maxByOrNull { it.value }?.takeIf { e -> wins.count { it.value == e.value } == 1 }?.key
    val decided: Boolean get() = wins.values.any { it >= needed } || roundsPlayed >= bestOf
    /** The round about to be played (1-based). */
    val nextRound: Int get() = roundsPlayed + 1
    /** The next round can end the series: everything is on the line. */
    val nextIsFinal: Boolean get() = !decided && nextRound == bestOf
}

object SeriesStore {
    @Volatile var current: Series? = null

    fun start(gameId: String, players: List<GamePlayer>, ownerId: String, bestOf: Int) {
        current = if (bestOf > 1) Series(gameId, players, ownerId, bestOf) else null
    }

    /** Count a finished match once. winnerId is null for a draw. */
    fun record(matchId: String, gameId: String, winnerId: String?): Series? {
        val s = current ?: return null
        if (s.gameId != gameId) { current = null; return null }
        if (matchId in s.countedMatches) return s
        val updated = s.copy(
            wins = if (winnerId != null) s.wins + (winnerId to (s.wins[winnerId] ?: 0) + 1) else s.wins,
            roundsPlayed = s.roundsPlayed + 1,
            countedMatches = s.countedMatches + matchId,
        )
        current = updated
        return updated
    }

    fun clear() { current = null }
}
