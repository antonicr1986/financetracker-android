package com.antoniocompany.financetracker

import android.content.Context
import android.content.DialogInterface
import androidx.test.core.app.ApplicationProvider
import com.antoniocompany.financetracker.data.ApiClient
import com.antoniocompany.financetracker.data.FakeApi
import com.antoniocompany.financetracker.data.SessionStore
import com.antoniocompany.financetracker.data.httpError
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.UserDto
import com.antoniocompany.financetracker.data.session
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Alta de cuenta con una API falsa. */
@RunWith(RobolectricTestRunner::class)
class RegisterActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionStore = SessionStore(context)

    @Before
    fun setUp() = sessionStore.clear()

    @After
    fun tearDown() = ApiClient.replaceForTests(null)

    private fun launch(api: FakeApi = FakeApi()): RegisterActivity {
        ApiClient.replaceForTests(api)
        return Robolectric.buildActivity(RegisterActivity::class.java).setup().get().also { idle() }
    }

    private fun RegisterActivity.fill(
        name: String = "Ana",
        password: String = "una-clave-larga",
        confirmation: String = password,
    ) {
        type(R.id.nameInput, name)
        type(R.id.emailInput, "ana@test.com")
        type(R.id.passwordInput, password)
        type(R.id.confirmInput, confirmation)
        click(R.id.registerButton)
    }

    /** API en la que el registro va bien y el login responde lo indicado. */
    private fun api(loginResponse: LoginResponse = session()) = object : FakeApi() {
        var registered: RegisterRequest? = null

        override suspend fun register(body: RegisterRequest): UserDto {
            registered = body
            return UserDto(id = 9, name = body.name, email = body.email)
        }

        override suspend fun login(body: LoginRequest) = loginResponse
    }

    @Test
    fun `checks the form before calling the API`() {
        val activity = launch()

        activity.fill(name = "")
        assertEquals(context.getString(R.string.error_fill_all), activity.textOf(R.id.errorText))

        activity.fill(password = "corta")
        assertEquals(context.getString(R.string.error_password_too_short), activity.textOf(R.id.errorText))

        activity.fill(confirmation = "otra-clave-larga")
        assertEquals(context.getString(R.string.error_passwords_dont_match), activity.textOf(R.id.errorText))
    }

    @Test
    fun `creates the account, signs in and opens the dashboard`() {
        val fake = api()
        val activity = launch(fake)

        activity.fill()

        assertEquals("ana@test.com", fake.registered?.email)
        assertEquals("jwt-123", sessionStore.token)
        assertEquals(MainActivity::class.java.name, activity.startedActivity())
    }

    @Test
    fun `an email already registered shows its error`() {
        val activity = launch(object : FakeApi() {
            override suspend fun register(body: RegisterRequest): UserDto = throw httpError(400, "email_already_exists")
        })

        activity.fill()

        assertEquals(context.getString(R.string.error_email_already_exists), activity.textOf(R.id.errorText))
        assertNull(activity.startedActivity())
    }

    @Test
    fun `with email verification it asks for the code, and back goes to the sign-in`() {
        val activity = launch(
            api(
                LoginResponse(
                    token = null,
                    expiration = null,
                    user = null,
                    emailVerificationRequired = true,
                    verificationToken = "verify-token",
                    email = "ana@test.com",
                )
            )
        )

        activity.fill()
        openDialog()!!.click(DialogInterface.BUTTON_NEGATIVE)

        assertNull(sessionStore.token)
        assertEquals(LoginActivity::class.java.name, activity.startedActivity())
    }
}
