package com.novafocus.alphabetlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.novafocus.alphabetlauncher.data.AppInfo
import com.novafocus.alphabetlauncher.data.FavouritesStore
import com.novafocus.alphabetlauncher.data.InstalledAppsRepository
import com.novafocus.alphabetlauncher.ui.LauncherScreen
import com.novafocus.alphabetlauncher.ui.LauncherViewModel
import com.novafocus.alphabetlauncher.ui.theme.AlphabetLauncherTheme

private const val MAX_RECENT_APPS = 8

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return LauncherViewModel(
                    InstalledAppsRepository(applicationContext),
                    FavouritesStore(applicationContext),
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Follows the system theme once, then lets the user override it
            // with the toggle button - rememberSaveable so it survives rotation.
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            var initialized by rememberSaveable { mutableStateOf(false) }
            val systemDark = isSystemInDarkTheme()
            if (!initialized) {
                isDarkTheme = systemDark
                initialized = true
            }

            // Newest launched app first, capped so the list doesn't grow forever.
            val recentApps = remember { mutableStateListOf<AppInfo>() }

            AlphabetLauncherTheme(darkTheme = isDarkTheme) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LauncherScreen(
                    state = state,
                    onLetterSelected = viewModel::onLetterSelected,
                    onPressChanged = viewModel::onPressChanged,
                    onLaunchApp = { app ->
                        recentApps.removeAll { it.packageName == app.packageName } // move to top if already there
                        recentApps.add(0, app)
                        if (recentApps.size > MAX_RECENT_APPS) recentApps.removeAt(recentApps.lastIndex)
                        launchApp(app)
                    },
                    onToggleFavourite = viewModel::onToggleFavourite,
                    recentApps = recentApps,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                )
            }
        }
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
