package com.lesco.speedmonitor

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    // =========================================================
    // COLORS
    // =========================================================

    private val bgColor = Color.rgb(10, 15, 25)
    private val cardColor = Color.rgb(20, 27, 40)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 180, 195)
    private val primary = Color.rgb(60, 150, 255)
    private val accent = Color.rgb(45, 210, 140)
    private val danger = Color.rgb(240, 80, 90)
    private val yellow = Color.rgb(245, 190, 60)

    // =========================================================
    // UI
    // =========================================================

    private lateinit var d: TextView
    private lateinit var u: TextView
    private lateinit var ping: TextView

    private lateinit var status: TextView

    private lateinit var level1Result: TextView
    private lateinit var sftpResult: TextView

    private lateinit var internetButton: Button
    private lateinit var level1Button: Button
    private lateinit var sftpButton: Button

    private lateinit var emailInput: EditText

    // =========================================================
    // LIVE TEST FLAGS
    // =========================================================

    @Volatile
    private var internetRunning = false

    @Volatile
    private var level1Running = false

    @Volatile
    private var sftpRunning = false

    // =========================================================
    // HANDLER
    // =========================================================

    private val handler =
        Handler(Looper.getMainLooper())

    // =========================================================
    // INTERVAL
    // =========================================================

    private val testInterval = 1000L

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor

        setContentView(buildUi())

        loadSavedEmail()
    }

    // =========================================================
    // UI
    // =========================================================

    private fun buildUi(): ScrollView {

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bgColor)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(24)
            )
        }

        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================================
        // HEADER
        // =====================================================

        root.addView(
            text(
                "SPEED MONITOR",
                27f,
                white,
                true
            )
        )

        root.addView(
            text(
                "Internet • Level 1 • SFTP",
                14f,
                muted,
                false
            ).apply {
                setPadding(
                    0,
                    0,
                    0,
                    dp(14)
                )
            }
        )

        // =====================================================
        // TOP CARDS
        // =====================================================

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val internetCard =
            statCard(
                "INTERNET",
                primary
            )

        d = internetCard.second[0]
        u = internetCard.second[1]
        ping = internetCard.second[2]

        top.addView(
            internetCard.first,
            weightParams(1f, 6)
        )

        val statusCard =
            statCard(
                "STATUS",
                accent
            )

        status = statusCard.second[0]

        statusCard.second[1].text = "Server"
        statusCard.second[2].text = "Ready"

        top.addView(
            statusCard.first,
            weightParams(1f, 6)
        )

        root.addView(
            top,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        // =====================================================
        // INTERNET BUTTON
        // =====================================================

        internetButton =
            button(
                "TEST INTERNET SPEED",
                primary
            )

        internetButton.setOnClickListener {

            if (internetRunning) {
                stopInternetTest()
            } else {
                startInternetTest()
            }
        }

        root.addView(
            internetButton,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
                0,
                0,
                0,
                18
            )
        )

        // =====================================================
        // LEVEL 1
        // =====================================================

        root.addView(
            sectionTitle(
                "LEVEL 1 SERVER"
            )
        )

        val level1Card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(14),
                    dp(16),
                    dp(14)
                )

                background =
                    rounded(
                        cardColor,
                        14
                    )
            }

        level1Result =
            text(
                "Not tested",
                16f,
                muted,
                false
            )

        level1Card.addView(
            level1Result
        )

        level1Card.addView(
            text(
                "usersnap.pitc.com.pk",
                13f,
                muted,
                false
            ).apply {

                setPadding(
                    0,
                    dp(6),
                    0,
                    0
                )
            }
        )

        root.addView(
            level1Card,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                0,
                0,
                10
            )
        )

        level1Button =
            button(
                "START LEVEL 1 TEST",
                accent
            )

        level1Button.setOnClickListener {

            if (level1Running) {
                stopLevel1Test()
            } else {
                startLevel1Test()
            }
        }

        root.addView(
            level1Button,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50),
                0,
                0,
                0,
                18
            )
        )

        // =====================================================
        // SFTP
        // =====================================================

        root.addView(
            sectionTitle(
                "SFTP SERVER"
            )
        )

        val sftpCard =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(14),
                    dp(16),
                    dp(14)
                )

                background =
                    rounded(
                        cardColor,
                        14
                    )
            }

        sftpResult =
            text(
                "Not tested",
                16f,
                muted,
                false
            )

        sftpCard.addView(
            sftpResult
        )

        sftpCard.addView(
            text(
                "snaps.pitc.com.pk : 2232",
                13f,
                muted,
                false
            ).apply {

                setPadding(
                    0,
                    dp(6),
                    0,
                    0
                )
            }
        )

        root.addView(
            sftpCard,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                0,
                0,
                10
            )
        )

        sftpButton =
            button(
                "START SFTP TEST",
                primary
            )

        sftpButton.setOnClickListener {

            if (sftpRunning) {
                stopSftpTest()
            } else {
                startSftpTest()
            }
        }

        root.addView(
            sftpButton,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50),
                0,
                0,
                0,
                18
            )
        )

        // =====================================================
        // EMAIL
        // =====================================================

        root.addView(
            sectionTitle(
                "SERVER DOWN ALERT"
            )
        )

        emailInput =
            EditText(this).apply {

                hint =
                    "Enter email address"

                setHintTextColor(
                    muted
                )

                setTextColor(
                    white
                )

                textSize = 15f

                setSingleLine(true)

                setPadding(
                    dp(14),
                    0,
                    dp(14),
                    0
                )

                background =
                    rounded(
                        cardColor,
                        12
                    )
            }

        root.addView(
            emailInput,
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
                0,
                0,
                0,
                10
            )
        )

        root.addView(
            button(
                "SAVE EMAIL",
                accent
            ).apply {

                setOnClickListener {
                    saveEmail()
                }
            },
            marginParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50),
                0,
                0,
                0,
                20
            )
        )

        // =====================================================
        // FOOTER
        // =====================================================

        root.addView(
            text(
                "LESCO IT Directorate",
                13f,
                muted,
                false
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(8)
                )
            }
        )

        return scroll
    }

    // =========================================================
    // START INTERNET
    // =========================================================

    private fun startInternetTest() {

        internetRunning = true

        internetButton.text =
            "STOP INTERNET TEST"

        internetButton.setBackgroundColor(
            danger
        )

        status.text =
            "LIVE"

        status.setTextColor(
            accent
        )

        runInternetCycle()
    }

    // =========================================================
    // STOP INTERNET
    // =========================================================

    private fun stopInternetTest() {

        internetRunning = false

        internetButton.text =
            "TEST INTERNET SPEED"

        internetButton.setBackgroundColor(
            primary
        )

        status.text =
            "Stopped"

        status.setTextColor(
            muted
        )
    }

    // =========================================================
    // INTERNET CYCLE
    // =========================================================

    private fun runInternetCycle() {

        if (!internetRunning) {
            return
        }

        Thread {

            var pingMs = -1L

            try {

                // -------------------------------------------------
                // PING
                // -------------------------------------------------

                val pingStart =
                    System.currentTimeMillis()

                val process =
                    Runtime.getRuntime().exec(
                        arrayOf(
                            "ping",
                            "-c",
                            "1",
                            "-W",
                            "1",
                            "8.8.8.8"
                        )
                    )

                process.waitFor()

                if (process.exitValue() == 0) {

                    pingMs =
                        System.currentTimeMillis() -
                                pingStart
                }

                // -------------------------------------------------
                // DOWNLOAD TEST
                // -------------------------------------------------

                val downloadMbps =
                    downloadSpeed()

                // -------------------------------------------------
                // UPLOAD TEST
                // -------------------------------------------------

                val uploadMbps =
                    uploadSpeed()

                // -------------------------------------------------
                // UPDATE UI
                // -------------------------------------------------

                runOnUiThread {

                    if (!internetRunning) {
                        return@runOnUiThread
                    }

                    if (pingMs >= 0) {

                        ping.text =
                            "Ping $pingMs ms"

                        status.text =
                            "LIVE"

                        status.setTextColor(
                            accent
                        )

                    } else {

                        ping.text =
                            "Ping -- ms"

                        status.text =
                            "OFFLINE"

                        status.setTextColor(
                            danger
                        )
                    }

                    if (downloadMbps >= 0) {

                        d.text =
                            String.format(
                                "%.2f Mbps",
                                downloadMbps
                            )

                    } else {

                        d.text =
                            "-- Mbps"
                    }

                    if (uploadMbps >= 0) {

                        u.text =
                            "↑ " +
                                    String.format(
                                        "%.2f Mbps",
                                        uploadMbps
                                    )

                    } else {

                        u.text =
                            "↑ -- Mbps"
                    }
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    if (!internetRunning) {
                        return@runOnUiThread
                    }

                    status.text =
                        "OFFLINE"

                    status.setTextColor(
                        danger
                    )

                    d.text =
                        "-- Mbps"

                    u.text =
                        "↑ -- Mbps"

                    ping.text =
                        "Ping -- ms"
                }
            }

            handler.postDelayed(
                {
                    runInternetCycle()
                },
                testInterval
            )

        }.start()
    }

    // =========================================================
    // DOWNLOAD SPEED
    // =========================================================

    private fun downloadSpeed(): Double {

        var connection:
                HttpURLConnection? = null

        try {

            val url =
                URL(
                    "https://speed.cloudflare.com/__down?bytes=262144"
                )

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.connectTimeout =
                5000

            connection.readTimeout =
                5000

            connection.requestMethod =
                "GET"

            connection.connect()

            val start =
                System.nanoTime()

            var totalBytes = 0L

            val buffer =
                ByteArray(16384)

            val input =
                BufferedInputStream(
                    connection.inputStream
                )

            while (true) {

                val count =
                    input.read(buffer)

                if (count == -1) {
                    break
                }

                totalBytes += count

                if (
                    totalBytes >=
                    262144L
                ) {
                    break
                }
            }

            input.close()

            val elapsed =
                max(
                    1L,
                    System.nanoTime() - start
                )

            return (
                totalBytes.toDouble() *
                        8.0 /
                        elapsed.toDouble()
                ) * 1000000000.0 /
                    1000000.0

        } catch (ex: Exception) {

            return -1.0

        } finally {

            connection?.disconnect()
        }
    }

    // =========================================================
    // UPLOAD SPEED
    // =========================================================

    private fun uploadSpeed(): Double {

        var connection:
                HttpURLConnection? = null

        try {

            val size =
                131072

            val data =
                ByteArray(size)

            val url =
                URL(
                    "https://speed.cloudflare.com/__up"
                )

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.connectTimeout =
                5000

            connection.readTimeout =
                5000

            connection.requestMethod =
                "POST"

            connection.doOutput =
                true

            connection.setFixedLengthStreamingMode(
                size
            )

            connection.setRequestProperty(
                "Content-Type",
                "application/octet-stream"
            )

            connection.connect()

            val start =
                System.nanoTime()

            val output:
                    OutputStream =
                connection.outputStream

            output.write(data)
            output.flush()
            output.close()

            connection.responseCode

            val elapsed =
                max(
                    1L,
                    System.nanoTime() - start
                )

            return (
                size.toDouble() *
                        8.0 /
                        elapsed.toDouble()
                ) * 1000000000.0 /
                    1000000.0

        } catch (ex: Exception) {

            return -1.0

        } finally {

            connection?.disconnect()
        }
    }

    // =========================================================
    // START LEVEL 1
    // =========================================================

    private fun startLevel1Test() {

        level1Running = true

        level1Button.text =
            "STOP LEVEL 1 TEST"

        level1Button.setBackgroundColor(
            danger
        )

        level1Result.text =
            "LIVE TEST STARTED..."

        level1Result.setTextColor(
            primary
        )

        runLevel1Cycle()
    }

    // =========================================================
    // STOP LEVEL 1
    // =========================================================

    private fun stopLevel1Test() {

        level1Running = false

        level1Button.text =
            "START LEVEL 1 TEST"

        level1Button.setBackgroundColor(
            accent
        )

        level1Result.text =
            "Test stopped"

        level1Result.setTextColor(
            muted
        )
    }

    // =========================================================
    // LEVEL 1 CYCLE
    // =========================================================

    private fun runLevel1Cycle() {

        if (!level1Running) {
            return
        }

        Thread {

            var connection:
                    HttpURLConnection? = null

            try {

                val start =
                    System.currentTimeMillis()

                val url =
                    URL(
                        "https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting"
                    )

                connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    10000

                connection.readTimeout =
                    10000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val json =
                    """
                    {
                        "BATCH":"01",
                        "DIV":"11164",
                        "CC_CODE":"1101",
                        "BILL_MONTH":"01-May-2026",
                        "PAGE_NUMBER":"1"
                    }
                    """.trimIndent()

                connection.outputStream.use {

                    it.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }

                val responseCode =
                    connection.responseCode

                val elapsed =
                    System.currentTimeMillis() -
                            start

                runOnUiThread {

                    if (!level1Running) {
                        return@runOnUiThread
                    }

                    if (
                        responseCode in
                        200..299
                    ) {

                        level1Result.text =
                            "ONLINE • $elapsed ms • HTTP $responseCode"

                        level1Result.setTextColor(
                            accent
                        )

                    } else {

                        level1Result.text =
                            "ERROR • HTTP $responseCode • $elapsed ms"

                        level1Result.setTextColor(
                            danger
                        )
                    }
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    if (!level1Running) {
                        return@runOnUiThread
                    }

                    level1Result.text =
                        "OFFLINE / ERROR"

                    level1Result.setTextColor(
                        danger
                    )
                }

            } finally {

                connection?.disconnect()
            }

            handler.postDelayed(
                {
                    runLevel1Cycle()
                },
                testInterval
            )

        }.start()
    }

    // =========================================================
    // START SFTP
    // =========================================================

    private fun startSftpTest() {

        sftpRunning = true

        sftpButton.text =
            "STOP SFTP TEST"

        sftpButton.setBackgroundColor(
            danger
        )

        sftpResult.text =
            "LIVE TEST STARTED..."

        sftpResult.setTextColor(
            primary
        )

        runSftpCycle()
    }

    // =========================================================
    // STOP SFTP
    // =========================================================

    private fun stopSftpTest() {

        sftpRunning = false

        sftpButton.text =
            "START SFTP TEST"

        sftpButton.setBackgroundColor(
            primary
        )

        sftpResult.text =
            "Test stopped"

        sftpResult.setTextColor(
            muted
        )
    }

    // =========================================================
    // SFTP CYCLE
    // =========================================================

    private fun runSftpCycle() {

        if (!sftpRunning) {
            return
        }

        Thread {

            var socket:
                    Socket? = null

            try {

                val start =
                    System.currentTimeMillis()

                socket =
                    Socket()

                socket.connect(
                    InetSocketAddress(
                        "snaps.pitc.com.pk",
                        2232
                    ),
                    5000
                )

                val elapsed =
                    System.currentTimeMillis() -
                            start

                runOnUiThread {

                    if (!sftpRunning) {
                        return@runOnUiThread
                    }

                    sftpResult.text =
                        "ONLINE • $elapsed ms"

                    sftpResult.setTextColor(
                        accent
                    )
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    if (!sftpRunning) {
                        return@runOnUiThread
                    }

                    sftpResult.text =
                        "OFFLINE / ERROR"

                    sftpResult.setTextColor(
                        danger
                    )
                }

            } finally {

                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }

            handler.postDelayed(
                {
                    runSftpCycle()
                },
                testInterval
            )

        }.start()
    }

    // =========================================================
    // SAVE EMAIL
    // =========================================================

    private fun saveEmail() {

        val email =
            emailInput.text
                .toString()
                .trim()

        if (email.isEmpty()) {

            Toast.makeText(
                this,
                "Please enter email address",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()
        ) {

            Toast.makeText(
                this,
                "Invalid email address",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        getSharedPreferences(
            "SpeedMonitor",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "alert_email",
                email
            )
            .apply()

        Toast.makeText(
            this,
            "Email saved successfully",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // LOAD EMAIL
    // =========================================================

    private fun loadSavedEmail() {

        val savedEmail =
            getSharedPreferences(
                "SpeedMonitor",
                MODE_PRIVATE
            )
                .getString(
                    "alert_email",
                    ""
                )

        if (!savedEmail.isNullOrEmpty()) {

            emailInput.setText(
                savedEmail
            )
        }
    }

    // =========================================================
    // SECTION TITLE
    // =========================================================

    private fun sectionTitle(
        value: String
    ): TextView {

        return text(
            value,
            15f,
            white,
            true
        ).apply {

            setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(8)
            )
        }
    }

    // =========================================================
    // STAT CARD
    // =========================================================

    private fun statCard(
        title: String,
        titleColor: Int
    ): Pair<LinearLayout, Array<TextView>> {

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(17),
                    dp(15),
                    dp(17),
                    dp(12)
                )

                background =
                    rounded(
                        cardColor,
                        14
                    )
            }

        val titleText =
            text(
                title,
                13f,
                titleColor,
                true
            )

        val value =
            text(
                "-- Mbps",
                25f,
                white,
                true
            )

        val upload =
            text(
                "↑ -- Mbps",
                14f,
                muted,
                false
            )

        val latency =
            text(
                "Ping -- ms",
                14f,
                muted,
                false
            )

        card.addView(
            titleText
        )

        card.addView(
            value,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        card.addView(
            upload
        )

        card.addView(
            latency
        )

        return Pair(
            card,
            arrayOf(
                value,
                upload,
                latency
            )
        )
    }

    // =========================================================
    // BUTTON
    // =========================================================

    private fun button(
        value: String,
        color: Int
    ): Button {

        return Button(this).apply {

            text = value

            textSize = 14f

            setTextColor(
                white
            )

            typeface =
                Typeface.DEFAULT_BOLD

            isAllCaps = false

            background =
                rounded(
                    color,
                    12
                )

            elevation =
                dp(2).toFloat()
        }
    }

    // =========================================================
    // TEXT
    // =========================================================

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {

        return TextView(this).apply {

            text = value

            textSize = size

            setTextColor(
                color
            )

            typeface =
                if (bold) {
                    Typeface.DEFAULT_BOLD
                } else {
                    Typeface.DEFAULT
                }
        }
    }

    // =========================================================
    // ROUNDED BACKGROUND
    // =========================================================

    private fun rounded(
        color: Int,
        radius: Int
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable
            .GradientDrawable()
            .apply {

                setColor(
                    color
                )

                cornerRadius =
                    dp(radius).toFloat()
            }
    }

    // =========================================================
    // WEIGHT PARAMS
    // =========================================================

    private fun weightParams(
        weight: Float,
        margin: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            weight
        ).apply {

            setMargins(
                dp(margin),
                0,
                dp(margin),
                0
            )
        }
    }

    // =========================================================
    // MARGIN PARAMS
    // =========================================================

    private fun marginParams(
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            width,
            height
        ).apply {

            setMargins(
                dp(left),
                dp(top),
                dp(right),
                dp(bottom)
            )
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        internetRunning = false
        level1Running = false
        sftpRunning = false

        handler.removeCallbacksAndMessages(
            null
        )

        super.onDestroy()
    }
}
