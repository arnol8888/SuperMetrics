package com.example.supermetrics.model

/**
 * Estado UI inmutable para la pantalla del carrito y escáner de precios.
 *
 * @param items Lista de productos actualmente en el carrito.
 * @param total Total acumulado del costo de los productos en el carrito.
 * @param currentCandidate Candidato detectado en tiempo real por el escáner (nombre y precio sugeridos).
 * @param hasCameraPermission Indica si se ha otorgado el permiso de uso de la cámara.
 */
data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val currentCandidate: ScannedCandidate? = null,
    val hasCameraPermission: Boolean = false
) {
    /**
     * Cantidad total de artículos agregados al carrito.
     */
    val itemCount: Int
        get() = items.size
}
