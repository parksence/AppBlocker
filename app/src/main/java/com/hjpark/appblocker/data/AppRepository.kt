package com.hjpark.appblocker.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
)

class AppRepository(
    private val context: Context,
    private val appDao: AppDao,
) {
    val blockedPackagesFlow: Flow<Set<String>> = appDao.observeBlockedApps()
        .map { list -> list.map { it.packageName }.toSet() }

    suspend fun loadUserInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<InstalledAppInfo>()
        for (info in activities) {
            val pkg = info.activityInfo.packageName
            if (!seen.add(pkg)) continue
            if (pkg == context.packageName) continue
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }
            if (!isUserVisibleApp(appInfo)) continue
            val label = pm.getApplicationLabel(appInfo).toString()
            result.add(InstalledAppInfo(packageName = pkg, label = label))
        }
        return result.sortedBy { it.label.lowercase() }
    }

    private fun isUserVisibleApp(appInfo: ApplicationInfo): Boolean {
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val updatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystem || updatedSystem
    }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        if (blocked) {
            appDao.upsert(BlockedApp(packageName = packageName, isBlocked = true))
        } else {
            appDao.deleteByPackage(packageName)
        }
    }

    suspend fun getBlockedPackages(): Set<String> =
        appDao.getBlockedApps().map { it.packageName }.toSet()
}
