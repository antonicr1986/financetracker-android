package com.antoniocompany.financetracker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
 * la pantalla se reinicia sola, sin animacion (overridePendingTransition(0,
 * 0)): la vieja no desaparece hasta que la nueva esta dibujada, y sin
 * fundido de por medio el cambio se ve instantaneo.
 *
 * El estado de la pantalla (lo escrito en los campos, etc.) viaja en el
 * Intent y se restaura como si Android la hubiera recreado.
 *
 * Ojo: pasar ese estado a `super.onCreate(bundle)` no basta para que vuelva
 * el texto de los EditText. Ese restablecimiento automático (por tener
 * `android:id`) solo lo dispara el sistema cuando es EL quien recrea la
 * Activity; aqui la recreamos nosotros con startActivity+finish, asi que
 * hay que guardar y devolver el estado de las vistas a mano con
 * `window.saveHierarchyState()`/`restoreHierarchyState()` (ver
 * `restartIfLookChanged` y `onPostCreate`).
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

    /**
     * Estado de las vistas (texto de los EditText, etc.) a la espera de que
     * `setContentView` las cree. No se puede restaurar aqui todavia: en
     * `onCreate` la pantalla hija aun no ha inflado su layout.
     */
    private var pendingViewState: Bundle? = null

    /** Si el teclado estaba abierto antes del reinicio, para no cerrarlo ni abrirlo de mas. */
    private var wasKeyboardVisible = false

    /**
     * Para pedir el teclado una sola vez al recuperar el foco, no cada vez
     * que la ventana gana el foco (tambien pasa al volver de segundo plano).
     */
    private var keyboardRestorePending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val carried = intent.getBundleExtra(EXTRA_STATE)
        intent.removeExtra(EXTRA_STATE)
        restartedForLook = carried != null
        pendingViewState = carried?.getBundle(KEY_VIEW_STATE)
        wasKeyboardVisible = carried?.getBoolean(KEY_KEYBOARD_VISIBLE) ?: false
        super.onCreate(savedInstanceState ?: carried)
        appliedLook = currentLook()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Aqui el layout de la pantalla hija ya esta inflado (se hizo en su
        // onCreate, antes de que este se llame). Ahora si se puede devolver
        // el texto escrito.
        pendingViewState?.let { window.restoreHierarchyState(it) }
        pendingViewState = null

        // Si el campo que tenia el foco lo recupera, Android decide solo si
        // hace falta teclado, y esa decision no siempre coincide con lo que
        // habia antes: a veces se ve un parpadeo de cerrar y volver a abrir.
        // Si no estaba abierto, se quita el foco. Si lo estaba, no se toca
        // aqui: se pide explicitamente en onWindowFocusChanged, que es el
        // primer momento en que la ventana nueva tiene el foco de verdad.
        if (restartedForLook) {
            if (wasKeyboardVisible) keyboardRestorePending = true
            else hideKeyboardAndClearFocus()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !keyboardRestorePending) return
        keyboardRestorePending = false
        val focused = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(focused, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboardAndClearFocus() {
        val focused = currentFocus ?: return
        focused.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(focused.windowToken, 0)
    }

    private fun isKeyboardVisible(): Boolean =
        ViewCompat.getRootWindowInsets(window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) ?: false

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
        state.putBundle(KEY_VIEW_STATE, window.saveHierarchyState())
        state.putBoolean(KEY_KEYBOARD_VISIBLE, isKeyboardVisible())
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
        // Sin fundido: un cambio de tema en el movil se espera instantaneo,
        // no una animacion que se note. El fundido (fade_in/fade_out) ya no
        // hace falta para evitar el hueco: eso lo resuelve configChanges +
        // el propio startActivity, que no quita la ventana vieja hasta que
        // la nueva esta dibujada.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun currentLook(): String =
        "${ThemePreference.isDark(this)}|${LanguagePreference.current()}"

    private companion object {
        const val EXTRA_STATE = "com.antoniocompany.financetracker.RESTART_STATE"
        const val KEY_VIEW_STATE = "com.antoniocompany.financetracker.VIEW_STATE"
        const val KEY_KEYBOARD_VISIBLE = "com.antoniocompany.financetracker.KEYBOARD_VISIBLE"
    }
}
