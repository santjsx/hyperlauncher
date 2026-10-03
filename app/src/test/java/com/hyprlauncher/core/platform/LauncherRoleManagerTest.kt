package com.hyprlauncher.core.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherRoleManagerTest {

    private lateinit var context: Context
    private lateinit var roleManager: LauncherRoleManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        roleManager = DefaultLauncherRoleManager(context)
    }

    @Test
    fun isDefaultLauncherReturnsBooleanWithoutCrashing() {
        // In clean test environment, app is initially not the default launcher
        val isDefault = roleManager.isDefaultLauncher()
        // Should evaluate cleanly without throwing any exceptions
        org.junit.Assert.assertFalse(isDefault)
    }

    @Test
    fun createRequestDefaultLauncherIntentProducesValidIntent() {
        val intent = roleManager.createRequestDefaultLauncherIntent()
        assertNotNull(intent)
    }
}
