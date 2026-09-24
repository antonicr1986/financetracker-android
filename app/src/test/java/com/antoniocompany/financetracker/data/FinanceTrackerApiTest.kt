package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Comprueba que cada llamada nueva sale con el metodo, la ruta y el cuerpo
 * que espera la API de .NET. Un error aqui (PUT en vez de POST, "Income" en
 * vez de "INCOME") solo lo veria el servidor en produccion.
 *
 * No hay servidor: un interceptor de OkHttp guarda la peticion y devuelve la
 * respuesta que se le diga, asi que corre en la JVM sin red.
 */
class FinanceTrackerApiTest {

    private var lastRequest: Request? = null
    private var lastBody: String = ""

    private fun api(code: Int, responseJson: String = ""): FinanceTrackerApi {
        val fake = Interceptor { chain ->
            val request = chain.request()
            lastRequest = request
            lastBody = request.body?.let { body -> Buffer().also { body.writeTo(it) }.readUtf8() }.orEmpty()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("")
                .body(responseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(OkHttpClient.Builder().addInterceptor(fake).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinanceTrackerApi::class.java)
    }

    private val input = TransactionInput(
        description = "Alquiler",
        amount = 750.0,
        date = "2026-09-03",
        type = TransactionType.EXPENSE,
        categoryId = 4
    )

    @Test
    fun `register posts the new account`() = runBlocking {
        val user = api(201, """{"id":9,"name":"Ana","email":"ana@mail.com"}""")
            .register(RegisterRequest("Ana", "ana@mail.com", "secreto"))

        assertEquals("POST", lastRequest!!.method)
        assertEquals("/api/Users/register", lastRequest!!.url.encodedPath)
        assertTrue(lastBody, lastBody.contains("\"email\":\"ana@mail.com\""))
        assertEquals(9, user.id)
    }

    @Test
    fun `update puts the transaction on its own id`() = runBlocking {
        api(204).updateTransaction(7, input)

        assertEquals("PUT", lastRequest!!.method)
        assertEquals("/api/Transactions/7", lastRequest!!.url.encodedPath)
        // La API espera el enum como texto con mayuscula inicial.
        assertTrue(lastBody, lastBody.contains("\"type\":\"Expense\""))
        assertTrue(lastBody, lastBody.contains("\"categoryId\":4"))
    }

    @Test
    fun `delete sends DELETE and accepts an empty 204`() = runBlocking {
        api(204).deleteTransaction(7)

        assertEquals("DELETE", lastRequest!!.method)
        assertEquals("/api/Transactions/7", lastRequest!!.url.encodedPath)
    }

    @Test
    fun `a budget for all categories is sent without categoryId`() = runBlocking {
        api(201, BUDGET_JSON).createBudget(
            BudgetInput("Casa", 300.0, month = 9, year = 2026, type = TransactionType.EXPENSE, categoryId = null)
        )

        assertEquals("POST", lastRequest!!.method)
        assertEquals("/api/Budgets", lastRequest!!.url.encodedPath)
        assertTrue(lastBody, lastBody.contains("\"month\":9"))
        // Gson omite los null: la API lo recibe como "todas las categorias".
        assertFalse(lastBody, lastBody.contains("categoryId"))
    }

    @Test
    fun `update budget puts it on its own id`() = runBlocking {
        api(204).updateBudget(
            5,
            BudgetInput("Casa", 300.0, month = 9, year = 2026, type = TransactionType.EXPENSE, categoryId = 4)
        )

        assertEquals("PUT", lastRequest!!.method)
        assertEquals("/api/Budgets/5", lastRequest!!.url.encodedPath)
        assertTrue(lastBody, lastBody.contains("\"categoryId\":4"))
    }

    @Test
    fun `delete budget sends DELETE and accepts an empty 204`() = runBlocking {
        api(204).deleteBudget(5)

        assertEquals("DELETE", lastRequest!!.method)
        assertEquals("/api/Budgets/5", lastRequest!!.url.encodedPath)
    }

    @Test
    fun `budgets are read with the figures the API computes`() = runBlocking {
        val budgets = api(200, "[$BUDGET_JSON]").getBudgets()

        assertEquals("GET", lastRequest!!.method)
        assertEquals("/api/Budgets", lastRequest!!.url.encodedPath)
        assertEquals(166.67, budgets.single().usagePercentage, 0.001)
        assertEquals(null, budgets.single().categoryId)
    }

    @Test
    fun `create category posts name and type and reads the new id`() = runBlocking {
        val created = api(201, """{"id":12,"name":"Regalos","type":"Income"}""")
            .createCategory(CategoryInput("Regalos", TransactionType.INCOME))

        assertEquals("POST", lastRequest!!.method)
        assertEquals("/api/Categories", lastRequest!!.url.encodedPath)
        assertTrue(lastBody, lastBody.contains("\"type\":\"Income\""))
        assertEquals(12, created.id)
        assertEquals(TransactionType.INCOME, created.type)
    }

    private companion object {
        const val BUDGET_JSON = """{"id":5,"name":"Casa","amount":300,"spentAmount":500,
            "remainingAmount":-200,"usagePercentage":166.67,"month":9,"year":2026,
            "type":"Expense","categoryId":null,"categoryName":null}"""
    }
}
