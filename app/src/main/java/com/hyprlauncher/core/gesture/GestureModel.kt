package com.hyprlauncher.core.gesture

/**
 * Supported gesture types conforming to PRD Section 18 & 19.
 */
enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    LONG_PRESS
}

/**
 * Structured launcher actions mappable to gestures (PRD Section 19).
 */
sealed interface LauncherAction {
    data object OpenAppDrawer : LauncherAction
    data object OpenSearch : LauncherAction
    data object NextWorkspace : LauncherAction
    data object PreviousWorkspace : LauncherAction
    data object OpenSettings : LauncherAction
    data object OpenCommandPalette : LauncherAction
    data object OpenNotificationShade : LauncherAction
    data object LockDevice : LauncherAction
    data class LaunchApplication(val packageName: String) : LauncherAction
    data object None : LauncherAction

    fun serialize(): String = when (this) {
        is OpenAppDrawer -> "OPEN_APP_DRAWER"
        is OpenSearch -> "OPEN_SEARCH"
        is NextWorkspace -> "NEXT_WORKSPACE"
        is PreviousWorkspace -> "PREVIOUS_WORKSPACE"
        is OpenSettings -> "OPEN_SETTINGS"
        is OpenCommandPalette -> "OPEN_COMMAND_PALETTE"
        is OpenNotificationShade -> "OPEN_NOTIFICATION_SHADE"
        is LockDevice -> "LOCK_DEVICE"
        is LaunchApplication -> "LAUNCH_APP:$packageName"
        is None -> "NONE"
    }

    companion object {
        fun deserialize(value: String): LauncherAction = when {
            value == "OPEN_APP_DRAWER" -> OpenAppDrawer
            value == "OPEN_SEARCH" -> OpenSearch
            value == "NEXT_WORKSPACE" -> NextWorkspace
            value == "PREVIOUS_WORKSPACE" -> PreviousWorkspace
            value == "OPEN_SETTINGS" -> OpenSettings
            value == "OPEN_COMMAND_PALETTE" -> OpenCommandPalette
            value == "OPEN_NOTIFICATION_SHADE" -> OpenNotificationShade
            value == "LOCK_DEVICE" -> LockDevice
            value.startsWith("LAUNCH_APP:") -> LaunchApplication(value.substringAfter("LAUNCH_APP:"))
            else -> None
        }
    }
}
