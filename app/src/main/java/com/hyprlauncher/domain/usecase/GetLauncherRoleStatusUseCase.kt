package com.hyprlauncher.domain.usecase

import android.content.Intent
import com.hyprlauncher.core.platform.LauncherRoleManager
import javax.inject.Inject

data class LauncherRoleStatus(
    val isDefault: Boolean,
    val requestIntent: Intent?
)

class GetLauncherRoleStatusUseCase @Inject constructor(
    private val roleManager: LauncherRoleManager
) {
    operator fun invoke(): LauncherRoleStatus {
        val isDefault = roleManager.isDefaultLauncher()
        val intent = if (!isDefault) roleManager.createRequestDefaultLauncherIntent() else null
        return LauncherRoleStatus(
            isDefault = isDefault,
            requestIntent = intent
        )
    }
}
