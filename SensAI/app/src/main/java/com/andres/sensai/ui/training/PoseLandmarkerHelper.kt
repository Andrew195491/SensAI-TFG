package com.andres.sensai.ui.training

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

private const val TAG_POSE = "SensAI-Pose"

class PoseLandmarkerHelper(
    context: Context,
    modelAssetPath: String,
    minPoseDetectionConfidence: Float,
    minPosePresenceConfidence: Float,
    minTrackingConfidence: Float,
    numPoses: Int,
) {
    private val landmarker: PoseLandmarker

    init {
        // Fail-fast: comprueba que el asset existe
        try {
            context.assets.open(modelAssetPath).close()
            Log.d(TAG_POSE, "Asset OK: $modelAssetPath")
        } catch (e: Exception) {
            Log.e(TAG_POSE, "Model not found in assets: $modelAssetPath", e)
            throw IllegalStateException("Model not found in assets: $modelAssetPath", e)
        }

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(modelAssetPath)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE) // ✅ para debug por frame
            .setNumPoses(numPoses)
            .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(minPosePresenceConfidence)
            .setMinTrackingConfidence(minTrackingConfidence)
            .setErrorListener { e ->
                Log.e(TAG_POSE, "MediaPipe error: ${e.message}", e)
            }
            .build()

        landmarker = PoseLandmarker.createFromOptions(context, options)
        Log.d(TAG_POSE, "PoseLandmarker created OK. model=$modelAssetPath")
    }

    fun detect(bitmap: Bitmap, rotationDegrees: Int): PoseLandmarkerResult? {
        val input = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap

        val mpImage: MPImage = BitmapImageBuilder(input).build()

        val imageOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees)
            .build()

        return landmarker.detect(mpImage, imageOptions)
    }

    fun close() {
        try { landmarker.close() } catch (_: Throwable) {}
    }
}
