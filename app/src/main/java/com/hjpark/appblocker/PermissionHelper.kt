package com.hjpark.appblocker

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

object PermissionHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * [Settings.ACTION_USAGE_ACCESS_SETTINGS]에 `package:` URI를 붙이면
     * AOSP 설정은 해당 앱 전용 사용 정보 접근 화면으로 연다(오버레이 권한과 같은 패턴).
     * 삼성 등 일부 기기는 무시하거나 크래시할 수 있어, 실패 시 URI 없이 전체 목록으로 폴백한다.
     */
    fun openUsageAccessSettings(context: Context) {
        val pkg = context.packageName
        val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(direct)
        } catch (_: ActivityNotFoundException) {
            openUsageAccessSettingsFallback(context)
        } catch (_: RuntimeException) {
            openUsageAccessSettingsFallback(context)
        }
    }

    private fun openUsageAccessSettingsFallback(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
