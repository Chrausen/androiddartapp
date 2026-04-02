package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrainingMode { TARGET_FIELD, AROUND_THE_CLOCK, SCORING_ROUNDS }

enum class TrainingDifficulty(val targetAvg: Int) {
    BEGINNER(20), INTERMEDIATE(40), PRO(60)
}

@Entity(
    tableName = "training_sessions",
    foreignKeys = [ForeignKey(Player::class, ["id"], ["playerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playerId")]
)
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val mode: String,          // TrainingMode name
    val difficulty: String,    // TrainingDifficulty name
    /** Total darts used (TARGET_FIELD / AROUND_THE_CLOCK) or average×10 (SCORING_ROUNDS). */
    val result: Int,
    val completedCount: Int,   // fields completed (TARGET/ATC) or rounds completed (SCORING)
    val completedAt: Long = System.currentTimeMillis()
)
