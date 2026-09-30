package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LocalStore
import com.example.ui.MainScreen
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SocialAppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
                val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        LocalStore.initialize(this)
        splashScreen.setOnExitAnimationListener { provider ->
            val fadeOut = android.animation.ObjectAnimator.ofFloat(
                provider.view,
                android.view.View.ALPHA,
                1f,
                0f
            )
            fadeOut.duration = 400L
            fadeOut.doOnEnd { provider.remove() }
            fadeOut.start()
        }
        enableEdgeToEdge(
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            val viewModel: SocialAppViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            var showSplash by remember { mutableStateOf(true) }
            MyApplicationTheme(darkTheme = isDarkMode) {
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
