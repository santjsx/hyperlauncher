package com.hyprlauncher.core.search

import com.hyprlauncher.data.database.entity.AppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSearchEngineTest {

    private lateinit var searchEngine: AppSearchEngine
    private lateinit var sampleApps: List<AppEntity>

    @Before
    fun setup() {
        searchEngine = DefaultAppSearchEngine()
        sampleApps = listOf(
            AppEntity(packageName = "org.mozilla.firefox", activityName = "MainActivity", label = "Firefox", launchCount = 10),
            AppEntity(packageName = "com.google.android.youtube", activityName = "HomeActivity", label = "YouTube", launchCount = 50),
            AppEntity(packageName = "com.android.calculator2", activityName = "CalcActivity", label = "Calculator", launchCount = 5),
            AppEntity(packageName = "com.android.camera", activityName = "CameraActivity", label = "Camera", launchCount = 2, isFavorite = true),
            AppEntity(packageName = "com.spotify.music", activityName = "MainActivity", label = "Spotify Music", launchCount = 20)
        )
    }

    @Test
    fun exactMatchYieldsHighestScore() {
        val results = searchEngine.rankApps("firefox", sampleApps)
        assertTrue(results.isNotEmpty())
        assertEquals("org.mozilla.firefox", results.first().app.packageName)
        assertEquals(MatchType.EXACT, results.first().matchType)
    }

    @Test
    fun prefixMatchRanksCorrectly() {
        val results = searchEngine.rankApps("calc", sampleApps)
        assertTrue(results.isNotEmpty())
        assertEquals("com.android.calculator2", results.first().app.packageName)
        assertEquals(MatchType.PREFIX, results.first().matchType)
    }

    @Test
    fun acronymMatchMatchesInitials() {
        val results = searchEngine.rankApps("yt", sampleApps)
        assertTrue(results.isNotEmpty())
        assertEquals("com.google.android.youtube", results.first().app.packageName)
        assertEquals(MatchType.ACRONYM, results.first().matchType)
    }

    @Test
    fun fuzzyMatchToleratesSmallTypos() {
        // "firefoz" has 1 edit distance from "firefox"
        val results = searchEngine.rankApps("firefoz", sampleApps)
        assertTrue(results.isNotEmpty())
        assertEquals("org.mozilla.firefox", results.first().app.packageName)
        assertEquals(MatchType.FUZZY, results.first().matchType)
    }

    @Test
    fun emptyQueryReturnsAllAppsWithFavoritesAndUsageRanking() {
        val results = searchEngine.rankApps("", sampleApps)
        assertEquals(sampleApps.size, results.size)
        // Camera is favorite (+500 score), so should be first
        assertEquals("com.android.camera", results.first().app.packageName)
    }
}
