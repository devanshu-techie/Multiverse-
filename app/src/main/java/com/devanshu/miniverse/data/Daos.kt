package com.devanshu.miniverse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity)

    @Query("SELECT * FROM player LIMIT 1")
    fun observeFirst(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player LIMIT 1")
    suspend fun first(): PlayerEntity?

    @Query("UPDATE player SET name = :name, avatar = :avatar WHERE playerId = :playerId")
    suspend fun updateProfile(playerId: String, name: String, avatar: String)
}

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(wallet: WalletEntity)

    @Query("UPDATE coin_wallet SET balance = balance + :delta, updatedAt = :now WHERE playerId = :playerId")
    suspend fun add(playerId: String, delta: Long, now: Long)

    @Query("SELECT * FROM coin_wallet WHERE playerId = :playerId")
    fun observe(playerId: String): Flow<WalletEntity?>

    @Query("SELECT balance FROM coin_wallet WHERE playerId = :playerId")
    suspend fun balance(playerId: String): Long?
}

@Dao
interface CoinDao {
    /** Returns -1 when the claimId already exists (duplicate reward attempt). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(txn: CoinTransactionEntity): Long

    @Query("SELECT * FROM coin_transaction WHERE playerId = :playerId ORDER BY createdAt DESC LIMIT 100")
    fun observeRecent(playerId: String): Flow<List<CoinTransactionEntity>>

    @Query("SELECT COUNT(*) FROM coin_transaction WHERE claimId = :claimId")
    fun claimCount(claimId: String): Flow<Int>

    @Query("SELECT claimId FROM coin_transaction WHERE playerId = :playerId AND claimId LIKE :prefix || '%'")
    fun claimsWithPrefix(playerId: String, prefix: String): Flow<List<String>>
}

/** Per-game totals for the Stats page. */
data class GameStat(val gameId: String, val plays: Int, val wins: Int)

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<MatchPlayerEntity>)

    @Query("UPDATE `match` SET status = :status, endedAt = :endedAt WHERE matchId = :matchId")
    suspend fun setStatus(matchId: String, status: String, endedAt: Long?)

    @Query("SELECT * FROM `match` ORDER BY startedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM match_player WHERE matchId = :matchId")
    suspend fun playersOf(matchId: String): List<MatchPlayerEntity>

    @Query("SELECT * FROM `match` WHERE matchId = :matchId")
    suspend fun byId(matchId: String): MatchEntity?

    @Query("SELECT COUNT(*) FROM match_player WHERE playerId = :playerId AND result = :result")
    fun countResults(playerId: String, result: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM match_player WHERE playerId = :playerId AND result != 'PENDING'")
    fun countPlayed(playerId: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT gameId) FROM `match` WHERE status IN ('COMPLETED','DRAW')")
    fun countDistinctGamesFinished(): Flow<Int>

    @Query(
        "SELECT m.gameId AS gameId, COUNT(*) AS plays, " +
            "SUM(CASE WHEN mp.result = 'WIN' THEN 1 ELSE 0 END) AS wins " +
            "FROM `match` m JOIN match_player mp ON mp.matchId = m.matchId " +
            "WHERE mp.playerId = :playerId AND mp.result != 'PENDING' " +
            "GROUP BY m.gameId ORDER BY plays DESC"
    )
    fun gameStats(playerId: String): Flow<List<GameStat>>
}
