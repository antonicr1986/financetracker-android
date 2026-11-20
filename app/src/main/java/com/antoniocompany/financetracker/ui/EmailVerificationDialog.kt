package com.antoniocompany.financetracker.ui

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.antoniocompany.financetracker.R
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.apiErrorCode
import com.antoniocompany.financetracker.data.model.EmailResendRequest
import com.antoniocompany.financetracker.data.model.EmailVerifyRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.databinding.DialogEmailCodeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Dialogo para verificar el correo con el codigo de 6 digitos que ha enviado
 * la API. Lo usan el acceso y el registro. "Verificar" y "Reenviar" no lo
 * cierran: si algo falla, el error se ve en el propio dialogo.
 *
 * @param onVerified codigo correcto: la respuesta trae la sesion (o, si la
 *   cuenta tuviera MFA, su paso).
 * @param onExpired han pasado los 30 minutos del paso: volver a la contraseña.
 * @param onBack el usuario ha pulsado "Volver".
 */
object EmailVerificationDialog {

    private val sixDigits = Regex("""^\d{6}$""")

    fun show(
        activity: AppCompatActivity,
        verificationToken: String,
        email: String,
        onVerified: (LoginResponse) -> Unit,
        onExpired: () -> Unit,
        onBack: () -> Unit,
    ) {
        val form = DialogEmailCodeBinding.inflate(activity.layoutInflater)
        form.bodyText.text = activity.getString(R.string.email_verification_body, email)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.email_verification_title)
            .setView(form.root)
            .setPositiveButton(R.string.mfa_verify, null)
            .setNeutralButton(R.string.email_verification_resend, null)
            .setNegativeButton(R.string.mfa_back) { _, _ -> onBack() }
            .setCancelable(false)
            .show()

        form.codeInput.requestFocus()

        val verifyButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val resendButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)

        fun setBusy(busy: Boolean) {
            verifyButton.isEnabled = !busy
            resendButton.isEnabled = !busy
        }

        fun showStatus(message: String?) {
            form.statusText.text = message.orEmpty()
            form.statusText.visibility = if (message == null) View.GONE else View.VISIBLE
        }

        verifyButton.setOnClickListener {
            val code = form.codeInput.text?.toString()?.trim().orEmpty()
            form.codeLayout.error = null
            showStatus(null)

            if (!sixDigits.matches(code)) {
                form.codeLayout.error = activity.getString(R.string.error_email_code_format)
                return@setOnClickListener
            }

            setBusy(true)
            activity.lifecycleScope.launch {
                try {
                    val response = ApiClient.get(activity)
                        .verifyEmail(EmailVerifyRequest(verificationToken, code))
                    dialog.dismiss()
                    onVerified(response)
                } catch (error: HttpException) {
                    val message = when (error.apiErrorCode()) {
                        "invalid_verification_token" -> null
                        "invalid_email_code" -> R.string.error_email_code_invalid
                        "email_code_expired" -> R.string.error_email_code_expired
                        else -> if (error.code() == 429) R.string.error_too_many_requests else R.string.error_server
                    }
                    if (message == null) {
                        dialog.dismiss()
                        onExpired()
                    } else {
                        form.codeLayout.error = activity.getString(message)
                    }
                } catch (error: IOException) {
                    form.codeLayout.error = activity.getString(R.string.error_network)
                } finally {
                    setBusy(false)
                }
            }
        }

        resendButton.setOnClickListener {
            form.codeLayout.error = null
            showStatus(null)
            setBusy(true)

            activity.lifecycleScope.launch {
                try {
                    ApiClient.get(activity).resendEmailCode(EmailResendRequest(verificationToken))
                    showStatus(activity.getString(R.string.email_verification_resent))
                } catch (error: HttpException) {
                    val message = when (error.apiErrorCode()) {
                        "invalid_verification_token" -> null
                        "email_code_recently_sent" -> R.string.error_email_code_recently_sent
                        else -> if (error.code() == 429) R.string.error_too_many_requests else R.string.error_server
                    }
                    if (message == null) {
                        dialog.dismiss()
                        onExpired()
                    } else {
                        showStatus(activity.getString(message))
                    }
                } catch (error: IOException) {
                    showStatus(activity.getString(R.string.error_network))
                } finally {
                    setBusy(false)
                }
            }
        }
    }
}
