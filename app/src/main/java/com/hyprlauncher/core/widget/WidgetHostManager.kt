package com.hyprlauncher.core.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.hyprlauncher.domain.model.WidgetProviderItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom AppWidgetHostView with crash isolation (PRD Section 25: untrusted UI failure isolation).
 */
class HyprAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    override fun getErrorView(): View {
        return TextView(context).apply {
            text = "[widget error: provider failed]"
            setTextColor(android.graphics.Color.parseColor("#EF4444"))
            textSize = 11f
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#171A21"))
        }
    }
}

/**
 * Custom AppWidgetHost allocating HyprAppWidgetHostView instances.
 */
class HyprAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return HyprAppWidgetHostView(context)
    }
}

interface WidgetHostManager {
    fun startListening()
    fun stopListening()
    fun allocateAppWidgetId(): Int
    fun deleteAppWidgetId(appWidgetId: Int)
    fun getAvailableProviders(): List<WidgetProviderItem>
    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo?
    fun createView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView?
    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean
    fun createBindWidgetIntent(appWidgetId: Int, provider: ComponentName): Intent? = null
    fun createConfigureWidgetIntent(appWidgetId: Int, configureComponent: ComponentName): Intent? = null
}

@Singleton
class DefaultWidgetHostManager @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetHostManager {

    companion object {
        const val HYPR_HOST_ID = 2048
    }

    private val appWidgetManager: AppWidgetManager? = AppWidgetManager.getInstance(context)
    private val appWidgetHost = HyprAppWidgetHost(context, HYPR_HOST_ID)
    private val packageManager: PackageManager = context.packageManager
    private var isListening = false

    override fun startListening() {
        if (!isListening) {
            runCatching {
                appWidgetHost.startListening()
                isListening = true
            }
        }
    }

    override fun stopListening() {
        if (isListening) {
            runCatching {
                appWidgetHost.stopListening()
                isListening = false
            }
        }
    }

    override fun allocateAppWidgetId(): Int {
        return runCatching { appWidgetHost.allocateAppWidgetId() }.getOrDefault(-1)
    }

    override fun deleteAppWidgetId(appWidgetId: Int) {
        runCatching {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
    }

    override fun getAvailableProviders(): List<WidgetProviderItem> {
        val manager = appWidgetManager ?: return emptyList()
        val providers = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.getInstalledProvidersForProfile(Process.myUserHandle())
            } else {
                manager.installedProviders
            }
        }.getOrDefault(emptyList())

        return providers.mapNotNull { info ->
            runCatching {
                val appInfo = packageManager.getApplicationInfo(info.provider.packageName, 0)
                val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                val widgetLabel = info.loadLabel(packageManager)

                // Minimum cell estimation based on standard 70dp cell formula
                val minSpanX = ((info.minWidth + 30) / 70).coerceIn(1, 8)
                val minSpanY = ((info.minHeight + 30) / 70).coerceIn(1, 8)

                WidgetProviderItem(
                    providerPackage = info.provider.packageName,
                    providerClass = info.provider.className,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    minWidthDp = info.minWidth,
                    minHeightDp = info.minHeight,
                    minSpanX = minSpanX,
                    minSpanY = minSpanY,
                    previewImageRes = info.previewImage,
                    iconRes = info.icon,
                    configureActivity = info.configure?.className
                )
            }.getOrNull()
        }.sortedBy { it.appLabel.lowercase() }
    }

    override fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return runCatching { appWidgetManager?.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    override fun createView(
        context: Context,
        appWidgetId: Int,
        info: AppWidgetProviderInfo
    ): AppWidgetHostView? {
        return runCatching {
            appWidgetHost.createView(context, appWidgetId, info)
        }.getOrNull()
    }

    override fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean {
        val manager = appWidgetManager ?: return false
        return runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        }.getOrDefault(false)
    }

    override fun createBindWidgetIntent(appWidgetId: Int, provider: ComponentName): Intent? {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, Process.myUserHandle())
            }
        }
    }

    override fun createConfigureWidgetIntent(appWidgetId: Int, configureComponent: ComponentName): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configureComponent
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }
}
