package com.lesco.speedmonitor

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

class MainActivity : android.app.Activity() {

    private val bg = Color.rgb(7, 17, 31)
    private val card = Color.rgb(20, 34, 56)
    private val text = Color.WHITE
    private val muted = Color.rgb(158, 175, 197)
    private val blue = Color.rgb(36, 123, 255)
    private val green = Color.rgb(55, 214, 160)
    private val red = Color.rgb(227, 79, 95)
    private val yellow = Color.rgb(245, 190, 60)

    private lateinit var download: TextView
    private lateinit var upload: TextView
    private lateinit var ping: TextView
    private lateinit var internetStatus: TextView
    private lateinit var level1Result: TextView
    private lateinit var sftpResult: TextView
    private lateinit var emailInput: EditText
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
            setImageResource(com.lesco.speedmonitor.R.drawable.ic_speed)
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(58), dp(58)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleBox.addView(tv("Speed Monitor", 26f, text, true))
        titleBox.addView(tv("Live network performance dashboard", 13f, muted, false))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)

        root.addView(space(16))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_card)
        }

        hero.addView(tv("INTERNET SPEED", 12f, blue, true))
        download = tv("-- Mbps", 38f, text, true)
        hero.addView(download)
        upload = tv("↑ -- Mbps", 15f, muted, false)
        hero.addView(upload)
        ping = tv("Ping -- ms", 14f, muted, false)
        hero.addView(ping)
        internetStatus = tv("READY", 13f, green, true)
        internetStatus.setPadding(0, dp(7), 0, 0)
        hero.addView(internetStatus)
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(190)))

        root.addView(space(12))

        internetButton = makeButton("▶  START LIVE SPEED TEST", blue)
        internetButton.setOnClickListener {
            if (internetRunning) stopInternet() else startInternet()
        }
        root.addView(internetButton, lp(-1, 54, 0, 0, 0, 14))

        root.addView(section("SERVER MONITORING"))

        root.addView(serverCard(
            "LEVEL 1",
            "usersnap.pitc.com.pk",
            { level1Result = it },
            green
        ))

        level1Button = makeButton("START LEVEL 1 MONITOR", green)
        level1Button.setOnClickListener {
            if (level1Running) stopLevel1() else startLevel1()
        }
        root.addView(level1Button, lp(-1, 50, 0, 0, 0, 12))

        root.addView(serverCard(
            "SFTP / FTP",
            "snaps.pitc.com.pk : 2232",
            { sftpResult = it },
            blue
        ))

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
            setTextColor(text)
            textSize = 15f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_input)
        }
        emailRow.addView(emailInput, LinearLayout.LayoutParams(0, 52.dp(), 1f))

        val save = smallButton("SAVE")
        save.setOnClickListener { saveEmail() }
        emailRow.addView(save, LinearLayout.LayoutParams(dp(86), 52.dp()).apply {
            setMargins(dp(8), 0, 0, 0)
        })
        root.addView(emailRow)

        val send = makeButton("✉  SEND TEST / SERVER ALERT MAIL", green)
        send.setOnClickListener { sendAlertMail() }
        root.addView(send, lp(-1, 50, 0, 10, 0, 16))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_card)
        }
        info.addView(tv("MONITORING", 12f, blue, true))
        info.addView(tv("• Internet: live download, upload & ping", 13f, muted, false))
        info.addView(tv("• Level 1: live API response / availability", 13f, muted, false))
        info.addView(tv("• SFTP: live port connectivity / response", 13f, muted, false))
        info.addView(tv("• Tests run only while the monitor is ON", 13f, muted, false))
        root.addView(info)

        root.addView(space(16))
        root.addView(tv("LESCO IT Directorate  •  Speed Monitor 1.0", 12f, muted, false).apply {
            gravity = Gravity.CENTER
        })

        return scroll
    }

    private fun serverCard(
        title: String,
        host: String,
        resultSetter: (TextView) -> Unit,
        accent: Int
    ): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_card)
        }
        box.addView(tv(title, 13f, accent, true))
        val result = tv("READY", 18f, muted, true)
        resultSetter(result)
        box.addView(result)
        box.addView(tv(host, 12f, muted, false))
        return box
    }

    private fun section(s: String): TextView =
        tv(s, 13f, muted, true).apply { setPadding(dp(2), dp(4), 0, dp(8)) }

    private fun startInternet() {
        internetRunning = true
        internetButton.text = "■  STOP LIVE SPEED TEST"
        internetButton.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_red)
        internetStatus.text = "LIVE"
        internetStatus.setTextColor(green)
        runInternetCycle()
    }

    private fun stopInternet() {
        internetRunning = false
        internetButton.text = "▶  START LIVE SPEED TEST"
        internetButton.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_button)
        internetStatus.text = "STOPPED"
        internetStatus.setTextColor(muted)
    }

    private fun runInternetCycle() {
        if (!internetRunning) return
        Thread {
            var p = -1L
            try {
                val ps = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-W", "1", "8.8.8.8"))
                process.waitFor()
                if (process.exitValue() == 0) p = System.currentTimeMillis() - ps

                val down = downloadSpeed()
                val up = uploadSpeed()

                runOnUiThread {
                    if (!internetRunning) return@runOnUiThread
                    ping.text = if (p >= 0) "Ping $p ms" else "Ping -- ms"
                    download.text = if (down >= 0) String.format("%.2f Mbps", down) else "-- Mbps"
                    upload.text = if (up >= 0) "↑ " + String.format("%.2f Mbps", up) else "↑ -- Mbps"
                    internetStatus.text = if (p >= 0) "LIVE • ONLINE" else "OFFLINE"
                    internetStatus.setTextColor(if (p >= 0) green else red)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!internetRunning) return@runOnUiThread
                    internetStatus.text = "OFFLINE"
                    internetStatus.setTextColor(red)
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
            val u = URL("https://speed.cloudflare.com/__down?bytes=262144")
            c = u.openConnection() as HttpURLConnection
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
            val u = URL("https://speed.cloudflare.com/__up")
            c = u.openConnection() as HttpURLConnection
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
        level1Button.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_red)
        level1Result.text = "TESTING..."
        level1Result.setTextColor(yellow)
        runLevel1Cycle()
    }

    private fun stopLevel1() {
        level1Running = false
        level1Button.text = "START LEVEL 1 MONITOR"
        level1Button.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_green)
        level1Result.text = "STOPPED"
        level1Result.setTextColor(muted)
    }

    private fun runLevel1Cycle() {
        if (!level1Running) return
        Thread {
            var c: HttpURLConnection? = null
            try {
                val start = System.currentTimeMillis()
                c = URL("https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting")
                    .openConnection() as HttpURLConnection
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
            } finally { c?.disconnect() }
            handler.postDelayed({ runLevel1Cycle() }, interval)
        }.start()
    }

    private fun startSftp() {
        sftpRunning = true
        sftpButton.text = "■  STOP SFTP MONITOR"
        sftpButton.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_red)
        sftpResult.text = "TESTING..."
        sftpResult.setTextColor(yellow)
        runSftpCycle()
    }

    private fun stopSftp() {
        sftpRunning = false
        sftpButton.text = "START SFTP MONITOR"
        sftpButton.background = getDrawable(com.lesco.speedmonitor.R.drawable.rounded_button)
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
        val email = getSharedPreferences("SpeedMonitor", MODE_PRIVATE)
            .getString("alert_email", "") ?: ""
        emailInput.setText(email)
    }

    private fun sendAlertMail() {
        val email = emailInput.text.toString().trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter and save a valid email first", Toast.LENGTH_SHORT).show()
            return
        }

        val subject = "Speed Monitor - Server Alert"
        val body = buildString {
            append("Speed Monitor Server Alert\n\n")
            append("Please check the network/server status.\n\n")
            append("This message was generated from LESCO Speed Monitor.\n")
        }

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

    private fun makeButton(label: String, color: Int): Button =
        Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(text)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = getDrawable(
                when (color) {
                    green -> com.lesco.speedmonitor.R.drawable.rounded_green
                    red -> com.lesco.speedmonitor.R.drawable.rounded_red
                    else -> com.lesco.speedmonitor.R.drawable.rounded_button
                }
            )
            elevation = dp(3).toFloat()
        }

    private fun smallButton(label: String): Button =
        makeButton(label, blue).apply { textSize = 12f }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun space(h: Int): View = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(h))
    }

    private fun lp(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, dp(h)).apply {
            setMargins(dp(l), dp(t), dp(r), dp(b))
        }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun Int.dp(): Int = dp(this)

    override fun onDestroy() {
        internetRunning = false
        level1Running = false
        sftpRunning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
