package com.devanshu.miniverse.data

import androidx.room.withTransaction
import com.devanshu.miniverse.engine.GameOutcome
import com.devanshu.miniverse.engine.GamePlayer
import java.util.UUID

data class RewardClaim(
    val matchId: String,
    val playerId: String,
    val reason: String,
    val amount: Long,
) {
    val claimId: String get() = "$matchId:$playerId:$reason"
}

/** Reward policy D2 from the roadmap: win 20, draw 10, loss 5. Starter grant 100. */
object RewardPolicy {
    const val STARTER = 100L
    const val WIN = 20L
    const val DRAW = 10L
    const val LOSS = 5L
}

class GameRepository(private val db: AppDatabase) {

    val player = db.playerDao().observeFirst()
    suspend fun playerExists(): Boolean = db.playerDao().first() != null
    suspend fun currentPlayer(): PlayerEntity? = db.playerDao().first()
    suspend fun updateProfile(playerId: String, name: String, avatar: String) =
        db.playerDao().updateProfile(playerId, name, avatar)

    fun wins(playerId: String) = db.matchDao().countResults(playerId, "WIN")
    fun played(playerId: String) = db.matchDao().countPlayed(playerId)
    fun distinctGamesFinished() = db.matchDao().countDistinctGamesFinished()
    fun wallet(playerId: String) = db.walletDao().observe(playerId)
    fun ledger(playerId: String) = db.coinDao().observeRecent(playerId)
    val recentMatches = db.matchDao().observeRecent()

    /** Onboarding: player + wallet + protected 100 MC starter claim, one transaction. */
    suspend fun createPlayer(name: String, avatar: String): PlayerEntity {
        val now = System.currentTimeMillis()
        val player = PlayerEntity(UUID.randomUUID().toString(), name.trim(), avatar, now)
        db.withTransaction {
            db.playerDao().insert(player)
            db.walletDao().insertIfAbsent(WalletEntity(player.playerId, 0, now))
            award(listOf(RewardClaim("onboarding", player.playerId, "starter", RewardPolicy.STARTER)))
        }
        return player
    }

    suspend fun createMatch(gameId: String, players: List<GamePlayer>, ownerId: String): Pair<String, Long> {
        val matchId = UUID.randomUUID().toString()
        val seed = System.nanoTime()
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.matchDao().insertMatch(
                MatchEntity(matchId, gameId, "LOCAL", "RUNNING", seed, now, null)
            )
            db.matchDao().insertPlayers(players.map {
                MatchPlayerEntity(
                    matchId = matchId, playerId = it.id, playerName = it.name, seat = it.seat,
                    score = 0, result = "PENDING", isLocalGuest = it.id != ownerId,
                )
            })
        }
        return matchId to seed
    }

    /**
     * Save first, animate second (roadmap 11.2): match result, per-player rows
     * and the coin claims commit in ONE transaction. Claims are idempotent -
     * re-running this for the same match pays nothing twice.
     * Only the profile owner has a wallet (decision D5: guest seats earn no coins).
     */
    suspend fun finishMatch(
        matchId: String,
        gameId: String,
        outcome: GameOutcome,
        players: List<GamePlayer>,
        scores: Map<String, Int>,
        ownerId: String,
    ) {
        val now = System.currentTimeMillis()
        val claims = mutableListOf<RewardClaim>()
        val rows = players.map { p ->
            val result = when (outcome) {
                is GameOutcome.Winner -> if (outcome.playerId == p.id) "WIN" else "LOSS"
                GameOutcome.Draw -> "DRAW"
                is GameOutcome.Lost -> "LOSS"
                GameOutcome.Incomplete -> "ABANDONED"
            }
            if (p.id == ownerId && outcome !is GameOutcome.Incomplete) {
                val amount = when (result) {
                    "WIN" -> RewardPolicy.WIN
                    "DRAW" -> RewardPolicy.DRAW
                    else -> RewardPolicy.LOSS
                }
                claims += RewardClaim(matchId, p.id, result.lowercase(), amount)
            }
            MatchPlayerEntity(matchId, p.id, p.name, p.seat, scores[p.id] ?: 0, result, p.id != ownerId)
        }
        val status = when (outcome) {
            is GameOutcome.Winner -> "COMPLETED"
            GameOutcome.Draw -> "DRAW"
            is GameOutcome.Lost -> "COMPLETED"
            GameOutcome.Incomplete -> "ABANDONED"
        }
        db.withTransaction {
            db.matchDao().setStatus(matchId, status, now)
            db.matchDao().insertPlayers(rows)
            award(claims)
        }
    }

    suspend fun abandonMatch(matchId: String) {
        db.matchDao().setStatus(matchId, "ABANDONED", System.currentTimeMillis())
    }

    suspend fun matchWithPlayers(matchId: String): Pair<MatchEntity?, List<MatchPlayerEntity>> =
        db.matchDao().byId(matchId) to db.matchDao().playersOf(matchId)

    /** Must be called inside a transaction. */
    private suspend fun award(claims: List<RewardClaim>) {
        val now = System.currentTimeMillis()
        for (c in claims) {
            if (c.amount <= 0) continue
            val inserted = db.coinDao().insertIgnore(
                CoinTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    claimId = c.claimId, playerId = c.playerId, matchId = c.matchId,
                    amount = c.amount, reason = c.reason, createdAt = now,
                )
            )
            if (inserted == -1L) continue // already paid
            db.walletDao().add(c.playerId, c.amount, now)
        }
    }
}
