package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): AppSettings?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun observe(key: String): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: AppSettings)

    suspend fun set(key: String, value: String) = set(AppSettings(key, value))

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettings>

    @Query("DELETE FROM app_settings")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<AppSettings>)
}
