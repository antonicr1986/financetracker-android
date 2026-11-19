package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.ChangePasswordRequest
import com.antoniocompany.financetracker.data.model.GoogleLoginRequest
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.MfaCodeRequest
import com.antoniocompany.financetracker.data.model.MfaRecoveryCodesResponse
import com.antoniocompany.financetracker.data.model.MfaSetupResponse
import com.antoniocompany.financetracker.data.model.MfaVerifyRequest
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.UserDto
import retrofit2.Response
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

    /**
     * Comprobacion de salud que consulta la base de datos. Solo se usa para
     * despertar la API y Azure SQL al abrir el login (ver LoginActivity). La
     * respuesta no importa: devuelve Response para no lanzar una excepcion con
     * el 503 que da mientras la base de datos despierta.
     */
    @GET("health/ready")
    suspend fun warmUp(): Response<Unit>

    @POST("api/Users/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    /** Misma respuesta que el login. 401 si Google no verifica la cuenta. */
    @POST("api/Users/google")
    suspend fun googleLogin(@Body body: GoogleLoginRequest): LoginResponse

    /**
     * Segundo paso del login cuando la cuenta tiene MFA. 401 con
     * "invalid_mfa_code" si el codigo no vale, o "invalid_mfa_token" si han
     * pasado los 5 minutos y hay que volver a poner la contraseña.
     */
    @POST("api/users/mfa/verify")
    suspend fun verifyMfa(@Body body: MfaVerifyRequest): LoginResponse

    /** Genera el secreto (aun sin activar). Requiere sesion. */
    @POST("api/users/mfa/setup")
    suspend fun setupMfa(): MfaSetupResponse

    /** Activa el MFA con el primer codigo. 400 "invalid_mfa_code" si no vale. */
    @POST("api/users/mfa/confirm")
    suspend fun confirmMfa(@Body body: MfaCodeRequest): MfaRecoveryCodesResponse

    /** Responde 204 sin contenido. */
    @POST("api/users/mfa/disable")
    suspend fun disableMfa(@Body body: MfaCodeRequest)

    /** "Olvidar todos los dispositivos". Responde 204 sin contenido. */
    @POST("api/users/mfa/forget-devices")
    suspend fun forgetDevices()

    /**
     * "Cerrar sesion en todos los dispositivos": invalida todos los JWT de la
     * cuenta, tambien el de este movil. 204 sin cuerpo.
     */
    @POST("api/users/sessions/revoke")
    suspend fun revokeSessions()

    /**
     * Cambiar la contraseña. Cierra todas las sesiones, tambien esta, y devuelve
     * un token nuevo para seguir dentro. Los errores son 400 con su codigo
     * (invalid_current_password, password_unchanged, password_breached,
     * password_not_available), nunca 401.
     */
    @POST("api/users/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): LoginResponse

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

    /**
     * Devuelve el presupuesto creado (201). 400 con category_not_found o
     * category_type_mismatch si la categoria no existe o es del otro tipo.
     */
    @POST("api/Budgets")
    suspend fun createBudget(@Body body: BudgetInput): BudgetDto

    /** 204 sin contenido; 404 si no existe; mismos 400 que el alta. */
    @PUT("api/Budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Int, @Body body: BudgetInput)

    /** 204 sin contenido; 404 si no existe. */
    @DELETE("api/Budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Int)

    /** Devuelve la categoria creada (201), con su id. */
    @POST("api/Categories")
    suspend fun createCategory(@Body body: CategoryInput): CategoryDto
}
