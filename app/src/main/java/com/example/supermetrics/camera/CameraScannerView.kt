package com.example.supermetrics.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.supermetrics.model.ScannedCandidate
import com.example.supermetrics.ui.theme.NeonCyan
import com.example.supermetrics.ui.theme.NeonGreen
import java.util.concurrent.Executors

/**
 * Vista de cámara en pantalla completa que procesa imágenes con [PriceTextAnalyzer]
 * y dibuja el marco visual del visor con retícula y recorte central.
 */
@Composable
fun CameraScannerView(
    isCandidateDetected: Boolean,
    onCandidateDetected: (ScannedCandidate) -> Unit,
    modifier: Modifier = Modifier,
    roiWidthPercent: Float = 0.75f,
    roiHeightPercent: Float = 0.42f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        PriceTextAnalyzer(onCandidateDetected).apply {
            this.roiWidthPercentage = roiWidthPercent
            this.roiHeightPercentage = roiHeightPercent
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor, analyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScannerView", "Fallo al vincular casos de uso de CameraX", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay con máscara semitransparente oscura, recorte central y retícula neón
        CameraRoiOverlay(
            isCandidateDetected = isCandidateDetected,
            roiWidthPercent = roiWidthPercent,
            roiHeightPercent = roiHeightPercent
        )
    }
}

/**
 * Dibuja un overlay semitransparente oscuro con un recorte central transparente
 * y una retícula neón que cambia de color al detectar un candidato.
 */
@Composable
fun CameraRoiOverlay(
    isCandidateDetected: Boolean,
    roiWidthPercent: Float,
    roiHeightPercent: Float,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCandidateDetected) NeonGreen else NeonCyan,
        animationSpec = tween(durationMillis = 300),
        label = "RoiBorderColor"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val boxWidth = canvasWidth * roiWidthPercent
        val boxHeight = canvasHeight * roiHeightPercent
        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2f

        // 1. Fondo oscuro semitransparente sobre toda la pantalla
        drawRect(color = Color.Black.copy(alpha = 0.55f))

        // 2. Recorte transparente del visor (ROI) donde se enfoca la etiqueta
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 3. Contorno redondeado sutil
        drawRoundRect(
            color = animatedBorderColor.copy(alpha = if (isCandidateDetected) 0.9f else 0.4f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = if (isCandidateDetected) 2.5.dp.toPx() else 1.5.dp.toPx())
        )

        // 4. Retícula en las 4 esquinas (Corner Brackets) estilo escáner profesional
        val cornerLength = 28.dp.toPx()
        val cornerStroke = Stroke(width = 4.dp.toPx())
        val cornerColor = animatedBorderColor

        // Esquina Superior Izquierda
        drawLine(
            color = cornerColor,
            start = Offset(left, top + cornerLength),
            end = Offset(left, top),
            strokeWidth = cornerStroke.width
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = cornerStroke.width
        )

        // Esquina Superior Derecha
        val right = left + boxWidth
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerLength, top),
            end = Offset(right, top),
            strokeWidth = cornerStroke.width
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, top),
            end = Offset(right, top + cornerLength),
            strokeWidth = cornerStroke.width
        )

        // Esquina Inferior Izquierda
        val bottom = top + boxHeight
        drawLine(
            color = cornerColor,
            start = Offset(left, bottom - cornerLength),
            end = Offset(left, bottom),
            strokeWidth = cornerStroke.width
        )
        drawLine(
            color = cornerColor,
            start = Offset(left, bottom),
            end = Offset(left + cornerLength, bottom),
            strokeWidth = cornerStroke.width
        )

        // Esquina Inferior Derecha
        drawLine(
            color = cornerColor,
            start = Offset(right - cornerLength, bottom),
            end = Offset(right, bottom),
            strokeWidth = cornerStroke.width
        )
        drawLine(
            color = cornerColor,
            start = Offset(right, bottom - cornerLength),
            end = Offset(right, bottom),
            strokeWidth = cornerStroke.width
        )
    }
}
