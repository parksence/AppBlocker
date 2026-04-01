package com.hjpark.appblocker.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * 유료/강력 차단 등 확장용 플래그.
 * 결제·원격 설정 등에서 [setStrictPowerBlockEnabled]로 켜면,
 * 이미 차단된 앱은 스위치로 해제할 수 없게 된다.
 */
object BlockingPreferences {

    const val KEY_STRICT_POWER_BLOCK = "strict_power_block"

    private const val PREFS_NAME = "appblocker_blocking"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isStrictPowerBlockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STRICT_POWER_BLOCK, false)

    fun setStrictPowerBlockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STRICT_POWER_BLOCK, enabled).apply()
    }
}
