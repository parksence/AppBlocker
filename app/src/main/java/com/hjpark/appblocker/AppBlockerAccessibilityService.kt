package com.hjpark.appblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 차단 목록 앱의 **전면 창으로 실제 전환되었을 때만** 잠금을 건다.
 *
 * - [TYPE_WINDOW_STATE_CHANGED]: 일반 앱 전환
 * - [TYPE_WINDOWS_CHANGED]: 분할 화면·팝업 등
 *
 * 일부 기기에서 이벤트가 거의 오지 않으면 [AppBlockerService]의 사용 기록 폴링이 보조한다.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()

    companion object {
        private const val TAG = "A11Y_EVENT"
        private const val APP_TAG = "AppBlocker"

        @Volatile
        private var instance: AppBlockerAccessibilityService? = null

        fun performGlobalHomeIfPossible(): Boolean {
            val s = instance ?: return false
            return s.performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private val scope = CoroutineScope(
        job + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "코루틴 예외(앱 종료 방지)", throwable)
        },
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        setServiceInfo(info)
        Log.i(APP_TAG, "접근성 서비스 연결됨 (전면 이벤트 수신)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = foregroundPackageFromWindowState(event) ?: return
                Log.d(TAG, "WINDOW_STATE_CHANGED 전면 패키지: $pkg")
                scope.launch {
                    evaluateForegroundPackageCandidate(applicationContext, pkg, "a11y_state")
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
                val pkg = focusedApplicationPackage() ?: packageFromActiveAccessibilityRoot() ?: return
                Log.d(TAG, "WINDOWS_CHANGED 전면 패키지: $pkg")
                scope.launch {
                    evaluateForegroundPackageCandidate(applicationContext, pkg, "a11y_windows")
                }
            }
            else -> Unit
        }
    }

    private fun foregroundPackageFromWindowState(event: AccessibilityEvent): String? {
        event.packageName?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        return packageFromActiveAccessibilityRoot()
    }

    private fun packageFromActiveAccessibilityRoot(): String? {
        val root = rootInActiveWindow ?: return null
        return try {
            root.packageName?.toString()?.takeIf { it.isNotBlank() }
        } finally {
            root.recycle()
        }
    }

    private fun focusedApplicationPackage(): String? {
        val wins = windows ?: return null
        var focusedPkg: String? = null
        var activePkg: String? = null
        for (w in wins) {
            try {
                if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
                val root = w.root ?: continue
                try {
                    val pkg = root.packageName?.toString()?.takeIf { it.isNotBlank() } ?: continue
                    if (w.isFocused) focusedPkg = pkg
                    if (w.isActive && activePkg == null) activePkg = pkg
                } finally {
                    root.recycle()
                }
            } finally {
                w.recycle()
            }
        }
        return focusedPkg ?: activePkg
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        job.cancel()
        super.onDestroy()
    }
}
