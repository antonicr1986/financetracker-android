package com.antoniocompany.financetracker

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.antoniocompany.financetracker.ui.LanguagePreference
import com.antoniocompany.financetracker.ui.ThemePreference

/**
 * Base de todas las pantallas: cambia de tema o idioma sin parpadeo.
 *
 * Por defecto Android aplica esos cambios con recreate(), que quita la ventana
 * vieja antes de tener lista la nueva: ese hueco es el parpadeo. Aqui las
 * pantallas declaran en el manifiesto que gestionan uiMode y locale ellas
 * mismas, asi que Android no las recrea: avisa con onConfigurationChanged y
 * la pantalla se reinicia sola con un fundido. El sistema funde las dos
 * ventanas y la vieja no desaparece hasta que la nueva se ha dibujado.
 *
 * El estado de la pantalla (lo escrito en los campos, etc.) viaja en el
 * Intent y se restaura como si Android la hubiera recreado.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Tema e idioma con los que se pinto esta pantalla. */
    private var appliedLook: String? = null

    /**
     * true si esta pantalla viene de un reinicio por cambio de tema o idioma.
     * El panel lo usa para no volver a pedir los datos a la API.
     */
    protected var restartedForLook = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        val carried = intent.getBundleExtra(EXTRA_STATE)
        intent.removeExtra(EXTRA_STATE)
        restartedForLook = carried != null
        super.onCreate(savedInstanceState ?: carried)
        appliedLook = currentLook()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // La pantalla visible se reinicia al momento. Las que estan detras
        // esperan a onResume: reiniciarlas ahora abriria ventanas de mas.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) restartIfLookChanged()
    }

    override fun onResume() {
        super.onResume()
        restartIfLookChanged()
    }

    private fun restartIfLookChanged() {
        if (isFinishing || appliedLook == currentLook()) return

        val state = Bundle().also { onSaveInstanceState(it) }
        // Un Intent nuevo y limpio, no una copia del actual: el de la pantalla
        // de arranque es el del icono (MAIN/LAUNCHER + NEW_TASK) y relanzarlo
        // solo trae al frente la tarea existente sin crear pantalla nueva; el
        // finish() de abajo cerraria entonces la unica que hay y la app se
        // cerraria. Tampoco se copian los flags del de cerrar sesion
        // (CLEAR_TASK). Solo viajan los extras y el estado.
        val restart = Intent(this, javaClass)
            .putExtras(intent.extras ?: Bundle())
            .putExtra(EXTRA_STATE, state)
        // Si a esta pantalla la abrieron esperando un resultado (el alta de
        // movimiento), la nueva se lo devolvera a quien lo esperaba.
        if (callingActivity != null) restart.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)

        startActivity(restart)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun currentLook(): String =
        "${ThemePreference.isDark(this)}|${LanguagePreference.current()}"

    private companion object {
        const val EXTRA_STATE = "com.antoniocompany.financetracker.RESTART_STATE"
    }
}
