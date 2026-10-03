package com.hyprlauncher.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherWidgetTest {

    @Test
    fun widgetsOverlapWhenSharingGridCells() {
        val w1 = LauncherWidget(
            id = "w1",
            appWidgetId = 1,
            workspaceId = 1,
            providerPackage = "pkg.a",
            providerClass = "cls.a",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        // w2 overlaps w1 at (1, 1)
        val w2 = LauncherWidget(
            id = "w2",
            appWidgetId = 2,
            workspaceId = 1,
            providerPackage = "pkg.b",
            providerClass = "cls.b",
            cellX = 1,
            cellY = 1,
            spanX = 2,
            spanY = 2
        )

        assertTrue(w1.overlaps(w2))
        assertTrue(w2.overlaps(w1))
    }

    @Test
    fun adjacentWidgetsDoNotOverlap() {
        val w1 = LauncherWidget(
            id = "w1",
            appWidgetId = 1,
            workspaceId = 1,
            providerPackage = "pkg.a",
            providerClass = "cls.a",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        // w2 starts right after w1 at cellX = 2
        val w2 = LauncherWidget(
            id = "w2",
            appWidgetId = 2,
            workspaceId = 1,
            providerPackage = "pkg.b",
            providerClass = "cls.b",
            cellX = 2,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        assertFalse(w1.overlaps(w2))
        assertFalse(w2.overlaps(w1))
    }

    @Test
    fun widgetsOnDifferentWorkspacesDoNotOverlap() {
        val w1 = LauncherWidget(
            id = "w1",
            appWidgetId = 1,
            workspaceId = 1,
            providerPackage = "pkg.a",
            providerClass = "cls.a",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        val w2 = LauncherWidget(
            id = "w2",
            appWidgetId = 2,
            workspaceId = 2, // Different workspace
            providerPackage = "pkg.b",
            providerClass = "cls.b",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        assertFalse(w1.overlaps(w2))
    }

    @Test
    fun sameWidgetDoesNotOverlapWithItself() {
        val w1 = LauncherWidget(
            id = "w1",
            appWidgetId = 1,
            workspaceId = 1,
            providerPackage = "pkg.a",
            providerClass = "cls.a",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2
        )

        assertFalse(w1.overlaps(w1))
    }
}
