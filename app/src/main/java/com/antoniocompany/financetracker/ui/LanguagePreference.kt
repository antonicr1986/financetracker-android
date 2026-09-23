package com.antoniocompany.financetracker.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Idioma de la app, elegido con el selector ES | EN de la barra superior.
 *
 * Usa el idioma por aplicacion de AppCompat: Android recarga las pantallas
 * abiertas con los textos de values/ o values-en/, y lo guarda solo (en
 * Android 13+ el propio sistema; en versiones anteriores, el servicio
 * AppLocalesMetadataHolderService declarado en el manifiesto). No hace falta
 * guardarlo a mano como el tema.
 */
object LanguagePreference {

    const val SPANISH = "es"
    const val ENGLISH = "en"

    /** El idioma que se ve: el elegido o, si no hay, el del sistema. */
    fun current(): String {
        val chosen = AppCompatDelegate.getApplicationLocales()
        val language = if (!chosen.isEmpty) chosen[0]?.language else Locale.getDefault().language
        // Igual que los textos: todo lo que no es ingles se ve en espanol.
        return if (language == ENGLISH) ENGLISH else SPANISH
    }

    fun set(language: String) {
        if (language == current()) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}
