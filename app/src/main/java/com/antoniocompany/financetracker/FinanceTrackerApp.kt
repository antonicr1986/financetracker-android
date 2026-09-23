package com.antoniocompany.financetracker

import android.app.Application
import com.antoniocompany.financetracker.ui.ThemePreference

/**
 * Se ejecuta una vez al arrancar la app, antes que cualquier pantalla. Es el
 * sitio para aplicar el tema guardado: hacerlo en una Activity llegaria tarde
 * y se veria un instante el tema equivocado.
 */
class FinanceTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreference.apply(this)
    }
}
