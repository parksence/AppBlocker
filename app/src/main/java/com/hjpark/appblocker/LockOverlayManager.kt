package com.hjpark.appblocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.hjpark.appblocker.ui.theme.AppBlockerTheme

/**
 * WindowManager 오버레이용: [SavedStateRegistryOwner] + [Lifecycle]을 한 오너로 묶어
 * Compose가 요구하는 ViewTree* 두 가지를 동시에 만족시킨다.
 */
private class OverlayComposeOwner : SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

/**
 * Activity 대신 [TYPE_APPLICATION_OVERLAY]로 잠금 UI를 띄워 BAL(백그라운드 Activity 시작 제한)을 피한다.
 * [Settings.canDrawOverlays]가 true일 때만 동작.
 */
object LockOverlayManager {

    private const val TAG = "LockOverlay"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var overlayView: ComposeView? = null

    @Volatile
    private var overlayOwner: OverlayComposeOwner? = null

    fun show(context: Context, blockedPackage: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return
        }
        val appContext = context.applicationContext
        mainHandler.post {
            if (overlayView != null) return@post
            try {
                // Application 컨텍스트에는 Material/Compose 테마가 없음 + 동적 색상 API는 Activity 계열이 안전
                val themedContext = ContextThemeWrapper(appContext, R.style.Theme_AppBlocker)
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val owner = OverlayComposeOwner()
                val composeView = ComposeView(themedContext).apply {
                    // Activity 밖 오버레이: Lifecycle + SavedStateRegistry 둘 다 필요 (Compose 1.7+)
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        // dynamicLightColorScheme(application) 등에서 API 34+ 기기 크래시 방지
                        AppBlockerTheme(dynamicColor = false) {
                            LockScreenContent(
                                blockedPackage = blockedPackage,
                                packageManager = appContext.packageManager,
                                onGoHome = {
                                    LockSuppressor.onUserRequestedGoHome()
                                    hide(appContext)
                                    if (!AppBlockerAccessibilityService.performGlobalHomeIfPossible()) {
                                        appContext.startActivity(
                                            Intent(Intent.ACTION_MAIN).apply {
                                                addCategory(Intent.CATEGORY_HOME)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                wm.addView(composeView, params)
                overlayView = composeView
                overlayOwner = owner
            } catch (e: Throwable) {
                Log.e(TAG, "오버레이 표시 실패", e)
            }
        }
    }

    fun hide(context: Context) {
        val appContext = context.applicationContext
        mainHandler.post {
            val v = overlayView ?: return@post
            runCatching {
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(v)
            }
            overlayOwner?.destroy()
            overlayOwner = null
            overlayView = null
        }
    }

    fun isShowing(): Boolean = overlayView != null
}
