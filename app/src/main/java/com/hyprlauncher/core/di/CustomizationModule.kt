package com.hyprlauncher.core.di

import com.hyprlauncher.data.repository.CustomizationRepository
import com.hyprlauncher.data.repository.DefaultCustomizationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CustomizationModule {

    @Binds
    @Singleton
    abstract fun bindCustomizationRepository(
        impl: DefaultCustomizationRepository
    ): CustomizationRepository
}
