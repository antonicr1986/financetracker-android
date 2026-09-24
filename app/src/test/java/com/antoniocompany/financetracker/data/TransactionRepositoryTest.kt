package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.LoginRequest
import com.antoniocompany.financetracker.data.model.LoginResponse
import com.antoniocompany.financetracker.data.model.PagedResult
import com.antoniocompany.financetracker.data.model.RegisterRequest
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.data.model.UserDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El bucle de paginacion es el sitio donde un error se nota tarde y mal: con
 * menos de 100 movimientos nunca se llega a la segunda pagina, asi que el fallo
 * aparece el dia que un usuario acumula datos. Por eso se prueba con una API
 * falsa en lugar de esperar a que pase.
 */
class TransactionRepositoryTest {

    /** Implementacion de mentira: devuelve las paginas que se le den. */
    private class FakeApi(
        private val pages: List<PagedResult<TransactionDto>>
    ) : FinanceTrackerApi {

        var callCount = 0
            private set

        // El resto del contrato existe porque la interfaz lo exige, no porque
        // estas pruebas lo necesiten. Si alguna acaba llamandolas, el error
        // dice exactamente que falta.
        override suspend fun login(body: LoginRequest): LoginResponse =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun register(body: RegisterRequest): UserDto =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun createTransaction(body: TransactionInput): TransactionDto =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun updateTransaction(id: Int, body: TransactionInput) =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun deleteTransaction(id: Int) =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun getBudgets(): List<BudgetDto> =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun createBudget(body: BudgetInput): BudgetDto =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun updateBudget(id: Int, body: BudgetInput) =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun deleteBudget(id: Int) =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun createCategory(body: CategoryInput): CategoryDto =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun getCategories(): List<CategoryDto> =
            throw UnsupportedOperationException("Sin usar en estas pruebas")

        override suspend fun getTransactions(
            pageNumber: Int,
            pageSize: Int
        ): PagedResult<TransactionDto> {
            callCount++
            return pages[pageNumber - 1]
        }
    }

    private fun transaction(id: Int) = TransactionDto(
        id = id,
        description = "Movimiento $id",
        amount = 10.0,
        date = "2026-02-01T00:00:00",
        type = TransactionType.EXPENSE,
        categoryId = 1,
        categoryName = "Supermercado"
    )

    private fun page(
        items: List<TransactionDto>?,
        pageNumber: Int,
        totalPages: Int
    ) = PagedResult(
        items = items,
        totalCount = 0,
        pageNumber = pageNumber,
        pageSize = 100,
        totalPages = totalPages
    )

    @Test
    fun `walks every page and returns all the transactions`() = runBlocking {
        val api = FakeApi(
            listOf(
                page(listOf(transaction(1), transaction(2)), 1, 3),
                page(listOf(transaction(3)), 2, 3),
                page(listOf(transaction(4)), 3, 3)
            )
        )

        val result = TransactionRepository(api).getAll()

        assertEquals(listOf(1, 2, 3, 4), result.map { it.id })
        assertEquals(3, api.callCount)
    }

    @Test
    fun `asks for a single page when there is only one`() = runBlocking {
        val api = FakeApi(listOf(page(listOf(transaction(1)), 1, 1)))

        val result = TransactionRepository(api).getAll()

        assertEquals(1, result.size)
        assertEquals(1, api.callCount)
    }

    @Test
    fun `treats a null items list as an empty page instead of failing`() = runBlocking {
        val api = FakeApi(listOf(page(null, 1, 1)))

        assertEquals(emptyList<TransactionDto>(), TransactionRepository(api).getAll())
    }

    @Test
    fun `stops after one call when the API reports zero pages`() = runBlocking {
        // Un usuario sin movimientos: totalPages llega a 0 y el bucle tiene que
        // terminar igualmente, no quedarse pidiendo paginas.
        val api = FakeApi(listOf(page(emptyList(), 1, 0)))

        val result = TransactionRepository(api).getAll()

        assertEquals(emptyList<TransactionDto>(), result)
        assertEquals(1, api.callCount)
    }
}
