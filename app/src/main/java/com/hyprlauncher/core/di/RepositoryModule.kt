package com.hyprlauncher.core.di

import com.hyprlauncher.data.repository.AppRepository
import com.hyprlauncher.data.repository.DefaultAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        repository: DefaultAppRepository
    ): AppRepository
}
