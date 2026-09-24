package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Los endpoints de la API, declarados como una interfaz. Retrofit genera la
 * implementacion; aqui solo se describe el contrato.
 *
 * Las funciones son `suspend`: se llaman desde una corrutina y el resultado se
 * lee en linea recta, sin callbacks anidados.
 */
interface FinanceTrackerApi {

    @POST("api/Users/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    /**
     * Crea la cuenta y devuelve el usuario (201), sin token. Si el correo ya
     * existe responde 400 con un ProblemDetails.
     */
    @POST("api/Users/register")
    suspend fun register(@Body body: RegisterRequest): UserDto

    /**
     * La API limita pageSize a 100 y responde con un PagedResult, no con un
     * array. Quien llame decide si recorre las paginas.
     */
    @GET("api/Transactions")
    suspend fun getTransactions(
        @Query("pageNumber") pageNumber: Int,
        @Query("pageSize") pageSize: Int
    ): PagedResult<TransactionDto>

    @POST("api/Transactions")
    suspend fun createTransaction(@Body body: TransactionInput): TransactionDto

    /** Mismo cuerpo que el alta. Responde 204 sin contenido; 404 si no existe. */
    @PUT("api/Transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: Int, @Body body: TransactionInput)

    /** Responde 204 sin contenido; 404 si no existe. */
    @DELETE("api/Transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int)

    /** Este si devuelve un array suelto, no un PagedResult. */
    @GET("api/Categories")
    suspend fun getCategories(): List<CategoryDto>

    /**
     * Todos los presupuestos de todos los meses: la API no admite filtro. El
     * panel se queda con los del mes que se ve. Array suelto, sin paginar.
     */
    @GET("api/Budgets")
    suspend fun getBudgets(): List<BudgetDto>

    /** Devuelve la categoria creada (201), con su id. */
    @POST("api/Categories")
    suspend fun createCategory(@Body body: CategoryInput): CategoryDto
}
