package com.example.supermetrics

import com.example.supermetrics.camera.PriceTagParser
import com.example.supermetrics.camera.TextBoundingBox
import com.example.supermetrics.camera.TextLineInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PriceTagParserTest {

    private fun createLine(text: String, top: Int, bottom: Int, left: Int = 100, right: Int = 400): TextLineInfo {
        val box = TextBoundingBox(left = left, top = top, right = right, bottom = bottom)
        return TextLineInfo(text = text, boundingBox = box)
    }

    @Test
    fun parseCandidate_withStandardTag_extractsNameAndPrice() {
        val lines = listOf(
            createLine("CEREAL DE CHOCOLATE 500G", top = 100, bottom = 140),
            createLine("$34.90", top = 160, bottom = 220)
        )

        val candidate = PriceTagParser.parseCandidate(lines)
        assertNotNull(candidate)
        assertEquals("CEREAL DE CHOCOLATE 500G", candidate?.name)
        assertEquals(34.90, candidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun parseCandidate_withCommaDecimal_convertsToDoubleCorrectly() {
        val lines = listOf(
            createLine("JUGO DE NARANJA 1L", top = 100, bottom = 140),
            createLine("18,50", top = 160, bottom = 220)
        )

        val candidate = PriceTagParser.parseCandidate(lines)
        assertNotNull(candidate)
        assertEquals("JUGO DE NARANJA 1L", candidate?.name)
        assertEquals(18.50, candidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun parseCandidate_withDatesAndBarcodes_filtersNoise() {
        val lines = listOf(
            createLine("EXP 15/12/2026", top = 50, bottom = 80),
            createLine("PASTA DENTAL 100ML", top = 100, bottom = 140),
            createLine("$12.50", top = 160, bottom = 220),
            createLine("7501031311309", top = 240, bottom = 280)
        )

        val candidate = PriceTagParser.parseCandidate(lines)
        assertNotNull(candidate)
        assertEquals("PASTA DENTAL 100ML", candidate?.name)
        assertEquals(12.50, candidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun parseCandidate_whenNoNameAbove_defaultsToProducto() {
        val lines = listOf(
            createLine("$120.00", top = 100, bottom = 180)
        )

        val candidate = PriceTagParser.parseCandidate(lines)
        assertNotNull(candidate)
        assertEquals("Producto", candidate?.name)
        assertEquals(120.00, candidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun parseCandidate_withReservedKeywordsOnly_defaultsToProducto() {
        val lines = listOf(
            createLine("PRECIO OFERTA", top = 100, bottom = 130),
            createLine("$45.00", top = 150, bottom = 210)
        )

        val candidate = PriceTagParser.parseCandidate(lines)
        assertNotNull(candidate)
        assertEquals("Producto", candidate?.name)
        assertEquals(45.00, candidate?.price ?: 0.0, 0.001)
    }

    @Test
    fun parseCandidate_withEmptyList_returnsNull() {
        val candidate = PriceTagParser.parseCandidate(emptyList())
        assertNull(candidate)
    }
}
