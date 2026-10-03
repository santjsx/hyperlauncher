package com.hyprlauncher.domain.model

import org.json.JSONObject

/**
 * Diagnostic metrics snapshot conforming to PRD Section 40.
 * Strictly sanitizes private information to protect user privacy.
 */
data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val startupTimeMs: Long,
    val fps: Double,
    val averageFrameTimeMs: Double,
    val jankFrames: Int,
    val jankPercentage: Double,
    val heapAllocatedMb: Long,
    val heapMaxMb: Long,
    val nativeHeapAllocatedMb: Long,
    val searchLatencyMs: Long,
    val avgSearchLatencyMs: Double,
    val iconCacheCount: Int,
    val iconCacheSizeBytes: Long,
    val iconCacheMaxSizeBytes: Long,
    val iconMemoryHits: Int,
    val iconDiskHits: Int,
    val databaseSizeBytes: Long,
    val appCount: Int,
    val widgetCount: Int,
    val workspaceCount: Int,
    val riceProfileCount: Int,
    val deviceModel: String,
    val androidVersion: String,
    val performanceMode: String
) {
    /**
     * Serializes to a clean, sanitized JSON object string without any user-identifiable data.
     */
    fun toJson(): String {
        val root = JSONObject().apply {
            put("timestamp", timestamp)
            put("device", JSONObject().apply {
                put("model", deviceModel)
                put("android", androidVersion)
                put("performanceMode", performanceMode)
            })
            put("startup", JSONObject().apply {
                put("coldStartMs", startupTimeMs)
            })
            put("frames", JSONObject().apply {
                put("fps", fps)
                put("avgFrameTimeMs", averageFrameTimeMs)
                put("jankFrames", jankFrames)
                put("jankPercentage", jankPercentage)
            })
            put("memory", JSONObject().apply {
                put("heapAllocatedMb", heapAllocatedMb)
                put("heapMaxMb", heapMaxMb)
                put("nativeHeapAllocatedMb", nativeHeapAllocatedMb)
            })
            put("search", JSONObject().apply {
                put("lastLatencyMs", searchLatencyMs)
                put("avgLatencyMs", avgSearchLatencyMs)
            })
            put("cache", JSONObject().apply {
                put("iconCount", iconCacheCount)
                put("sizeBytes", iconCacheSizeBytes)
                put("maxSizeBytes", iconCacheMaxSizeBytes)
                put("memoryHits", iconMemoryHits)
                put("diskHits", iconDiskHits)
            })
            put("database", JSONObject().apply {
                put("sizeBytes", databaseSizeBytes)
                put("appsIndexed", appCount)
                put("widgetsPlaced", widgetCount)
                put("workspaces", workspaceCount)
                put("riceProfiles", riceProfileCount)
            })
        }
        return root.toString(2)
    }

    /**
     * Formats into a Hyprland/Arch Linux fastfetch-style telemetry report.
     */
    fun toAsciiReport(): String {
        val dbKb = databaseSizeBytes / 1024
        val cacheKb = iconCacheSizeBytes / 1024
        return """
       /\_/\          hyprctl @ diagnostics
      ( o.o )         ----------------------
       > ^ <          OS: $androidVersion
      HYPRLAND        Host: $deviceModel
                      Mode: $performanceMode
                      
                      [PERFORMANCE METRICS]
                      Startup Time:   ${startupTimeMs}ms
                      FPS:            ${fps} fps
                      Frame Time:     ${averageFrameTimeMs}ms
                      Jank Frames:    $jankFrames (${jankPercentage}%)
                      Search Latency: ${searchLatencyMs}ms (avg: ${avgSearchLatencyMs}ms)
                      
                      [MEMORY & STORAGE]
                      Heap:           ${heapAllocatedMb}MB / ${heapMaxMb}MB
                      Native Heap:    ${nativeHeapAllocatedMb}MB
                      Icon Cache:     ${cacheKb}KB ($iconCacheCount icons, $iconMemoryHits hits)
                      Database Size:  ${dbKb}KB
                      
                      [LAUNCHER INVENTORY]
                      Indexed Apps:   $appCount
                      Active Widgets: $widgetCount
                      Workspaces:     $workspaceCount
                      Rice Profiles:  $riceProfileCount
        """.trimIndent()
    }
}
