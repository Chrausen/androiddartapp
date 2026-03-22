package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "elo_match_entries")
data class EloMatchEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val playerId: Long,
    val eloBefore: Double,
    val eloAfter: Double,
    val eloChange: Double   // signed: positive = gain, negative = loss
)
