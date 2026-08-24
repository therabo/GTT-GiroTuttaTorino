package it.girotuttatorino.gtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import it.girotuttatorino.gtt.ui.intro.AppIntroScreen
import it.girotuttatorino.gtt.ui.tickets.TicketsScreen
import it.girotuttatorino.gtt.ui.theme.GTTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            var showIntro by rememberSaveable { mutableStateOf(true) }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = showIntro
                    isAppearanceLightNavigationBars = showIntro
                }
            }

            GTTTheme(darkTheme = false) {
                if (showIntro) {
                    AppIntroScreen(onFinished = { showIntro = false })
                } else {
                    TicketsScreen()
                }
            }
        }
    }
}
