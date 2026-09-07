package com.example.supermetrics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.supermetrics.model.CartItem
import com.example.supermetrics.model.CartUiState
import com.example.supermetrics.model.ScannedCandidate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ViewModel encargado de la lógica de negocio y gestión del estado del carrito.
 */
class CartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<CartSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    /**
     * Actualiza el candidato detectado actualmente por la cámara u OCR.
     */
    fun setCandidate(candidate: ScannedCandidate?) {
        _uiState.update { it.copy(currentCandidate = candidate) }
    }

    /**
     * Sobrecarga de conveniencia para registrar un candidato detectado.
     */
    fun setCandidate(name: String = CartItem.DEFAULT_NAME, price: Double) {
        val resolvedName = name.trim().ifBlank { CartItem.DEFAULT_NAME }
        setCandidate(ScannedCandidate(name = resolvedName, price = price))
    }

    /**
     * Limpia el candidato actual detectado por el escáner.
     */
    fun clearCandidate() {
        _uiState.update { it.copy(currentCandidate = null) }
    }

    /**
     * Actualiza el estado del permiso de la cámara.
     */
    fun updateCameraPermission(isGranted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = isGranted) }
    }

    /**
     * Añade el candidato detectado actualmente al carrito, limpia el candidato
     * y emite un evento para feedback háptico.
     */
    fun addScannedItem() {
        val candidate = _uiState.value.currentCandidate ?: return
        addScannedItem(candidate)
    }

    /**
     * Añade un candidato específico directamente al carrito y emite feedback háptico.
     */
    fun addScannedItem(candidate: ScannedCandidate) {
        val resolvedName = candidate.name.trim().ifBlank { CartItem.DEFAULT_NAME }
        val newItem = CartItem(
            name = resolvedName,
            price = candidate.price
        )

        _uiState.update { state ->
            val updatedItems = state.items + newItem
            state.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems),
                currentCandidate = null
            )
        }

        viewModelScope.launch {
            _sideEffect.send(CartSideEffect.HapticFeedback)
        }
    }

    /**
     * Añade un artículo directamente al carrito (útil para adición manual o pruebas).
     */
    fun addItem(name: String = CartItem.DEFAULT_NAME, price: Double) {
        val resolvedName = name.trim().ifBlank { CartItem.DEFAULT_NAME }
        val newItem = CartItem(
            name = resolvedName,
            price = price
        )

        _uiState.update { state ->
            val updatedItems = state.items + newItem
            state.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems)
            )
        }
    }

    /**
     * Elimina un producto específico del carrito y recalcula el total.
     */
    fun removeItem(item: CartItem) {
        _uiState.update { state ->
            val updatedItems = state.items.filterNot { it.id == item.id }
            state.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems)
            )
        }
    }

    /**
     * Deshace la última adición realizada al carrito.
     */
    fun undoLast() {
        _uiState.update { state ->
            if (state.items.isEmpty()) return@update state
            val updatedItems = state.items.dropLast(1)
            state.copy(
                items = updatedItems,
                total = calculateTotal(updatedItems)
            )
        }
    }

    /**
     * Vacía el carrito por completo.
     */
    fun clearCart() {
        _uiState.update { state ->
            state.copy(
                items = emptyList(),
                total = 0.0
            )
        }
    }

    /**
     * Calcula la suma total de los precios asegurando redondeo a dos decimales.
     */
    private fun calculateTotal(items: List<CartItem>): Double {
        val sum = items.sumOf { it.price }
        return BigDecimal.valueOf(sum)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
