package com.clubdarts.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.clubdarts.data.db.dao.*
import com.clubdarts.data.model.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE games ADD COLUMN isTeamGame INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE games ADD COLUMN winningTeamIndex INTEGER")
        database.execSQL("ALTER TABLE game_players ADD COLUMN teamIndex INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE players ADD COLUMN elo REAL NOT NULL DEFAULT 1000.0")
        database.execSQL("ALTER TABLE players ADD COLUMN matchesPlayed INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE players ADD COLUMN wins INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE players ADD COLUMN losses INTEGER NOT NULL DEFAULT 0")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS elo_matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playerAId INTEGER NOT NULL,
                playerBId INTEGER NOT NULL,
                winnerId INTEGER NOT NULL,
                playerAEloBefore REAL NOT NULL,
                playerBEloBefore REAL NOT NULL,
                playerAEloAfter REAL NOT NULL,
                playerBEloAfter REAL NOT NULL,
                eloChange REAL NOT NULL,
                playedAt INTEGER NOT NULL
            )
        """)
    }
}

@Database(
    entities = [Player::class, Game::class, GamePlayer::class,
                Leg::class, Throw::class, AppSettings::class, EloMatch::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun legDao(): LegDao
    abstract fun throwDao(): ThrowDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun eloMatchDao(): EloMatchDao
}
