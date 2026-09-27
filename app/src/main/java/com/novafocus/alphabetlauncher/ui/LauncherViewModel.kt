package com.novafocus.alphabetlauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafocus.alphabetlauncher.data.AppInfo
import com.novafocus.alphabetlauncher.data.FavouritesStore
import com.novafocus.alphabetlauncher.data.InstalledAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val favourites: List<AppInfo> = emptyList(),
    val letterHasApps: Set<Char> = emptySet(), // which letters have at least one app
    val selectedLetter: Char? = null,
    val isDragging: Boolean = false,
) {
    // Apps matching whichever letter is currently selected, sorted A-Z.
    val appsForSelectedLetter: List<AppInfo>
        get() = selectedLetter?.let { letter ->
            allApps.filter { it.firstLetter == letter }.sortedBy { it.label.lowercase() }
        } ?: emptyList()
}

class LauncherViewModel(
    private val repository: InstalledAppsRepository,
    private val favouritesStore: FavouritesStore,
    private val fallbackHints: List<String> = DEFAULT_FAVOURITE_HINTS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        val apps = repository.loadApps()
        applyApps(apps)

        // Refresh if the user installs/removes something while the app is open.
        viewModelScope.launch {
            repository.observePackageChanges().collect { apps -> applyApps(apps) }
        }
    }

    fun onLetterSelected(letter: Char?) {
        if (letter != _uiState.value.selectedLetter) {
            _uiState.value = _uiState.value.copy(selectedLetter = letter)
        }
    }

    fun onPressChanged(pressed: Boolean) {
        _uiState.value = _uiState.value.copy(
            isDragging = pressed,
            selectedLetter = if (pressed) _uiState.value.selectedLetter else null,
        )
    }

    // Long-press on an app row calls this. Persists immediately so it
    // survives the app being closed and reopened.
    fun onToggleFavourite(app: AppInfo) {
        favouritesStore.toggleFavourite(app.packageName)
        applyApps(_uiState.value.allApps)
    }

    private fun applyApps(apps: List<AppInfo>) {
        _uiState.value = _uiState.value.copy(
            allApps = apps,
            favourites = pickFavourites(apps),
            letterHasApps = apps.map { it.firstLetter }.toSet(),
        )
    }

    // User's own long-pressed favourites win if there are any. Otherwise
    // fall back to a guess at common apps, or just the first few installed.
    private fun pickFavourites(apps: List<AppInfo>): List<AppInfo> {
        val saved = favouritesStore.getFavourites()
        if (saved.isNotEmpty()) {
            return apps.filter { it.packageName in saved }.sortedBy { it.label.lowercase() }
        }
        val byPackage = apps.associateBy { it.packageName }
        val matched = fallbackHints.mapNotNull { byPackage[it] }
        return if (matched.size >= 5) matched.take(7) else apps.take(6)
    }

    companion object {
        private val DEFAULT_FAVOURITE_HINTS = listOf(
            "com.android.dialer",
            "com.android.mms",
            "com.android.chrome",
            "com.google.android.gm",
            "com.android.camera2",
            "com.google.android.apps.photos",
            "com.android.settings",
        )
    }
}
