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
}

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(wallet: WalletEntity)

    @Query("UPDATE coin_wallet SET balance = balance + :delta, updatedAt = :now WHERE playerId = :playerId")
    suspend fun add(playerId: String, delta: Long, now: Long)

    @Query("SELECT * FROM coin_wallet WHERE playerId = :playerId")
    fun observe(playerId: String): Flow<WalletEntity?>
}

@Dao
interface CoinDao {
    /** Returns -1 when the claimId already exists (duplicate reward attempt). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(txn: CoinTransactionEntity): Long

    @Query("SELECT * FROM coin_transaction WHERE playerId = :playerId ORDER BY createdAt DESC LIMIT 100")
    fun observeRecent(playerId: String): Flow<List<CoinTransactionEntity>>
}

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
}
