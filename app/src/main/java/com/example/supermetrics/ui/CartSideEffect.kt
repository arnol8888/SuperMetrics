package com.example.supermetrics.ui

/**
 * Efectos secundarios de un solo uso emitidos por el ViewModel hacia la vista (Compose).
 */
sealed interface CartSideEffect {
    /**
     * Notifica a la UI que debe disparar una vibración o feedback háptico.
     */
    data object HapticFeedback : CartSideEffect

    /**
     * Notifica a la UI para mostrar un mensaje o snackbar.
     */
    data class ShowMessage(val message: String) : CartSideEffect
}
