package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "elo_matches")
data class EloMatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val winnerId: Long,
    val playedAt: Long = System.currentTimeMillis()
)
