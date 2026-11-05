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
import com.pedro.common.onMainThreadHandler
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.multiple.MultiStream
import com.pedro.library.multiple.MultiType
import com.pedro.library.base.recording.RecordController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), ConnectChecker {
    private lateinit var stream: MultiStream
    private lateinit var preview: SurfaceView
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var facebookUrl: EditText
    private lateinit var youtubeUrl: EditText
    private lateinit var liveButton: Button
    private lateinit var recordButton: Button
    private var recordPath = ""
    private var facebookStarted = false
    private var youtubeStarted = false
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
        stats = text("One camera encode • two independent RTMP outputs • 720p / 30 FPS • 3 Mbps", 12f, Color.LTGRAY)
        root.addView(stats)

        facebookUrl = destinationField("Facebook RTMPS URL + stream key")
        youtubeUrl = destinationField("YouTube RTMPS URL + stream key")
        root.addView(text("BROADCAST DESTINATIONS", 12f, Color.rgb(212,175,55), true))
        root.addView(facebookUrl)
        root.addView(youtubeUrl)

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
            text = "GO LIVE TO BOTH"
            setOnClickListener {
                if (!::stream.isInitialized) return@setOnClickListener
                if (facebookStarted || youtubeStarted) {
                    stopDestinations()
                } else {
                    val fb = facebookUrl.text.toString().trim()
                    val yt = youtubeUrl.text.toString().trim()
                    val validFb = fb.startsWith("rtmp://") || fb.startsWith("rtmps://")
                    val validYt = yt.startsWith("rtmp://") || yt.startsWith("rtmps://")
                    if (!validFb || !validYt) {
                        Toast.makeText(this@MainActivity, "Enter both valid Facebook and YouTube RTMP(S) destinations", Toast.LENGTH_LONG).show()
                    } else {
                        try {
                            status.text = "● CONNECTING TO FACEBOOK + YOUTUBE…"
                            stream.startStream(MultiType.RTMP, 0, fb)
                            facebookStarted = true
                            stream.startStream(MultiType.RTMP, 1, yt)
                            youtubeStarted = true
                            liveButton.text = "STOP BOTH"
                        } catch (e: Exception) {
                            stopDestinations()
                            status.text = "● START FAILED: " + (e.message ?: "Check destinations")
                        }
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
        root.addView(text("GGDM TV sends one encoded camera feed to two RTMP destinations directly from the phone. Account authorization and automatic destination loading are the next integration step.", 11f, Color.LTGRAY))
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

    private fun destinationField(hintText: String) = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.GRAY)
        setTextColor(Color.WHITE)
        textSize = 13f
        setSingleLine(true)
        setPadding(10, 4, 10, 4)
        setBackgroundColor(Color.rgb(38,38,38))
    }

    private fun prepareCamera() {
        if (::stream.isInitialized) return
        stream = MultiStream(this, arrayOf<ConnectChecker>(this, this), null, null, null)
        val videoReady = stream.prepareVideo(1280, 720, videoBitrate)
        val audioReady = stream.prepareAudio(32000, true, 128000)
        if (!videoReady || !audioReady) {
            status.text = "● CAMERA OR MICROPHONE PREPARATION FAILED"
            return
        }
        if (preview.holder.surface.isValid) stream.startPreview(preview)
        status.text = "● CAMERA READY — NOT LIVE"
    }

    private fun stopDestinations() {
        if (!::stream.isInitialized) return
        try { if (facebookStarted) stream.stopStream(MultiType.RTMP, 0) } catch (_: Exception) {}
        try { if (youtubeStarted) stream.stopStream(MultiType.RTMP, 1) } catch (_: Exception) {}
        facebookStarted = false
        youtubeStarted = false
        liveButton.text = "GO LIVE TO BOTH"
        status.text = "● BROADCAST STOPPED"
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

    override fun onConnectionStarted(url: String) { onMainThreadHandler { status.text = "● CONNECTING — " + url.substringBefore("?") } }
    override fun onConnectionSuccess() { onMainThreadHandler { status.text = "● OUTPUT CONNECTED — CHECK BOTH PLATFORM PREVIEWS" } }
    override fun onConnectionFailed(reason: String) {
        onMainThreadHandler { status.text = "● OUTPUT FAILED: " + reason }
    }
    override fun onDisconnect() { onMainThreadHandler { status.text = "● AN OUTPUT DISCONNECTED" } }
    override fun onAuthError() { onMainThreadHandler { status.text = "● STREAM AUTHENTICATION ERROR" } }
    override fun onAuthSuccess() { onMainThreadHandler { status.text = "● STREAM AUTHENTICATED" } }

    override fun onDestroy() {
        if (::stream.isInitialized) {
            stopDestinations()
            stream.release()
        }
        super.onDestroy()
    }
}
