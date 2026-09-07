package com.example.supermetrics.camera

import com.example.supermetrics.model.CartItem
import com.example.supermetrics.model.ScannedCandidate

/**
 * Representa una caja delimitadora 2D independiente del framework de Android,
 * facilitando pruebas unitarias en la JVM sin necesidad de mocks.
 */
data class TextBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2

    fun contains(x: Int, y: Int): Boolean = x in left..right && y in top..bottom
}

/**
 * Representa una línea de texto reconocida con su posición en la imagen.
 */
data class TextLineInfo(
    val text: String,
    val boundingBox: TextBoundingBox
)

/**
 * Motor de análisis sintáctico y extracción de precios y nombres de productos
 * a partir de las líneas detectadas por el OCR.
 */
object PriceTagParser {

    // Regex para detectar fechas de caducidad o elaboración (ej. 12/04/2025, 2026-05-10, 15/08/26)
    private val DATE_REGEX = Regex("""\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b|\b\d{4}[/-]\d{1,2}[/-]\d{1,2}\b""")

    // Regex para detectar códigos de barras largos (entre 7 y 14 dígitos aislados)
    private val BARCODE_REGEX = Regex("""\b\d{7,14}\b""")

    // Palabras reservadas que no deben considerarse nombre del producto
    private val IGNORED_WORDS = setOf(
        "PRECIO", "TOTAL", "OFERTA", "PVP", "P.V.P.", "IVA", "IMPUESTO",
        "PESO", "CADUCIDAD", "CAD", "EXP", "VTO", "LOTE", "FECHA", "UNITARIO",
        "PIEZA", "PZA", "PZ", "KILO", "KG", "GRAMO", "GR", "G", "NETO"
    )

    // Regex para precios con o sin símbolo de moneda
    // Ejemplos: $12.50, $ 12.50, $12,50, 120.00, $1,250.00, 45.99
    private val PRICE_REGEX = Regex(
        """(?:\$|USD|\b)\s*([0-9]{1,4}(?:[.,][0-9]{2})|[0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})|\b[0-9]{1,4}\b)"""
    )

    /**
     * Analiza las líneas presentes dentro del ROI para extraer el candidato más probable (nombre y precio).
     *
     * @param lines Lista de líneas dentro de la región de interés.
     * @return [ScannedCandidate] si se encuentra un precio válido, o null en caso contrario.
     */
    fun parseCandidate(lines: List<TextLineInfo>): ScannedCandidate? {
        if (lines.isEmpty()) return null

        // 1. Descartar líneas que sean únicamente códigos de barras o fechas
        val cleanLines = lines.filterNot { isBarcodeOrDate(it.text) }
        if (cleanLines.isEmpty()) return null

        // 2. Extraer candidatos de precio
        val priceMatches = mutableListOf<PriceMatch>()
        for (line in cleanLines) {
            val matches = PRICE_REGEX.findAll(line.text)
            for (match in matches) {
                val rawValue = match.groupValues[1]
                val normalizedPrice = parseToDouble(rawValue)

                // Rango razonable de precio en supermercado (0.01 a 99,999.00)
                if (normalizedPrice != null && normalizedPrice > 0.05 && normalizedPrice < 100_000.0) {
                    val hasCurrencySymbol = match.value.contains("$") || match.value.contains("USD")
                    val isExplicitDecimal = rawValue.contains(".") || rawValue.contains(",")
                    priceMatches.add(
                        PriceMatch(
                            price = normalizedPrice,
                            line = line,
                            hasCurrencySymbol = hasCurrencySymbol,
                            isExplicitDecimal = isExplicitDecimal,
                            fontHeight = line.boundingBox.height
                        )
                    )
                }
            }
        }

        if (priceMatches.isEmpty()) return null

        // En etiquetas de góndola el precio principal tiene mayor tamaño de fuente y/o signo $
        val bestPriceMatch = priceMatches.maxWithOrNull(
            compareBy<PriceMatch> { it.hasCurrencySymbol }
                .thenBy { it.isExplicitDecimal }
                .thenBy { it.fontHeight }
                .thenBy { it.price }
        ) ?: return null

        // 3. Buscar nombre del producto: líneas situadas por encima del precio dentro del ROI
        val priceTopY = bestPriceMatch.line.boundingBox.top
        val priceCenterY = bestPriceMatch.line.boundingBox.centerY

        val candidateNameLines = cleanLines.filter {
            (it.boundingBox.bottom <= priceTopY || it.boundingBox.centerY < priceCenterY) &&
                    it != bestPriceMatch.line
        }

        val detectedName = candidateNameLines
            .map { it.text.trim() }
            .filter { isValidProductName(it) }
            .maxByOrNull { it.length } // Selecciona la línea de descripción más completa
            ?: CartItem.DEFAULT_NAME

        return ScannedCandidate(
            name = detectedName,
            price = bestPriceMatch.price
        )
    }

    private fun isBarcodeOrDate(text: String): Boolean {
        val trimmed = text.trim()
        if (DATE_REGEX.containsMatchIn(trimmed)) return true
        if (BARCODE_REGEX.matches(trimmed)) return true
        return false
    }

    private fun isValidProductName(text: String): Boolean {
        if (text.length < 2) return false
        val words = text.uppercase().split(Regex("""\s+"""))
        // Descartar si solo contiene palabras reservadas (ej. "PRECIO TOTAL")
        if (words.all { it in IGNORED_WORDS }) return false
        // Descartar si son solo números o puntuación
        if (text.all { it.isDigit() || it.isWhitespace() || it == '.' || it == ',' || it == '$' || it == '-' }) {
            return false
        }
        return true
    }

    private fun parseToDouble(raw: String): Double? {
        val cleaned = raw.trim()
        val standardized = if (cleaned.contains(",") && !cleaned.contains(".")) {
            // Formato decimal con coma: 12,50 -> 12.50
            cleaned.replace(",", ".")
        } else if (cleaned.contains(",") && cleaned.contains(".")) {
            // Formato con miles y decimales: 1,250.50 o 1.250,50
            if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                cleaned.replace(".", "").replace(",", ".")
            } else {
                cleaned.replace(",", "")
            }
        } else {
            cleaned
        }
        return standardized.toDoubleOrNull()
    }

    private data class PriceMatch(
        val price: Double,
        val line: TextLineInfo,
        val hasCurrencySymbol: Boolean,
        val isExplicitDecimal: Boolean,
        val fontHeight: Int
    )
}
