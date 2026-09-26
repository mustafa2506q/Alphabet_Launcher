package com.novafocus.alphabetlauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Loads the device's launchable apps exactly once, caches the result, and
 * exposes cheap re-reads plus a live-update stream for install/uninstall
 * events (bonus requirement).
 *
 * Requirement #10 (Performance) lives here: everything downstream — the
 * curve animation, the letter bubble, the filtered list — reads from
 * [cachedApps], a plain in-memory list. Nothing on the touch/drag path
 * ever calls into PackageManager.
 */
class InstalledAppsRepository(private val appContext: Context) {

    /** Populated once by [loadApps]; safe to read from any thread after that. */
    @Volatile
    private var cachedApps: List<AppInfo> = emptyList()

    val apps: List<AppInfo> get() = cachedApps

    /**
     * Queries every activity that resolves ACTION_MAIN / CATEGORY_LAUNCHER.
     *
     * Package-visibility note (Android 11+): this requires QUERY_ALL_PACKAGES
     * (declared in AndroidManifest). A CATEGORY_HOME intent filter alone is
     * NOT a reliable substitute — without the permission, queryIntentActivities()
     * only returns a small set of always-visible packages, not third-party apps.
     */
    fun loadApps(): List<AppInfo> {
        val pm = appContext.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        val result = resolved
            .asSequence()
            .mapNotNull { resolveInfo ->
                val label = resolveInfo.loadLabel(pm)?.toString()?.trim()
                if (label.isNullOrEmpty()) return@mapNotNull null

                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                // Skip ourselves so "Alphabet Launcher" doesn't list itself under A.
                if (packageName == appContext.packageName) return@mapNotNull null

                val icon = resolveInfo.loadIcon(pm) ?: return@mapNotNull null
                val firstLetter = label.first().uppercaseChar()
                    .let { if (it in 'A'..'Z') it else '#' }

                AppInfo(
                    label = label,
                    packageName = packageName,
                    icon = icon,
                    firstLetter = firstLetter,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()

        cachedApps = result
        return result
    }

    /**
     * Bonus: re-runs [loadApps] whenever an app is installed, removed, or
     * has its package data changed/replaced, without ever touching
     * PackageManager on the animation/touch path.
     */
    fun observePackageChanges(): Flow<List<AppInfo>> = callbackFlow {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(loadApps())
            }
        }
        appContext.registerReceiver(receiver, filter)
        // Emit the current cache immediately so collectors get a value right away.
        trySend(cachedApps)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}