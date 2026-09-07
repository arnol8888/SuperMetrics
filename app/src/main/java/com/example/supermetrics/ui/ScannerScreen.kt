package com.example.supermetrics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.supermetrics.camera.CameraPermissionContainer
import com.example.supermetrics.camera.CameraScannerView
import com.example.supermetrics.ui.components.CandidateCard
import com.example.supermetrics.ui.components.CartBottomSheetContent
import com.example.supermetrics.ui.theme.DarkBackground
import com.example.supermetrics.ui.theme.DarkSurface
import com.example.supermetrics.ui.theme.NeonCyan
import com.example.supermetrics.ui.theme.NeonGreen

/**
 * Pantalla principal de escaneo de precios y gestión del carrito en tiempo real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: CartViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberBottomSheetScaffoldState()

    // Escucha de eventos de una sola vez (efectos secundarios hápticos y mensajes)
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is CartSideEffect.HapticFeedback -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is CartSideEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 115.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = DarkSurface,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            CartBottomSheetContent(
                uiState = uiState,
                onUndoLast = viewModel::undoLast,
                onClearCart = viewModel::clearCart,
                onRemoveItem = viewModel::removeItem
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Manejador de permisos de cámara reactivo en tiempo de ejecución
            CameraPermissionContainer(
                hasPermission = uiState.hasCameraPermission,
                onPermissionResult = viewModel::updateCameraPermission
            ) {
                // 1. Visor de CameraX en pantalla completa con retícula y máscara ROI
                CameraScannerView(
                    isCandidateDetected = uiState.currentCandidate != null,
                    onCandidateDetected = { candidate ->
                        viewModel.setCandidate(candidate)
                    }
                )
            }

            // Barra superior de la aplicación (Encabezado con estilo moderno)
            TopScannerBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )

            // 2. Tarjeta flotante de candidato detectado
            CandidateCard(
                candidate = uiState.currentCandidate,
                onAddCandidate = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.addScannedItem()
                },
                onDismissCandidate = viewModel::clearCandidate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * Barra superior de estado y marca visual de SuperMetrics con degradado sutil.
 */
@Composable
private fun TopScannerBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.75f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "SuperMetrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = CircleShape,
                color = Color(0xFF1E1E28).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333348))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NeonGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Góndola AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
