package org.ggdm.tv

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pedro.common.ConnectChecker
import com.pedro.common.StreamingStatsReport
import com.pedro.common.Throughput
import com.pedro.common.onMainThreadHandler
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.generic.GenericStream
import com.pedro.library.base.recording.RecordController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), ConnectChecker {
    private lateinit var stream: GenericStream
    private lateinit var preview: SurfaceView
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var url: EditText
    private lateinit var liveButton: Button
    private lateinit var recordButton: Button
    private var recordPath = ""
    private val videoBitrate = 3000000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 8, 8)
        window.navigationBarColor = Color.rgb(8, 8, 8)
        makeStudio()
        if (permissionsGranted()) prepareCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 40)
    }

    private fun permissionsGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun makeStudio() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(Color.rgb(12, 12, 12))
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(6, 5, 6, 5)
            }
        root.addView(text("GGDM TV  •  LIVE STUDIO", 22f, Color.rgb(212,175,55), true))
        root.addView(text("GRACE OF GOD DELIVERANCE MINISTRY  |  WEBUYE", 12f, Color.WHITE, true))
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        preview = SurfaceView(this)
        frame.addView(preview, FrameLayout.LayoutParams(-1, -1))
        val brand = text("Giving Humanity a Fresh Beginning", 13f, Color.WHITE, true).apply {
            setBackgroundColor(0x99000000.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
        }
        frame.addView(brand, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        status = text("● READY — waiting for camera permission", 13f, Color.LTGRAY, true)
        root.addView(status)
        stats = text("Landscape 1280×720 • 30 FPS target • 3 Mbps", 12f, Color.LTGRAY)
        root.addView(stats)
        url = EditText(this).apply {
            hint = "Paste full RTMP / RTMPS URL including stream key"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); textSize = 13f
            setSingleLine(true); setPadding(10, 4, 10, 4)
            setBackgroundColor(Color.rgb(38,38,38))
        }
        root.addView(url)
        val title = EditText(this).apply {
            hint = "Programme title / lower-third text"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setSingleLine(true)
            setBackgroundColor(Color.rgb(38,38,38))
        }
        root.addView(title)
        val ticker = EditText(this).apply {
            hint = "Ticker / scrolling announcement"
            setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setSingleLine(true)
            setBackgroundColor(Color.rgb(38,38,38))
        }
        root.addView(ticker)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
        liveButton = Button(this).apply {
            text = "GO LIVE"
            setOnClickListener {
                if (!::stream.isInitialized) return@setOnClickListener
                if (stream.isStreaming) {
                    stream.stopStream()
                    text = "GO LIVE"
                } else {
                    val destination = url.text.toString().trim()
                    if (!(destination.startsWith("rtmp://") || destination.startsWith("rtmps://"))) {
                        Toast.makeText(this@MainActivity, "Enter a valid RTMP or RTMPS destination", Toast.LENGTH_LONG).show()
                    } else {
                        stream.startStream(destination)
                        text = "STOP LIVE"
                    }
                }
            }
        }
        val flip = Button(this).apply {
            text = "FLIP CAMERA"
            setOnClickListener { if (::stream.isInitialized) (stream.videoSource as? Camera2Source)?.switchCamera() }
        }
        recordButton = Button(this).apply { text = "RECORD"; setOnClickListener { toggleRecord() } }
        row.addView(liveButton, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(flip, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(recordButton, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        root.addView(text("Direct RTMP broadcast is available. Direct Facebook / YouTube account publishing and graphic compositing require the next integration stage.", 11f, Color.LTGRAY))
        setContentView(root)
        preview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (::stream.isInitialized && !stream.isOnPreview) stream.startPreview(preview)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (::stream.isInitialized) stream.getGlInterface().setPreviewResolution(width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (::stream.isInitialized && stream.isOnPreview) stream.stopPreview()
            }
        })
    }

    private fun prepareCamera() {
        if (::stream.isInitialized) return
        stream = GenericStream(this, this)
        val videoReady = stream.prepareVideo(1280, 720, videoBitrate)
        val audioReady = stream.prepareAudio(32000, true, 128000)
        if (!videoReady || !audioReady) {
            status.text = "● CAMERA OR MICROPHONE PREPARATION FAILED"
            return
        }
        stream.getStreamClient().setReTries(10)
        if (preview.holder.surface.isValid) stream.startPreview(preview)
        status.text = "● CAMERA READY — NOT LIVE"
    }

    private fun toggleRecord() {
        if (!::stream.isInitialized) return
        if (stream.isRecording) {
            stream.stopRecord()
            recordButton.text = "RECORD"
            status.text = "● RECORDING SAVED"
        } else {
            val folder = getExternalFilesDir("recordings") ?: filesDir
            if (!folder.exists()) folder.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            recordPath = File(folder, "GGDM_TV_" + stamp + ".mp4").absolutePath
            stream.startRecord(recordPath) { state ->
                if (state == RecordController.Status.RECORDING) onMainThreadHandler {
                    recordButton.text = "STOP REC"
                    status.text = "● RECORDING LOCALLY"
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 40 && permissionsGranted()) prepareCamera()
        else status.text = "● CAMERA AND MICROPHONE PERMISSIONS REQUIRED"
    }

    override fun onConnectionStarted(url: String) { onMainThreadHandler { status.text = "● CONNECTING…" } }
    override fun onConnectionSuccess() { onMainThreadHandler { status.text = "● LIVE — STREAM CONNECTED" } }
    override fun onConnectionFailed(reason: String) {
        onMainThreadHandler { status.text = "● CONNECTION FAILED: " + reason; liveButton.text = "GO LIVE" }
    }
    override fun onDisconnect() { onMainThreadHandler { status.text = "● DISCONNECTED"; liveButton.text = "GO LIVE" } }
    override fun onAuthError() { onMainThreadHandler { status.text = "● STREAM AUTHENTICATION ERROR"; liveButton.text = "GO LIVE" } }
    override fun onAuthSuccess() { onMainThreadHandler { status.text = "● STREAM AUTHENTICATED" } }
    override fun onStreamingStats(report: StreamingStatsReport) {
        onMainThreadHandler {
            if (report.throughput != Throughput.UNKNOWN) {
                stats.text = "Upload " + (report.smoothedBitrate / 1000000f) + " Mbps • " +
                    report.throughput.name + " • Queue " + (report.queueBytesOut / 1024) + " KB"
            }
        }
    }

    override fun onDestroy() {
        if (::stream.isInitialized) stream.release()
        super.onDestroy()
    }
}
