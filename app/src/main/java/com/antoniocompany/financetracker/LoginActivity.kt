package com.antoniocompany.financetracker

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.TrustedDeviceStore
import com.antoniocompany.financetracker.data.apiErrorCode
import com.antoniocompany.financetracker.data.model.GoogleLoginRequest
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.MfaVerifyRequest
import com.antoniocompany.financetracker.databinding.ActivityLoginBinding
import com.antoniocompany.financetracker.databinding.DialogMfaCodeBinding
import com.antoniocompany.financetracker.ui.EmailVerificationDialog
import com.antoniocompany.financetracker.ui.LanguagePreference
import com.antoniocompany.financetracker.ui.bind
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Pantalla de acceso.
 *
 * Es la actividad de arranque: si ya hay sesion guardada pasa de largo al panel
 * sin ensenarse. El boton de la cuenta de demostracion hace exactamente lo
 * mismo que el formulario, con unas credenciales publicas a proposito.
 */
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionStore
    private lateinit var trustedDevice: TrustedDeviceStore

    /** Cuenta atras del aviso de arranque en frio. */
    private var wakingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionStore(this)
        trustedDevice = TrustedDeviceStore(this)

        if (session.hasSession) {
            goToDashboard()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sin sesion, "Salir" aparece deshabilitado.
        binding.topBar.bind(this)

        // Precalentamiento: la API (plan gratuito de Azure) se descarga tras
        // un rato sin uso y la base de datos serverless se pausa. Se despiertan
        // ya, en segundo plano, para que la espera ocurra mientras se mira la
        // pantalla y no al pulsar "Entrar". Si falla da igual: el login
        // funciona igual, solo que con la espera de siempre.
        lifecycleScope.launch {
            try {
                ApiClient.get(this@LoginActivity).warmUp()
            } catch (_: IOException) {
            }
        }

        // Si venimos aquí porque la API devolvió 401 (token caducado a los
        // 60 minutos, sin refresh token), avisamos en vez de dejar que
        // parezca un cierre de sesión cualquiera. Se consume una sola vez:
        // si luego se cambia de tema o idioma y la pantalla se reinicia, no
        // vuelve a aparecer.
        if (intent.getBooleanExtra(MainActivity.EXTRA_SESSION_EXPIRED, false)) {
            intent.removeExtra(MainActivity.EXTRA_SESSION_EXPIRED)
            showError(getString(R.string.error_session_expired))
        }

        binding.signInButton.setOnClickListener {
            signIn(
                binding.emailInput.text?.toString()?.trim().orEmpty(),
                binding.passwordInput.text?.toString().orEmpty()
            )
        }

        binding.demoButton.setOnClickListener {
            signIn(DEMO_EMAIL, DEMO_PASSWORD)
        }

        binding.googleButton.setOnClickListener {
            signInWithGoogle()
        }

        // El login se queda debajo: "Entrar" en el registro solo tiene que
        // cerrar esa pantalla para volver aqui.
        binding.createAccountLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fill_credentials))
            return
        }

        showError(null)
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.get(this@LoginActivity)
                    .login(LoginRequest(email, password, trustedDevice.token))

                onSignedIn(response, email)
            } catch (error: HttpException) {
                // Un 401 aqui no es una sesion caducada: todavia no habia
                // sesion. Son credenciales que la API no acepta.
                showError(
                    when (error.code()) {
                        401 -> getString(R.string.error_invalid_credentials)
                        429 -> getString(R.string.error_too_many_requests)
                        else -> getString(R.string.error_server)
                    }
                )
            } catch (error: IOException) {
                // Sin red, o el servidor no responde a tiempo.
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Abre el selector de cuentas de Google del sistema (Credential Manager),
     * manda el ID token a la API y guarda la sesion igual que el login normal.
     * Cancelar el selector o cualquier fallo deja al usuario aqui con un aviso.
     */
    private fun signInWithGoogle() {
        showError(null)
        setLoading(true)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(getString(R.string.google_web_client_id)).build()
            )
            .build()

        lifecycleScope.launch {
            try {
                val credential = CredentialManager.create(this@LoginActivity)
                    .getCredential(this@LoginActivity, request)
                    .credential
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

                val response = ApiClient.get(this@LoginActivity)
                    .googleLogin(GoogleLoginRequest(idToken, LanguagePreference.current(), trustedDevice.token))

                onSignedIn(response, "")
            } catch (error: GetCredentialException) {
                // Incluye cancelar el selector y no tener cuentas de Google.
                showError(getString(R.string.error_google_sign_in))
            } catch (error: GoogleIdTokenParsingException) {
                showError(getString(R.string.error_google_sign_in))
            } catch (error: HttpException) {
                showError(
                    when (error.code()) {
                        401 -> getString(R.string.error_google_sign_in)
                        429 -> getString(R.string.error_too_many_requests)
                        else -> getString(R.string.error_server)
                    }
                )
            } catch (error: IOException) {
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Con MFA activo el login no trae token, sino un mfaToken: se pide el
     * codigo antes de abrir la sesion.
     */
    private fun onSignedIn(response: LoginResponse, fallbackEmail: String) {
        // Correo sin verificar: antes que nada, el codigo que le ha llegado.
        val verificationToken = response.verificationToken
        if (response.emailVerificationRequired && verificationToken != null) {
            EmailVerificationDialog.show(
                this,
                verificationToken,
                response.email ?: fallbackEmail,
                onVerified = { verified -> onSignedIn(verified, fallbackEmail) },
                onExpired = { showError(getString(R.string.error_mfa_expired)) },
                onBack = {},
            )
            return
        }

        val mfaToken = response.mfaToken
        if (response.mfaRequired && mfaToken != null) {
            askMfaCode(mfaToken, fallbackEmail)
            return
        }

        startSession(response, fallbackEmail)
    }

    private fun startSession(response: LoginResponse, fallbackEmail: String) {
        session.token = response.token
        session.email = response.user?.email ?: fallbackEmail
        session.mfaEnabled = response.user?.mfaEnabled ?: false
        session.hasPassword = response.user?.hasPassword != false
        goToDashboard()
    }

    /**
     * Dialogo del segundo paso. El boton "Verificar" no cierra el dialogo por
     * si solo: si el codigo no vale, el error se ve en el propio campo.
     */
    private fun askMfaCode(mfaToken: String, fallbackEmail: String) {
        val dialogBinding = DialogMfaCodeBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mfa_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.mfa_verify, null)
            .setNegativeButton(R.string.mfa_back, null)
            .setCancelable(false)
            .show()

        dialogBinding.codeInput.requestFocus()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { button ->
            val code = dialogBinding.codeInput.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) return@setOnClickListener

            dialogBinding.codeLayout.error = null
            button.isEnabled = false

            lifecycleScope.launch {
                try {
                    val response = ApiClient.get(this@LoginActivity)
                        .verifyMfa(
                            MfaVerifyRequest(mfaToken, code, dialogBinding.rememberDeviceCheck.isChecked)
                        )
                    response.trustedDeviceToken?.let { trustedDevice.token = it }
                    dialog.dismiss()
                    startSession(response, fallbackEmail)
                } catch (error: HttpException) {
                    when {
                        error.code() == 429 ->
                            dialogBinding.codeLayout.error = getString(R.string.error_too_many_requests)
                        // Han pasado los 5 minutos: vuelta a la contraseña.
                        error.apiErrorCode() == "invalid_mfa_token" -> {
                            dialog.dismiss()
                            showError(getString(R.string.error_mfa_expired))
                        }
                        error.code() == 401 ->
                            dialogBinding.codeLayout.error = getString(R.string.error_mfa_invalid_code)
                        else ->
                            dialogBinding.codeLayout.error = getString(R.string.error_server)
                    }
                } catch (error: IOException) {
                    dialogBinding.codeLayout.error = getString(R.string.error_network)
                } finally {
                    button.isEnabled = true
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.signInButton.isEnabled = !loading
        binding.demoButton.isEnabled = !loading
        binding.googleButton.isEnabled = !loading
        binding.createAccountLink.isEnabled = !loading
        binding.signInButton.setText(
            if (loading) R.string.login_submitting else R.string.login_submit
        )

        wakingJob?.cancel()
        binding.wakingText.visibility = View.GONE

        // La base de datos de Azure se pausa sola y despertarla lleva su tiempo.
        // Desde fuera solo se ve un boton quieto: si la espera se alarga, se
        // explica por que en lugar de dejar que parezca que esta roto.
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

    private fun goToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        // Publicas a proposito: es la cuenta con la que cualquiera puede ver la
        // aplicacion sin registrarse, la misma que ofrece la version web.
        const val DEMO_EMAIL = "demo@financetracker.app"
        const val DEMO_PASSWORD = "Demo1234!" // gitleaks:allow

        const val WAKING_NOTICE_DELAY_MS = 4_000L
    }
}
