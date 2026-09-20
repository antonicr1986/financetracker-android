package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de las funciones que derivan el panel.
 *
 * Son Kotlin puro, sin una sola referencia a Android, asi que corren en la JVM
 * en milisegundos y sin emulador. Es la razon de que esta logica viva en
 * `domain` y no dentro de la Activity.
 */
class DeriveTest {

    private var nextId = 1

    private fun transaction(
        date: String,
        amount: Double,
        type: TransactionType = TransactionType.EXPENSE,
        description: String = "Movimiento",
        categoryName: String? = "Supermercado"
    ) = TransactionDto(
        id = nextId++,
        description = description,
        amount = amount,
        date = date,
        type = type,
        categoryId = 1,
        categoryName = categoryName
    )

    @Test
    fun `monthKey takes the month from the string without building a date`() {
        // Si pasara por un tipo de fecha, un dispositivo al oeste de Greenwich
        // leeria esto como el 31 de diciembre y lo agruparia en el mes anterior.
        assertEquals("2026-01", monthKey("2026-01-01T00:00:00"))
    }

    @Test
    fun `availableMonths returns each month once, oldest first`() {
        val months = availableMonths(
            listOf(
                transaction("2026-03-10T00:00:00", 10.0),
                transaction("2026-01-05T00:00:00", 10.0),
                transaction("2026-03-22T00:00:00", 10.0)
            )
        )

        assertEquals(listOf("2026-01", "2026-03"), months)
    }

    @Test
    fun `transactionsOfMonth keeps only that month, newest first`() {
        val result = transactionsOfMonth(
            listOf(
                transaction("2026-02-01T00:00:00", 10.0, description = "Primero"),
                transaction("2026-03-15T00:00:00", 10.0, description = "Otro mes"),
                transaction("2026-02-20T00:00:00", 10.0, description = "Ultimo")
            ),
            "2026-02"
        )

        assertEquals(listOf("Ultimo", "Primero"), result.map { it.description })
    }

    @Test
    fun `summaryOf adds income and expenses separately and subtracts for the balance`() {
        val summary = summaryOf(
            listOf(
                transaction("2026-02-01T00:00:00", 1500.0, TransactionType.INCOME),
                transaction("2026-02-02T00:00:00", 400.0),
                transaction("2026-02-03T00:00:00", 100.0)
            )
        )

        assertEquals(1500.0, summary.totalIncome, DELTA)
        assertEquals(500.0, summary.totalExpense, DELTA)
        assertEquals(1000.0, summary.balance, DELTA)
    }

    @Test
    fun `summaryOf reports zeros for an empty month instead of failing`() {
        val summary = summaryOf(emptyList())

        assertEquals(0.0, summary.totalIncome, DELTA)
        assertEquals(0.0, summary.totalExpense, DELTA)
        assertEquals(0.0, summary.balance, DELTA)
    }

    @Test
    fun `summaryOf reports a negative balance when the month closes in the red`() {
        val summary = summaryOf(
            listOf(
                transaction("2026-02-01T00:00:00", 100.0, TransactionType.INCOME),
                transaction("2026-02-02T00:00:00", 250.0)
            )
        )

        assertEquals(-150.0, summary.balance, DELTA)
    }

    private companion object {
        // Los importes son Double: se comparan con tolerancia, no con igualdad.
        const val DELTA = 0.001
    }
}
