package com.hyprlauncher.core.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.hyprlauncher.data.database.dao.AppDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LaunchResult {
    data object Success : LaunchResult
    data object AppNotFound : LaunchResult
    data class SecurityError(val message: String) : LaunchResult
    data class Error(val throwable: Throwable) : LaunchResult
}

interface AppLauncher {
    fun launchApp(packageName: String, activityName: String? = null): LaunchResult
}

@Singleton
class DefaultAppLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) : AppLauncher {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun launchApp(packageName: String, activityName: String?): LaunchResult {
        return try {
            val intent = if (!activityName.isNullOrBlank()) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                    ?: return LaunchResult.AppNotFound
                launchIntent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            }

            context.startActivity(intent)

            // Record launch metric asynchronously in local index
            applicationScope.launch {
                runCatching {
                    appDao.recordAppLaunch(packageName, System.currentTimeMillis())
                }
            }

            LaunchResult.Success
        } catch (e: ActivityNotFoundException) {
            LaunchResult.AppNotFound
        } catch (e: SecurityException) {
            LaunchResult.SecurityError(e.message ?: "Permission denied launching $packageName")
        } catch (t: Throwable) {
            LaunchResult.Error(t)
        }
    }
}
