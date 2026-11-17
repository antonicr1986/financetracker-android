package com.antoniocompany.financetracker

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.apiErrorCode
import com.antoniocompany.financetracker.data.model.MfaCodeRequest
import com.antoniocompany.financetracker.databinding.ActivityMfaBinding
import com.antoniocompany.financetracker.ui.bind
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Activar o desactivar la verificacion en dos pasos.
 *
 * Activarla: la API genera el secreto, se lleva a la app autenticadora (con el
 * enlace otpauth:// o copiando la clave) y se confirma con el primer codigo.
 * Entonces llegan los codigos de recuperacion, que solo se ven esa vez.
 */
class MfaActivity : BaseActivity() {

    private lateinit var binding: ActivityMfaBinding
    private lateinit var session: SessionStore

    private enum class Mode { IDLE, CONFIRM, DISABLE, CODES }

    private var mode = Mode.IDLE
    private var otpAuthUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionStore(this)
        binding = ActivityMfaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.bind(this)

        binding.actionButton.setOnClickListener {
            if (session.mfaEnabled) show(Mode.DISABLE) else startSetup()
        }
        binding.openAppButton.setOnClickListener { openAuthenticator() }
        binding.copySecretButton.setOnClickListener {
            copySensitive(binding.secretText.text.toString())
        }
        binding.confirmButton.setOnClickListener {
            if (mode == Mode.DISABLE) disable() else confirm()
        }
        binding.cancelButton.setOnClickListener { show(Mode.IDLE) }
        binding.copyCodesButton.setOnClickListener {
            copySensitive(binding.recoveryCodesText.text.toString())
        }
        binding.doneButton.setOnClickListener { show(Mode.IDLE) }

        show(Mode.IDLE)
    }

    private fun show(newMode: Mode) {
        mode = newMode
        val enabled = session.mfaEnabled

        binding.statusText.setText(if (enabled) R.string.mfa_status_on else R.string.mfa_status_off)
        binding.actionButton.setText(if (enabled) R.string.mfa_disable else R.string.mfa_enable)
        binding.actionButton.visibility = if (newMode == Mode.IDLE) View.VISIBLE else View.GONE

        binding.setupGroup.visibility = if (newMode == Mode.CONFIRM) View.VISIBLE else View.GONE
        binding.codeGroup.visibility =
            if (newMode == Mode.CONFIRM || newMode == Mode.DISABLE) View.VISIBLE else View.GONE
        binding.codesGroup.visibility = if (newMode == Mode.CODES) View.VISIBLE else View.GONE

        binding.codeHelpText.setText(
            if (newMode == Mode.DISABLE) R.string.mfa_disable_help else R.string.mfa_confirm_help
        )
        binding.confirmButton.setText(
            if (newMode == Mode.DISABLE) R.string.mfa_disable else R.string.mfa_confirm
        )

        if (newMode != Mode.CODES) binding.recoveryCodesText.text = ""
        if (newMode == Mode.IDLE) {
            binding.secretText.text = ""
            otpAuthUri = null
        }
        binding.codeInput.setText("")
        binding.codeLayout.error = null
        showError(null)
    }

    private fun startSetup() = call {
        val setup = ApiClient.get(this).setupMfa()
        otpAuthUri = setup.otpAuthUri
        show(Mode.CONFIRM)
        // En grupos de 4 para copiarla a mano sin perderse.
        binding.secretText.text = setup.secret.chunked(4).joinToString(" ")
    }

    private fun confirm() {
        val code = binding.codeInput.text?.toString()?.trim().orEmpty()
        if (code.length != 6) return

        call {
            val result = ApiClient.get(this).confirmMfa(MfaCodeRequest(code))
            session.mfaEnabled = true
            show(Mode.CODES)
            binding.recoveryCodesText.text = result.recoveryCodes.joinToString("\n")
        }
    }

    private fun disable() {
        val code = binding.codeInput.text?.toString()?.trim().orEmpty()
        if (code.length != 6) return

        call {
            ApiClient.get(this).disableMfa(MfaCodeRequest(code))
            session.mfaEnabled = false
            show(Mode.IDLE)
        }
    }

    /** Las apps autenticadoras (Google, Microsoft...) abren los enlaces otpauth://. */
    private fun openAuthenticator() {
        val uri = otpAuthUri ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.mfa_no_app, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copia al portapapeles marcandolo como sensible: en Android 13+ el aviso
     * de "copiado" no muestra el contenido.
     */
    private fun copySensitive(text: String) {
        val clip = ClipData.newPlainText(getString(R.string.mfa_title), text.replace(" ", ""))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.mfa_copied, Toast.LENGTH_SHORT).show()
        }
    }

    /** Ejecuta la llamada mostrando la carga y traduciendo los errores de la API. */
    private fun call(block: suspend () -> Unit) {
        showError(null)
        binding.codeLayout.error = null
        setLoading(true)

        lifecycleScope.launch {
            try {
                block()
            } catch (error: HttpException) {
                if (error.code() == 401) {
                    // Sesion caducada: igual que en el panel.
                    session.clear()
                    startActivity(
                        Intent(this@MfaActivity, LoginActivity::class.java)
                            .putExtra(MainActivity.EXTRA_SESSION_EXPIRED, true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                    return@launch
                }
                describe(error)
            } catch (error: IOException) {
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun describe(error: HttpException) {
        when (error.apiErrorCode()) {
            "invalid_mfa_code" -> binding.codeLayout.error = getString(R.string.error_mfa_invalid_code)
            "mfa_not_available" -> showError(getString(R.string.error_mfa_not_available))
            "mfa_already_enabled" -> {
                // La sesion se guardo antes de activarlo (en otro dispositivo).
                session.mfaEnabled = true
                show(Mode.IDLE)
            }
            "mfa_not_enabled" -> {
                session.mfaEnabled = false
                show(Mode.IDLE)
            }
            else -> showError(
                getString(if (error.code() == 429) R.string.error_too_many_requests else R.string.error_server)
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.actionButton.isEnabled = !loading
        binding.confirmButton.isEnabled = !loading
        binding.cancelButton.isEnabled = !loading
    }

    private fun showError(message: String?) {
        binding.errorText.text = message.orEmpty()
        binding.errorText.visibility = if (message == null) View.GONE else View.VISIBLE
    }
}
