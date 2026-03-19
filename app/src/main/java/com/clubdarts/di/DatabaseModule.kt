package com.clubdarts.di

import android.content.Context
import androidx.room.Room
import com.clubdarts.data.db.AppDatabase
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
        Room.databaseBuilder(context, AppDatabase::class.java, "clubdarts.db").build()

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
}
