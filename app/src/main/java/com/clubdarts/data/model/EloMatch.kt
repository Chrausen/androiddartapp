package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "elo_matches")
data class EloMatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerAId: Long,
    val playerBId: Long,
    val winnerId: Long,
    val playerAEloBefore: Double,
    val playerBEloBefore: Double,
    val playerAEloAfter: Double,
    val playerBEloAfter: Double,
    val eloChange: Double,
    val playedAt: Long = System.currentTimeMillis()
)
