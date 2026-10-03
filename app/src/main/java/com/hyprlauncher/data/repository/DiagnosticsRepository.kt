package com.hyprlauncher.data.repository

import android.content.Context
import android.os.Build
import com.hyprlauncher.core.di.IoDispatcher
import com.hyprlauncher.core.diagnostics.PerformanceMetricsManager
import com.hyprlauncher.core.diagnostics.StartupTracker
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.domain.model.DiagnosticReport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface DiagnosticsRepository {
    suspend fun getLiveReport(): DiagnosticReport
    suspend fun exportDiagnostics(asJson: Boolean = false): Result<String>
    suspend fun clearCache(): Result<Unit>
    suspend fun rebuildAppIndex(): Result<Unit>
    suspend fun resetUiState(): Result<Unit>
    fun recordSearchLatency(durationMs: Long)
    fun recordFrameTime(durationNanos: Long)
}

@Singleton
class DefaultDiagnosticsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val startupTracker: StartupTracker,
    private val metricsManager: PerformanceMetricsManager,
    private val iconCache: IconCache,
    private val appRepository: AppRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val widgetRepository: WidgetRepository,
    private val riceRepository: RiceRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val appDao: AppDao,
    private val widgetDao: WidgetDao,
    private val riceDao: RiceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DiagnosticsRepository {

    override suspend fun getLiveReport(): DiagnosticReport = withContext(ioDispatcher) {
        val frameMetrics = metricsManager.getFrameMetrics()
        val memMetrics = metricsManager.getMemoryMetrics()
        val cacheStats = iconCache.getStats()

        val appCount = runCatching { appDao.getAppCount() }.getOrDefault(0)
        val widgetCount = runCatching { widgetRepository.allWidgets.first().size }.getOrDefault(0)
        val workspaceCount = runCatching { workspaceRepository.allWorkspaces.first().size }.getOrDefault(5)
        val riceProfileCount = runCatching { riceRepository.allRices.first().size }.getOrDefault(0)
        val prefs = runCatching { preferencesRepository.preferences.first() }.getOrNull()

        val dbFile = context.getDatabasePath("hyprlauncher.db")
        val dbWal = File(dbFile.path + "-wal")
        val dbShm = File(dbFile.path + "-shm")
        val dbSize = (if (dbFile.exists()) dbFile.length() else 0L) +
                (if (dbWal.exists()) dbWal.length() else 0L) +
                (if (dbShm.exists()) dbShm.length() else 0L)

        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        DiagnosticReport(
            timestamp = System.currentTimeMillis(),
            startupTimeMs = startupTracker.getColdStartTimeMs(),
            fps = frameMetrics.fps,
            averageFrameTimeMs = frameMetrics.averageFrameTimeMs,
            jankFrames = frameMetrics.jankFrames,
            jankPercentage = frameMetrics.jankPercentage,
            heapAllocatedMb = memMetrics.heapAllocatedMb,
            heapMaxMb = memMetrics.heapMaxMb,
            nativeHeapAllocatedMb = memMetrics.nativeHeapAllocatedMb,
            searchLatencyMs = metricsManager.getLastSearchLatencyMs(),
            avgSearchLatencyMs = metricsManager.getAverageSearchLatencyMs(),
            iconCacheCount = (cacheStats.memorySizeBytes / 1024 / 48).coerceAtLeast(0), // approx count
            iconCacheSizeBytes = cacheStats.memorySizeBytes.toLong(),
            iconCacheMaxSizeBytes = cacheStats.maxMemorySizeBytes.toLong(),
            iconMemoryHits = cacheStats.memoryHits,
            iconDiskHits = cacheStats.diskHits,
            databaseSizeBytes = dbSize,
            appCount = appCount,
            widgetCount = widgetCount,
            workspaceCount = workspaceCount,
            riceProfileCount = riceProfileCount,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            performanceMode = prefs?.performanceMode?.name ?: "BALANCED"
        )
    }

    override suspend fun exportDiagnostics(asJson: Boolean): Result<String> {
        return runCatching {
            val report = getLiveReport()
            if (asJson) report.toJson() else report.toAsciiReport()
        }
    }

    override suspend fun clearCache(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            iconCache.clear()
            Unit
        }
    }

    override suspend fun rebuildAppIndex(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            appRepository.syncAllApps()
            Unit
        }
    }

    override suspend fun resetUiState(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            metricsManager.resetMetrics()
            Unit
        }
    }

    override fun recordSearchLatency(durationMs: Long) {
        metricsManager.recordSearchLatency(durationMs)
    }

    override fun recordFrameTime(durationNanos: Long) {
        metricsManager.recordFrame(durationNanos)
    }
}
