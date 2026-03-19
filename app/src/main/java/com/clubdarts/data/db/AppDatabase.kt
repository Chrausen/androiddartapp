package com.clubdarts.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clubdarts.data.db.dao.*
import com.clubdarts.data.model.*

@Database(
    entities = [Player::class, Game::class, GamePlayer::class,
                Leg::class, Throw::class, AppSettings::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun legDao(): LegDao
    abstract fun throwDao(): ThrowDao
    abstract fun appSettingsDao(): AppSettingsDao
}
