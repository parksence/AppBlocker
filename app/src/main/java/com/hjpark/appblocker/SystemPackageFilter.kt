package com.hjpark.appblocker

/**
 * UsageStats / 이벤트 스트림에 끼어드는 삼성·시스템 패키지를 걸러 전면 "사용자 앱"만 남긴다.
 */
object SystemPackageFilter {

    private val EXCLUDED_PREFIXES = listOf(
        "com.samsung.android",
        "com.sec.android",
        "com.android.systemui",
        "com.google.android.inputmethod",
        "com.sec.android.app.launcher",
        "com.android.launcher",
        "com.lge.android",
        "com.miui.",
        "com.huawei.",
        "android",
    )

    fun isUserForegroundCandidate(packageName: String, selfPackage: String): Boolean {
        if (packageName == selfPackage) return false
        if (packageName == "android") return false
        return EXCLUDED_PREFIXES.none { packageName.startsWith(it) }
    }
}
