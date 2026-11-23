package com.antoniocompany.financetracker.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Cabeceras de version y aviso de actualizar (426), contra un servidor falso.
 */
class ApiClientVersionTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private var upgrades = 0

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder()
            .addInterceptor(ApiClient.clientVersionInterceptor("1.9.1") { upgrades++ })
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun send(code: Int = 200): okhttp3.mockwebserver.RecordedRequest {
        server.enqueue(MockResponse().setResponseCode(code))
        client.newCall(Request.Builder().url(server.url("/api/Transactions")).build())
            .execute().close()
        return server.takeRequest()
    }

    @Test
    fun `every request sends the android platform and version`() {
        val request = send()

        assertEquals("android", request.getHeader("X-Client-Platform"))
        assertEquals("1.9.1", request.getHeader("X-Client-Version"))
    }

    @Test
    fun `a 426 reports the upgrade once`() {
        send(426)
        send(426)

        assertEquals(1, upgrades)
    }

    @Test
    fun `other responses do not report an upgrade`() {
        send(200)
        send(401)

        assertEquals(0, upgrades)
    }
}
