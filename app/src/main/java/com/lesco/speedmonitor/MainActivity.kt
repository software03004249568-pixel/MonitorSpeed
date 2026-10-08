package com.lesco.speedmonitor

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

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

    // =========================================================
    // UI VARIABLES
    // =========================================================

    private lateinit var d: TextView
    private lateinit var u: TextView
    private lateinit var ping: TextView

    private lateinit var status: TextView

    private lateinit var level1Result: TextView
    private lateinit var sftpResult: TextView

    private lateinit var emailInput: EditText

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
    // BUILD UI
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

        // INTERNET CARD

        val internetCard = statCard(
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

        // STATUS CARD

        val statusCard = statCard(
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

        root.addView(
            button(
                "TEST INTERNET SPEED",
                primary
            ).apply {

                setOnClickListener {
                    testInternet()
                }

            },
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
            sectionTitle("LEVEL 1 SERVER")
        )

        val level1Card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )

            background = rounded(
                cardColor,
                14
            )
        }

        level1Result = text(
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
                "Server: usersnap.pitc.com.pk",
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

        root.addView(
            button(
                "TEST LEVEL 1",
                accent
            ).apply {

                setOnClickListener {
                    testLevel1()
                }

            },
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
            sectionTitle("SFTP SERVER")
        )

        val sftpCard = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )

            background = rounded(
                cardColor,
                14
            )
        }

        sftpResult = text(
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
                "Server: snaps.pitc.com.pk : 2232",
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

        root.addView(
            button(
                "TEST SFTP",
                primary
            ).apply {

                setOnClickListener {
                    testSftp()
                }

            },
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
        // EMAIL ALERT
        // =====================================================

        root.addView(
            sectionTitle("SERVER DOWN ALERT")
        )

        emailInput = EditText(this).apply {

            hint = "Enter email address"

            setHintTextColor(muted)

            setTextColor(white)

            textSize = 15f

            setSingleLine(true)

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            background = rounded(
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

                gravity = Gravity.CENTER

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
    // INTERNET TEST
    // =========================================================

    private fun testInternet() {

        status.text = "Testing..."
        status.setTextColor(primary)

        d.text = "-- Mbps"
        u.text = "↑ -- Mbps"
        ping.text = "Ping -- ms"

        Thread {

            try {

                val start =
                    System.currentTimeMillis()

                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "ping",
                        "-c",
                        "1",
                        "-W",
                        "2",
                        "8.8.8.8"
                    )
                )

                process.waitFor()

                val elapsed =
                    System.currentTimeMillis() - start

                val reachable =
                    process.exitValue() == 0

                runOnUiThread {

                    if (reachable) {

                        status.text = "Online"
                        status.setTextColor(accent)

                        ping.text =
                            "Ping $elapsed ms"

                        d.text =
                            "Online"

                        u.text =
                            "↑ Connected"

                    } else {

                        status.text = "Offline"
                        status.setTextColor(danger)

                        d.text = "-- Mbps"
                        u.text = "↑ -- Mbps"
                        ping.text = "Ping -- ms"
                    }
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    status.text = "Error"
                    status.setTextColor(danger)

                    d.text = "-- Mbps"
                    u.text = "↑ -- Mbps"
                    ping.text = "Ping -- ms"
                }
            }

        }.start()
    }

    // =========================================================
    // LEVEL 1 TEST
    // =========================================================

    private fun testLevel1() {

        level1Result.text =
            "Testing Level-1 server..."

        level1Result.setTextColor(primary)

        Thread {

            try {

                val start =
                    System.currentTimeMillis()

                val url = URL(
                    "https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting"
                )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod = "POST"

                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.doOutput = true

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

                connection.outputStream.use { output ->

                    output.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                }

                val code =
                    connection.responseCode

                val elapsed =
                    System.currentTimeMillis() - start

                connection.disconnect()

                runOnUiThread {

                    if (code in 200..299) {

                        level1Result.text =
                            "Online • $elapsed ms • HTTP $code"

                        level1Result.setTextColor(
                            accent
                        )

                    } else {

                        level1Result.text =
                            "Server error • HTTP $code"

                        level1Result.setTextColor(
                            danger
                        )
                    }
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    level1Result.text =
                        "Offline / Error"

                    level1Result.setTextColor(
                        danger
                    )
                }
            }

        }.start()
    }

    // =========================================================
    // SFTP SERVER TEST
    // =========================================================

    private fun testSftp() {

        sftpResult.text =
            "Testing SFTP server..."

        sftpResult.setTextColor(primary)

        Thread {

            try {

                val start =
                    System.currentTimeMillis()

                val address =
                    InetSocketAddress(
                        "snaps.pitc.com.pk",
                        2232
                    )

                val socket =
                    Socket()

                socket.connect(
                    address,
                    10000
                )

                val elapsed =
                    System.currentTimeMillis() - start

                socket.close()

                runOnUiThread {

                    sftpResult.text =
                        "Server reachable • $elapsed ms"

                    sftpResult.setTextColor(
                        accent
                    )
                }

            } catch (ex: Exception) {

                runOnUiThread {

                    sftpResult.text =
                        "SFTP server unavailable"

                    sftpResult.setTextColor(
                        danger
                    )
                }
            }

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
    // LOAD SAVED EMAIL
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

        card.addView(titleText)

        card.addView(
            value,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        card.addView(upload)
        card.addView(latency)

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

            setTextColor(white)

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

            setTextColor(color)

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

                setColor(color)

                cornerRadius =
                    dp(radius).toFloat()
            }
    }

    // =========================================================
    // WEIGHT PARAMETERS
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
    // MARGIN PARAMETERS
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

    private fun dp(value: Int): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}
