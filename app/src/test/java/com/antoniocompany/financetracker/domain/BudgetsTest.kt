package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Presupuestos del panel. Las cifras las calcula la API; aqui se prueba lo
 * que decide la app: que presupuestos se ven, de que color y el selector de
 * meses del formulario.
 */
class BudgetsTest {

    private fun budget(
        year: Int,
        month: Int,
        usage: Double = 50.0,
        remaining: Double = 10.0
    ) = BudgetDto(
        id = year * 100 + month,
        name = "Presupuesto",
        amount = 100.0,
        spentAmount = 100.0 - remaining,
        remainingAmount = remaining,
        usagePercentage = usage,
        month = month,
        year = year,
        type = TransactionType.EXPENSE,
        categoryId = null,
        categoryName = null
    )

    @Test
    fun `only the budgets of the month are shown`() {
        val all = listOf(budget(2026, 9), budget(2026, 8), budget(2025, 9))
        assertEquals(listOf(all[0]), budgetsOfMonth(all, "2026-09"))
    }

    @Test
    fun `a malformed month shows no budgets instead of crashing`() {
        assertEquals(emptyList<BudgetDto>(), budgetsOfMonth(listOf(budget(2026, 9)), "sin-mes"))
    }

    @Test
    fun `the colour changes at 80 and 100 percent, like the web`() {
        assertEquals(BudgetTone.ON_TRACK, budgetTone(79))
        assertEquals(BudgetTone.WARNING, budgetTone(80))
        assertEquals(BudgetTone.WARNING, budgetTone(99))
        assertEquals(BudgetTone.OVER, budgetTone(100))
        assertEquals(BudgetTone.OVER, budgetTone(167))
    }

    @Test
    fun `the percentage is rounded, so 79,6 is already a warning`() {
        assertEquals(80, budgetPercentage(budget(2026, 9, usage = 79.6)))
        assertEquals(BudgetTone.WARNING, budgetTone(budgetPercentage(budget(2026, 9, usage = 79.6))))
    }

    @Test
    fun `within limit counts the ones not exceeded, exactly at zero included`() {
        val budgets = listOf(
            budget(2026, 9, remaining = 20.0),
            budget(2026, 9, remaining = 0.0),
            budget(2026, 9, remaining = -5.0)
        )
        assertEquals(2, budgetsWithinLimit(budgets))
    }

    @Test
    fun `month keys are zero padded`() {
        assertEquals("2026-03", monthKeyOf(2026, 3))
        assertEquals("2026-12", monthKeyOf(2026, 12))
    }

    @Test
    fun `the month picker crosses the year in both directions`() {
        val months = monthKeysAround("2026-01", before = 2, after = 2)
        assertEquals(listOf("2025-11", "2025-12", "2026-01", "2026-02", "2026-03"), months)
    }

    @Test
    fun `the month picker offers a year back and a year ahead by default`() {
        val months = monthKeysAround("2026-09")
        assertEquals(25, months.size)
        assertEquals("2025-09", months.first())
        assertEquals("2027-09", months.last())
    }
}
