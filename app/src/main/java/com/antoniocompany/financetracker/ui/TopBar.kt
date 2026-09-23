package com.antoniocompany.financetracker.ui

import com.antoniocompany.financetracker.R
import com.antoniocompany.financetracker.databinding.ViewTopBarBinding

/**
 * Conecta el boton de tema de la barra superior. Esta aqui para que el panel
 * y el login no repitan la misma logica.
 */
fun ViewTopBarBinding.bindThemeToggle() {
    val context = root.context
    // Luna en claro (para pasar a oscuro), sol en oscuro (para volver).
    themeToggleButton.setIconResource(
        if (ThemePreference.isDark(context)) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
    )
    themeToggleButton.setOnClickListener { ThemePreference.toggle(context) }
}
