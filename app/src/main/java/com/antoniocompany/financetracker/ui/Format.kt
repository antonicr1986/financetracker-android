package com.antoniocompany.financetracker.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Formatos de la interfaz.
 *
 * El idioma esta fijado en espanol porque los textos de la aplicacion tambien
 * lo estan. El dia que se anada ingles bastara con cambiar esta constante y
 * anadir un res/values-en, y las cifras y las fechas seguiran al idioma solas.
 */
private val LOCALE: Locale = Locale("es", "ES")

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE)

/**
 * SimpleDateFormat y no java.time porque este proyecto tiene minSdk 24 y
 * java.time necesita API 26 o activar el desugaring de la biblioteca estandar.
 * No se comparten entre hilos: aqui solo se usan desde el hilo principal.
 */
private val isoParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dayMonthFormat = SimpleDateFormat("d MMM", LOCALE)

private val monthParser = SimpleDateFormat("yyyy-MM", Locale.US)
private val monthYearFormat = SimpleDateFormat("LLLL yyyy", LOCALE)

fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

/** "2026-09-22T00:00:00" -> "22 sept". */
fun formatShortDate(isoDate: String): String {
    val parsed = isoParser.parse(isoDate.take(10)) ?: return isoDate.take(10)
    return dayMonthFormat.format(parsed)
}

/** "2026-09" -> "septiembre 2026". */
fun formatMonth(monthKey: String): String {
    val parsed = monthParser.parse(monthKey) ?: return monthKey
    return monthYearFormat.format(parsed)
        .replaceFirstChar { it.uppercase(LOCALE) }
}
