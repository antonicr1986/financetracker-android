package com.antoniocompany.financetracker

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.antoniocompany.financetracker.data.ApiClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.antoniocompany.financetracker.ui.ThemePreference

/**
 * Se ejecuta una vez al arrancar la app, antes que cualquier pantalla. Es el
 * sitio para aplicar el tema guardado: hacerlo en una Activity llegaria tarde
 * y se veria un instante el tema equivocado.
 */
class FinanceTrackerApp : Application() {

    /** La pantalla visible, para mostrar sobre ella el aviso de actualizar. */
    private var visibleActivity: Activity? = null

    /** El aviso de actualizar abierto, para no abrir otro encima. */
    private var updateDialog: Dialog? = null

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

        // 426 de la API: esta version ya no esta soportada. Un solo aviso para
        // toda la app, sobre la pantalla que este abierta. El interceptor corre
        // en un hilo de red, asi que el dialogo se abre en el hilo principal.
        registerActivityLifecycleCallbacks(VisibleActivityTracker())
        ApiClient.onUpgradeRequired = {
            Handler(Looper.getMainLooper()).post { visibleActivity?.let(::showUpdateRequired) }
        }
    }

    private fun showUpdateRequired(activity: Activity) {
        if (updateDialog?.isShowing == true) return

        updateDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.update_title)
            .setMessage(R.string.update_message)
            .setPositiveButton(R.string.update_download) { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LATEST_APK_URL)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { updateDialog = null }
            .show()
    }

    private inner class VisibleActivityTracker : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            visibleActivity = activity
        }

        override fun onActivityPaused(activity: Activity) {
            if (visibleActivity === activity) visibleActivity = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private companion object {
        const val LATEST_APK_URL =
            "https://github.com/antonicr1986/financetracker-android/releases/latest/download/financetracker.apk"
    }
}
