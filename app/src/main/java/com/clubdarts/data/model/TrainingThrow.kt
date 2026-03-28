package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single dart throw recorded during a training session.
 * Used for precision/dispersion analytics (TARGET_FIELD and AROUND_THE_CLOCK modes).
 * Coordinates are stored in mm from board centre (same convention as the throws table).
 */
@Entity(
    tableName = "training_throws",
    foreignKeys = [ForeignKey(TrainingSession::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")]
)
data class TrainingThrow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val throwIndex: Int,
    /** Field notation: "Miss", "Bull", "Bullseye", "S{n}", "D{n}", "T{n}" */
    val targetField: String,
    val actualField: String,
    val isHit: Boolean,
    /** Geometric centre of the target field in mm from board centre (null if not applicable). */
    val targetX: Double? = null,
    val targetY: Double? = null,
    /** Actual dart position in mm from board centre (null when entered via numpad). */
    val actualX: Double? = null,
    val actualY: Double? = null
)
