package dev.pukan.metroprague

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.pukan.metroprague.ui.screens.MainScreen
import dev.pukan.metroprague.ui.theme.AppThemeViewModel
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import dev.pukan.metroprague.ui.theme.resolveTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appThemeViewModel: AppThemeViewModel = hiltViewModel()
            val themePreference by appThemeViewModel.themePreference.collectAsStateWithLifecycle()
            val darkTheme = resolveTheme(themePreference, isSystemInDarkTheme())

            MetroPragueTheme(darkTheme = darkTheme) {
                MainScreen()
            }
        }
    }
}
