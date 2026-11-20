package com.antoniocompany.financetracker

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.apiErrorCode
import com.antoniocompany.financetracker.data.model.ForgotPasswordRequest
import com.antoniocompany.financetracker.data.model.ResetPasswordRequest
import com.antoniocompany.financetracker.databinding.ActivityForgotPasswordBinding
import com.antoniocompany.financetracker.domain.PasswordResetProblem
import com.antoniocompany.financetracker.domain.isValidEmail
import com.antoniocompany.financetracker.domain.validatePasswordReset
import com.antoniocompany.financetracker.ui.bind
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * "¿Has olvidado tu contraseña?", como en la web: primero el correo, despues
 * el codigo que llega con la contraseña nueva. No inicia sesion: la API cierra
 * todas al cambiarla, asi que al terminar se vuelve al login.
 */
class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    /** Correo al que se pidio el codigo; null mientras se esta en el primer paso. */
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sin sesion, "Salir" aparece deshabilitado.
        binding.topBar.bind(this)

        binding.sendButton.setOnClickListener { sendCode() }
        binding.resetButton.setOnClickListener { reset() }
        binding.resendButton.setOnClickListener { email?.let { requestCode(it) } }
        binding.backButton.setOnClickListener { showEmailStep() }

        // Se llega aqui desde el login: volver es cerrar esta pantalla.
        binding.signInLink.setOnClickListener { finish() }
    }

    private fun sendCode() {
        val typed = binding.emailInput.text?.toString()?.trim().orEmpty()
        if (!isValidEmail(typed)) {
            showError(getString(R.string.error_invalid_email))
            return
        }
        requestCode(typed)
    }

    private fun requestCode(to: String) {
        showError(null)
        setLoading(true)

        lifecycleScope.launch {
            try {
                ApiClient.get(this@ForgotPasswordActivity).forgotPassword(ForgotPasswordRequest(to))
                email = to
                showCodeStep(to)
            } catch (error: HttpException) {
                showError(describe(error))
            } catch (error: IOException) {
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun reset() {
        val to = email ?: return
        val code = binding.codeInput.text?.toString()?.trim().orEmpty()
        val password = binding.newPasswordInput.text?.toString().orEmpty()
        val confirmation = binding.confirmInput.text?.toString().orEmpty()

        // Las mismas reglas que la API: se evita una peticion para un error evidente.
        val problem = when (validatePasswordReset(code, password, confirmation)) {
            PasswordResetProblem.INVALID_CODE_FORMAT -> R.string.error_email_code_format
            PasswordResetProblem.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
            PasswordResetProblem.PASSWORDS_DONT_MATCH -> R.string.error_passwords_dont_match
            null -> null
        }
        if (problem != null) {
            showError(getString(problem))
            return
        }

        showError(null)
        setLoading(true)

        lifecycleScope.launch {
            try {
                ApiClient.get(this@ForgotPasswordActivity)
                    .resetPassword(ResetPasswordRequest(to, code, password))
                showDone()
            } catch (error: HttpException) {
                showError(describe(error))
            } catch (error: IOException) {
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun describe(error: HttpException): String = when (error.apiErrorCode()) {
        "invalid_reset_code" -> getString(R.string.error_reset_code_invalid)
        "reset_code_expired" -> {
            // Codigo gastado: se borra para escribir el del correo nuevo.
            binding.codeInput.text = null
            getString(R.string.error_reset_code_expired)
        }
        "password_breached" -> getString(R.string.error_password_breached)
        else -> when {
            error.code() == 429 -> getString(R.string.error_too_many_requests)
            error.code() >= 500 -> getString(R.string.error_server)
            else -> getString(R.string.password_reset_failed)
        }
    }

    private fun showCodeStep(to: String) {
        binding.statusText.text = getString(R.string.password_reset_code_sent, to)
        binding.emailStep.visibility = View.GONE
        binding.codeStep.visibility = View.VISIBLE
        binding.codeInput.requestFocus()
    }

    private fun showEmailStep() {
        email = null
        binding.codeInput.text = null
        showError(null)
        binding.codeStep.visibility = View.GONE
        binding.emailStep.visibility = View.VISIBLE
    }

    /** Hecho: aviso de que se han cerrado las sesiones y vuelta al login. */
    private fun showDone() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.password_reset_title)
            .setMessage(R.string.password_reset_done)
            .setCancelable(false)
            .setPositiveButton(R.string.password_reset_go_to_login) { _, _ -> finish() }
            .show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.sendButton.isEnabled = !loading
        binding.resetButton.isEnabled = !loading
        binding.resendButton.isEnabled = !loading
        binding.backButton.isEnabled = !loading
    }

    private fun showError(message: String?) {
        binding.errorText.text = message.orEmpty()
        binding.errorText.visibility = if (message == null) View.GONE else View.VISIBLE
    }
}
