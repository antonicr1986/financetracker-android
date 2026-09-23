package com.antoniocompany.financetracker.ui

import androidx.appcompat.app.AppCompatDelegate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formatos de la interfaz.
 *
 * Siguen al idioma de la app, igual que la web (lib/i18n/format.ts): espanol
 * con es-ES e ingles con en-GB. En-GB y no en-US porque la moneda sigue siendo
 * el euro, y en-GB lo escribe "€1,234.56", que es lo natural para un europeo
 * leyendo en ingles.
 */
private val SPANISH: Locale = Locale.forLanguageTag("es-ES")
private val ENGLISH: Locale = Locale.forLanguageTag("en-GB")
private val EURO: Currency = Currency.getInstance("EUR")

/**
 * El idioma que se ve ahora mismo. Primero el elegido en la app (el selector
 * ES | EN); si no se ha elegido ninguno, el del sistema. Cualquier idioma que
 * no sea ingles se muestra en espanol, igual que los textos.
 */
private fun uiLocale(): Locale {
    val chosen = AppCompatDelegate.getApplicationLocales()
    val language = if (!chosen.isEmpty) chosen[0]?.language else Locale.getDefault().language
    return if (language == "en") ENGLISH else SPANISH
}

/**
 * Los formateadores de un idioma, creados una vez y reutilizados. Se piden
 * en cada llamada a traves de formats(), asi que al cambiar de idioma se
 * pasa a usar los del otro sin tener que reiniciar nada.
 *
 * SimpleDateFormat y no java.time porque este proyecto tiene minSdk 24 y
 * java.time necesita API 26 o activar el desugaring de la biblioteca estandar.
 * No se comparten entre hilos: aqui solo se usan desde el hilo principal.
 */
private class LocaleFormats(val locale: Locale) {
    // getCurrencyInstance(en-GB) usaria libras: la moneda se fija a euros.
    val currency: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply { currency = EURO }
    val dayMonth = SimpleDateFormat("d MMM", locale)
    val monthYear = SimpleDateFormat("LLLL yyyy", locale)
    val shortMonth = SimpleDateFormat("LLL", locale)
}

private val formatsCache = mutableMapOf<Locale, LocaleFormats>()

private fun formats(): LocaleFormats {
    val locale = uiLocale()
    return formatsCache.getOrPut(locale) { LocaleFormats(locale) }
}

private val isoParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val monthParser = SimpleDateFormat("yyyy-MM", Locale.US)

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

fun formatCurrency(amount: Double): String = formats().currency.format(amount)

/** Hoy, en la zona del dispositivo, como "AAAA-MM-DD". */
fun todayIso(): String = isoLocalFormat.format(Date())

/** Milisegundos UTC del selector -> "AAAA-MM-DD". */
fun utcMillisToIso(millis: Long): String = isoUtcFormat.format(Date(millis))

/** "AAAA-MM-DD" -> milisegundos UTC, para abrir el selector en esa fecha. */
fun isoToUtcMillis(iso: String): Long =
    isoUtcFormat.parse(iso)?.time ?: System.currentTimeMillis()

/** "2026-09-22T00:00:00" -> "22 sept" / "22 Sept". */
fun formatShortDate(isoDate: String): String {
    val parsed = isoParser.parse(isoDate.take(10)) ?: return isoDate.take(10)
    return formats().dayMonth.format(parsed)
}

/** "2026-09" -> "Sept". Para los chips, donde no cabe el mes entero. */
fun formatShortMonth(monthKey: String): String {
    val parsed = monthParser.parse(monthKey) ?: return monthKey
    val f = formats()
    return f.shortMonth.format(parsed)
        .replace(".", "")
        .replaceFirstChar { it.uppercase(f.locale) }
}

/** "2026-09" -> "Septiembre 2026" / "September 2026". */
fun formatMonth(monthKey: String): String {
    val parsed = monthParser.parse(monthKey) ?: return monthKey
    val f = formats()
    return f.monthYear.format(parsed)
        .replaceFirstChar { it.uppercase(f.locale) }
}
