package com.clubdarts.di

import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import com.clubdarts.data.repository.SettingsRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Repositories are @Singleton and @Inject constructor, so Hilt handles them automatically
}
