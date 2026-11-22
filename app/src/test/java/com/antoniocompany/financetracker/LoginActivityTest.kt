package com.antoniocompany.financetracker

import android.content.Context
import android.content.DialogInterface
import androidx.test.core.app.ApplicationProvider
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.FakeApi
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.httpError
import com.antoniocompany.financetracker.data.model.EmailVerifyRequest
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.MfaVerifyRequest
import com.antoniocompany.financetracker.data.session
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Pantalla de acceso con una API falsa: lo que se ve y adonde lleva cada respuesta. */
@RunWith(RobolectricTestRunner::class)
class LoginActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionStore = SessionStore(context)

    @Before
    fun setUp() = sessionStore.clear()

    @After
    fun tearDown() = ApiClient.replaceForTests(null)

    private fun launch(api: FakeApi = FakeApi()): LoginActivity {
        ApiClient.replaceForTests(api)
        return Robolectric.buildActivity(LoginActivity::class.java).setup().get().also { idle() }
    }

    private fun LoginActivity.signIn(email: String = "ana@test.com", password: String = "una-clave-larga") {
        type(R.id.emailInput, email)
        type(R.id.passwordInput, password)
        click(R.id.signInButton)
    }

    @Test
    fun `asks for both fields before calling the API`() {
        val activity = launch()

        activity.click(R.id.signInButton)

        assertEquals(context.getString(R.string.error_fill_credentials), activity.textOf(R.id.errorText))
    }

    @Test
    fun `wrong credentials show an error and stay here`() {
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest): LoginResponse = throw httpError(401, "invalid_credentials")
        })

        activity.signIn()

        assertEquals(context.getString(R.string.error_invalid_credentials), activity.textOf(R.id.errorText))
        assertNull(activity.startedActivity())
    }

    @Test
    fun `no network shows its own error`() {
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest): LoginResponse = throw IOException("sin red")
        })

        activity.signIn()

        assertEquals(context.getString(R.string.error_network), activity.textOf(R.id.errorText))
    }

    @Test
    fun `a good sign-in saves the session and opens the dashboard`() {
        var sent: LoginRequest? = null
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest): LoginResponse {
                sent = body
                return session()
            }
        })

        activity.signIn()

        assertEquals("ana@test.com", sent?.email)
        assertEquals("jwt-123", sessionStore.token)
        assertEquals(MainActivity::class.java.name, activity.startedActivity())
    }

    @Test
    fun `the demo button signs in with the demo account`() {
        var sent: LoginRequest? = null
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest): LoginResponse {
                sent = body
                return session("demo@financetracker.app")
            }
        })

        activity.click(R.id.demoButton)

        assertEquals("demo@financetracker.app", sent?.email)
        assertEquals(MainActivity::class.java.name, activity.startedActivity())
    }

    @Test
    fun `with MFA it asks for the code and then enters`() {
        var sent: MfaVerifyRequest? = null
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest) =
                LoginResponse(token = null, expiration = null, user = null, mfaRequired = true, mfaToken = "mfa-token")

            override suspend fun verifyMfa(body: MfaVerifyRequest): LoginResponse {
                sent = body
                return session()
            }
        })

        activity.signIn()
        val dialog = openDialog()!!
        dialog.type(R.id.codeInput, "123456")
        dialog.click(DialogInterface.BUTTON_POSITIVE)

        assertEquals("mfa-token", sent?.mfaToken)
        assertEquals("123456", sent?.code)
        assertEquals(MainActivity::class.java.name, activity.startedActivity())
    }

    @Test
    fun `an expired MFA step goes back to the password`() {
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest) =
                LoginResponse(token = null, expiration = null, user = null, mfaRequired = true, mfaToken = "mfa-token")

            override suspend fun verifyMfa(body: MfaVerifyRequest): LoginResponse =
                throw httpError(401, "invalid_mfa_token")
        })

        activity.signIn()
        val dialog = openDialog()!!
        dialog.type(R.id.codeInput, "123456")
        dialog.click(DialogInterface.BUTTON_POSITIVE)

        assertFalse(dialog.isShowing)
        assertEquals(context.getString(R.string.error_mfa_expired), activity.textOf(R.id.errorText))
        assertNull(sessionStore.token)
    }

    @Test
    fun `an unverified email asks for the emailed code and then enters`() {
        var sent: EmailVerifyRequest? = null
        val activity = launch(object : FakeApi() {
            override suspend fun login(body: LoginRequest) = LoginResponse(
                token = null,
                expiration = null,
                user = null,
                emailVerificationRequired = true,
                verificationToken = "verify-token",
                email = "ana@test.com",
            )

            override suspend fun verifyEmail(body: EmailVerifyRequest): LoginResponse {
                sent = body
                return session()
            }
        })

        activity.signIn()
        val dialog = openDialog()!!
        dialog.type(R.id.codeInput, "654321")
        dialog.click(DialogInterface.BUTTON_POSITIVE)

        assertEquals(EmailVerifyRequest("verify-token", "654321"), sent)
        assertEquals(MainActivity::class.java.name, activity.startedActivity())
    }

    @Test
    fun `with a saved session it goes straight to the dashboard`() {
        sessionStore.token = "jwt-guardado"

        val activity = launch()

        assertEquals(MainActivity::class.java.name, activity.startedActivity())
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `links open the register and the password reset`() {
        val activity = launch()

        activity.click(R.id.createAccountLink)
        assertEquals(RegisterActivity::class.java.name, activity.startedActivity())

        activity.click(R.id.forgotPasswordLink)
        assertEquals(ForgotPasswordActivity::class.java.name, activity.startedActivity())
    }
}
