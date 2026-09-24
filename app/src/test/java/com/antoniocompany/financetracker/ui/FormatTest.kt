package com.antoniocompany.financetracker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Formatos por idioma. Se fija el idioma en cada llamada para no depender
 * del ordenador donde corran las pruebas.
 *
 * Java separa la cifra de la moneda con un espacio duro (U+00A0 o U+202F):
 * se normaliza a un espacio normal para comparar lo que se ve.
 */
class FormatTest {

    private fun String.plainSpaces() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun `amounts in Spanish use dot for thousands, comma for decimals and the euro after`() {
        assertEquals("12.345,60 €", formatCurrency(12345.6, SPANISH_LOCALE).plainSpaces())
    }

    @Test
    fun `amounts in English use the euro before the figure, as the web does`() {
        assertEquals("€12,345.60", formatCurrency(12345.6, ENGLISH_LOCALE).plainSpaces())
    }

    @Test
    fun `English is still in euros, never in pounds`() {
        val text = formatCurrency(10.0, ENGLISH_LOCALE)
        assertTrue(text, text.contains("€"))
        assertFalse(text, text.contains("£"))
    }

    @Test
    fun `the month title starts with a capital letter in both languages`() {
        assertEquals("Septiembre 2026", formatMonth("2026-09", SPANISH_LOCALE))
        assertEquals("September 2026", formatMonth("2026-09", ENGLISH_LOCALE))
    }

    @Test
    fun `the chip month is short, capitalised and without a dot`() {
        // "Sep" o "Sept" segun la version de los datos de idioma de Java.
        listOf(SPANISH_LOCALE, ENGLISH_LOCALE).forEach { locale ->
            val chip = formatShortMonth("2026-09", locale)
            assertTrue(chip, chip.startsWith("Sep"))
            assertFalse(chip, chip.contains("."))
        }
    }

    @Test
    fun `short dates ignore the time the API sends`() {
        val date = formatShortDate("2026-09-22T00:00:00", ENGLISH_LOCALE)
        assertTrue(date, date.startsWith("22 Sep"))
    }

    @Test
    fun `a malformed month is shown as it came instead of crashing`() {
        assertEquals("sin-fecha", formatMonth("sin-fecha", SPANISH_LOCALE))
    }
}
