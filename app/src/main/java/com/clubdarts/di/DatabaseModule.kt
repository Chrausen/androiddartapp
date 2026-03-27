package com.clubdarts.di

import android.content.Context
import androidx.room.Room
import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.MIGRATION_1_2
import com.clubdarts.data.db.MIGRATION_2_3
import com.clubdarts.data.db.MIGRATION_3_4
import com.clubdarts.data.db.MIGRATION_4_5
import com.clubdarts.data.db.MIGRATION_5_6
import com.clubdarts.data.db.MIGRATION_6_7
import com.clubdarts.data.db.MIGRATION_7_8
import com.clubdarts.data.db.MIGRATION_8_9
import com.clubdarts.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "clubdarts.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .build()

    @Provides
    @Singleton
    fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()

    @Provides
    @Singleton
    fun provideGameDao(db: AppDatabase): GameDao = db.gameDao()

    @Provides
    @Singleton
    fun provideLegDao(db: AppDatabase): LegDao = db.legDao()

    @Provides
    @Singleton
    fun provideThrowDao(db: AppDatabase): ThrowDao = db.throwDao()

    @Provides
    @Singleton
    fun provideAppSettingsDao(db: AppDatabase): AppSettingsDao = db.appSettingsDao()

    @Provides
    @Singleton
    fun provideEloMatchDao(db: AppDatabase): EloMatchDao = db.eloMatchDao()

    @Provides
    @Singleton
    fun provideEloMatchEntryDao(db: AppDatabase): EloMatchEntryDao = db.eloMatchEntryDao()
}
