package dev.pukan.metroprague

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.pukan.metroprague.ui.screens.MainScreen
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }
    }
}
