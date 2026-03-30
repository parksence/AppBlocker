package com.hjpark.appblocker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

/**
 * 사용 통계 API는 자기 앱으로 전환된 뒤에도 직전 전면 앱(CGV 등)이 남는 경우가 많아,
 * 서비스에서 "우리 앱 UI가 보이는지"를 별도로 판단하는 데 쓴다.
 */
class AppBlockerApplication : Application() {

    private val visibleOurActivities = AtomicInteger(0)

    fun isOurAppUiVisible(): Boolean = visibleOurActivities.get() > 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (activity.application.packageName == packageName) {
                    visibleOurActivities.incrementAndGet()
                }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                if (activity.application.packageName == packageName) {
                    visibleOurActivities.decrementAndGet()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
