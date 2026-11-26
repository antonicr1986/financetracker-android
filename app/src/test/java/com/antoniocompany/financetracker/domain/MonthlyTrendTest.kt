package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType
import com.antoniocompany.financetracker.data.model.TransactionType.EXPENSE
import com.antoniocompany.financetracker.data.model.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyTrendTest {

    private var nextId = 1

    private fun transaction(date: String, amount: Double, type: TransactionType) = TransactionDto(
        id = nextId++,
        description = "Movimiento",
        amount = amount,
        date = date,
        type = type,
        categoryId = 1,
        categoryName = "Varios"
    )

    @Test
    fun `monthly series has income and expense per month, oldest first, across years`() {
        val all = listOf(
            transaction("2026-01-10T00:00:00", 300.0, EXPENSE),
            transaction("2025-12-05T00:00:00", 1000.0, INCOME),
            transaction("2026-01-20T00:00:00", 1200.0, INCOME),
            transaction("2025-12-31T00:00:00", 50.25, EXPENSE),
            transaction("2026-01-25T00:00:00", 99.75, EXPENSE),
        )

        assertEquals(
            listOf(
                MonthlyPoint("2025-12", income = 1000.0, expense = 50.25),
                MonthlyPoint("2026-01", income = 1200.0, expense = 399.75),
            ),
            monthlySeries(all)
        )
    }

    @Test
    fun `monthly series is empty without transactions`() {
        assertTrue(monthlySeries(emptyList()).isEmpty())
    }

    @Test
    fun `axis max rounds up to a nice number`() {
        assertEquals(2000.0, axisMax(1234.0), 0.0)
        assertEquals(900.0, axisMax(870.0), 0.0)
        assertEquals(100.0, axisMax(100.0), 0.0)
        assertEquals(0.0, axisMax(0.0), 0.0)
    }

    @Test
    fun `bar height is relative to the axis max`() {
        assertEquals(50, barPercent(1000.0, 2000.0))
        assertEquals(3, barPercent(50.0, 2000.0))   // 2,5 % se redondea hacia arriba, como la web
        assertEquals(100, barPercent(2000.0, 2000.0))
        assertEquals(0, barPercent(0.0, 0.0))       // sin datos, sin barra (y sin dividir entre 0)
    }
}
