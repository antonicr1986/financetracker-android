package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.ChangePasswordRequest
import com.antoniocompany.financetracker.data.model.EmailResendRequest
import com.antoniocompany.financetracker.data.model.EmailVerifyRequest
import com.antoniocompany.financetracker.data.model.ForgotPasswordRequest
import com.antoniocompany.financetracker.data.model.GoogleLoginRequest
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.MfaCodeRequest
import com.antoniocompany.financetracker.data.model.MfaRecoveryCodesResponse
import com.antoniocompany.financetracker.data.model.MfaSetupResponse
import com.antoniocompany.financetracker.data.model.MfaVerifyRequest
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.ResetPasswordRequest
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.UserDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * API falsa para los tests de las pantallas (ApiClient.replaceForTests). Cada
 * test sobrescribe solo lo que usa; el resto falla si se llama. Responde sin
 * esperar, asi que lo que lanza la pantalla termina dentro del propio clic.
 */
open class FakeApi : FinanceTrackerApi {
    override suspend fun warmUp(): Response<Unit> = Response.success(Unit)
    override suspend fun login(body: LoginRequest): LoginResponse = unused()
    override suspend fun googleLogin(body: GoogleLoginRequest): LoginResponse = unused()
    override suspend fun verifyMfa(body: MfaVerifyRequest): LoginResponse = unused()
    override suspend fun setupMfa(): MfaSetupResponse = unused()
    override suspend fun confirmMfa(body: MfaCodeRequest): MfaRecoveryCodesResponse = unused()
    override suspend fun disableMfa(body: MfaCodeRequest): Unit = unused()
    override suspend fun forgetDevices(): Unit = unused()
    override suspend fun revokeSessions(): Unit = unused()
    override suspend fun changePassword(body: ChangePasswordRequest): LoginResponse = unused()
    override suspend fun verifyEmail(body: EmailVerifyRequest): LoginResponse = unused()
    override suspend fun resendEmailCode(body: EmailResendRequest): Unit = unused()
    override suspend fun forgotPassword(body: ForgotPasswordRequest): Unit = unused()
    override suspend fun resetPassword(body: ResetPasswordRequest): Unit = unused()
    override suspend fun register(body: RegisterRequest): UserDto = unused()
    override suspend fun getTransactions(pageNumber: Int, pageSize: Int): PagedResult<TransactionDto> = unused()
    override suspend fun createTransaction(body: TransactionInput): TransactionDto = unused()
    override suspend fun updateTransaction(id: Int, body: TransactionInput): Unit = unused()
    override suspend fun deleteTransaction(id: Int): Unit = unused()
    override suspend fun getCategories(): List<CategoryDto> = unused()
    override suspend fun getBudgets(): List<BudgetDto> = unused()
    override suspend fun createBudget(body: BudgetInput): BudgetDto = unused()
    override suspend fun updateBudget(id: Int, body: BudgetInput): Unit = unused()
    override suspend fun deleteBudget(id: Int): Unit = unused()
    override suspend fun createCategory(body: CategoryInput): CategoryDto = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("Sin usar en esta prueba")
}

/** Un error de la API con su codigo en el cuerpo, como un ProblemDetails. */
fun httpError(status: Int, code: String? = null): HttpException {
    val body = (if (code == null) "" else """{"code":"$code"}""")
        .toResponseBody("application/problem+json".toMediaType())
    return HttpException(Response.error<Any>(status, body))
}

/** Respuesta de un login correcto. */
fun session(email: String = "ana@test.com") =
    LoginResponse(token = "jwt-123", expiration = null, user = UserDto(id = 7, name = "Ana", email = email))
