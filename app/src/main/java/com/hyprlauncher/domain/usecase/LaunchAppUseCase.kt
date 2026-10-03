package com.hyprlauncher.domain.usecase

import com.hyprlauncher.core.platform.AppLauncher
import com.hyprlauncher.core.platform.LaunchResult
import javax.inject.Inject

class LaunchAppUseCase @Inject constructor(
    private val appLauncher: AppLauncher
) {
    operator fun invoke(packageName: String, activityName: String? = null): LaunchResult {
        return appLauncher.launchApp(packageName, activityName)
    }
}
