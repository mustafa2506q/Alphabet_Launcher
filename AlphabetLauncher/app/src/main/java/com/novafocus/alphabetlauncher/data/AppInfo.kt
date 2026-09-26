package com.novafocus.alphabetlauncher.data

import android.graphics.drawable.Drawable

/**
 * Immutable snapshot of one launchable app.
 * [firstLetter] is precomputed once at load time so the touch path never
 * has to re-derive it (see AlphabetBar for why that matters at 60fps).
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val firstLetter: Char,
)
