package com.hyprlauncher.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.domain.model.WidgetProviderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WidgetRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var widgetDao: WidgetDao
    private lateinit var fakeHostManager: TestWidgetHostManager
    private lateinit var repository: WidgetRepository

    class TestWidgetHostManager : WidgetHostManager {
        val deletedHostIds = mutableListOf<Int>()
        var listening = false

        override fun startListening() { listening = true }
        override fun stopListening() { listening = false }
        override fun allocateAppWidgetId(): Int = 42
        override fun deleteAppWidgetId(appWidgetId: Int) { deletedHostIds.add(appWidgetId) }
        override fun getAvailableProviders(): List<WidgetProviderItem> = listOf(
            WidgetProviderItem(
                providerPackage = "com.test.widget",
                providerClass = "com.test.widget.TestProvider",
                appLabel = "TestApp",
                widgetLabel = "TestWidget",
                minWidthDp = 100,
                minHeightDp = 50,
                minSpanX = 2,
                minSpanY = 1
            )
        )
        override fun getAppWidgetInfo(appWidgetId: Int): android.appwidget.AppWidgetProviderInfo? = null
        override fun createView(
            context: Context,
            appWidgetId: Int,
            info: android.appwidget.AppWidgetProviderInfo
        ): android.appwidget.AppWidgetHostView? = null
        override fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: android.content.ComponentName): Boolean = true
    }

    @Before
    fun setup() = runBlocking(testDispatcher) {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        widgetDao = database.widgetDao()
        fakeHostManager = TestWidgetHostManager()
        repository = DefaultWidgetRepository(widgetDao, fakeHostManager)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            runCatching { database.close() }
        }
    }

    @Test
    fun placeWidgetPersistsAndClampsCoordinates() = runTest(testDispatcher) {
        val result = repository.placeWidget(
            workspaceId = 1,
            appWidgetId = 101,
            providerPackage = "com.example.calendar",
            providerClass = "com.example.calendar.CalendarWidget",
            cellX = -5, // Should clamp to >= 0
            cellY = 2,
            spanX = 10, // Should clamp to <= 8
            spanY = 0,  // Should clamp to >= 1
            label = "My Calendar"
        )

        assertTrue(result.isSuccess)
        val widget = result.getOrNull()
        assertNotNull(widget)
        assertEquals(0, widget!!.cellX)
        assertEquals(2, widget.cellY)
        assertEquals(8, widget.spanX)
        assertEquals(1, widget.spanY)
        assertEquals(1, widget.workspaceId)
        assertEquals("My Calendar", widget.label)

        val retrieved = repository.getWidgetById(widget.id)
        assertNotNull(retrieved)
        assertEquals(widget.id, retrieved!!.id)
    }

    @Test
    fun moveWidgetUpdatesCoordinates() = runTest(testDispatcher) {
        val placed = repository.placeWidget(
            workspaceId = 1,
            appWidgetId = 102,
            providerPackage = "com.example.clock",
            providerClass = "com.example.clock.ClockWidget",
            cellX = 0,
            cellY = 0
        ).getOrThrow()

        val moveResult = repository.moveWidget(placed.id, newCellX = 3, newCellY = 4)
        assertTrue(moveResult.isSuccess)

        val updated = repository.getWidgetById(placed.id)
        assertNotNull(updated)
        assertEquals(3, updated!!.cellX)
        assertEquals(4, updated.cellY)
    }

    @Test
    fun moveNonExistentWidgetReturnsFailure() = runTest(testDispatcher) {
        val result = repository.moveWidget("non-existent-id", 2, 2)
        assertTrue(result.isFailure)
    }

    @Test
    fun resizeWidgetUpdatesSpanWithClamping() = runTest(testDispatcher) {
        val placed = repository.placeWidget(
            workspaceId = 1,
            appWidgetId = 103,
            providerPackage = "com.example.notes",
            providerClass = "com.example.notes.NoteWidget",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        ).getOrThrow()

        val resizeResult = repository.resizeWidget(placed.id, newSpanX = 12, newSpanY = -1)
        assertTrue(resizeResult.isSuccess)

        val updated = repository.getWidgetById(placed.id)
        assertNotNull(updated)
        assertEquals(8, updated!!.spanX) // clamped to 8
        assertEquals(1, updated.spanY) // clamped to 1
    }

    @Test
    fun resizeNonExistentWidgetReturnsFailure() = runTest(testDispatcher) {
        val result = repository.resizeWidget("non-existent-id", 2, 2)
        assertTrue(result.isFailure)
    }

    @Test
    fun removeWidgetDeletesFromDbAndCleansUpHostId() = runTest(testDispatcher) {
        val placed = repository.placeWidget(
            workspaceId = 1,
            appWidgetId = 205,
            providerPackage = "com.example.music",
            providerClass = "com.example.music.MusicWidget",
            cellX = 0,
            cellY = 0
        ).getOrThrow()

        val deleteResult = repository.removeWidget(placed.id)
        assertTrue(deleteResult.isSuccess)

        val retrieved = repository.getWidgetById(placed.id)
        assertNull(retrieved)
        assertTrue(fakeHostManager.deletedHostIds.contains(205))
    }

    @Test
    fun removeWidgetByAppWidgetIdDeletesAndCleansUp() = runTest(testDispatcher) {
        val placed = repository.placeWidget(
            workspaceId = 2,
            appWidgetId = 305,
            providerPackage = "com.example.battery",
            providerClass = "com.example.battery.BatteryWidget",
            cellX = 0,
            cellY = 0
        ).getOrThrow()

        val deleteResult = repository.removeWidgetByAppWidgetId(placed.appWidgetId)
        assertTrue(deleteResult.isSuccess)

        val retrieved = repository.getWidgetById(placed.id)
        assertNull(retrieved)
        assertTrue(fakeHostManager.deletedHostIds.contains(305))
    }

    @Test
    fun getWidgetsForWorkspaceFiltersCorrectly() = runTest(testDispatcher) {
        repository.placeWidget(1, 10, "pkg.a", "cls.a", 0, 0)
        repository.placeWidget(1, 11, "pkg.b", "cls.b", 1, 0)
        repository.placeWidget(2, 12, "pkg.c", "cls.c", 0, 0)

        val ws1Widgets = repository.getWidgetsForWorkspace(1).first()
        val ws2Widgets = repository.getWidgetsForWorkspace(2).first()
        val allWidgets = repository.allWidgets.first()

        assertEquals(2, ws1Widgets.size)
        assertEquals(1, ws2Widgets.size)
        assertEquals(3, allWidgets.size)
    }

    @Test
    fun deleteWidgetsForWorkspaceRemovesOnlyTargetWorkspaceWidgets() = runTest(testDispatcher) {
        repository.placeWidget(1, 10, "pkg.a", "cls.a", 0, 0)
        repository.placeWidget(1, 11, "pkg.b", "cls.b", 1, 0)
        repository.placeWidget(2, 12, "pkg.c", "cls.c", 0, 0)

        repository.deleteWidgetsForWorkspace(1)

        val ws1Widgets = repository.getWidgetsForWorkspace(1).first()
        val ws2Widgets = repository.getWidgetsForWorkspace(2).first()

        assertEquals(0, ws1Widgets.size)
        assertEquals(1, ws2Widgets.size)
    }

    @Test
    fun getAvailableWidgetProvidersDelegatesToHostManager() {
        val providers = repository.getAvailableWidgetProviders()
        assertEquals(1, providers.size)
        assertEquals("TestApp", providers.first().appLabel)
        assertEquals("TestWidget", providers.first().widgetLabel)
    }
}
