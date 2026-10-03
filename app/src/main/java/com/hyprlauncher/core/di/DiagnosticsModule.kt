package com.hyprlauncher.core.di

import com.hyprlauncher.core.diagnostics.DefaultPerformanceMetricsManager
import com.hyprlauncher.core.diagnostics.PerformanceMetricsManager
import com.hyprlauncher.data.repository.DefaultDiagnosticsRepository
import com.hyprlauncher.data.repository.DiagnosticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {

    @Binds
    @Singleton
    abstract fun bindPerformanceMetricsManager(
        impl: DefaultPerformanceMetricsManager
    ): PerformanceMetricsManager

    @Binds
    @Singleton
    abstract fun bindDiagnosticsRepository(
        impl: DefaultDiagnosticsRepository
    ): DiagnosticsRepository
}
