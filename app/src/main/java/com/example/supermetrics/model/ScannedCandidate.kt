package com.example.supermetrics.model

/**
 * Representa el candidato detectado actualmente por la cámara u OCR.
 *
 * @param name Nombre sugerido del producto detectado ("Producto" si no se detectó texto).
 * @param price Precio sugerido detectado.
 */
data class ScannedCandidate(
    val name: String = CartItem.DEFAULT_NAME,
    val price: Double
)
