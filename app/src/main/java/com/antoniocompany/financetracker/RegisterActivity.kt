package com.antoniocompany.financetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.databinding.ActivityRegisterBinding
import com.antoniocompany.financetracker.domain.RegistrationProblem
import com.antoniocompany.financetracker.domain.validateRegistration
import com.antoniocompany.financetracker.ui.EmailVerificationDialog
import com.antoniocompany.financetracker.ui.LanguagePreference
import com.antoniocompany.financetracker.ui.bind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Alta de cuenta.
 *
 * Hace lo mismo que la pagina de registro de la web: comprueba el formulario
 * antes de enviarlo, crea la cuenta y, como el registro no devuelve token,
 * inicia sesion con los mismos datos para entrar directamente al panel.
 */
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionStore

    /** Cuenta atras del aviso de arranque en frio, igual que en el login. */
    private var wakingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionStore(this)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sin sesion, "Salir" aparece deshabilitado.
        binding.topBar.bind(this)

        binding.registerButton.setOnClickListener { register() }

        // Se llega aqui desde el login, asi que volver es cerrar esta pantalla.
        binding.signInLink.setOnClickListener { finish() }
    }

    private fun register() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirmation = binding.confirmInput.text?.toString().orEmpty()

        // Los errores evidentes se detectan aqui, sin gastar una peticion.
        // Son las mismas reglas que valida la API en RegisterUserDto.
        val problem = when (validateRegistration(name, email, password, confirmation)) {
            RegistrationProblem.MISSING_FIELDS -> R.string.error_fill_all
            RegistrationProblem.INVALID_EMAIL -> R.string.error_invalid_email
            RegistrationProblem.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
            RegistrationProblem.PASSWORDS_DONT_MATCH -> R.string.error_passwords_dont_match
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
                val api = ApiClient.get(this@RegisterActivity)
                api.register(RegisterRequest(name, email, password, LanguagePreference.current()))

                // El registro devuelve el usuario, no un token: se encadena el
                // login para no obligar a escribir otra vez los mismos datos.
                onSignedIn(api.login(LoginRequest(email, password)), email)
            } catch (error: HttpException) {
                showError(describe(error))
            } catch (error: IOException) {
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Con la verificacion de correo activa, el login de una cuenta recien
     * creada pide el codigo que ha llegado al correo. Si algo no encaja (paso
     * caducado, "Volver", o un MFA que una cuenta nueva no deberia tener), se
     * sigue desde el acceso: la cuenta ya existe.
     */
    private fun onSignedIn(response: LoginResponse, email: String) {
        val verificationToken = response.verificationToken
        if (response.emailVerificationRequired && verificationToken != null) {
            EmailVerificationDialog.show(
                this,
                verificationToken,
                response.email ?: email,
                onVerified = { verified -> onSignedIn(verified, email) },
                onExpired = { goToLogin() },
                onBack = { goToLogin() },
            )
            return
        }

        if (response.token == null) {
            goToLogin()
            return
        }

        session.token = response.token
        session.email = response.user?.email ?: email
        session.mfaEnabled = response.user?.mfaEnabled ?: false
        session.hasPassword = response.user?.hasPassword != false
        goToDashboard()
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    /**
     * La API responde 400 con el codigo "email_already_exists" cuando el
     * correo ya esta dado de alta, 400 con "password_breached" si la
     * contraseña aparece en filtraciones y 429 si hay demasiados intentos.
     * Cualquier otro 4xx es un dato que no ha aceptado; un 5xx, un fallo suyo.
     */
    private fun describe(error: HttpException): String {
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        return when {
            body.contains("email_already_exists") -> getString(R.string.error_email_already_exists)
            body.contains("password_breached") -> getString(R.string.error_password_breached)
            error.code() == 429 -> getString(R.string.error_too_many_requests)
            error.code() >= 500 -> getString(R.string.error_server)
            else -> getString(R.string.error_create_account_failed)
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !loading
        binding.signInLink.isEnabled = !loading
        binding.registerButton.setText(
            if (loading) R.string.register_submitting else R.string.register_submit
        )

        wakingJob?.cancel()
        binding.wakingText.visibility = View.GONE

        if (loading) {
            wakingJob = lifecycleScope.launch {
                delay(WAKING_NOTICE_DELAY_MS)
                binding.wakingText.visibility = View.VISIBLE
            }
        }
    }

    private fun showError(message: String?) {
        binding.errorText.text = message.orEmpty()
        binding.errorText.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    /**
     * Limpia la pila para que "atras" desde el panel no vuelva al registro ni
     * al login, que ya quedaron detras.
     */
    private fun goToDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private companion object {
        const val WAKING_NOTICE_DELAY_MS = 4_000L
    }
}
