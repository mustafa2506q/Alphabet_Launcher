package com.novafocus.alphabetlauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafocus.alphabetlauncher.data.AppInfo
import com.novafocus.alphabetlauncher.data.InstalledAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val favourites: List<AppInfo> = emptyList(),
    val selectedLetter: Char? = null,
    val isDragging: Boolean = false,
) {
    /** Requirement 6/7: filtered, sorted list for the selected letter, with an empty state. */
    val appsForSelectedLetter: List<AppInfo>
        get() = selectedLetter?.let { letter ->
            allApps.filter { it.firstLetter == letter }.sortedBy { it.label.lowercase() }
        } ?: emptyList()
}

class LauncherViewModel(
    private val repository: InstalledAppsRepository,
    favouritePackageNames: List<String> = DEFAULT_FAVOURITE_HINTS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val favouriteHints = favouritePackageNames

    init {
        // Load once at startup — this is the *only* place PackageManager is queried
        // outside of the live-update listener below (requirement #10).
        val apps = repository.loadApps()
        _uiState.value = _uiState.value.copy(
            allApps = apps,
            favourites = pickFavourites(apps),
        )

        viewModelScope.launch {
            repository.observePackageChanges().collect { refreshedApps ->
                _uiState.value = _uiState.value.copy(
                    allApps = refreshedApps,
                    favourites = pickFavourites(refreshedApps),
                )
            }
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

    private fun pickFavourites(apps: List<AppInfo>): List<AppInfo> {
        val byPackage = apps.associateBy { it.packageName }
        val matched = favouriteHints.mapNotNull { byPackage[it] }
        return if (matched.size >= 5) matched.take(7) else apps.take(6)
    }

    companion object {
        // Best-effort common package names; falls back to "first N installed
        // apps" per the assignment's own wording if none of these are present.
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
