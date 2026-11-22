package com.antoniocompany.financetracker

import android.content.Context
import android.content.DialogInterface
import androidx.test.core.app.ApplicationProvider
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.FakeApi
import com.antoniocompany.financetracker.data.httpError
import com.antoniocompany.financetracker.data.model.ForgotPasswordRequest
import com.antoniocompany.financetracker.data.model.ResetPasswordRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** "¿Has olvidado tu contraseña?" con una API falsa. */
@RunWith(RobolectricTestRunner::class)
class ForgotPasswordActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Apunta lo que se le pide y falla el cambio con [resetError] si lo hay. */
    private class Api(val resetError: Exception? = null) : FakeApi() {
        val requested = mutableListOf<String>()
        val resets = mutableListOf<ResetPasswordRequest>()

        override suspend fun forgotPassword(body: ForgotPasswordRequest) {
            requested += body.email
        }

        override suspend fun resetPassword(body: ResetPasswordRequest) {
            resets += body
            resetError?.let { throw it }
        }
    }

    @After
    fun tearDown() = ApiClient.replaceForTests(null)

    private fun launch(api: FakeApi): ForgotPasswordActivity {
        ApiClient.replaceForTests(api)
        return Robolectric.buildActivity(ForgotPasswordActivity::class.java).setup().get().also { idle() }
    }

    private fun ForgotPasswordActivity.sendCode() {
        type(R.id.emailInput, " ana@test.com ")
        click(R.id.sendButton)
    }

    private fun ForgotPasswordActivity.reset(
        code: String = "123456",
        password: String = "una-clave-nueva-larga",
        confirmation: String = password,
    ) {
        type(R.id.codeInput, code)
        type(R.id.newPasswordInput, password)
        type(R.id.confirmInput, confirmation)
        click(R.id.resetButton)
    }

    @Test
    fun `a broken email is rejected before calling the API`() {
        val api = Api()
        val activity = launch(api)

        activity.type(R.id.emailInput, "ana@test")
        activity.click(R.id.sendButton)

        assertEquals(context.getString(R.string.error_invalid_email), activity.textOf(R.id.errorText))
        assertTrue(api.requested.isEmpty())
    }

    @Test
    fun `sending the code moves to the code step and says where it went`() {
        val api = Api()
        val activity = launch(api)

        activity.sendCode()

        assertEquals(listOf("ana@test.com"), api.requested)
        assertTrue(activity.isShown(R.id.codeStep))
        assertFalse(activity.isShown(R.id.emailStep))
        assertTrue(activity.textOf(R.id.statusText).contains("ana@test.com"))
    }

    @Test
    fun `the code step checks the form before calling the API`() {
        val api = Api()
        val activity = launch(api)
        activity.sendCode()

        activity.reset(code = "12345")
        assertEquals(context.getString(R.string.error_email_code_format), activity.textOf(R.id.errorText))

        activity.reset(password = "corta")
        assertEquals(context.getString(R.string.error_password_too_short), activity.textOf(R.id.errorText))

        activity.reset(confirmation = "otra-clave-nueva")
        assertEquals(context.getString(R.string.error_passwords_dont_match), activity.textOf(R.id.errorText))

        assertTrue(api.resets.isEmpty())
    }

    @Test
    fun `a good reset says so and goes back to the sign-in`() {
        val api = Api()
        val activity = launch(api)
        activity.sendCode()

        activity.reset()
        val dialog = openDialog()!!
        dialog.click(DialogInterface.BUTTON_POSITIVE)

        assertEquals(ResetPasswordRequest("ana@test.com", "123456", "una-clave-nueva-larga"), api.resets.single())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `an expired code is cleared to type the new one`() {
        val activity = launch(Api(resetError = httpError(400, "reset_code_expired")))
        activity.sendCode()

        activity.reset()

        assertEquals(context.getString(R.string.error_reset_code_expired), activity.textOf(R.id.errorText))
        assertEquals("", activity.textOf(R.id.codeInput))
    }

    @Test
    fun `a wrong code is kept to fix it`() {
        val activity = launch(Api(resetError = httpError(400, "invalid_reset_code")))
        activity.sendCode()

        activity.reset()

        assertEquals(context.getString(R.string.error_reset_code_invalid), activity.textOf(R.id.errorText))
        assertEquals("123456", activity.textOf(R.id.codeInput))
    }

    @Test
    fun `resend asks again for the same email and back returns to the email step`() {
        val api = Api()
        val activity = launch(api)
        activity.sendCode()

        activity.click(R.id.resendButton)
        assertEquals(listOf("ana@test.com", "ana@test.com"), api.requested)

        activity.click(R.id.backButton)
        assertTrue(activity.isShown(R.id.emailStep))
        assertFalse(activity.isShown(R.id.codeStep))
    }
}
