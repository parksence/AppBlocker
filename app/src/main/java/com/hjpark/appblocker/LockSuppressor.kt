package com.hjpark.appblocker

import android.os.SystemClock

/**
 * 「홈으로 돌아가기」 직후 접근성이 잠깐 이전 전면 앱을 다시 보내는 경우가 있어,
 * 그 짧은 구간에서 잠금이 한 번 더 뜨는 것을 줄인다.
 */
object LockSuppressor {

    @Volatile
    private var suppressUntilElapsed: Long = 0L

    fun onUserRequestedGoHome(durationMs: Long = 600L) {
        suppressUntilElapsed = SystemClock.elapsedRealtime() + durationMs
    }

    fun shouldSuppressForegroundLock(): Boolean =
        SystemClock.elapsedRealtime() < suppressUntilElapsed
}
