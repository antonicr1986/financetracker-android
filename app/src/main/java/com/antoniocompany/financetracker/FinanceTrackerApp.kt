package com.antoniocompany.financetracker

import android.app.Application
import android.content.Intent
import com.antoniocompany.financetracker.data.ApiClient
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

        // La API ha rechazado la sesion desde cualquier pantalla: al acceso,
        // con el aviso de sesion caducada y sin pantallas detras a las que
        // volver con "atras".
        ApiClient.onSessionRejected = {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(MainActivity.EXTRA_SESSION_EXPIRED, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }
}
