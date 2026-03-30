package com.hjpark.appblocker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccessibilityHelper {

    fun isAppBlockerAccessibilityEnabled(context: Context): Boolean {
        val cn = ComponentName(context, AppBlockerAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(":").mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            runCatching { ComponentName.unflattenFromString(part)?.flattenToString() }.getOrNull()
        }.any { it == cn }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
