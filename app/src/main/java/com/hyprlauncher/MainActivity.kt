package com.hyprlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyprlauncher.core.designsystem.theme.DefaultHyprColors
import com.hyprlauncher.core.designsystem.theme.DefaultHyprShapes
import com.hyprlauncher.core.designsystem.theme.DefaultHyprTypography
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.data.repository.ThemeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors by themeRepository.currentColors.collectAsStateWithLifecycle(initialValue = DefaultHyprColors)
            val shapes by themeRepository.currentShapes.collectAsStateWithLifecycle(initialValue = DefaultHyprShapes)
            val typography by themeRepository.currentTypography.collectAsStateWithLifecycle(initialValue = DefaultHyprTypography)

            HyprTheme(
                colors = colors,
                shapes = shapes,
                typography = typography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = HyprTheme.colors.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}
