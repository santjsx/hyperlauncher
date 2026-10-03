package com.hyprlauncher.core.platform

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AppLauncherTest {

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private lateinit var appLauncher: AppLauncher

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
        appLauncher = DefaultAppLauncher(context, appDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun launchNonExistentAppReturnsAppNotFoundGracefully() = runTest {
        val result = appLauncher.launchApp("com.nonexistent.fake.app")
        assertTrue(result is LaunchResult.AppNotFound)
    }
}
