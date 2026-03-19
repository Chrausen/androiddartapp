package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CheckoutRule { STRAIGHT, DOUBLE, TRIPLE }

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startScore: Int,
    val checkoutRule: CheckoutRule,
    val legsToWin: Int,
    val isSolo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val winnerId: Long? = null
)
