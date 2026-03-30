package com.hjpark.appblocker

import android.app.ActivityManager
import android.content.Context

private const val PREFS_NAME = "app_blocker_status"
private const val KEY_SERVICE_MARKED_RUNNING = "service_marked_running"

/**
 * 포그라운드 서비스 실행 여부(시스템 조회 + 로컬 플래그).
 * 일부 기기에서 getRunningServices가 제한될 수 있어 플래그를 함께 쓴다.
 */
@Suppress("DEPRECATION")
fun Context.isBlockingServiceRunning(): Boolean {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val fromSystem = am.getRunningServices(64).any { info ->
        info.service.className == AppBlockerService::class.java.name
    }
    val fromFlag = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SERVICE_MARKED_RUNNING, false)
    return fromSystem || fromFlag
}

internal fun Context.setBlockingServiceRunningFlag(running: Boolean) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_SERVICE_MARKED_RUNNING, running)
        .apply()
}
