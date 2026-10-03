package com.hyprlauncher.core.di

import com.hyprlauncher.core.platform.AppLauncher
import com.hyprlauncher.core.platform.DefaultAppLauncher
import com.hyprlauncher.core.platform.DefaultLauncherRoleManager
import com.hyprlauncher.core.platform.DefaultPackageDiscoveryManager
import com.hyprlauncher.core.platform.LauncherRoleManager
import com.hyprlauncher.core.platform.PackageDiscoveryManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindLauncherRoleManager(
        manager: DefaultLauncherRoleManager
    ): LauncherRoleManager

    @Binds
    @Singleton
    abstract fun bindPackageDiscoveryManager(
        manager: DefaultPackageDiscoveryManager
    ): PackageDiscoveryManager

    @Binds
    @Singleton
    abstract fun bindAppLauncher(
        launcher: DefaultAppLauncher
    ): AppLauncher
}
