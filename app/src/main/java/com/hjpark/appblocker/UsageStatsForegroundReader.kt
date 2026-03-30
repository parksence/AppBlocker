package com.hjpark.appblocker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * 최근 시간 창에서 마지막으로 전면(MOVE_TO_FOREGROUND / ACTIVITY_RESUMED)으로 올라온 패키지.
 * 접근성 이벤트가 OEM에서 끊길 때 보조용.
 */
internal object UsageStatsForegroundReader {

    private const val WINDOW_MS = 45_000L

    fun lastForegroundPackage(context: Context): String? {
        if (!PermissionHelper.hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - WINDOW_MS
        val events = usm.queryEvents(begin, end) ?: return null
        val ev = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> last = ev.packageName
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    ) {
                        last = ev.packageName
                    }
                }
            }
        }
        return last?.takeIf { it.isNotBlank() }
    }
}
