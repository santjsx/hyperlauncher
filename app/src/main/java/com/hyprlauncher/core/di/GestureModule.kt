package com.hyprlauncher.core.di

import com.hyprlauncher.core.gesture.DefaultGestureActionExecutor
import com.hyprlauncher.core.gesture.DefaultGestureRepository
import com.hyprlauncher.core.gesture.GestureActionExecutor
import com.hyprlauncher.core.gesture.GestureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GestureModule {

    @Binds
    @Singleton
    abstract fun bindGestureRepository(
        impl: DefaultGestureRepository
    ): GestureRepository

    @Binds
    @Singleton
    abstract fun bindGestureActionExecutor(
        impl: DefaultGestureActionExecutor
    ): GestureActionExecutor
}
