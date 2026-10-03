package com.hyprlauncher.core.icon

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.hyprlauncher.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class CacheStats(
    val memoryHits: Int,
    val diskHits: Int,
    val systemFetches: Int,
    val memorySizeBytes: Int,
    val maxMemorySizeBytes: Int
)

/**
 * Two-level (Memory -> Disk -> PackageManager) Icon Cache conforming to PRD Section 16 & Section 33.
 */
interface IconCache {
    suspend fun getIcon(packageName: String, activityName: String? = null): Bitmap?
    suspend fun preloadIcons(packageNames: List<String>) {}
    suspend fun invalidate(packageName: String)
    suspend fun clear()
    fun trimMemory(level: Int) {}
    fun getStats(): CacheStats
}

@Singleton
class DefaultIconCache @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IconCache {

    private val packageManager: PackageManager = context.packageManager
    private val diskCacheDir: File = File(context.cacheDir, "icons").apply { mkdirs() }

    // Memory cache: Allocate 1/8th of runtime heap (in KB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(1024 * 8)
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemoryKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val memoryHits = AtomicInteger(0)
    private val diskHits = AtomicInteger(0)
    private val systemFetches = AtomicInteger(0)

    private val standardIconSize = 144 // 144x144 px standard high-DPI icon canvas

    override suspend fun preloadIcons(packageNames: List<String>) = withContext(ioDispatcher) {
        packageNames.forEach { pkg ->
            val inMemory = synchronized(memoryCache) { memoryCache.get(pkg) != null }
            if (!inMemory) {
                getIcon(pkg)
            }
        }
    }

    override suspend fun getIcon(packageName: String, activityName: String?): Bitmap? = withContext(ioDispatcher) {
        val cacheKey = packageName

        // Level 1: Memory Cache
        synchronized(memoryCache) {
            val cached = memoryCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                memoryHits.incrementAndGet()
                return@withContext cached
            }
        }

        // Level 2: Disk Cache
        val diskFile = getDiskFile(packageName)
        if (diskFile.exists() && diskFile.length() > 0) {
            val diskBitmap = runCatching {
                BitmapFactory.decodeFile(diskFile.absolutePath)
            }.getOrNull()

            if (diskBitmap != null) {
                diskHits.incrementAndGet()
                synchronized(memoryCache) {
                    memoryCache.put(cacheKey, diskBitmap)
                }
                return@withContext diskBitmap
            }
        }

        // Level 3: Android PackageManager
        systemFetches.incrementAndGet()
        val rawDrawable = runCatching {
            packageManager.getApplicationIcon(packageName)
        }.getOrNull() ?: return@withContext null

        val renderedBitmap = drawableToBitmap(rawDrawable, standardIconSize, standardIconSize)

        // Save to Memory Cache
        synchronized(memoryCache) {
            memoryCache.put(cacheKey, renderedBitmap)
        }

        // Save to Disk Cache
        runCatching {
            FileOutputStream(diskFile).use { out ->
                renderedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        renderedBitmap
    }

    override suspend fun invalidate(packageName: String) = withContext(ioDispatcher) {
        synchronized(memoryCache) {
            memoryCache.remove(packageName)
        }
        val file = getDiskFile(packageName)
        if (file.exists()) {
            file.delete()
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        runCatching {
            diskCacheDir.deleteRecursively()
            diskCacheDir.mkdirs()
        }
        Unit
    }

    override fun trimMemory(level: Int) {
        synchronized(memoryCache) {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    memoryCache.evictAll()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                    memoryCache.trimToSize(memoryCache.maxSize() / 2)
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    memoryCache.trimToSize(memoryCache.maxSize() * 3 / 4)
                }
            }
        }
    }

    override fun getStats(): CacheStats {
        return synchronized(memoryCache) {
            CacheStats(
                memoryHits = memoryHits.get(),
                diskHits = diskHits.get(),
                systemFetches = systemFetches.get(),
                memorySizeBytes = memoryCache.size() * 1024,
                maxMemorySizeBytes = memoryCache.maxSize() * 1024
            )
        }
    }

    private fun getDiskFile(packageName: String): File {
        val sanitized = packageName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(diskCacheDir, "$sanitized.png")
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val src = drawable.bitmap
            if (src.width == width && src.height == height) {
                return src
            }
        }

        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
