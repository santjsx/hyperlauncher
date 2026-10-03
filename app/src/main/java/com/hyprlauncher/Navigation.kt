package com.hyprlauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
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
                    onAppClick = { pkg, act -> viewModel.launchApp(pkg, act) },
                    onSetDefaultLauncher = {
                        uiState.requestDefaultIntent?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    loadIcon = { pkg -> viewModel.getAppIcon(pkg) }
                )
            }
        }
    )
}
