package com.lesco.speedmonitor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.max

class MainActivity : Activity() {

    private val bg = Color.rgb(7, 17, 31)
    private val card = Color.rgb(20, 34, 56)
    private val white = Color.WHITE
    private val muted = Color.rgb(158, 175, 197)
    private val blue = Color.rgb(36, 123, 255)
    private val green = Color.rgb(55, 214, 160)
    private val red = Color.rgb(227, 79, 95)
    private val yellow = Color.rgb(245, 190, 60)

    private lateinit var download: TextView
    private lateinit var upload: TextView
    private lateinit var ping: TextView
    private lateinit var internetState: TextView
    private lateinit var level1Result: TextView
    private lateinit var sftpResult: TextView
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var backgroundButton: Button
    private lateinit var internetButton: Button
    private lateinit var level1Button: Button
    private lateinit var sftpButton: Button

    @Volatile private var internetRunning = false
    @Volatile private var level1Running = false
    @Volatile private var sftpRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        setContentView(buildUi())
        loadEmail()
        loadPhone()
        requestNotificationPermission()
        // Start monitoring automatically when the dashboard opens.
        if (!getSharedPreferences("SpeedMonitor", MODE_PRIVATE).getBoolean("background_monitor", false)) {
            val serviceIntent = Intent(this, MonitoringService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putBoolean("background_monitor", true).apply()
            } catch (_: Exception) { }
        }
        backgroundButton.text = "■  STOP BACKGROUND MONITORING"
        backgroundButton.background = getDrawable(R.drawable.red)
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(26))
        }
        scroll.addView(root, ViewGroup.LayoutParams(-1, -2))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_speed)
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(58), dp(58)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleBox.addView(tv("Speed Monitor", 26f, white, true))
        titleBox.addView(tv("Live Network Performance", 13f, muted, false))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)

        root.addView(space(16))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = getDrawable(R.drawable.card)
        }
        hero.addView(tv("INTERNET SPEED", 12f, blue, true))
        download = tv("-- Mbps", 38f, white, true)
        hero.addView(download)
        upload = tv("↑ -- Mbps", 15f, muted, false)
        hero.addView(upload)
        ping = tv("Ping -- ms", 14f, muted, false)
        hero.addView(ping)
        internetState = tv("READY", 13f, green, true)
        hero.addView(internetState)
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(185)))

        root.addView(space(12))

        internetButton = makeButton("▶  START LIVE SPEED TEST", blue)
        internetButton.setOnClickListener {
            if (internetRunning) stopInternet() else startInternet()
        }
        root.addView(internetButton, lp(-1, 54, 0, 0, 0, 14))

        root.addView(section("SERVER MONITORING"))

        val levelCard = serverCard("LEVEL 1", "usersnap.pitc.com.pk", green) { level1Result = it }
        root.addView(levelCard, lp(-1, 92, 0, 0, 0, 8))

        level1Button = makeButton("START LEVEL 1 MONITOR", green)
        level1Button.setOnClickListener {
            if (level1Running) stopLevel1() else startLevel1()
        }
        root.addView(level1Button, lp(-1, 50, 0, 0, 0, 12))

        val sftpCard = serverCard("SFTP", "snaps.pitc.com.pk : 2232", blue) { sftpResult = it }
        root.addView(sftpCard, lp(-1, 92, 0, 0, 0, 8))

        sftpButton = makeButton("START SFTP MONITOR", blue)
        sftpButton.setOnClickListener {
            if (sftpRunning) stopSftp() else startSftp()
        }
        root.addView(sftpButton, lp(-1, 50, 0, 0, 0, 16))

        root.addView(section("SERVER DOWN ALERT"))

        val emailRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        emailInput = EditText(this).apply {
            hint = "Alert email address"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = getDrawable(R.drawable.input)
        }
        emailRow.addView(emailInput, LinearLayout.LayoutParams(0, dp(52), 1f))

        val save = makeButton("SAVE", blue)
        save.textSize = 12f
        emailRow.addView(save, LinearLayout.LayoutParams(dp(88), dp(52)).apply {
            setMargins(dp(8), 0, 0, 0)
        })
        save.setOnClickListener { saveEmail() }

        root.addView(emailRow)

        val send = makeButton("✉  COMPOSE COMPLAINT EMAIL", green)
        send.setOnClickListener { sendAlertMail("Manual complaint") }
        root.addView(send, lp(-1, 50, 0, 10, 0, 10))

        val phoneRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        phoneInput = EditText(this).apply {
            hint = "WhatsApp number (e.g. +923001234567)"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setPadding(dp(14), 0, dp(14), 0)
            background = getDrawable(R.drawable.input)
        }
        phoneRow.addView(phoneInput, LinearLayout.LayoutParams(0, dp(52), 1f))
        val savePhone = makeButton("SAVE", blue).apply { textSize = 12f }
        phoneRow.addView(savePhone, LinearLayout.LayoutParams(dp(88), dp(52)).apply { setMargins(dp(8), 0, 0, 0) })
        savePhone.setOnClickListener { savePhone() }
        root.addView(phoneRow, lp(-1, 52, 0, 0, 0, 8))

        val whatsapp = makeButton("◉  SEND COMPLAINT ON WHATSAPP", green)
        whatsapp.setOnClickListener { openWhatsAppComplaint() }
        root.addView(whatsapp, lp(-1, 50, 0, 0, 0, 10))

        backgroundButton = makeButton("▶  START BACKGROUND MONITORING", blue)
        backgroundButton.setOnClickListener { toggleBackgroundMonitoring() }
        root.addView(backgroundButton, lp(-1, 52, 0, 0, 0, 16))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = getDrawable(R.drawable.card)
        }
        info.addView(tv("MONITORING MODE", 12f, blue, true))
        info.addView(tv("• Internet: live Download / Upload / Ping", 13f, muted, false))
        info.addView(tv("• Level 1: live API response time", 13f, muted, false))
        info.addView(tv("• SFTP: live port response time", 13f, muted, false))
        info.addView(tv("• Manual tests run only after START", 13f, muted, false))
        info.addView(tv("• Background monitoring alerts when Level 1 / SFTP goes down", 13f, muted, false))
        info.addView(tv("• WhatsApp opens with complaint text; you press Send", 13f, muted, false))
        root.addView(info)

        root.addView(space(16))
        root.addView(tv("LESCO IT Directorate  •  Speed Monitor 1.0", 12f, muted, false).apply {
            gravity = Gravity.CENTER
        })

        return scroll
    }

    private fun serverCard(title: String, host: String, accent: Int, setter: (TextView) -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            background = getDrawable(R.drawable.card)
        }
        box.addView(tv(title, 12f, accent, true))
        val result = tv("READY", 17f, muted, true)
        setter(result)
        box.addView(result)
        box.addView(tv(host, 12f, muted, false))
        return box
    }

    private fun section(s: String) = tv(s, 13f, muted, true).apply {
        setPadding(dp(2), dp(4), 0, dp(8))
    }

    private fun startInternet() {
        internetRunning = true
        internetButton.text = "■  STOP LIVE SPEED TEST"
        internetButton.background = getDrawable(R.drawable.red)
        internetState.text = "LIVE"
        internetState.setTextColor(green)
        runInternetCycle()
    }

    private fun stopInternet() {
        internetRunning = false
        internetButton.text = "▶  START LIVE SPEED TEST"
        internetButton.background = getDrawable(R.drawable.blue)
        internetState.text = "STOPPED"
        internetState.setTextColor(muted)
    }

    private fun runInternetCycle() {
        if (!internetRunning) return
        Thread {
            var pingMs = -1L
            try {
                val start = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-W", "1", "8.8.8.8"))
                process.waitFor()
                if (process.exitValue() == 0) pingMs = System.currentTimeMillis() - start

                val down = downloadSpeed()
                val up = uploadSpeed()

                runOnUiThread {
                    if (!internetRunning) return@runOnUiThread
                    ping.text = if (pingMs >= 0) "Ping $pingMs ms" else "Ping -- ms"
                    download.text = if (down >= 0) String.format("%.2f Mbps", down) else "-- Mbps"
                    upload.text = if (up >= 0) "↑ " + String.format("%.2f Mbps", up) else "↑ -- Mbps"
                    internetState.text = if (pingMs >= 0) "LIVE • ONLINE" else "OFFLINE"
                    internetState.setTextColor(if (pingMs >= 0) green else red)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!internetRunning) return@runOnUiThread
                    internetState.text = "OFFLINE"
                    internetState.setTextColor(red)
                    download.text = "-- Mbps"
                    upload.text = "↑ -- Mbps"
                    ping.text = "Ping -- ms"
                }
            }
            handler.postDelayed({ runInternetCycle() }, interval)
        }.start()
    }

    private fun downloadSpeed(): Double {
        var c: HttpURLConnection? = null
        return try {
            c = URL("https://speed.cloudflare.com/__down?bytes=262144").openConnection() as HttpURLConnection
            c.connectTimeout = 5000
            c.readTimeout = 5000
            c.requestMethod = "GET"
            c.connect()
            val start = System.nanoTime()
            var total = 0L
            val buffer = ByteArray(16384)
            BufferedInputStream(c.inputStream).use { input ->
                while (total < 262144L) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                }
            }
            val ns = max(1L, System.nanoTime() - start)
            total * 8.0 * 1_000_000_000.0 / ns / 1_000_000.0
        } catch (_: Exception) { -1.0 }
        finally { c?.disconnect() }
    }

    private fun uploadSpeed(): Double {
        var c: HttpURLConnection? = null
        return try {
            val data = ByteArray(131072)
            c = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
            c.connectTimeout = 5000
            c.readTimeout = 5000
            c.requestMethod = "POST"
            c.doOutput = true
            c.setFixedLengthStreamingMode(data.size)
            c.setRequestProperty("Content-Type", "application/octet-stream")
            c.connect()
            val start = System.nanoTime()
            c.outputStream.use { it.write(data) }
            c.responseCode
            val ns = max(1L, System.nanoTime() - start)
            data.size * 8.0 * 1_000_000_000.0 / ns / 1_000_000.0
        } catch (_: Exception) { -1.0 }
        finally { c?.disconnect() }
    }

    private fun startLevel1() {
        level1Running = true
        level1Button.text = "■  STOP LEVEL 1 MONITOR"
        level1Button.background = getDrawable(R.drawable.red)
        level1Result.text = "TESTING..."
        level1Result.setTextColor(yellow)
        runLevel1Cycle()
    }

    private fun stopLevel1() {
        level1Running = false
        level1Button.text = "START LEVEL 1 MONITOR"
        level1Button.background = getDrawable(R.drawable.green)
        level1Result.text = "STOPPED"
        level1Result.setTextColor(muted)
    }

    private fun runLevel1Cycle() {
        if (!level1Running) return
        Thread {
            var c: HttpURLConnection? = null
            try {
                val start = System.currentTimeMillis()
                c = URL("https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 8000
                c.readTimeout = 8000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                val json = """{"BATCH":"01","DIV":"11164","CC_CODE":"1101","BILL_MONTH":"01-May-2026","PAGE_NUMBER":"1"}"""
                c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                val ms = System.currentTimeMillis() - start

                runOnUiThread {
                    if (!level1Running) return@runOnUiThread
                    if (code in 200..299) {
                        level1Result.text = "ONLINE  •  $ms ms  •  HTTP $code"
                        level1Result.setTextColor(green)
                    } else {
                        level1Result.text = "SERVER ERROR  •  HTTP $code  •  $ms ms"
                        level1Result.setTextColor(red)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!level1Running) return@runOnUiThread
                    level1Result.text = "OFFLINE / SERVER ERROR"
                    level1Result.setTextColor(red)
                }
            } finally {
                c?.disconnect()
            }
            handler.postDelayed({ runLevel1Cycle() }, interval)
        }.start()
    }

    private fun startSftp() {
        sftpRunning = true
        sftpButton.text = "■  STOP SFTP MONITOR"
        sftpButton.background = getDrawable(R.drawable.red)
        sftpResult.text = "TESTING..."
        sftpResult.setTextColor(yellow)
        runSftpCycle()
    }

    private fun stopSftp() {
        sftpRunning = false
        sftpButton.text = "START SFTP MONITOR"
        sftpButton.background = getDrawable(R.drawable.blue)
        sftpResult.text = "STOPPED"
        sftpResult.setTextColor(muted)
    }

    private fun runSftpCycle() {
        if (!sftpRunning) return
        Thread {
            var socket: Socket? = null
            try {
                val start = System.currentTimeMillis()
                socket = Socket()
                socket.connect(InetSocketAddress("snaps.pitc.com.pk", 2232), 5000)
                val ms = System.currentTimeMillis() - start
                runOnUiThread {
                    if (!sftpRunning) return@runOnUiThread
                    sftpResult.text = "ONLINE  •  $ms ms  •  PORT 2232"
                    sftpResult.setTextColor(green)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!sftpRunning) return@runOnUiThread
                    sftpResult.text = "OFFLINE / CONNECTION ERROR"
                    sftpResult.setTextColor(red)
                }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
            handler.postDelayed({ runSftpCycle() }, interval)
        }.start()
    }

    private fun saveEmail() {
        val email = emailInput.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit()
            .putString("alert_email", email).apply()
        Toast.makeText(this, "Email saved", Toast.LENGTH_SHORT).show()
    }

    private fun loadEmail() {
        emailInput.setText(
            getSharedPreferences("SpeedMonitor", MODE_PRIVATE)
                .getString("alert_email", "") ?: ""
        )
    }

    private fun sendAlertMail(reason: String) {
        val email = emailInput.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter and save a valid email first", Toast.LENGTH_SHORT).show()
            return
        }

        val subject = if (level1Result.text.toString().contains("OFFLINE", true) || level1Result.text.toString().contains("ERROR", true))
            "Complaint: Level-1 Server Not Responding" else "Complaint: LESCO Server Connectivity Issue"
        val body = complaintBody(reason)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "No email application is available", Toast.LENGTH_LONG).show()
        }
    }

    private fun complaintBody(reason: String): String {
        val levelDown = level1Result.text.toString().contains("OFFLINE", true) || level1Result.text.toString().contains("ERROR", true)
        val sftpDown = sftpResult.text.toString().contains("OFFLINE", true) || sftpResult.text.toString().contains("ERROR", true)
        val issue = when {
            levelDown && sftpDown -> "Level-1 application server and SFTP server are not responding from the monitoring agent."
            levelDown -> "The Level-1 application server is not responding from the monitoring agent."
            sftpDown -> "The SFTP server (snaps.pitc.com.pk:2232) is not responding from the monitoring agent."
            else -> "A connectivity/performance issue has been observed. Please verify the current server status."
        }
        return """Dear PIDC Support Team,

COMPLAINT: SERVER NOT RESPONDING

This is to report a server connectivity issue detected by LESCO IT Directorate's Speed Monitor.

Issue: $issue

Current Status:
• Level-1: ${level1Result.text}
• SFTP: ${sftpResult.text}
• Internet: ${internetState.text}

Kindly investigate the issue and restore the service at the earliest. Please share an update after resolution.

Regards,
LESCO IT Directorate
Speed Monitor

Reference: $reason""".trimIndent()
    }

    private fun savePhone() {
        val phone = phoneInput.text.toString().trim().replace(" ", "")
        if (phone.length < 8) {
            Toast.makeText(this, "Enter a valid WhatsApp number with country code", Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putString("alert_phone", phone).apply()
        Toast.makeText(this, "WhatsApp number saved", Toast.LENGTH_SHORT).show()
    }

    private fun loadPhone() {
        phoneInput.setText(getSharedPreferences("SpeedMonitor", MODE_PRIVATE).getString("alert_phone", "") ?: "")
    }

    private fun openWhatsAppComplaint() {
        val phone = phoneInput.text.toString().trim().replace("+", "").replace(" ", "").replace("-", "")
        if (phone.length < 8) {
            Toast.makeText(this, "Save a valid WhatsApp number with country code first", Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putString("alert_phone", phoneInput.text.toString().trim()).apply()
        val uri = Uri.parse("https://wa.me/$phone?text=" + Uri.encode(complaintBody("Manual WhatsApp complaint")))
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: Exception) { Toast.makeText(this, "WhatsApp is not available", Toast.LENGTH_LONG).show() }
    }

    private fun toggleBackgroundMonitoring() {
        val intent = Intent(this, MonitoringService::class.java)
        if (getSharedPreferences("SpeedMonitor", MODE_PRIVATE).getBoolean("background_monitor", false)) {
            stopService(intent)
            getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putBoolean("background_monitor", false).apply()
            backgroundButton.text = "▶  START BACKGROUND MONITORING"
            backgroundButton.background = getDrawable(R.drawable.blue)
            Toast.makeText(this, "Background monitoring stopped", Toast.LENGTH_SHORT).show()
        } else {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                getSharedPreferences("SpeedMonitor", MODE_PRIVATE).edit().putBoolean("background_monitor", true).apply()
                backgroundButton.text = "■  STOP BACKGROUND MONITORING"
                backgroundButton.background = getDrawable(R.drawable.red)
                Toast.makeText(this, "Background monitoring started", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "Could not start monitoring. Allow notifications and try again.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 510)
        }
    }

    private fun makeButton(label: String, color: Int) =
        Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = getDrawable(
                when (color) {
                    green -> R.drawable.green
                    red -> R.drawable.red
                    else -> R.drawable.blue
                }
            )
            elevation = dp(3).toFloat()
        }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun space(h: Int) = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun lp(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int) =
        LinearLayout.LayoutParams(w, dp(h)).apply {
            setMargins(dp(l), dp(t), dp(r), dp(b))
        }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        internetRunning = false
        level1Running = false
        sftpRunning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
