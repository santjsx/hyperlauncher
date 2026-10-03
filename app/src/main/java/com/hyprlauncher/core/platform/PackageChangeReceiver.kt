package com.hyprlauncher.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hyprlauncher.domain.usecase.SyncPackageUpdateUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for handling package lifecycle events:
 * - Application installed
 * - Application updated
 * - Application removed
 * - Application changed
 * Conforms to PRD Section 13 (Application Indexing) and Section 34 (Event-driven background updates).
 */
@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncPackageUpdateUseCase: SyncPackageUpdateUseCase

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val uri = intent.data ?: return
        val packageName = uri.schemeSpecificPart ?: return

        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                scope.launch {
                    syncPackageUpdateUseCase.handlePackageAddedOrUpdated(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    scope.launch {
                        syncPackageUpdateUseCase.handlePackageRemoved(packageName)
                    }
                }
            }
        }
    }
}
