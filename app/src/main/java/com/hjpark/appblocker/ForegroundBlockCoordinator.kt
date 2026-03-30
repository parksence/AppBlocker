package com.hjpark.appblocker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hjpark.appblocker.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOG_TAG = "AppBlocker"

/**
 * 접근성 + 사용 기록 폴링이 **같은 세션·스로틀**을 쓰도록 공유한다.
 */
internal object BlockingSession {

    @Volatile
    var lastLockedSessionPackage: String? = null

    val lock = Any()

    @Volatile
    var lastLockPackage: String? = null

    @Volatile
    var lastLockTimeMs: Long = 0L
}

/**
 * 전면 후보 패키지가 차단 목록이면 잠금 UI를 띄운다.
 *
 * @param source 로그용 (`a11y`, `usage_poll` 등)
 */
internal suspend fun evaluateForegroundPackageCandidate(
    appContext: Context,
    pkg: String,
    source: String,
) {
    val selfPkg = appContext.packageName
    if (!SystemPackageFilter.isUserForegroundCandidate(pkg, selfPkg)) {
        synchronized(BlockingSession.lock) { BlockingSession.lastLockedSessionPackage = null }
        Log.d(LOG_TAG, "[$source] 후보 제외: $pkg")
        return
    }

    val dao = AppDatabase.getInstance(appContext).appDao()
    val blocked = dao.getBlockedApps().map { it.packageName }.toSet()
    Log.d(LOG_TAG, "[$source] 차단=$blocked | 현재=$pkg | 매칭=${blocked.contains(pkg)}")
    if (!blocked.contains(pkg)) {
        synchronized(BlockingSession.lock) { BlockingSession.lastLockedSessionPackage = null }
        return
    }

    val shouldShow = synchronized(BlockingSession.lock) {
        if (LockSuppressor.shouldSuppressForegroundLock()) {
            return@synchronized false to "잠금 생략(홈 직후)"
        }
        if (pkg == BlockingSession.lastLockedSessionPackage) {
            return@synchronized false to "동일 차단 앱 세션"
        }
        if (shouldThrottleLock(pkg)) {
            return@synchronized false to "스로틀"
        }
        BlockingSession.lastLockedSessionPackage = pkg
        true to null
    }
    if (!shouldShow.first) {
        shouldShow.second?.let { Log.d(LOG_TAG, "[$source] $it") }
        return
    }

    Log.i(LOG_TAG, "[$source] 잠금 표시 → $pkg")
    withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Settings.canDrawOverlays(appContext)
        ) {
            LockOverlayManager.show(appContext, pkg)
        } else {
            appContext.startActivity(
                Intent(appContext, LockActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    putExtra(LockActivity.EXTRA_BLOCKED_PACKAGE, pkg)
                },
            )
        }
    }
}

private fun shouldThrottleLock(pkg: String): Boolean {
    if (!LockOverlayManager.isShowing()) {
        BlockingSession.lastLockPackage = pkg
        BlockingSession.lastLockTimeMs = System.currentTimeMillis()
        return false
    }
    val now = System.currentTimeMillis()
    if (pkg == BlockingSession.lastLockPackage && now - BlockingSession.lastLockTimeMs < 900L) {
        return true
    }
    BlockingSession.lastLockPackage = pkg
    BlockingSession.lastLockTimeMs = now
    return false
}
