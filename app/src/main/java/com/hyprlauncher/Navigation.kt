package com.hyprlauncher

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hyprlauncher.feature.diagnostics.DiagnosticsScreen
import com.hyprlauncher.feature.diagnostics.DiagnosticsViewModel
import com.hyprlauncher.feature.drawer.DrawerScreen
import com.hyprlauncher.feature.drawer.DrawerViewModel
import com.hyprlauncher.feature.home.HomeScreen
import com.hyprlauncher.feature.home.HomeViewModel
import com.hyprlauncher.feature.home.WidgetPlacementRequest
import com.hyprlauncher.feature.rice.RiceStudioScreen
import com.hyprlauncher.feature.rice.RiceStudioViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(HomeKey)
    val context = LocalContext.current

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<HomeKey> {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                // Auto-refresh role status when returning to app
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshRoleStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                var pendingBindReq by remember { mutableStateOf<WidgetPlacementRequest.BindPermissionNeeded?>(null) }
                var pendingConfigureReq by remember { mutableStateOf<WidgetPlacementRequest.ConfigureNeeded?>(null) }

                val roleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.refreshRoleStatus()
                }

                val bindWidgetLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val req = pendingBindReq
                    if (req != null) {
                        viewModel.onWidgetBindResult(result.resultCode == Activity.RESULT_OK, req)
                        pendingBindReq = null
                    }
                }

                val configureWidgetLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val req = pendingConfigureReq
                    if (req != null) {
                        viewModel.onWidgetConfigureResult(result.resultCode == Activity.RESULT_OK, req)
                        pendingConfigureReq = null
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.widgetEvents.collect { event ->
                        when (event) {
                            is WidgetPlacementRequest.BindPermissionNeeded -> {
                                pendingBindReq = event
                                runCatching { bindWidgetLauncher.launch(event.intent) }
                                    .onFailure { viewModel.onWidgetBindResult(false, event) }
                            }
                            is WidgetPlacementRequest.ConfigureNeeded -> {
                                pendingConfigureReq = event
                                runCatching { configureWidgetLauncher.launch(event.intent) }
                                    .onFailure { viewModel.onWidgetConfigureResult(false, event) }
                            }
                        }
                    }
                }

                HomeScreen(
                    uiState = uiState,
                    onWorkspaceSelected = viewModel::onWorkspaceSelected,
                    onSearchQueryChange = viewModel::onSearchQueryChanged,
                    widgetHostManager = viewModel.widgetHostManager,
                    onPlaceWidget = viewModel::placeWidget,
                    onResizeWidget = viewModel::resizeWidget,
                    onRemoveWidget = viewModel::removeWidget,
                    onOpenDrawer = { backStack.add(DrawerKey) },
                    onOpenRiceStudio = { backStack.add(RiceStudioKey) },
                    onOpenDiagnostics = { backStack.add(DiagnosticsKey) },
                    onGesture = { gesture ->
                        viewModel.onGestureTriggered(gesture, onOpenDrawer = { backStack.add(DrawerKey) })
                    },
                    onCreateWorkspace = viewModel::createWorkspace,
                    onRenameWorkspace = viewModel::renameWorkspace,
                    onDeleteWorkspace = viewModel::deleteWorkspace,
                    onSelectThemePreset = viewModel::selectTheme,
                    onSelectAnimationScale = viewModel::setAnimationScale,
                    onSelectCornerRadius = viewModel::setCornerRadius,
                    onUpdateLayout = viewModel::updateLayoutConfig,
                    onUpdateDock = viewModel::updateDockConfig,
                    onUpdateGrid = viewModel::updateAppGridConfig,
                    onUpdateSearch = viewModel::updateSearchConfig,
                    onUpdateTypography = viewModel::updateTypographyConfig,
                    onUpdateIcons = viewModel::updateIconConfig,
                    onPinApp = viewModel::pinAppToDock,
                    onUnpinApp = viewModel::unpinAppFromDock,
                    onResetCustomizationDefaults = viewModel::resetCustomizationDefaults,
                    onAppClick = { pkg, act -> viewModel.launchApp(pkg, act) },
                    onSetDefaultLauncher = {
                        uiState.requestDefaultIntent?.let { intent ->
                            runCatching {
                                roleLauncher.launch(intent)
                            }.onFailure {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            }
                        }
                    },
                    loadIcon = viewModel::getAppIcon
                )
            }
            entry<DrawerKey> {
                val drawerViewModel: DrawerViewModel = hiltViewModel()
                val drawerState by drawerViewModel.uiState.collectAsStateWithLifecycle()
                DrawerScreen(
                    uiState = drawerState,
                    onSearchQueryChange = drawerViewModel::onSearchQueryChanged,
                    onSelectTab = drawerViewModel::onSelectTab,
                    onAppClick = { pkg, act ->
                        drawerViewModel.launchApp(pkg, act)
                        backStack.removeLastOrNull()
                    },
                    onToggleFavorite = drawerViewModel::toggleFavorite,
                    onDismiss = { backStack.removeLastOrNull() },
                    loadIcon = drawerViewModel::getAppIcon
                )
            }
            entry<RiceStudioKey> {
                val studioViewModel: RiceStudioViewModel = hiltViewModel()
                RiceStudioScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
            entry<DiagnosticsKey> {
                val diagViewModel: DiagnosticsViewModel = hiltViewModel()
                DiagnosticsScreen(
                    viewModel = diagViewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
