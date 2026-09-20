package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    /** Este si devuelve un array suelto, no un PagedResult. */
    @GET("api/Categories")
    suspend fun getCategories(): List<CategoryDto>
}
