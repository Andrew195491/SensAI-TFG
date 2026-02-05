package com.andres.sensai.ui.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import com.andres.sensai.core.pose.PoseResult
import androidx.compose.ui.graphics.Color


@Composable
fun PoseOverlay(
    pose: PoseResult?,
    modifier: Modifier = Modifier,
    mirrorX: Boolean = true,
    minScore: Float = 0.3f
) {
    if (pose == null) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        pose.keypoints.forEach { kp ->
            if (kp.score >= minScore) {
                val xNorm = if (mirrorX) (1f - kp.x) else kp.x
                val x = xNorm * w
                val y = kp.y * h
                drawCircle(
                    color = Color.Red,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
