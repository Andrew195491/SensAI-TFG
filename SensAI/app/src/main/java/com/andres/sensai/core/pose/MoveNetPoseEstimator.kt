package com.andres.sensai.core.pose

import android.content.Context
import android.os.SystemClock
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer

class MoveNetPoseEstimator(
    context: Context,
    modelAssetName: String = "movenet_lightning.tflite"
) {
    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, modelAssetName)
        interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4) })
    }

    fun estimate(input: ByteBuffer): PoseResult {
        // Output: [1, 1, 17, 3] => (y, x, score)
        val output = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }

        val start = SystemClock.elapsedRealtime()
        interpreter.run(input, output)
        val end = SystemClock.elapsedRealtime()

        val keypoints = output[0][0].map {
            val y = it[0]
            val x = it[1]
            val score = it[2]
            Keypoint(x = x, y = y, score = score)
        }

        return PoseResult(keypoints = keypoints, inferenceMs = end - start)
    }

    fun close() = interpreter.close()
}
