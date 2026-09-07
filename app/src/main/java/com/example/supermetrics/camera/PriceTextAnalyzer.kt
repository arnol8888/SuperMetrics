package com.example.supermetrics.camera

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.supermetrics.model.ScannedCandidate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Analizador de fotogramas de CameraX que procesa cada frame con ML Kit Text Recognition.
 *
 * Aplica una Región de Interés (ROI) central para descartar ruido exterior
 * y extrae los precios y nombres de productos dentro del visor.
 */
class PriceTextAnalyzer(
    private val onCandidateDetected: (ScannedCandidate) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Porcentaje central del fotograma que compone la Región de Interés (ROI)
    var roiWidthPercentage: Float = 0.70f
    var roiHeightPercentage: Float = 0.45f

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Calcular dimensiones reales teniendo en cuenta la rotación de la imagen
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val rotatedWidth = if (isRotated) imageProxy.height else imageProxy.width
        val rotatedHeight = if (isRotated) imageProxy.width else imageProxy.height

        // Construcción de la caja delimitadora de la ROI centrada
        val marginX = ((1f - roiWidthPercentage) / 2f * rotatedWidth).toInt()
        val marginY = ((1f - roiHeightPercentage) / 2f * rotatedHeight).toInt()
        val roiRect = Rect(
            marginX,
            marginY,
            rotatedWidth - marginX,
            rotatedHeight - marginY
        )

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val linesInRoi = mutableListOf<TextLineInfo>()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox ?: continue
                        // Descartar texto fuera del recuadro central (ROI)
                        if (roiRect.contains(box.centerX(), box.centerY())) {
                            linesInRoi.add(
                                TextLineInfo(
                                    text = line.text,
                                    boundingBox = TextBoundingBox(box.left, box.top, box.right, box.bottom)
                                )
                            )
                        }
                    }
                }

                val candidate = PriceTagParser.parseCandidate(linesInRoi)
                if (candidate != null) {
                    onCandidateDetected(candidate)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error durante el análisis OCR de ML Kit", exception)
            }
            .addOnCompleteListener {
                // Es indispensable cerrar el ImageProxy en cada frame para evitar fugas de memoria
                // y permitir que CameraX envíe el siguiente fotograma.
                imageProxy.close()
            }
    }

    override fun close() {
        recognizer.close()
    }

    companion object {
        private const val TAG = "PriceTextAnalyzer"
    }
}
