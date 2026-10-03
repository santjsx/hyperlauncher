package com.hyprlauncher.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class WorkspaceDaoTest {

    private lateinit var database: HyprDatabase
    private lateinit var workspaceDao: WorkspaceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workspaceDao = database.workspaceDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveWorkspaces() = runTest {
        val workspaces = listOf(
            WorkspaceEntity(id = 1, name = "Main", orderIndex = 1),
            WorkspaceEntity(id = 2, name = "Work", orderIndex = 2),
            WorkspaceEntity(id = 3, name = "Dev", orderIndex = 3)
        )

        workspaceDao.upsertWorkspaces(workspaces)

        val retrieved = workspaceDao.getAllWorkspaces().first()
        assertEquals(3, retrieved.size)
        assertEquals("Main", retrieved[0].name)
        assertEquals("Work", retrieved[1].name)
        assertEquals("Dev", retrieved[2].name)
    }

    @Test
    fun getWorkspaceById() = runTest {
        val ws = WorkspaceEntity(id = 42, name = "Hacking", orderIndex = 42, iconName = "code")
        workspaceDao.upsertWorkspace(ws)

        val retrieved = workspaceDao.getWorkspaceById(42)
        assertNotNull(retrieved)
        assertEquals("Hacking", retrieved?.name)
        assertEquals("code", retrieved?.iconName)
    }

    @Test
    fun deleteWorkspaceRemovesEntry() = runTest {
        val ws = WorkspaceEntity(id = 99, name = "Temporary", orderIndex = 99)
        workspaceDao.upsertWorkspace(ws)
        assertEquals(1, workspaceDao.getWorkspaceCount())

        workspaceDao.deleteWorkspace(99)
        assertEquals(0, workspaceDao.getWorkspaceCount())
        assertNull(workspaceDao.getWorkspaceById(99))
    }
}
