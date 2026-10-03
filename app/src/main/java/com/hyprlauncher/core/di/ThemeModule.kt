package com.hyprlauncher.core.di

import com.hyprlauncher.data.repository.DefaultThemeRepository
import com.hyprlauncher.data.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: DefaultThemeRepository
    ): ThemeRepository
}
