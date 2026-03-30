package com.hjpark.appblocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hjpark.appblocker.ui.theme.AppBlockerTheme

class LockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()

        setContent {
            AppBlockerTheme {
                LockScreenContent(
                    blockedPackage = blockedPackage,
                    packageManager = packageManager,
                    onGoHome = {
                        LockSuppressor.onUserRequestedGoHome()
                        if (!AppBlockerAccessibilityService.performGlobalHomeIfPossible()) {
                            startActivity(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                },
                            )
                        }
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }
}
