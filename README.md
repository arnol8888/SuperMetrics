# SuperMetrics 🛒📱

**SuperMetrics** es una aplicación Android nativa desarrollada con **Jetpack Compose** y **Material 3** en Kotlin, diseñada para escanear precios de supermercado en tiempo real directamente desde las etiquetas de góndola con la cámara y sumarlos automáticamente al carrito de compras.

---

## ✨ Características Principales

* 🎯 **Góndola AI (Local Vision Engine):**
  * Integración con **CameraX** y **Google ML Kit Text Recognition** para análisis en tiempo real en el dispositivo (sin necesidad de conexión a internet).
  * **Región de Interés (ROI) central**: Enfoque exclusivo en la etiqueta seleccionada, ignorando ruido exterior como códigos de barra de inventario y fechas de caducidad.
  * **Parser Inteligente de Precios**: Extracción automática de precios (`$12.50`, `120.00`, etc.) y detección contextual del nombre del producto ubicado sobre el precio.
* ⚡ **Experiencia de Usuario 100% Dark Mode:**
  * Interfaz futurista con acentos neón y visor con retícula HUD.
  * Tarjeta flotante interactiva de candidato detectado: `+ Agregar [Producto] - $[Precio]`.
  * **Feedback Háptico** integrado al pulsar agregar.
* 🛍️ **Gestión Completa del Carrito (MVVM):**
  * **Panel Inferior / Bottom Sheet** persistente con Total Acumulado en tipografía grande y conteo de artículos.
  * Botón directo de **Deshacer (Undo)** y opción para **Vaciar Carrito**.
  * Lista deslizable con eliminación individual de productos.
  * Precisión matemática controlada para divisas y monedas.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Kotlin 2.x
* **UI:** Jetpack Compose & Material Design 3 (Dark Mode)
* **Arquitectura:** MVVM (Model-View-ViewModel) + UDF (Unidirectional Data Flow) con `StateFlow`
* **Cámara & ML:**
  * CameraX 1.3.4 (Core, Camera2, Lifecycle, View)
  * Google ML Kit Text Recognition
* **Concurrencia:** Kotlin Coroutines & Flows
* **Testing:** JUnit 4 & Kotlinx Coroutines Test

---

## 🚀 Compilación e Instalación

Para compilar el proyecto en modo debug:

```bash
./gradlew assembleDebug
```

El APK se generará en:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para ejecutar las pruebas unitarias:
```bash
./gradlew testDebugUnitTest
```
