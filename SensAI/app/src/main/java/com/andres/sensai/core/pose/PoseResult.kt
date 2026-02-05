package com.andres.sensai.core.pose

data class Keypoint(val x: Float, val y: Float, val score: Float)

data class PoseResult(
    val keypoints: List<Keypoint>, // 17
    val inferenceMs: Long
)
