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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS elo_matches")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS elo_matches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                winnerId INTEGER NOT NULL,
                playedAt INTEGER NOT NULL
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS elo_match_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                matchId INTEGER NOT NULL,
                playerId INTEGER NOT NULL,
                eloBefore REAL NOT NULL,
                eloAfter REAL NOT NULL,
                eloChange REAL NOT NULL
            )
        """)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS index_throws_playerId ON throws (playerId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_throws_legId ON throws (legId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE games ADD COLUMN isRanked INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes — version bump to align with devices already at v7.
    }
}

@Database(
    entities = [Player::class, Game::class, GamePlayer::class,
                Leg::class, Throw::class, AppSettings::class,
                EloMatch::class, EloMatchEntry::class],
    version = 7,
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
    abstract fun eloMatchEntryDao(): EloMatchEntryDao
}
