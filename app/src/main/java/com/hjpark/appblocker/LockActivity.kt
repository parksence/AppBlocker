package com.hjpark.appblocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
                        val home = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(home)
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

@Composable
private fun LockScreenContent(
    blockedPackage: String,
    packageManager: android.content.pm.PackageManager,
    onGoHome: () -> Unit,
) {
    val appLabel = remember(blockedPackage) {
        try {
            val info = packageManager.getApplicationInfo(blockedPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            blockedPackage
        }
    }
    val iconBitmap: ImageBitmap? = remember(blockedPackage) {
        try {
            val d = packageManager.getApplicationIcon(blockedPackage)
            d.toBitmap(256, 256).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = appLabel,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "이 앱은 차단되었습니다 🚫",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onGoHome) {
                Text("홈으로 돌아가기")
            }
        }
    }
}
