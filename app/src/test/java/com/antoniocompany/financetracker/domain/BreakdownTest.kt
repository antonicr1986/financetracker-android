package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakdownTest {

    private var nextId = 1

    private fun transaction(
        amount: Double,
        type: TransactionType = TransactionType.EXPENSE,
        categoryName: String? = "Supermercado"
    ) = TransactionDto(
        id = nextId++,
        description = "Movimiento",
        amount = amount,
        date = "2026-09-10T00:00:00",
        type = type,
        categoryId = if (categoryName == null) null else 1,
        categoryName = categoryName
    )

    @Test
    fun `ignores income, only sums expenses`() {
        val transactions = listOf(
            transaction(100.0, type = TransactionType.EXPENSE, categoryName = "Ocio"),
            transaction(2000.0, type = TransactionType.INCOME, categoryName = "Nomina")
        )

        assertEquals(listOf(CategoryBreakdown("Ocio", 100.0)), breakdownOf(transactions, "Sin categoria"))
    }

    @Test
    fun `sums several expenses of the same category`() {
        val transactions = listOf(
            transaction(30.0, categoryName = "Supermercado"),
            transaction(20.0, categoryName = "Supermercado")
        )

        assertEquals(
            listOf(CategoryBreakdown("Supermercado", 50.0)),
            breakdownOf(transactions, "Sin categoria")
        )
    }

    @Test
    fun `sorts from the most to the least spent`() {
        val transactions = listOf(
            transaction(50.0, categoryName = "Transporte"),
            transaction(300.0, categoryName = "Supermercado"),
            transaction(150.0, categoryName = "Ocio")
        )

        assertEquals(
            listOf(
                CategoryBreakdown("Supermercado", 300.0),
                CategoryBreakdown("Ocio", 150.0),
                CategoryBreakdown("Transporte", 50.0)
            ),
            breakdownOf(transactions, "Sin categoria")
        )
    }

    @Test
    fun `expenses with no category fall under the no-category label`() {
        val transactions = listOf(
            transaction(10.0, categoryName = null),
            transaction(5.0, categoryName = null)
        )

        assertEquals(
            listOf(CategoryBreakdown("Sin categoria", 15.0)),
            breakdownOf(transactions, "Sin categoria")
        )
    }

    @Test
    fun `no expenses returns an empty list`() {
        val transactions = listOf(transaction(100.0, type = TransactionType.INCOME))

        assertEquals(emptyList<CategoryBreakdown>(), breakdownOf(transactions, "Sin categoria"))
    }
}
