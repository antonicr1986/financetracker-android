package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.TransactionDto

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

    private companion object {
        const val PAGE_SIZE = 100
    }
}
