package com.hyprlauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hyprlauncher.feature.drawer.DrawerScreen
import com.hyprlauncher.feature.drawer.DrawerViewModel
import com.hyprlauncher.feature.home.HomeScreen
import com.hyprlauncher.feature.home.HomeViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(HomeKey)
    val context = LocalContext.current

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeKey> {
                val viewModel: HomeViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    uiState = uiState,
                    onWorkspaceSelected = viewModel::onWorkspaceSelected,
                    onSearchQueryChange = viewModel::onSearchQueryChanged,
                    onOpenDrawer = { backStack.add(DrawerKey) },
                    onGesture = { gesture ->
                        viewModel.onGestureTriggered(gesture, onOpenDrawer = { backStack.add(DrawerKey) })
                    },
                    onCreateWorkspace = viewModel::createWorkspace,
                    onRenameWorkspace = viewModel::renameWorkspace,
                    onDeleteWorkspace = viewModel::deleteWorkspace,
                    onAppClick = { pkg, act -> viewModel.launchApp(pkg, act) },
                    onSetDefaultLauncher = {
                        uiState.requestDefaultIntent?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    loadIcon = { pkg -> viewModel.getAppIcon(pkg) }
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
                    loadIcon = { pkg -> drawerViewModel.getAppIcon(pkg) }
                )
            }
        }
    )
}
