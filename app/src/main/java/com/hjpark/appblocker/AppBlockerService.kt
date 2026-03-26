package com.hjpark.appblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hjpark.appblocker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private var monitorJob: Job? = null
    private var collectJob: Job? = null

    private val usageStatsManager: UsageStatsManager by lazy {
        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val dao = AppDatabase.getInstance(applicationContext).appDao()
        collectJob = serviceScope.launch {
            dao.observeBlockedApps().collect { list ->
                blockedPackages.update { list.map { it.packageName }.toSet() }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
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

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            var lastLocked: String? = null
            while (isActive) {
                val foreground = getForegroundPackageName()
                val blocked = blockedPackages.value
                val selfPkg = packageName
                if (
                    foreground != null &&
                    foreground != selfPkg &&
                    blocked.contains(foreground)
                ) {
                    if (lastLocked != foreground) {
                        lastLocked = foreground
                        launchLockScreen(foreground)
                    }
                } else {
                    lastLocked = null
                }
                delay(500L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        collectJob?.cancel()
        serviceScope.cancel()
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
            description = "차단 서비스 실행 중 알림"
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
            .setContentText("차단 목록에 있는 앱 사용을 감시하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(openApp)
            .addAction(0, "중지", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun getForegroundPackageName(): String? {
        val end = System.currentTimeMillis()
        val begin = end - 10_000L
        val events = UsageEvents()
        usageStatsManager.queryEvents(begin, end, events)
        var lastPkg: String? = null
        var lastTs = 0L
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            val isForeground = when (type) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> true
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        type == UsageEvents.Event.ACTIVITY_RESUMED
                    } else {
                        false
                    }
                }
            }
            if (isForeground && event.timeStamp >= lastTs) {
                lastTs = event.timeStamp
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun launchLockScreen(blockedPackage: String) {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        startActivity(intent)
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
