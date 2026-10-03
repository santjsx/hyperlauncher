package com.hyprlauncher.core.di

import com.hyprlauncher.core.search.AppSearchEngine
import com.hyprlauncher.core.search.DefaultAppSearchEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @Singleton
    abstract fun bindAppSearchEngine(
        impl: DefaultAppSearchEngine
    ): AppSearchEngine
}
