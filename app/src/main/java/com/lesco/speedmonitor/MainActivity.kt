package com.lesco.speedmonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
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

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        d=findViewById(R.id.tvDownload); u=findViewById(R.id.tvUpload); ping=findViewById(R.id.tvPing)
        status=findViewById(R.id.tvStatus); l1=findViewById(R.id.tvLevel1); l1d=findViewById(R.id.tvLevel1Detail)
        sftp=findViewById(R.id.tvSftp); sftpd=findViewById(R.id.tvSftpDetail); email=findViewById(R.id.etEmail)
        alert=findViewById(R.id.tvAlert)
        val p=getSharedPreferences("settings",0); email.setText(p.getString("email",""))

        findViewById<Button>(R.id.btnInternet).setOnClickListener { internet() }
        findViewById<Button>(R.id.btnLevel1).setOnClickListener { level1() }
        findViewById<Button>(R.id.btnSftp).setOnClickListener { sftpTest() }
        findViewById<Button>(R.id.btnSaveEmail).setOnClickListener {
            p.edit().putString("email",email.text.toString().trim()).apply(); alert.text="Alert email saved"
        }
        findViewById<Button>(R.id.btnSendEmail).setOnClickListener { sendMail("Server Status Alert","Please check Level 1 / SFTP server status.") }
    }

    private fun internet() {
        status.text="TESTING"
        lifecycleScope.launch {
            val r=withContext(Dispatchers.IO){ pingTest() }
            ping.text="Ping ${r} ms"; status.text=if(r>=0)"ONLINE" else "OFFLINE"
            // Real Mbps measurement is intentionally a separate module: ping is not bandwidth.
            d.text="-- Mbps"; u.text="↑ -- Mbps"
        }
    }

    private fun level1() {
        status.text="LEVEL 1"
        lifecycleScope.launch {
            val r=withContext(Dispatchers.IO){ level1Test() }
            l1.text=if(r.ok)"● ONLINE" else "● DOWN"; l1d.text="Response ${r.ms} ms"
            if(!r.ok){ alert.text="⚠ Level 1 Server is DOWN"; sendMail("Level 1 Server Down","Level 1 server is not responding. Response: ${r.ms} ms") }
            internet()
        }
    }

    private fun sftpTest() {
        status.text="SFTP"
        sftp.text="MODULE READY"
        sftpd.text="SFTP transfer-speed test will use a controlled test file and bytes/time calculation."
        internet()
    }

    private fun pingTest():Long=try {
        val t=System.currentTimeMillis()
        val c=URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection
        c.connectTimeout=5000;c.readTimeout=5000;c.connect();c.disconnect()
        System.currentTimeMillis()-t
    } catch(_:Exception){-1}

    private fun level1Test():R {
        val t=System.currentTimeMillis()
        return try {
            val c=URL("https://usersnap.pitc.com.pk/api/SnapsForPrinting/ToPrinting").openConnection() as HttpURLConnection
            c.requestMethod="POST";c.connectTimeout=10000;c.readTimeout=40000;c.doOutput=true
            c.setRequestProperty("Content-Type","application/json")
            c.outputStream.use { it.write("""{"BATCH":"01","DIV":"11164","CC_CODE":"1101","BILL_MONTH":"01-May-2026","PAGE_NUMBER":"1"}""".toByteArray()) }
            val x=c.inputStream.bufferedReader().use{it.readText()}
            R(x.contains("TOTAL_RECORDS")||x.contains("READY_FOR_UPLOAD")||x.contains("SNAP_1"),System.currentTimeMillis()-t)
        } catch(_:Exception){R(false,System.currentTimeMillis()-t)}
    }

    private fun sendMail(subject:String,body:String){
        val to=email.text.toString().trim()
        if(to.isEmpty()){alert.text="Save an alert email first";return}
        val i=Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to"))
        i.putExtra(Intent.EXTRA_SUBJECT,subject);i.putExtra(Intent.EXTRA_TEXT,body)
        try{startActivity(i)}catch(_:Exception){alert.text="No email app available"}
    }
    data class R(val ok:Boolean,val ms:Long)
}
