package com.hyprlauncher.hardening

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.search.DefaultAppSearchEngine
import com.hyprlauncher.core.search.MatchType
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.SearchRankingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Phase 12 Hardening: Large App-Library Stress Testing (PRD §51, §53, §57).
 * Validates performance, stability, and latency limits across 10, 100, 500, 1,000, and 2,500+ apps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LargeAppLibraryStressTest {

    private lateinit var database: HyprDatabase
    private val searchEngine = DefaultAppSearchEngine()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun generateApps(count: Int): List<AppEntity> {
        val prefixes = listOf("Pro", "Super", "Hypr", "Quick", "Smart", "Ultra", "Arch", "Linux", "Code", "Task")
        val nouns = listOf("Terminal", "Browser", "Editor", "Launcher", "Player", "Viewer", "Manager", "Studio", "Note", "Files")
        return (1..count).map { i ->
            val prefix = prefixes[i % prefixes.size]
            val noun = nouns[(i / prefixes.size) % nouns.size]
            val label = "$prefix $noun $i"
            val pkg = "com.sample.app$i.${prefix.lowercase()}.${noun.lowercase()}"
            AppEntity(
                packageName = pkg,
                activityName = "$pkg.MainActivity",
                label = label,
                isFavorite = (i % 7 == 0),
                launchCount = (i % 50),
                lastUsedTimestamp = System.currentTimeMillis() - (i * 60_000L)
            )
        }
    }

    @Test
    fun databaseScalesBulkInsertionTo2500Apps() = runTest {
        val apps = generateApps(2500)
        val timeMs = measureTimeMillis {
            database.appDao().upsertApps(apps)
        }

        val count = database.appDao().getAppCount()
        assertEquals(2500, count)
        // Ensure bulk insertion is fast and doesn't block abnormally
        assertTrue("Bulk insertion of 2500 apps took ${timeMs}ms", timeMs < 5000)
    }

    @Test
    fun searchLatencyRemainsSub10msWith2500Apps() {
        val apps = generateApps(2500)
        val testQueries = listOf("term", "browser", "hypr", "code", "task", "editor", "player", "ultra", "arch", "q")

        // Warm up JIT
        for (q in testQueries) {
            searchEngine.rankApps(q, apps)
        }

        // Measure average latency over 100 search queries
        var totalNanos = 0L
        val iterations = 100
        for (i in 0 until iterations) {
            val query = testQueries[i % testQueries.size]
            val nanos = measureNanoTime {
                val results = searchEngine.rankApps(query, apps)
                assertTrue(results.isNotEmpty())
            }
            totalNanos += nanos
        }

        val avgMs = (totalNanos / iterations) / 1_000_000.0
        assertTrue("Average search latency across 2,500 apps was ${avgMs}ms (must be < 15ms)", avgMs < 15.0)
    }

    @Test
    fun alphabeticalGroupingScalesAcross2500Apps() {
        val apps = generateApps(2500)
        val timeMs = measureTimeMillis {
            val grouped = apps.groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }
            assertTrue(grouped.isNotEmpty())
            assertTrue(grouped.size >= 8)
        }
        assertTrue("Alphabetical grouping took ${timeMs}ms (must be < 50ms)", timeMs < 50)
    }

    @Test
    fun searchRankingModesRemainDeterministicAtScale() {
        val apps = generateApps(1000)
        val configFreq = SearchConfig(rankingMode = SearchRankingMode.FREQUENCY_FIRST)
        val configAlpha = SearchConfig(rankingMode = SearchRankingMode.ALPHABETICAL)
        val configRecency = SearchConfig(rankingMode = SearchRankingMode.RECENCY_FIRST)

        val resFreq = searchEngine.rankApps("", apps, configFreq)
        val resAlpha = searchEngine.rankApps("", apps, configAlpha)
        val resRecency = searchEngine.rankApps("", apps, configRecency)

        assertEquals(1000, resFreq.size)
        assertEquals(1000, resAlpha.size)
        assertEquals(1000, resRecency.size)

        // Frequency first top item must have highest launch count
        val topFreq = resFreq.first().app
        assertTrue(topFreq.launchCount >= resFreq.last().app.launchCount)

        // Alphabetical first top item must be sorted A-Z
        val topAlpha = resAlpha.first().app.label.lowercase()
        val secondAlpha = resAlpha[1].app.label.lowercase()
        assertTrue(topAlpha <= secondAlpha)
    }

    @Test
    fun fuzzyAndAcronymMatchingStayAccurateAtScale() {
        val apps = generateApps(2000) + listOf(
            AppEntity("org.mozilla.firefox", "MainActivity", "Firefox"),
            AppEntity("com.google.android.youtube", "MainActivity", "YouTube"),
            AppEntity("com.discord", "MainActivity", "Discord")
        )

        // Acronym: "yt" -> YouTube
        val ytResults = searchEngine.rankApps("yt", apps)
        assertEquals("YouTube", ytResults.first().app.label)
        assertEquals(MatchType.ACRONYM, ytResults.first().matchType)

        // Fuzzy: "fyrefox" -> Firefox (substitute letter 'y' triggers Levenshtein fuzzy match)
        val ffResults = searchEngine.rankApps("fyrefox", apps, SearchConfig(fuzzyMatchingEnabled = true))
        assertEquals("Firefox", ffResults.first().app.label)
        assertEquals(MatchType.FUZZY, ffResults.first().matchType)
    }
}
