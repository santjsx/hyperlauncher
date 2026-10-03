package com.hyprlauncher.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AppDaoTest {

    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveApps() = runTest {
        val app1 = AppEntity(
            packageName = "org.mozilla.firefox",
            activityName = "org.mozilla.firefox.App",
            label = "Firefox",
            category = "INTERNET",
            workspaceId = 1
        )
        val app2 = AppEntity(
            packageName = "com.spotify.music",
            activityName = "com.spotify.music.MainActivity",
            label = "Spotify",
            category = "AUDIO",
            workspaceId = 4
        )

        appDao.upsertApps(listOf(app1, app2))

        val allApps = appDao.getAllVisibleApps().first()
        assertEquals(2, allApps.size)
        assertEquals("Firefox", allApps[0].label)
        assertEquals("Spotify", allApps[1].label)
    }

    @Test
    fun searchAppsByPrefixAndContent() = runTest {
        val app1 = AppEntity(
            packageName = "com.google.android.youtube",
            activityName = "com.google.android.youtube.HomeActivity",
            label = "YouTube",
            workspaceId = 4
        )
        val app2 = AppEntity(
            packageName = "com.google.android.apps.youtube.music",
            activityName = "com.google.android.apps.youtube.music.MainActivity",
            label = "YouTube Music",
            workspaceId = 4
        )
        val app3 = AppEntity(
            packageName = "org.videolan.vlc",
            activityName = "org.videolan.vlc.StartActivity",
            label = "VLC",
            workspaceId = 4
        )

        appDao.upsertApps(listOf(app1, app2, app3))

        val results = appDao.searchApps("You").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.label == "YouTube" })
        assertTrue(results.any { it.label == "YouTube Music" })
    }

    @Test
    fun recordAppLaunchIncrementsCount() = runTest {
        val app = AppEntity(
            packageName = "com.terminal.app",
            activityName = "com.terminal.app.TermActivity",
            label = "Terminal",
            launchCount = 0
        )
        appDao.upsertApp(app)

        val timestamp = 1700000000000L
        appDao.recordAppLaunch("com.terminal.app", timestamp)

        val updated = appDao.getApp("com.terminal.app")
        assertNotNull(updated)
        assertEquals(1, updated?.launchCount)
        assertEquals(timestamp, updated?.lastUsedTimestamp)
    }

    @Test
    fun deleteAppRemovesFromDatabase() = runTest {
        val app = AppEntity(
            packageName = "com.temp.test",
            activityName = "com.temp.test.TestActivity",
            label = "Test App"
        )
        appDao.upsertApp(app)
        assertEquals(1, appDao.getAppCount())

        appDao.deleteApp("com.temp.test")
        assertEquals(0, appDao.getAppCount())
        assertNull(appDao.getApp("com.temp.test"))
    }
}
