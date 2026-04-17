package com.heartape.whisper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.heartape.whisper.component.WhisperForceUpdateDialog
import com.heartape.whisper.presentation.navigation.AppNavHost
import com.heartape.whisper.presentation.viewmodel.MainViewModel
import com.heartape.whisper.repository.SettingsRepository
import com.heartape.whisper.utils.AppStoreNavigator
import com.heartape.whisper.utils.animateColorSchemeAsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by settingsRepo.isDarkMode.collectAsState()
            val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            val animatedColorScheme = animateColorSchemeAsState(colorScheme)

            MaterialTheme(colorScheme = animatedColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val startDest = if (settingsRepo.prefsManager.isLoggedIn()) {
                        "main"
                    } else {
                        "login"
                    }

                    AppNavHost(navController = navController, startDestination = startDest)
                    GlobalForceUpdateOverlay()
                }
            }
        }
    }
}

/**
 * 全局更新遮罩 (带状态的容器组件)
 * 直接读取 ViewModel 状态并将其映射给底层的无状态弹窗
 */
@Composable
fun GlobalForceUpdateOverlay(
    viewModel: MainViewModel = hiltViewModel()
) {
    val forceUpdateInfo by viewModel.forceUpdateInfo.collectAsState()
    val context = LocalContext.current

    forceUpdateInfo?.let { info ->
        WhisperForceUpdateDialog(
            versionName = info.versionName,
            releaseNotes = info.releaseNotes,
            // ★ 直接拉起商店
            onGoToStore = { AppStoreNavigator.goToStore(context, info.downloadUrl, viewModel.appConfig.fallbackWebUrl) }
        )
    }
}