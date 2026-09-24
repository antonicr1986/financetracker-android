package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.BudgetInput
import com.antoniocompany.financetracker.data.model.CategoryDto
import com.antoniocompany.financetracker.data.model.CategoryInput
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionInput

/**
 * Trae los movimientos del usuario.
 *
 * La API limita pageSize a 100 y responde con un PagedResult, asi que hay que
 * recorrer las paginas. El panel necesita el historico completo para poder
 * agrupar por mes, igual que en la version web.
 */
class TransactionRepository(private val api: FinanceTrackerApi) {

    suspend fun getAll(): List<TransactionDto> {
        val all = mutableListOf<TransactionDto>()
        var pageNumber = 1
        var totalPages = 1

        do {
            val page = api.getTransactions(pageNumber, PAGE_SIZE)

            all += page.items.orEmpty()
            totalPages = page.totalPages.coerceAtLeast(1)
            pageNumber++
        } while (pageNumber <= totalPages)

        return all
    }

    suspend fun create(input: TransactionInput): TransactionDto =
        api.createTransaction(input)

    suspend fun update(id: Int, input: TransactionInput) =
        api.updateTransaction(id, input)

    suspend fun delete(id: Int) = api.deleteTransaction(id)

    suspend fun getCategories(): List<CategoryDto> = api.getCategories()

    suspend fun getBudgets(): List<BudgetDto> = api.getBudgets()

    suspend fun createBudget(input: BudgetInput): BudgetDto = api.createBudget(input)

    suspend fun updateBudget(id: Int, input: BudgetInput) = api.updateBudget(id, input)

    suspend fun createCategory(input: CategoryInput): CategoryDto = api.createCategory(input)

    private companion object {
        const val PAGE_SIZE = 100
    }
}
