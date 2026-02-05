package com.andres.sensai.core.camera

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.andres.sensai.core.analysis.FrameAnalyzer
import java.util.concurrent.Executor

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: FrameAnalyzer? = null,
    analysisExecutor: Executor? = null,
    lensFacing: Int = CameraSelector.LENS_FACING_FRONT
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lensFacing, analyzer, analysisExecutor) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val executor = analysisExecutor ?: mainExecutor

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val analysisUseCase = analyzer?.let { frameAnalyzer ->
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                            frameAnalyzer.analyze(imageProxy)
                        }
                    }
            }

            try {
                cameraProvider.unbindAll()
                if (analysisUseCase != null) {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysisUseCase)
                } else {
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, mainExecutor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}
