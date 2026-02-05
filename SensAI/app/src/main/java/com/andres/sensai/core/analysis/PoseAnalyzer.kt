package com.andres.sensai.core.analysis

import android.content.Context
import androidx.camera.core.ImageProxy
import com.andres.sensai.core.pose.MoveNetPoseEstimator
import com.andres.sensai.core.pose.MoveNetPreprocessor
import com.andres.sensai.core.pose.PoseResult

class PoseAnalyzer(
    context: Context,
    private val isFrontCamera: Boolean,
    private val onPose: (PoseResult) -> Unit
) : FrameAnalyzer {

    private val estimator = MoveNetPoseEstimator(context)

    override fun analyze(image: ImageProxy) {
        try {
            val input = MoveNetPreprocessor.toInputBuffer(image, isFrontCamera)
            val result = estimator.estimate(input)
            onPose(result)
        } finally {
            image.close()
        }
    }

    fun close() = estimator.close()
}
