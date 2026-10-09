package com.lesco.speedmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MonitoringService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var lastLevel1Down = false
    @Volatile private var lastSftpDown = false

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(4001, buildNotification("Background monitoring is active"))
        executor.scheduleWithFixedDelay({ checkServers() }, 0, 15, TimeUnit.SECONDS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun checkServers() {
        val level1Down = !checkLevel1()
        val sftpDown = !checkSftp()
        if (level1Down && !lastLevel1Down) alert("Level-1 Server Not Responding", "Level-1 API is not responding from the monitoring agent.")
        if (sftpDown && !lastSftpDown) alert("SFTP Server Not Responding", "snaps.pitc.com.pk:2232 is not responding.")
        if (!level1Down && lastLevel1Down) alert("Level-1 Server Restored", "Level-1 API is responding again.")
        if (!sftpDown && lastSftpDown) alert("SFTP Server Restored", "SFTP port 2232 is responding again.")
        lastLevel1Down = level1Down
        lastSftpDown = sftpDown
        val status = "Level-1: ${if (level1Down) "DOWN" else "ONLINE"} • SFTP: ${if (sftpDown) "DOWN" else "ONLINE"}"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(4001, buildNotification(status))
    }

    private fun checkLevel1(): Boolean {
        var c: HttpURLConnection? = null
        return try {
            c = URL("https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 7000
            c.readTimeout = 7000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            val json = """{"BATCH":"01","DIV":"11164","CC_CODE":"1101","BILL_MONTH":"01-May-2026","PAGE_NUMBER":"1"}"""
            c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            c.responseCode in 200..299
        } catch (_: Exception) { false } finally { c?.disconnect() }
    }

    private fun checkSftp(): Boolean {
        return try {
            Socket().use { it.connect(InetSocketAddress("snaps.pitc.com.pk", 2232), 5000) }
            true
        } catch (_: Exception) { false }
    }

    private fun alert(title: String, message: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(if (title.contains("Level-1")) 4101 else 4102,
            Notification.Builder(this, "server_alerts")
                .setSmallIcon(R.drawable.ic_speed)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setCategory(Notification.CATEGORY_ERROR)
                .setAutoCancel(true)
                .build())
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vibrator = (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 450, 180, 450), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 450, 180, 450), -1)
            }
        } catch (_: Exception) { }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("monitor_status", "Monitoring Status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Persistent status for server monitoring"
            })
            nm.createNotificationChannel(NotificationChannel("server_alerts", "Server Down Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Sound and vibration when monitored servers go down"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 180, 450)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
            })
        }
    }

    private fun buildNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "monitor_status")
                .setSmallIcon(R.drawable.ic_speed).setContentTitle("Speed Monitor")
                .setContentText(text).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build()
        } else {
            @Suppress("DEPRECATION")
            val builder = Notification.Builder(this)
            builder.setSmallIcon(R.drawable.ic_speed).setContentTitle("Speed Monitor")
                .setContentText(text).setOngoing(true).build()
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putBoolean("background_monitor", false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
