package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "legs",
    foreignKeys = [ForeignKey(Game::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")]
)
data class Leg(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val legNumber: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val winnerId: Long? = null
)
