package com.hyprlauncher.core.di

import com.hyprlauncher.core.widget.DefaultWidgetHostManager
import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.data.repository.DefaultWidgetRepository
import com.hyprlauncher.data.repository.WidgetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    @Singleton
    abstract fun bindWidgetHostManager(
        impl: DefaultWidgetHostManager
    ): WidgetHostManager

    @Binds
    @Singleton
    abstract fun bindWidgetRepository(
        impl: DefaultWidgetRepository
    ): WidgetRepository
}
