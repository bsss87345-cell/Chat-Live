package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

    // معرّف المنشور القادم من رابط خارجي، بانتظار جاهزية الـViewModel
    private var pendingDeepLinkPostId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
                val splashScreen = installSplashScreen()
               super.onCreate(savedInstanceState)
                LocalStore.initialize(this)

        // 🔑 تفعيل مخزن الجلسة المشفّر (Keystore) — بدونه لا تُحفظ الجلسة ولا تُستعاد
        com.example.data.SecureSessionStore.initialize(this)
                
        // الفتح البارد: الرابط يجي مع نية الإطلاق
        pendingDeepLinkPostId = extractPostId(intent)
        // التطبيق شغّال أصلاً: يصل الرابط كنية جديدة
        addOnNewIntentListener { newIntent ->
            pendingDeepLinkPostId = extractPostId(newIntent)
        }
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
            val online by applicationContext.observeInternet().collectAsState(initial = true)

            // الرابط يُقرأ بالنشاط، ويُمرَّر للـViewModel هنا حيث يكون جاهزاً
            LaunchedEffect(pendingDeepLinkPostId) {
                pendingDeepLinkPostId?.let { postId ->
                    viewModel.openPostFromLink(postId)
                    pendingDeepLinkPostId = null
                }
            }
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

        override fun onStop() {
        super.onStop()
        // كتابة فورية لآخر التغييرات بدل انتظار التجميع (300ms)،
        // حتى لا تضيع آخر عملية لو أغلق المستخدم التطبيق مباشرة بعدها.
        LocalStore.flush()
    }

    /** يستخرج معرّف المنشور من رابط بصيغة chatlive://post/<postId> */
    private fun extractPostId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "chatlive" || data.host != "post") return null
        return data.lastPathSegment?.takeIf { it.isNotBlank() }
    }
}
