package com.novafocus.alphabetlauncher.data

import android.content.Context

// Saves which apps the user long-pressed to favourite, so the list survives
// an app restart. Plain SharedPreferences is enough here - it's just a
// small set of package names, not real structured data.
class FavouritesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFavourites(): Set<String> =
        prefs.getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()

    fun toggleFavourite(packageName: String) {
        val current = getFavourites().toMutableSet()
        if (!current.remove(packageName)) current.add(packageName)
        prefs.edit().putStringSet(KEY_FAVOURITES, current).apply()
    }

    companion object {
        private const val PREFS_NAME = "alphabet_launcher_prefs"
        private const val KEY_FAVOURITES = "favourite_packages"
    }
}
