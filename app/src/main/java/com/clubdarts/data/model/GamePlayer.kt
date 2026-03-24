package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_players",
    primaryKeys = ["gameId", "playerId"],
    foreignKeys = [
        ForeignKey(Game::class,   ["id"], ["gameId"],   onDelete = ForeignKey.CASCADE),
        ForeignKey(Player::class, ["id"], ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("playerId")]
)
data class GamePlayer(
    val gameId: Long,
    val playerId: Long,
    val throwOrder: Int,
    val teamIndex: Int = -1   // -1=single mode, 0=Team A (Red), 1=Team B (Blue)
)
