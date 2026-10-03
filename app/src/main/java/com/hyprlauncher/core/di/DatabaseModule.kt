package com.hyprlauncher.core.di

import android.content.Context
import androidx.room.Room
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHyprDatabase(
        @ApplicationContext context: Context
    ): HyprDatabase {
        return Room.databaseBuilder(
            context,
            HyprDatabase::class.java,
            HyprDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideAppDao(database: HyprDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    fun provideWorkspaceDao(database: HyprDatabase): WorkspaceDao {
        return database.workspaceDao()
    }

    @Provides
    fun provideRiceDao(database: HyprDatabase): RiceDao {
        return database.riceDao()
    }

    @Provides
    fun provideWidgetDao(database: HyprDatabase): com.hyprlauncher.data.database.dao.WidgetDao {
        return database.widgetDao()
    }
}
