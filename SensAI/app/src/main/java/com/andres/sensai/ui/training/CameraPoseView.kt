package com.andres.sensai.ui.training

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG_CAMERA = "SensAI-Camera"

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPoseView(
    modifier: Modifier = Modifier,
    drawOverlay: Boolean = true,
    mirror: Boolean = true,
    onLandmarks: (landmarks: List<NormalizedLandmark>, timestampMs: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = ContextCompat.getMainExecutor(context)

    // Pose state (solo para dibujar overlay)
    var poseLandmarks by remember { mutableStateOf<List<NormalizedLandmark>>(emptyList()) }
    var procW by remember { mutableIntStateOf(0) }
    var procH by remember { mutableIntStateOf(0) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val yuvConverter = remember { YuvToRgbConverter() }

    val landmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            modelAssetPath = "models/pose_landmarker_full.task",
            minPoseDetectionConfidence = 0.1f,
            minPosePresenceConfidence = 0.1f,
            minTrackingConfidence = 0.1f,
            numPoses = 1
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try { cameraExecutor.shutdown() } catch (_: Throwable) {}
            try { landmarkerHelper.close() } catch (_: Throwable) {}
        }
    }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    LaunchedEffect(previewView) {
        val pv = previewView ?: return@LaunchedEffect

        val cameraProvider = context.getCameraProviderNoConcurrent()
        val displayRotation = pv.display.rotation

        val preview = Preview.Builder()
            .setTargetRotation(displayRotation)
            .build()
            .apply { setSurfaceProvider(pv.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setTargetRotation(displayRotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }

            try {
                val rot = imageProxy.imageInfo.rotationDegrees

                // 1) YUV -> Bitmap
                val bitmap = yuvConverter.yuv420ToBitmap(mediaImage)
                // 2) Rotate upright
                val upright = rotateBitmap(bitmap, rot)
                // 3) Detect (rotation=0)
                val result = landmarkerHelper.detect(upright, 0)

                val points = result?.landmarks()?.firstOrNull()?.toList().orEmpty()
                val nowMs = SystemClock.elapsedRealtime()

                mainExecutor.execute {
                    procW = upright.width
                    procH = upright.height
                    poseLandmarks = points

                    // callback hacia el padre (TrainScreen)
                    onLandmarks(points, nowMs)
                }
            } catch (t: Throwable) {
                Log.e(TAG_CAMERA, "Analyzer error", t)
            } finally {
                imageProxy.close()
            }
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
        } catch (t: Throwable) {
            Log.e(TAG_CAMERA, "bindToLifecycle error", t)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            }
        )

        if (drawOverlay) {
            PoseOverlay(
                modifier = Modifier.fillMaxSize(),
                landmarks = poseLandmarks,
                mirror = mirror,
                imageWidth = procW,
                imageHeight = procH,
                drawConnections = true
            )
        }
    }
}

private suspend fun Context.getCameraProviderNoConcurrent(): ProcessCameraProvider {
    return suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try { cont.resume(future.get()) }
                catch (e: Exception) { cont.resumeWithException(e) }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
}

private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
    if (degrees % 360 == 0) return src
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}