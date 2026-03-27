package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "throws",
    foreignKeys = [
        ForeignKey(Leg::class,    ["id"], ["legId"],    onDelete = ForeignKey.CASCADE),
        ForeignKey(Player::class, ["id"], ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("playerId"),
        Index("legId")
    ]
)
data class Throw(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val legId: Long,
    val playerId: Long,
    val visitNumber: Int,
    val dart1Score: Int = 0, val dart1Mult: Int = 0,
    val dart2Score: Int = 0, val dart2Mult: Int = 0,
    val dart3Score: Int = 0, val dart3Mult: Int = 0,
    val dartsUsed: Int,
    val visitTotal: Int,
    val isBust: Boolean = false,
    val isCheckoutAttempt: Boolean = false,
    // Board-input coordinates in mm from board centre (null when thrown via numpad)
    val dart1X: Double? = null, val dart1Y: Double? = null,
    val dart2X: Double? = null, val dart2Y: Double? = null,
    val dart3X: Double? = null, val dart3Y: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
