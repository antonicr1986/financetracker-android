package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltersTest {

    private var nextId = 1

    private fun transaction(
        description: String,
        type: TransactionType = TransactionType.EXPENSE,
        categoryName: String? = "Supermercado"
    ) = TransactionDto(
        id = nextId++,
        description = description,
        amount = 10.0,
        date = "2026-09-10T00:00:00",
        type = type,
        categoryId = if (categoryName == null) null else 1,
        categoryName = categoryName
    )

    @Test
    fun `no filters returns everything`() {
        val all = listOf(transaction("Compra"), transaction("Nomina", type = TransactionType.INCOME))

        assertEquals(2, filterTransactions(all, null, null, "", "Sin categoria").size)
    }

    @Test
    fun `filters by type`() {
        val expense = transaction("Compra")
        val income = transaction("Nomina", type = TransactionType.INCOME)

        val result = filterTransactions(
            listOf(expense, income), TransactionType.INCOME, null, "", "Sin categoria"
        )

        assertEquals(listOf(income), result)
    }

    @Test
    fun `filters by category name, including no-category label`() {
        val supermarket = transaction("Compra", categoryName = "Supermercado")
        val noCategory = transaction("Varios", categoryName = null)

        val result = filterTransactions(
            listOf(supermarket, noCategory), null, "Sin categoria", "", "Sin categoria"
        )

        assertEquals(listOf(noCategory), result)
    }

    @Test
    fun `search matches the description without case and trims spaces`() {
        val match = transaction("Cena mariscada")
        val other = transaction("Gasolina")

        val result = filterTransactions(listOf(match, other), null, null, "  MARISCADA  ", "Sin categoria")

        assertEquals(listOf(match), result)
    }

    @Test
    fun `blank search does not filter anything out`() {
        val all = listOf(transaction("Compra"), transaction("Nomina"))

        assertEquals(2, filterTransactions(all, null, null, "   ", "Sin categoria").size)
    }

    @Test
    fun `combines type, category and search at the same time`() {
        val wanted = transaction("Cena fuera", type = TransactionType.EXPENSE, categoryName = "Ocio")
        val wrongType = transaction("Cena fuera", type = TransactionType.INCOME, categoryName = "Ocio")
        val wrongCategory = transaction("Cena fuera", type = TransactionType.EXPENSE, categoryName = "Comida")
        val wrongSearch = transaction("Cine", type = TransactionType.EXPENSE, categoryName = "Ocio")

        val result = filterTransactions(
            listOf(wanted, wrongType, wrongCategory, wrongSearch),
            TransactionType.EXPENSE,
            "Ocio",
            "cena",
            "Sin categoria"
        )

        assertEquals(listOf(wanted), result)
    }

    @Test
    fun `categoryNamesOf lists distinct names sorted, with the no-category label included`() {
        val transactions = listOf(
            transaction("A", categoryName = "Transporte"),
            transaction("B", categoryName = "Ocio"),
            transaction("C", categoryName = "Ocio"),
            transaction("D", categoryName = null)
        )

        assertEquals(
            listOf("Ocio", "Sin categoria", "Transporte"),
            categoryNamesOf(transactions, "Sin categoria")
        )
    }
}
