package com.hyprlauncher.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.domain.model.Workspace
import com.hyprlauncher.domain.model.WorkspaceLayoutConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkspaceRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var workspaceDao: WorkspaceDao
    private lateinit var repository: WorkspaceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workspaceDao = database.workspaceDao()
        repository = DefaultWorkspaceRepository(workspaceDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun ensureDefaultWorkspacesSeedsFiveWorkspaces() = runTest(testDispatcher) {
        assertEquals(0, workspaceDao.getWorkspaceCount())

        repository.ensureDefaultWorkspaces()

        val workspaces = repository.allWorkspaces.first()
        assertEquals(5, workspaces.size)
        assertEquals("Main", workspaces[0].name)
        assertEquals("Work", workspaces[1].name)
        assertEquals("Dev", workspaces[2].name)
        assertEquals("Media", workspaces[3].name)
        assertEquals("Games", workspaces[4].name)
    }

    @Test
    fun createWorkspaceGeneratesNextIdAndOrderIndex() = runTest(testDispatcher) {
        repository.ensureDefaultWorkspaces()

        val created = repository.createWorkspace("Hacking", "terminal")
        assertEquals(6, created.id)
        assertEquals("Hacking", created.name)
        assertEquals(5, created.orderIndex)

        val all = repository.allWorkspaces.first()
        assertEquals(6, all.size)
    }

    @Test
    fun updateWorkspaceRenamesAndPersists() = runTest(testDispatcher) {
        repository.ensureDefaultWorkspaces()

        val original = repository.getWorkspaceById(1)
        assertNotNull(original)

        val updated = original!!.copy(name = "Primary Console")
        repository.updateWorkspace(updated)

        val retrieved = repository.getWorkspaceById(1)
        assertEquals("Primary Console", retrieved?.name)
    }

    @Test
    fun deleteWorkspaceRemovesWorkspaceWhenMultipleExist() = runTest(testDispatcher) {
        repository.ensureDefaultWorkspaces()

        val deleted = repository.deleteWorkspace(5)
        assertTrue(deleted)

        val remaining = repository.allWorkspaces.first()
        assertEquals(4, remaining.size)
        assertEquals(null, repository.getWorkspaceById(5))
    }

    @Test
    fun deleteWorkspacePreventsDeletingLastRemainingWorkspace() = runTest(testDispatcher) {
        // Seed only 1 workspace
        repository.ensureDefaultWorkspaces()
        repository.deleteWorkspace(2)
        repository.deleteWorkspace(3)
        repository.deleteWorkspace(4)
        repository.deleteWorkspace(5)

        val remaining = repository.allWorkspaces.first()
        assertEquals(1, remaining.size)

        val deleted = repository.deleteWorkspace(remaining.first().id)
        assertFalse(deleted)
        assertEquals(1, repository.allWorkspaces.first().size)
    }

    @Test
    fun reorderWorkspacesUpdatesOrderIndices() = runTest(testDispatcher) {
        repository.ensureDefaultWorkspaces()

        // Reverse order: 5, 4, 3, 2, 1
        repository.reorderWorkspaces(listOf(5, 4, 3, 2, 1))

        val reordered = repository.allWorkspaces.first()
        assertEquals(5, reordered[0].id)
        assertEquals(4, reordered[1].id)
        assertEquals(3, reordered[2].id)
        assertEquals(2, reordered[3].id)
        assertEquals(1, reordered[4].id)
    }

    @Test
    fun updateWorkspaceLayoutPersistsConfigurationAndAssignedApps() = runTest(testDispatcher) {
        repository.ensureDefaultWorkspaces()

        val customConfig = WorkspaceLayoutConfig(
            gridColumns = 5,
            gridRows = 7,
            showClock = false,
            showSearchBar = true,
            wallpaperDim = 0.45f,
            assignedPackageNames = listOf("org.mozilla.firefox", "com.termux")
        )

        repository.updateWorkspaceLayout(3, customConfig)

        val workspace = repository.getWorkspaceById(3)
        assertNotNull(workspace)
        assertEquals(5, workspace!!.layoutConfig.gridColumns)
        assertEquals(7, workspace.layoutConfig.gridRows)
        assertEquals(false, workspace.layoutConfig.showClock)
        assertEquals(true, workspace.layoutConfig.showSearchBar)
        assertEquals(0.45f, workspace.layoutConfig.wallpaperDim)
        assertEquals(listOf("org.mozilla.firefox", "com.termux"), workspace.layoutConfig.assignedPackageNames)
    }

    @Test
    fun workspaceLayoutConfigJsonSerializationRoundtrip() {
        val config = WorkspaceLayoutConfig(
            gridColumns = 4,
            gridRows = 6,
            showClock = true,
            showSearchBar = false,
            wallpaperUri = "content://media/external/images/media/42",
            wallpaperDim = 0.3f,
            assignedPackageNames = listOf("com.example.app1", "com.example.app2")
        )

        val json = config.toJson()
        val deserialized = WorkspaceLayoutConfig.fromJson(json)

        assertEquals(config.gridColumns, deserialized.gridColumns)
        assertEquals(config.gridRows, deserialized.gridRows)
        assertEquals(config.showClock, deserialized.showClock)
        assertEquals(config.showSearchBar, deserialized.showSearchBar)
        assertEquals(config.wallpaperUri, deserialized.wallpaperUri)
        assertEquals(config.wallpaperDim, deserialized.wallpaperDim)
        assertEquals(config.assignedPackageNames, deserialized.assignedPackageNames)
    }
}
