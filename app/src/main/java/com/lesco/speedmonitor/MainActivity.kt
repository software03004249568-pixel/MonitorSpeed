package com.lesco.speedmonitor

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var d: TextView
    private lateinit var u: TextView
    private lateinit var ping: TextView
    private lateinit var status: TextView
    private lateinit var l1: TextView
    private lateinit var l1d: TextView
    private lateinit var sftp: TextView
    private lateinit var sftpd: TextView
    private lateinit var email: EditText
    private lateinit var alert: TextView

    private val white = Color.rgb(245, 247, 250)
    private val muted = Color.rgb(165, 174, 188)
    private val primary = Color.rgb(80, 170, 255)
    private val accent = Color.rgb(90, 220, 180)
    private val danger = Color.rgb(255, 95, 105)
    private val success = Color.rgb(90, 220, 140)
    private val cardColor = Color.rgb(30, 38, 50)
    private val bgColor = Color.rgb(15, 20, 28)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        email.setText(prefs.getString("email", ""))
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this).apply { setBackgroundColor(bgColor) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        scroll.addView(root, ViewGroup.LayoutParams(-1, -2))

        root.addView(text("SPEED MONITOR", 27f, white, true))
        root.addView(text("Internet • Level 1 • SFTP", 14f, muted, false).apply {
            setPadding(0, 0, 0, dp(14))
        })

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        top.addView(statCard("INTERNET", primary).also { card ->
            d = card.second[0]
            u = card.second[1]
            ping = card.second[2]
        }, weightParams(1f, 6))
        top.addView(statCard("STATUS", accent).also { card ->
            status = card.second[0]
        }, weightParams(1f, 6))
        root.addView(top, LinearLayout.LayoutParams(-1, dp(150)))

        val internetButton = button("TEST INTERNET SPEED")
        internetButton.setOnClickListener { internet() }
        root.addView(internetButton, marginParams(-1, 58, 14, 0))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val b1 = button("CHECK LEVEL 1")
        val b2 = button("CHECK SFTP")
        b1.setOnClickListener { level1() }
        b2.setOnClickListener { sftpTest() }
        buttons.addView(b1, weightParams(1f, 5))
        buttons.addView(b2, weightParams(1f, 5))
        root.addView(buttons, LinearLayout.LayoutParams(-1, dp(60)))

        val l1Card = simpleCard("LEVEL 1 SERVER", primary)
        l1 = l1Card.first
        l1d = l1Card.second
        root.addView(l1Card.third, marginParams(-1, 110, 12, 0))

        val sftpCard = simpleCard("SFTP SERVER", accent)
        sftp = sftpCard.first
        sftpd = sftpCard.second
        root.addView(sftpCard.third, marginParams(-1, 110, 12, 0))

        val alertCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(14), dp(17), dp(14))
            background = rounded(cardColor, 14)
        }
        alertCard.addView(text("SERVER DOWN ALERT", 14f, danger, true))
        email = EditText(this).apply {
            hint = "Alert email address"
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        alertCard.addView(email, LinearLayout.LayoutParams(-1, dp(55)))

        val emailButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val save = button("SAVE EMAIL")
        val send = button("SEND EMAIL")
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        save.setOnClickListener {
            prefs.edit().putString("email", email.text.toString().trim()).apply()
            alert.text = "Alert email saved"
        }
        send.setOnClickListener { sendMail("Server Status Alert", "Please check Level 1 / SFTP server status.") }
        emailButtons.addView(save, weightParams(1f, 5))
        emailButtons.addView(send, weightParams(1f, 5))
        alertCard.addView(emailButtons, LinearLayout.LayoutParams(-1, dp(55)))

        alert = text("No alert", 13f, muted, false)
        alertCard.addView(alert)
        root.addView(alertCard, marginParams(-1, -2, 0, 0))

        status.text = "READY"
        l1.text = "Not checked"
        l1d.text = "Response time"
        sftp.text = "Not checked"
        sftpd.text = "Connection / transfer speed"
        d.text = "-- Mbps"
        u.text = "↑ -- Mbps"
        ping.text = "Ping -- ms"

        return scroll
    }

    private fun statCard(title: String, titleColor: Int): Pair<LinearLayout, Array<TextView>> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(15), dp(17), dp(12))
            background = rounded(cardColor, 14)
        }
        card.addView(text(title, 13f, titleColor, true))
        val a = text("-- Mbps", 25f, white, true)
        val b = text("↑ -- Mbps", 14f, muted, false)
        val c = text("Ping -- ms", 14f, muted, false)
        card.addView(a, LinearLayout.LayoutParams(-1, dp(40)))
        card.addView(b)
        card.addView(c)
        return Pair(card, arrayOf(a, b, c))
    }

    private fun simpleCard(title: String, color: Int): Triple<TextView, TextView, LinearLayout> {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(14), dp(17), dp(12))
            background = rounded(cardColor, 14)
        }
        card.addView(text(title, 13f, color, true))
        val value = text("Not checked", 21f, white, true)
        val detail = text("Response time", 13f, muted, false)
        card.addView(value)
        card.addView(detail)
        return Triple(value, detail, card)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(label: String): Button = Button(this).apply {
        text = label
        isAllCaps = false
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun weightParams(weight: Float, margin: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(0, -1, weight).apply {
        setMargins(dp(margin), 0, dp(margin), 0)
    }

    private fun marginParams(width: Int, height: Int, top: Int, bottom: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(width, if (height == -2) -2 else dp(height)).apply {
        setMargins(0, dp(top), 0, dp(bottom))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun internet() {
        status.text = "TESTING"
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { pingTest() }
            ping.text = if (r >= 0) "Ping ${r} ms" else "Ping -- ms"
            status.text = if (r >= 0) "ONLINE" else "OFFLINE"
            d.text = "-- Mbps"
            u.text = "↑ -- Mbps"
        }
    }

    private fun level1() {
        status.text = "LEVEL 1"
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { level1Test() }
            l1.text = if (r.ok) "● ONLINE" else "● DOWN"
            l1d.text = "Response ${r.ms} ms"
            if (!r.ok) {
                alert.text = "⚠ Level 1 Server is DOWN"
                sendMail("Level 1 Server Down", "Level 1 server is not responding. Response: ${r.ms} ms")
            }
        }
    }

    private fun sftpTest() {
        status.text = "SFTP"
        sftp.text = "MODULE READY"
        sftpd.text = "SFTP transfer-speed test will use a controlled test file and bytes/time calculation."
    }

    private fun pingTest(): Long = try {
        val t = System.currentTimeMillis()
        val c = URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
        c.connectTimeout = 5000
        c.readTimeout = 5000
        c.connect()
        c.disconnect()
        System.currentTimeMillis() - t
    } catch (_: Exception) { -1 }

    private fun level1Test(): Result = try {
        val t = System.currentTimeMillis()
        val c = URL("https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 10000
        c.readTimeout = 40000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write("""{"BATCH":"01","DIV":"11164","CC_CODE":"1101","BILL_MONTH":"01-May-2026","PAGE_NUMBER":"1"}""".toByteArray()) }
        val response = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        Result(response.contains("TOTAL_RECORDS") || response.contains("READY_FOR_UPLOAD") || response.contains("SNAP_1"), System.currentTimeMillis() - t)
    } catch (_: Exception) { Result(false, 0) }

    private fun sendMail(subject: String, body: String) {
        val to = email.text.toString().trim()
        if (to.isEmpty()) { alert.text = "Save an alert email first"; return }
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to"))
        i.putExtra(Intent.EXTRA_SUBJECT, subject)
        i.putExtra(Intent.EXTRA_TEXT, body)
        try { startActivity(i) } catch (_: Exception) { alert.text = "No email app available" }
    }

    data class Result(val ok: Boolean, val ms: Long)
}
