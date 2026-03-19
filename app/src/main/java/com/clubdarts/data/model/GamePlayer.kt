package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "game_players",
    primaryKeys = ["gameId", "playerId"],
    foreignKeys = [
        ForeignKey(Game::class,   ["id"], ["gameId"],   onDelete = ForeignKey.CASCADE),
        ForeignKey(Player::class, ["id"], ["playerId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class GamePlayer(val gameId: Long, val playerId: Long, val throwOrder: Int)
