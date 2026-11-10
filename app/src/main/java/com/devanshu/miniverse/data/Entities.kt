package com.devanshu.miniverse.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val playerId: String,
    val name: String,
    val avatar: String,
    val createdAt: Long,
)

@Entity(tableName = "coin_wallet")
data class WalletEntity(
    @PrimaryKey val playerId: String,
    val balance: Long,
    val updatedAt: Long,
)

/**
 * Append-only coin ledger. claimId = "matchId:playerId:reason" and is UNIQUE,
 * so one reward can never be paid twice (roadmap 7.3), while one match can
 * still pay several different reasons.
 */
@Entity(
    tableName = "coin_transaction",
    indices = [Index(value = ["claimId"], unique = true), Index(value = ["playerId", "createdAt"])],
)
data class CoinTransactionEntity(
    @PrimaryKey val transactionId: String,
    val claimId: String,
    val playerId: String,
    val matchId: String?,
    val amount: Long,
    val reason: String,
    val createdAt: Long,
)

@Entity(tableName = "match", indices = [Index("gameId"), Index("status")])
data class MatchEntity(
    @PrimaryKey val matchId: String,
    val gameId: String,
    val mode: String,
    val status: String,
    val seed: Long,
    val startedAt: Long,
    val endedAt: Long?,
)

@Entity(tableName = "match_player", primaryKeys = ["matchId", "playerId"])
data class MatchPlayerEntity(
    val matchId: String,
    val playerId: String,
    val playerName: String,
    val seat: Int,
    val score: Int,
    val result: String, // WIN, LOSS, DRAW, ABANDONED
    val isLocalGuest: Boolean,
)
