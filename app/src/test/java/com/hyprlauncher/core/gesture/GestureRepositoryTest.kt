package com.hyprlauncher.core.gesture

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GestureRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: GestureRepository

    @Before
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("gesture_test.preferences_pb") }
        )
        repository = DefaultGestureRepository(dataStore)
    }

    @Test
    fun defaultBindingsAreProvidedInitially() = runTest(testDispatcher) {
        val bindings = repository.gestureBindings.first()

        assertEquals(GestureType.entries.size, bindings.size)
        assertEquals(LauncherAction.OpenAppDrawer, bindings[GestureType.SWIPE_UP])
        assertEquals(LauncherAction.OpenNotificationShade, bindings[GestureType.SWIPE_DOWN])
        assertEquals(LauncherAction.NextWorkspace, bindings[GestureType.SWIPE_LEFT])
        assertEquals(LauncherAction.PreviousWorkspace, bindings[GestureType.SWIPE_RIGHT])
        assertEquals(LauncherAction.OpenCommandPalette, bindings[GestureType.DOUBLE_TAP])
        assertEquals(LauncherAction.OpenSettings, bindings[GestureType.LONG_PRESS])
    }

    @Test
    fun setBindingUpdatesAndPersistsAction() = runTest(testDispatcher) {
        repository.setBinding(
            GestureType.SWIPE_UP,
            LauncherAction.LaunchApplication("org.mozilla.firefox")
        )

        val updated = repository.gestureBindings.first()
        assertEquals(
            LauncherAction.LaunchApplication("org.mozilla.firefox"),
            updated[GestureType.SWIPE_UP]
        )
        // Other gestures remain unchanged
        assertEquals(LauncherAction.OpenNotificationShade, updated[GestureType.SWIPE_DOWN])
    }

    @Test
    fun resetDefaultsRestoresOriginalBindings() = runTest(testDispatcher) {
        repository.setBinding(GestureType.SWIPE_UP, LauncherAction.None)
        repository.setBinding(GestureType.DOUBLE_TAP, LauncherAction.LockDevice)

        var bindings = repository.gestureBindings.first()
        assertEquals(LauncherAction.None, bindings[GestureType.SWIPE_UP])
        assertEquals(LauncherAction.LockDevice, bindings[GestureType.DOUBLE_TAP])

        repository.resetDefaults()

        bindings = repository.gestureBindings.first()
        assertEquals(LauncherAction.OpenAppDrawer, bindings[GestureType.SWIPE_UP])
        assertEquals(LauncherAction.OpenCommandPalette, bindings[GestureType.DOUBLE_TAP])
    }

    @Test
    fun launcherActionSerializationRoundTrip() {
        val actions = listOf(
            LauncherAction.OpenAppDrawer,
            LauncherAction.OpenNotificationShade,
            LauncherAction.NextWorkspace,
            LauncherAction.PreviousWorkspace,
            LauncherAction.OpenSearch,
            LauncherAction.OpenCommandPalette,
            LauncherAction.OpenSettings,
            LauncherAction.LockDevice,
            LauncherAction.LaunchApplication("com.example.testapp"),
            LauncherAction.None
        )

        for (action in actions) {
            val serialized = action.serialize()
            val deserialized = LauncherAction.deserialize(serialized)
            assertEquals("Mismatch for $action", action, deserialized)
        }
    }

    @Test
    fun launcherActionDeserializationFallbackOnUnknownString() {
        val fallback = LauncherAction.deserialize("invalid:random:action")
        assertEquals(LauncherAction.None, fallback)
    }
}
