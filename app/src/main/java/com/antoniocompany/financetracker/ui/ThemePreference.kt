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
     * Aplica el tema guardado. Se llama al arrancar la app, antes de que se
     * cree ninguna pantalla, para que no se vea un parpadeo del otro tema.
     * Si nunca se ha elegido nada, se sigue el ajuste del sistema.
     */
    fun apply(context: Context) {
        val mode = when (prefs(context).getString(KEY, null)) {
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /** Si la pantalla se esta mostrando ahora mismo en oscuro. */
    fun isDark(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Pasa al tema contrario del que se ve y lo guarda. AppCompat vuelve a
     * crear las pantallas abiertas, que se redibujan con los colores del
     * nuevo tema (values o values-night).
     */
    fun toggle(context: Context) {
        val toDark = !isDark(context)
        prefs(context).edit().putString(KEY, if (toDark) DARK else LIGHT).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (toDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
