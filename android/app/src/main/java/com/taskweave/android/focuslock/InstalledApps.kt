package com.taskweave.android.focuslock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

/** Enumerates apps that have a launcher icon — the ones worth blocking. */
@Singleton
class InstalledApps @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun launchableApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("QueryPermissionsNeeded")
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .mapNotNull { pkg ->
                runCatching {
                    val ai = pm.getApplicationInfo(pkg, 0)
                    LaunchableApp(
                        packageName = pkg,
                        label = pm.getApplicationLabel(ai).toString(),
                        icon = pm.getApplicationIcon(ai),
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
