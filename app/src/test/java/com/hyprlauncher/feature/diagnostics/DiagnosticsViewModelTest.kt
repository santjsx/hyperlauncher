package com.hyprlauncher.feature.diagnostics

import com.hyprlauncher.data.repository.DiagnosticsRepository
import com.hyprlauncher.domain.model.DiagnosticReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DiagnosticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakeDiagnosticsRepository
    private lateinit var viewModel: DiagnosticsViewModel

    class FakeDiagnosticsRepository : DiagnosticsRepository {
        var clearCacheCalled = false
        var rebuildAppIndexCalled = false
        var resetUiStateCalled = false

        val sampleReport = DiagnosticReport(
            timestamp = 1000L,
            startupTimeMs = 250L,
            fps = 60.0,
            averageFrameTimeMs = 16.6,
            jankFrames = 0,
            jankPercentage = 0.0,
            heapAllocatedMb = 32L,
            heapMaxMb = 256L,
            nativeHeapAllocatedMb = 18L,
            searchLatencyMs = 8L,
            avgSearchLatencyMs = 7.5,
            iconCacheCount = 120,
            iconCacheSizeBytes = 512000L,
            iconCacheMaxSizeBytes = 10485760L,
            iconMemoryHits = 80,
            iconDiskHits = 10,
            databaseSizeBytes = 128000L,
            appCount = 42,
            widgetCount = 2,
            workspaceCount = 5,
            riceProfileCount = 5,
            deviceModel = "Pixel 8 Pro",
            androidVersion = "Android 14 (API 34)",
            performanceMode = "NORMAL"
        )

        override suspend fun getLiveReport(): DiagnosticReport = sampleReport

        override suspend fun exportDiagnostics(asJson: Boolean): Result<String> {
            return if (asJson) Result.success(sampleReport.toJson()) else Result.success(sampleReport.toAsciiReport())
        }

        override suspend fun clearCache(): Result<Unit> {
            clearCacheCalled = true
            return Result.success(Unit)
        }

        override suspend fun rebuildAppIndex(): Result<Unit> {
            rebuildAppIndexCalled = true
            return Result.success(Unit)
        }

        override suspend fun resetUiState(): Result<Unit> {
            resetUiStateCalled = true
            return Result.success(Unit)
        }

        override fun recordSearchLatency(durationMs: Long) {}
        override fun recordFrameTime(durationNanos: Long) {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDiagnosticsRepository()
        viewModel = DiagnosticsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsLiveReport() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.report != null }
        val report = state.report
        assertNotNull(report)
        assertEquals(42, report!!.appCount)
        assertEquals(60.0, report.fps, 0.1)
        assertFalse(state.isLoading)
        assertTrue(state.autoRefresh)
    }

    @Test
    fun toggleAutoRefreshFlipsBoolean() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.autoRefresh)

        viewModel.toggleAutoRefresh()
        assertFalse(viewModel.uiState.value.autoRefresh)

        viewModel.toggleAutoRefresh()
        assertTrue(viewModel.uiState.value.autoRefresh)
    }

    @Test
    fun clearCacheCallsRepositoryAndSetsStatusMessage() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.clearCache()
        val state = viewModel.uiState.first { it.statusMessage != null }

        assertTrue(fakeRepository.clearCacheCalled)
        assertTrue(state.statusMessage!!.contains("cleared"))
    }

    @Test
    fun rebuildAppIndexCallsRepositoryAndSetsStatusMessage() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.rebuildAppIndex()
        val state = viewModel.uiState.first { it.statusMessage != null }

        assertTrue(fakeRepository.rebuildAppIndexCalled)
        assertTrue(state.statusMessage!!.contains("rebuilt"))
    }

    @Test
    fun resetUiStateCallsRepositoryAndSetsStatusMessage() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.resetUiState()
        val state = viewModel.uiState.first { it.statusMessage != null }

        assertTrue(fakeRepository.resetUiStateCalled)
        assertTrue(state.statusMessage!!.contains("reset"))
    }

    @Test
    fun exportDiagnosticsJsonPopulatesExportedContent() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.exportDiagnostics(asJson = true)
        val state = viewModel.uiState.first { it.exportedContent != null }

        assertNotNull(state.exportedContent)
        assertTrue(state.exportedContent!!.contains("\"appsIndexed\": 42"))
        assertTrue(state.statusMessage!!.contains("JSON"))

        viewModel.dismissExport()
        assertNull(viewModel.uiState.value.exportedContent)
    }

    @Test
    fun exportDiagnosticsAsciiPopulatesExportedContent() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.exportDiagnostics(asJson = false)
        val state = viewModel.uiState.first { it.exportedContent != null }

        assertNotNull(state.exportedContent)
        assertTrue(state.exportedContent!!.contains("hyprctl @ diagnostics"))
        assertTrue(state.statusMessage!!.contains("ASCII"))

        viewModel.dismissStatusMessage()
        assertNull(viewModel.uiState.value.statusMessage)
    }
}
