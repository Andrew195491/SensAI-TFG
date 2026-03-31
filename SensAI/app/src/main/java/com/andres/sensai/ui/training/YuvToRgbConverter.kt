package com.andres.sensai.ui.training

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream

/**
 * Convierte android.media.Image (YUV_420_888) a Bitmap ARGB_8888.
 * Método simple y compatible (usa YuvImage -> JPEG -> Bitmap).
 * Para producción lo optimizamos luego, pero para debug "sí o sí" funciona.
 */
class YuvToRgbConverter {

    fun yuv420ToBitmap(image: Image): Bitmap {
        val nv21 = yuv420ToNv21(image)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val jpegBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Y
        yBuffer.get(nv21, 0, ySize)

        // VU interleaved (NV21)
        val rowStride = image.planes[1].rowStride
        val pixelStride = image.planes[1].pixelStride
        val width = image.width
        val height = image.height

        uBuffer.rewind()
        vBuffer.rewind()

        var offset = ySize
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val index = row * rowStride + col * pixelStride
                nv21[offset++] = vBuffer.get(index)
                nv21[offset++] = uBuffer.get(index)
            }
        }
        return nv21
    }
}
