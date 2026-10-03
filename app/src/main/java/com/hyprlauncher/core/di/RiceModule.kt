package com.hyprlauncher.core.di

import com.hyprlauncher.data.repository.DefaultRiceRepository
import com.hyprlauncher.data.repository.RiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RiceModule {

    @Binds
    @Singleton
    abstract fun bindRiceRepository(
        impl: DefaultRiceRepository
    ): RiceRepository
}
