package com.andres.sensai.core.pose

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MoveNetPreprocessor {
    private const val INPUT_SIZE = 192

    fun toInputBuffer(image: ImageProxy, isFrontCamera: Boolean): ByteBuffer {
        val bitmap = image.toBitmap()

        val rotated = bitmap.rotate(image.imageInfo.rotationDegrees)
        val finalBmp = if (isFrontCamera) rotated.mirrorHorizontally() else rotated

        val resized = Bitmap.createScaledBitmap(finalBmp, INPUT_SIZE, INPUT_SIZE, true)

        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (p in pixels) {
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    private fun Bitmap.mirrorHorizontally(): Bitmap {
        val m = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }
}
