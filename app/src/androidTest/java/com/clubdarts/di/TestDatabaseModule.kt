package com.clubdarts.di

import android.content.Context
import androidx.room.Room
import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces [DatabaseModule] in instrumented tests with an in-memory Room database
 * so every test run starts with a clean slate and no data is left on the device.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides @Singleton fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()
    @Provides @Singleton fun provideGameDao(db: AppDatabase): GameDao = db.gameDao()
    @Provides @Singleton fun provideLegDao(db: AppDatabase): LegDao = db.legDao()
    @Provides @Singleton fun provideThrowDao(db: AppDatabase): ThrowDao = db.throwDao()
    @Provides @Singleton fun provideAppSettingsDao(db: AppDatabase): AppSettingsDao = db.appSettingsDao()
    @Provides @Singleton fun provideEloMatchDao(db: AppDatabase): EloMatchDao = db.eloMatchDao()
    @Provides @Singleton fun provideEloMatchEntryDao(db: AppDatabase): EloMatchEntryDao = db.eloMatchEntryDao()
}
