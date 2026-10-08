package com.antoniocompany.financetracker.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Recuerda si el usuario ha elegido tema claro u oscuro, igual que la web lo
 * guarda en localStorage con la clave "theme".
 *
 * Va en un fichero de preferencias propio y no en el de la sesion: al cerrar
 * sesion se borra la sesion, pero el tema elegido tiene que seguir.
 */
object ThemePreference {

    private const val FILE_NAME = "theme"
    private const val KEY = "theme"
    private const val DARK = "dark"
    private const val LIGHT = "light"

    /**
     * Las tres opciones del menu del tema. SYSTEM es la de por defecto: la
     * app se ve clara u oscura segun lo que tenga puesto el movil, y cambia
     * sola si el usuario cambia el ajuste del sistema.
     */
    enum class Mode { SYSTEM, LIGHT, DARK }

    /**
     * Aplica el tema guardado. Se llama al arrancar la app, antes de que se
     * cree ninguna pantalla, para que no se vea un parpadeo del otro tema.
     * Si nunca se ha elegido nada, se sigue el ajuste del sistema.
     */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(current(context)))
    }

    /** La opcion elegida por el usuario (SYSTEM si nunca ha elegido). */
    fun current(context: Context): Mode = when (prefs(context).getString(KEY, null)) {
        DARK -> Mode.DARK
        LIGHT -> Mode.LIGHT
        else -> Mode.SYSTEM
    }

    /** Si la pantalla se esta mostrando ahora mismo en oscuro. */
    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Guarda la opcion y la aplica. Si cambia lo que se ve, AppCompat avisa a
     * las pantallas abiertas, que se redibujan con los colores del nuevo tema
     * (values o values-night). SYSTEM borra la clave en vez de guardar
     * "system", asi una instalacion nueva y "Segun el sistema" son lo mismo.
     */
    fun set(context: Context, mode: Mode) {
        prefs(context).edit().apply {
            when (mode) {
                Mode.SYSTEM -> remove(KEY)
                Mode.LIGHT -> putString(KEY, LIGHT)
                Mode.DARK -> putString(KEY, DARK)
            }
        }.apply()
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
    }

    private fun nightModeFor(mode: Mode) = when (mode) {
        Mode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        Mode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        Mode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
