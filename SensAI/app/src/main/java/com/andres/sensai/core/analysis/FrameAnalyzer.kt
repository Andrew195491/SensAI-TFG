package com.andres.sensai.core.analysis

import androidx.camera.core.ImageProxy

fun interface FrameAnalyzer {
    fun analyze(image: ImageProxy)
}
