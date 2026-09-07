package com.example.supermetrics.model

import java.util.UUID

/**
 * Representa un artículo en el carrito de compras.
 *
 * @param id Identificador único del artículo.
 * @param name Nombre del producto ("Producto" por defecto si no se detectó texto).
 * @param price Precio del producto en formato Double.
 * @param timestamp Marca de tiempo en milisegundos cuando se agregó el producto.
 */
data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = DEFAULT_NAME,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_NAME = "Producto"
    }
}
