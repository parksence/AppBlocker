package com.hjpark.appblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hjpark.appblocker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val USAGE_POLL_INTERVAL_MS = 550L

/**
 * 포그라운드 알림·중지 버튼으로 감시 상태를 유지하고,
 * **사용 기록으로 마지막 전면 앱을 주기적으로 읽어** 접근성이 끊긴 기기에서도 차단을 보조한다.
 */
class AppBlockerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private var usagePollJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                usagePollJob?.cancel()
                usagePollJob = null
                applicationContext.setBlockingServiceRunningFlag(false)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        applicationContext.setBlockingServiceRunningFlag(true)

        if (usagePollJob?.isActive != true) {
            usagePollJob = scope.launch {
                Log.i("AppBlocker", "차단 감시: 사용 기록 폴링 시작(접근성 보조)")
                while (isActive) {
                    delay(USAGE_POLL_INTERVAL_MS)
                    if (!applicationContext.isBlockingServiceRunning()) continue
                    if (!PermissionHelper.hasUsageStatsPermission(applicationContext)) continue
                    val pkg = withContext(Dispatchers.IO) {
                        UsageStatsForegroundReader.lastForegroundPackage(applicationContext)
                    } ?: continue
                    evaluateForegroundPackageCandidate(
                        applicationContext,
                        pkg,
                        "usage_poll",
                    )
                }
            }
        }

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        usagePollJob?.cancel()
        usagePollJob = null
        job.cancel()
        applicationContext.setBlockingServiceRunningFlag(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "앱 차단",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "차단 감시 알림"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AppBlockerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 차단 중")
            .setContentText("차단 목록 앱이 전면이면 잠금합니다. 접근성 + 사용 기록으로 감지합니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp)
            .addAction(0, "중지", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.hjpark.appblocker.action.STOP_BLOCKING"
        private const val CHANNEL_ID = "app_blocker_fg"
        private const val NOTIFICATION_ID = 42

        fun start(context: Context) {
            val i = Intent(context, AppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
