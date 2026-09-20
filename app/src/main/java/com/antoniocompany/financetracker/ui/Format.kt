package com.antoniocompany.financetracker.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formatos de la interfaz.
 *
 * El idioma esta fijado en espanol porque los textos de la aplicacion tambien
 * lo estan. El dia que se anada ingles bastara con cambiar esta constante y
 * anadir un res/values-en, y las cifras y las fechas seguiran al idioma solas.
 */
private val LOCALE: Locale = Locale.forLanguageTag("es-ES")

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
private val shortMonthFormat = SimpleDateFormat("LLL", LOCALE)

/**
 * El selector de fecha de Material trabaja en UTC: devuelve la medianoche UTC
 * del dia elegido. Formatear esos milisegundos con la zona del dispositivo
 * restaria un dia a quien este al oeste de Greenwich, asi que este formateador
 * va fijado a UTC. Es el mismo problema de siempre, por el otro lado.
 */
private val isoUtcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private val isoLocalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

/** Hoy, en la zona del dispositivo, como "AAAA-MM-DD". */
fun todayIso(): String = isoLocalFormat.format(Date())

/** Milisegundos UTC del selector -> "AAAA-MM-DD". */
fun utcMillisToIso(millis: Long): String = isoUtcFormat.format(Date(millis))

/** "AAAA-MM-DD" -> milisegundos UTC, para abrir el selector en esa fecha. */
fun isoToUtcMillis(iso: String): Long =
    isoUtcFormat.parse(iso)?.time ?: System.currentTimeMillis()

/** "2026-09-22T00:00:00" -> "22 sept". */
fun formatShortDate(isoDate: String): String {
    val parsed = isoParser.parse(isoDate.take(10)) ?: return isoDate.take(10)
    return dayMonthFormat.format(parsed)
}

/** "2026-09" -> "sept". Para los chips, donde no cabe el mes entero. */
fun formatShortMonth(monthKey: String): String {
    val parsed = monthParser.parse(monthKey) ?: return monthKey
    return shortMonthFormat.format(parsed)
        .replace(".", "")
        .replaceFirstChar { it.uppercase(LOCALE) }
}

/** "2026-09" -> "septiembre 2026". */
fun formatMonth(monthKey: String): String {
    val parsed = monthParser.parse(monthKey) ?: return monthKey
    return monthYearFormat.format(parsed)
        .replaceFirstChar { it.uppercase(LOCALE) }
}
