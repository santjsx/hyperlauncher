package com.hyprlauncher.core.di

import com.hyprlauncher.data.repository.DefaultWorkspaceRepository
import com.hyprlauncher.data.repository.WorkspaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkspaceModule {

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(
        impl: DefaultWorkspaceRepository
    ): WorkspaceRepository
}
