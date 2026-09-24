package com.antoniocompany.financetracker.ui

import android.app.Activity
import android.content.Intent
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

private fun ViewTopBarBinding.bindThemeToggle() {
    val context = root.context
    // Luna en claro (para pasar a oscuro), sol en oscuro (para volver).
    themeToggleButton.setIconResource(
        if (ThemePreference.isDark(context)) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
    )
    themeToggleButton.setOnClickListener {
        ThemePreference.toggle(context)
    }
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
