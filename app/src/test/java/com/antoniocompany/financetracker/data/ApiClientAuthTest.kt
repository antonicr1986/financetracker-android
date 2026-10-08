package com.antoniocompany.financetracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiClientAuthTest {

    private lateinit var server: MockWebServer
    private lateinit var session: SessionStore
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        session = SessionStore(context).apply { clear() }
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder()
            .addInterceptor(ApiClient.authInterceptor(session))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        session.clear()
    }

    private fun sendAndCapture(): okhttp3.mockwebserver.RecordedRequest {
        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(Request.Builder().url(server.url("/api/Transactions")).build())
            .execute().close()
        return server.takeRequest()
    }

    @Test
    fun `without session no Authorization header is sent`() {
        assertNull(sendAndCapture().getHeader("Authorization"))
    }

    @Test
    fun `with session the bearer token is sent`() {
        session.token = "abc"

        assertEquals("Bearer abc", sendAndCapture().getHeader("Authorization"))
    }

    @Test
    fun `token is read on every request, not when the client is built`() {
        session.token = "first"
        assertEquals("Bearer first", sendAndCapture().getHeader("Authorization"))

        session.token = "second"
        assertEquals("Bearer second", sendAndCapture().getHeader("Authorization"))

        session.clear()
        assertNull(sendAndCapture().getHeader("Authorization"))
    }
}
