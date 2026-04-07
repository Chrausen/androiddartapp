package com.clubdarts.data.repository

import androidx.room.withTransaction
import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.*
import com.clubdarts.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val legDao: LegDao,
    private val throwDao: ThrowDao,
    private val appSettingsDao: AppSettingsDao,
    private val eloMatchDao: EloMatchDao,
    private val eloMatchEntryDao: EloMatchEntryDao,
    private val trainingSessionDao: TrainingSessionDao,
    private val trainingThrowDao: TrainingThrowDao,
) {
    companion object {
        const val BACKUP_VERSION = 1
        const val SCHEMA_VERSION = 13
    }

    /** Serializes all data to a JSON string. */
    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("players", playerDao.getAllPlayersList().toJsonArray { it.toJson() })
        root.put("games", gameDao.getAllGamesList().toJsonArray { it.toJson() })
        root.put("gamePlayers", gameDao.getAllGamePlayers().toJsonArray { it.toJson() })
        root.put("legs", legDao.getAll().toJsonArray { it.toJson() })
        root.put("throws", throwDao.getAll().toJsonArray { it.toJson() })
        root.put("appSettings", appSettingsDao.getAll().toJsonArray { it.toJson() })
        root.put("eloMatches", eloMatchDao.getAll().toJsonArray { it.toJson() })
        root.put("eloMatchEntries", eloMatchEntryDao.getAll().toJsonArray { it.toJson() })
        root.put("trainingSessions", trainingSessionDao.getAll().toJsonArray { it.toJson() })
        root.put("trainingThrows", trainingThrowDao.getAll().toJsonArray { it.toJson() })

        return root.toString(2)
    }

    /**
     * Parses [json], clears all existing data, and restores from the backup.
     * Runs inside a single Room transaction — all-or-nothing.
     * Throws [BackupException] on invalid format.
     */
    suspend fun importJson(json: String) {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw BackupException("Not a valid JSON file", e)
        }

        if (!root.has("backupVersion")) {
            throw BackupException("Missing backupVersion — not a Club Darts backup file")
        }

        db.withTransaction {
            // Clear in reverse FK order
            trainingThrowDao.deleteAll()
            trainingSessionDao.deleteAll()
            eloMatchEntryDao.deleteAll()
            eloMatchDao.deleteAll()
            throwDao.deleteAll()
            legDao.deleteAll()
            gameDao.deleteAll()    // cascades game_players
            playerDao.deleteAll()
            appSettingsDao.deleteAll()

            // Insert in FK-dependency order
            playerDao.insertAll(root.getJSONArray("players").mapObjects { it.toPlayer() })
            gameDao.insertAll(root.getJSONArray("games").mapObjects { it.toGame() })
            gameDao.insertAllGamePlayers(root.getJSONArray("gamePlayers").mapObjects { it.toGamePlayer() })
            legDao.insertAll(root.getJSONArray("legs").mapObjects { it.toLeg() })
            throwDao.insertAll(root.getJSONArray("throws").mapObjects { it.toThrow() })
            appSettingsDao.insertAll(root.getJSONArray("appSettings").mapObjects { it.toAppSettings() })
            eloMatchDao.insertAll(root.getJSONArray("eloMatches").mapObjects { it.toEloMatch() })
            eloMatchEntryDao.insertAll(root.getJSONArray("eloMatchEntries").mapObjects { it.toEloMatchEntry() })
            trainingSessionDao.insertAll(root.getJSONArray("trainingSessions").mapObjects { it.toTrainingSession() })
            trainingThrowDao.insertAll(root.getJSONArray("trainingThrows").mapObjects { it.toTrainingThrow() })
        }
    }

    /** Returns the raw SharedPreferences section from a backup JSON string, or null if absent. */
    fun parseSharedPreferences(json: String): JSONObject? {
        return try {
            val root = JSONObject(json)
            if (root.has("sharedPreferences")) root.getJSONObject("sharedPreferences") else null
        } catch (_: Exception) {
            null
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private inline fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray {
        val arr = JSONArray()
        forEach { arr.put(transform(it)) }
        return arr
    }

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    // ─── Serializers ──────────────────────────────────────────────────────────

    private fun Player.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("elo", elo)
        put("matchesPlayed", matchesPlayed)
        put("wins", wins)
        put("losses", losses)
        put("createdAt", createdAt)
    }

    private fun Game.toJson() = JSONObject().apply {
        put("id", id)
        put("startScore", startScore)
        put("checkoutRule", checkoutRule.name)
        put("legsToWin", legsToWin)
        put("isSolo", isSolo)
        put("isTeamGame", isTeamGame)
        put("isRanked", isRanked)
        put("createdAt", createdAt)
        putOpt("finishedAt", finishedAt)
        putOpt("winnerId", winnerId)
        putOpt("winningTeamIndex", winningTeamIndex)
    }

    private fun GamePlayer.toJson() = JSONObject().apply {
        put("gameId", gameId)
        put("playerId", playerId)
        put("throwOrder", throwOrder)
        put("teamIndex", teamIndex)
        putOpt("placement", placement)
    }

    private fun Leg.toJson() = JSONObject().apply {
        put("id", id)
        put("gameId", gameId)
        put("legNumber", legNumber)
        put("startedAt", startedAt)
        putOpt("finishedAt", finishedAt)
        putOpt("winnerId", winnerId)
    }

    private fun Throw.toJson() = JSONObject().apply {
        put("id", id)
        put("legId", legId)
        put("playerId", playerId)
        put("visitNumber", visitNumber)
        put("dart1Score", dart1Score); put("dart1Mult", dart1Mult)
        put("dart2Score", dart2Score); put("dart2Mult", dart2Mult)
        put("dart3Score", dart3Score); put("dart3Mult", dart3Mult)
        put("dartsUsed", dartsUsed)
        put("visitTotal", visitTotal)
        put("isBust", isBust)
        put("isCheckoutAttempt", isCheckoutAttempt)
        putOpt("dart1X", dart1X); putOpt("dart1Y", dart1Y)
        putOpt("dart2X", dart2X); putOpt("dart2Y", dart2Y)
        putOpt("dart3X", dart3X); putOpt("dart3Y", dart3Y)
        put("createdAt", createdAt)
    }

    private fun AppSettings.toJson() = JSONObject().apply {
        put("key", key)
        put("value", value)
    }

    private fun EloMatch.toJson() = JSONObject().apply {
        put("id", id)
        put("winnerId", winnerId)
        put("playedAt", playedAt)
        putOpt("gameId", gameId)
    }

    private fun EloMatchEntry.toJson() = JSONObject().apply {
        put("id", id)
        put("matchId", matchId)
        put("playerId", playerId)
        put("eloBefore", eloBefore)
        put("eloAfter", eloAfter)
        put("eloChange", eloChange)
    }

    private fun TrainingSession.toJson() = JSONObject().apply {
        put("id", id)
        put("playerId", playerId)
        put("mode", mode)
        put("difficulty", difficulty)
        put("result", result)
        put("completedCount", completedCount)
        put("completedAt", completedAt)
        put("startedAt", startedAt)
    }

    private fun TrainingThrow.toJson() = JSONObject().apply {
        put("id", id)
        put("sessionId", sessionId)
        put("throwIndex", throwIndex)
        put("targetField", targetField)
        put("actualField", actualField)
        put("isHit", isHit)
        putOpt("targetX", targetX); putOpt("targetY", targetY)
        putOpt("actualX", actualX); putOpt("actualY", actualY)
    }

    // ─── Deserializers ────────────────────────────────────────────────────────

    private fun JSONObject.toPlayer() = Player(
        id = getLong("id"),
        name = getString("name"),
        elo = getDouble("elo"),
        matchesPlayed = getInt("matchesPlayed"),
        wins = getInt("wins"),
        losses = getInt("losses"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toGame() = Game(
        id = getLong("id"),
        startScore = getInt("startScore"),
        checkoutRule = CheckoutRule.valueOf(getString("checkoutRule")),
        legsToWin = getInt("legsToWin"),
        isSolo = getBoolean("isSolo"),
        isTeamGame = getBoolean("isTeamGame"),
        isRanked = getBoolean("isRanked"),
        createdAt = getLong("createdAt"),
        finishedAt = optLongOrNull("finishedAt"),
        winnerId = optLongOrNull("winnerId"),
        winningTeamIndex = if (isNull("winningTeamIndex")) null else getInt("winningTeamIndex")
    )

    private fun JSONObject.toGamePlayer() = GamePlayer(
        gameId = getLong("gameId"),
        playerId = getLong("playerId"),
        throwOrder = getInt("throwOrder"),
        teamIndex = getInt("teamIndex"),
        placement = if (isNull("placement") || !has("placement")) null else getInt("placement")
    )

    private fun JSONObject.toLeg() = Leg(
        id = getLong("id"),
        gameId = getLong("gameId"),
        legNumber = getInt("legNumber"),
        startedAt = getLong("startedAt"),
        finishedAt = optLongOrNull("finishedAt"),
        winnerId = optLongOrNull("winnerId")
    )

    private fun JSONObject.toThrow() = Throw(
        id = getLong("id"),
        legId = getLong("legId"),
        playerId = getLong("playerId"),
        visitNumber = getInt("visitNumber"),
        dart1Score = getInt("dart1Score"), dart1Mult = getInt("dart1Mult"),
        dart2Score = getInt("dart2Score"), dart2Mult = getInt("dart2Mult"),
        dart3Score = getInt("dart3Score"), dart3Mult = getInt("dart3Mult"),
        dartsUsed = getInt("dartsUsed"),
        visitTotal = getInt("visitTotal"),
        isBust = getBoolean("isBust"),
        isCheckoutAttempt = getBoolean("isCheckoutAttempt"),
        dart1X = optDoubleOrNull("dart1X"), dart1Y = optDoubleOrNull("dart1Y"),
        dart2X = optDoubleOrNull("dart2X"), dart2Y = optDoubleOrNull("dart2Y"),
        dart3X = optDoubleOrNull("dart3X"), dart3Y = optDoubleOrNull("dart3Y"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toAppSettings() = AppSettings(
        key = getString("key"),
        value = getString("value")
    )

    private fun JSONObject.toEloMatch() = EloMatch(
        id = getLong("id"),
        winnerId = getLong("winnerId"),
        playedAt = getLong("playedAt"),
        gameId = optLongOrNull("gameId")
    )

    private fun JSONObject.toEloMatchEntry() = EloMatchEntry(
        id = getLong("id"),
        matchId = getLong("matchId"),
        playerId = getLong("playerId"),
        eloBefore = getDouble("eloBefore"),
        eloAfter = getDouble("eloAfter"),
        eloChange = getDouble("eloChange")
    )

    private fun JSONObject.toTrainingSession() = TrainingSession(
        id = getLong("id"),
        playerId = getLong("playerId"),
        mode = getString("mode"),
        difficulty = getString("difficulty"),
        result = getInt("result"),
        completedCount = getInt("completedCount"),
        completedAt = getLong("completedAt"),
        startedAt = getLong("startedAt")
    )

    private fun JSONObject.toTrainingThrow() = TrainingThrow(
        id = getLong("id"),
        sessionId = getLong("sessionId"),
        throwIndex = getInt("throwIndex"),
        targetField = getString("targetField"),
        actualField = getString("actualField"),
        isHit = getBoolean("isHit"),
        targetX = optDoubleOrNull("targetX"), targetY = optDoubleOrNull("targetY"),
        actualX = optDoubleOrNull("actualX"), actualY = optDoubleOrNull("actualY")
    )

    // ─── Null-safe helpers for org.json ───────────────────────────────────────

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key) || !has(key)) null else getDouble(key)
}
