package org.ggdm.tv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 8, 8)
        window.navigationBarColor = Color.rgb(8, 8, 8)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(12, 12, 12))
        }
        fun line(text: String, size: Float, color: Int, bold: Boolean = false) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 12, 8, 12)
            })
        }
        line("GGDM TV", 32f, Color.rgb(212, 175, 55), true)
        line("GRACE OF GOD DELIVERANCE MINISTRY", 16f, Color.WHITE, true)
        line("Giving Humanity a Fresh Beginning", 14f, Color.LTGRAY)
        line("Native Android studio foundation", 18f, Color.WHITE, true)
        line("Camera broadcast controls and connected Facebook / YouTube destinations are the next integration stage.", 14f, Color.LTGRAY)
        setContentView(root)
    }
}
