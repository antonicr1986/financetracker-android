package com.antoniocompany.financetracker.ui

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import androidx.appcompat.widget.PopupMenu
import com.antoniocompany.financetracker.LoginActivity
import com.antoniocompany.financetracker.R
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.databinding.ViewTopBarBinding

/**
 * Conecta los botones de la barra superior. Todas las pantallas la usan, asi
 * que la logica vive aqui y no repetida en cada Activity.
 */
fun ViewTopBarBinding.bind(activity: Activity) {
    bindLanguageToggle()
    bindThemeToggle()
    bindSignOut(activity)
}

/**
 * El boton muestra el idioma al que se cambia, no el actual: en espanol pone
 * "EN" y en ingles "ES". Asi ocupa un solo hueco en la barra.
 */
private fun ViewTopBarBinding.bindLanguageToggle() {
    val toEnglish = LanguagePreference.current() == LanguagePreference.SPANISH
    languageButton.setText(if (toEnglish) R.string.language_en else R.string.language_es)
    languageButton.contentDescription = root.context.getString(
        if (toEnglish) R.string.language_switch_to_en else R.string.language_switch_to_es
    )
    languageButton.setOnClickListener {
        LanguagePreference.set(
            if (toEnglish) LanguagePreference.ENGLISH else LanguagePreference.SPANISH
        )
    }
}

/**
 * El boton abre un menu con las tres opciones (segun el sistema, claro,
 * oscuro) y la elegida aparece marcada. El icono muestra la opcion actual:
 * medio circulo para "segun el sistema", sol para claro y luna para oscuro.
 */
private fun ViewTopBarBinding.bindThemeToggle() {
    val context = root.context
    showThemeIcon()
    themeToggleButton.setOnClickListener { anchor ->
        val popup = PopupMenu(context, anchor, Gravity.END)
        popup.menuInflater.inflate(R.menu.theme_menu, popup.menu)
        val checkedId = when (ThemePreference.current(context)) {
            ThemePreference.Mode.SYSTEM -> R.id.theme_system
            ThemePreference.Mode.LIGHT -> R.id.theme_light
            ThemePreference.Mode.DARK -> R.id.theme_dark
        }
        popup.menu.findItem(checkedId).isChecked = true
        popup.setOnMenuItemClickListener { item ->
            val mode = when (item.itemId) {
                R.id.theme_light -> ThemePreference.Mode.LIGHT
                R.id.theme_dark -> ThemePreference.Mode.DARK
                else -> ThemePreference.Mode.SYSTEM
            }
            ThemePreference.set(context, mode)
            // Si la opcion nueva se ve igual que la anterior (p. ej. pasar de
            // "oscuro" a "segun el sistema" con el movil en oscuro), la
            // pantalla no se redibuja: hay que cambiar el icono a mano.
            showThemeIcon()
            true
        }
        popup.show()
    }
}

private fun ViewTopBarBinding.showThemeIcon() {
    val context = root.context
    val mode = ThemePreference.current(context)
    themeToggleButton.setIconResource(
        when (mode) {
            ThemePreference.Mode.SYSTEM -> R.drawable.ic_theme_system
            ThemePreference.Mode.LIGHT -> R.drawable.ic_theme_light
            ThemePreference.Mode.DARK -> R.drawable.ic_theme_dark
        }
    )
    themeToggleButton.contentDescription = context.getString(
        R.string.theme_button_description,
        context.getString(
            when (mode) {
                ThemePreference.Mode.SYSTEM -> R.string.theme_system
                ThemePreference.Mode.LIGHT -> R.string.theme_light
                ThemePreference.Mode.DARK -> R.string.theme_dark
            }
        )
    )
}

/**
 * "Salir" esta siempre en la barra, como en la web, pero solo se puede pulsar
 * si hay sesion: en el login y el registro aparece deshabilitado.
 */
private fun ViewTopBarBinding.bindSignOut(activity: Activity) {
    val session = SessionStore(activity)
    signOutButton.isEnabled = session.hasSession
    signOutButton.setOnClickListener {
        session.clear()
        // Se vacia la pila: "atras" desde el login no debe volver a una
        // pantalla que necesitaba la sesion que se acaba de cerrar.
        activity.startActivity(
            Intent(activity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }
}
