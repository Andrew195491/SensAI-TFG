package com.andres.sensai.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.min

@Composable
fun PoseOverlay(
    modifier: Modifier,
    landmarks: List<NormalizedLandmark>,
    imageWidth: Int,
    imageHeight: Int,
    mirror: Boolean,
    drawConnections: Boolean = true
) {
    Canvas(modifier = modifier) {
        if (landmarks.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val viewW = size.width
        val viewH = size.height

        val scale = min(viewW / imageWidth.toFloat(), viewH / imageHeight.toFloat())
        val scaledW = imageWidth * scale
        val scaledH = imageHeight * scale

        val offsetX = (viewW - scaledW) / 2f
        val offsetY = (viewH - scaledH) / 2f

        fun mapPoint(lm: NormalizedLandmark): Offset {
            var x = lm.x()
            val y = lm.y()
            if (mirror) x = 1f - x
            return Offset(
                x = offsetX + x * scaledW,
                y = offsetY + y * scaledH
            )
        }

        if (drawConnections) {
            drawSimpleConnections(landmarks, ::mapPoint)
        }

        landmarks.forEach { lm ->
            drawCircle(
                color = Color.Cyan,
                radius = 6f,
                center = mapPoint(lm)
            )
        }
    }
}

private fun DrawScope.drawSimpleConnections(
    landmarks: List<NormalizedLandmark>,
    map: (NormalizedLandmark) -> Offset
) {
    fun line(a: Int, b: Int) {
        if (a !in landmarks.indices || b !in landmarks.indices) return
        drawLine(
            color = Color(0xFF00E5FF),
            start = map(landmarks[a]),
            end = map(landmarks[b]),
            strokeWidth = 4f
        )
    }

    // Cara básica
    line(0, 1); line(1, 2); line(2, 3); line(3, 7)
    line(0, 4); line(4, 5); line(5, 6); line(6, 8)

    // Hombros / torso
    line(11, 12)
    line(11, 23); line(12, 24)
    line(23, 24)

    // Brazos
    line(11, 13); line(13, 15)
    line(12, 14); line(14, 16)

    // Piernas
    line(23, 25); line(25, 27)
    line(24, 26); line(26, 28)

    // Pies (si quieres más detalle)
    line(27, 29); line(29, 31)
    line(28, 30); line(30, 32)
}