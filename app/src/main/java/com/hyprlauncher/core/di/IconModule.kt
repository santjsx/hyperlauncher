package com.hyprlauncher.core.di

import com.hyprlauncher.core.icon.DefaultIconCache
import com.hyprlauncher.core.icon.IconCache
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IconModule {

    @Binds
    @Singleton
    abstract fun bindIconCache(
        iconCache: DefaultIconCache
    ): IconCache
}
