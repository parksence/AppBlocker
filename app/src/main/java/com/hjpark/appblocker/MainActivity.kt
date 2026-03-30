package com.hjpark.appblocker

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjpark.appblocker.ui.theme.AppBlockerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppBlockerTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val context = LocalContext.current
                var usageGranted by remember {
                    mutableStateOf(PermissionHelper.hasUsageStatsPermission(context))
                }
                var accessibilityEnabled by remember {
                    mutableStateOf(AccessibilityHelper.isAppBlockerAccessibilityEnabled(context))
                }
                var overlayGranted by remember {
                    mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            usageGranted = PermissionHelper.hasUsageStatsPermission(context)
                            accessibilityEnabled =
                                AccessibilityHelper.isAppBlockerAccessibilityEnabled(context)
                            overlayGranted = OverlayPermissionHelper.canDrawOverlays(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                var blockingStatusRevision by remember { mutableIntStateOf(0) }
                val view = LocalView.current

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        val already = context.isBlockingServiceRunning()
                        AppBlockerService.start(context)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (already) {
                            Toast.makeText(
                                context,
                                "이미 차단 감시가 켜져 있어요.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "차단 감시를 시작했습니다.\n알림이 떠 있는지 확인해 주세요.",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        blockingStatusRevision++
                    } else {
                        Toast.makeText(
                            context,
                            "알림 권한이 없으면 차단 서비스를 시작할 수 없습니다.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                fun tryStartBlocking() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNoti = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasNoti) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return
                        }
                    }
                    val already = context.isBlockingServiceRunning()
                    AppBlockerService.start(context)
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    if (already) {
                        Toast.makeText(
                            context,
                            "이미 차단 감시가 켜져 있어요.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "차단 감시를 시작했습니다.\n알림이 떠 있는지 확인해 주세요.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    blockingStatusRevision++
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (usageGranted && overlayGranted) {
                        val app: Application = context.applicationContext as Application
                        AppListScreen(
                            onStartBlocking = { tryStartBlocking() },
                            blockingStatusRevision = blockingStatusRevision,
                            accessibilityEnabled = accessibilityEnabled,
                            onOpenAccessibilitySettings = {
                                AccessibilityHelper.openAccessibilitySettings(context)
                            },
                            viewModel = viewModel(
                                factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
                                    app,
                                ),
                            ),
                        )
                    } else {
                        SetupPermissionsScreen(
                            usageGranted = usageGranted,
                            overlayGranted = overlayGranted,
                            onOpenUsageSettings = {
                                PermissionHelper.openUsageAccessSettings(context)
                            },
                            onOpenOverlaySettings = {
                                OverlayPermissionHelper.openOverlaySettings(context)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
