package com.andres.sensai.core.analysis

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy

class FpsLoggerAnalyzer(
    private val tag: String = "SensAI-Analyzer"
) : FrameAnalyzer {

    private var lastLogTime = 0L
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        try {
            frameCount++
            val now = SystemClock.elapsedRealtime()
            if (lastLogTime == 0L) lastLogTime = now

            val elapsed = now - lastLogTime
            if (elapsed >= 1000) {
                val fps = frameCount * 1000f / elapsed
                Log.d(tag, "FPS approx: %.1f".format(fps))
                frameCount = 0
                lastLogTime = now
            }
        } finally {
            image.close()
        }
    }
}
