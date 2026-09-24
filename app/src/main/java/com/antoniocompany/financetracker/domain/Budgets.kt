package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.BudgetDto
import kotlin.math.roundToInt

/*
 * Presupuestos del panel, en Kotlin puro. Las cifras las da la API; aqui solo
 * se elige cuales se ven y como se pinta cada uno.
 */

/** Los presupuestos de un mes "AAAA-MM", en el orden en que llegan. */
fun budgetsOfMonth(budgets: List<BudgetDto>, month: String): List<BudgetDto> {
    val year = month.take(4).toIntOrNull() ?: return emptyList()
    val number = month.drop(5).take(2).toIntOrNull() ?: return emptyList()
    return budgets.filter { it.year == year && it.month == number }
}

/** Como va un presupuesto. Mismos cortes que la web: 80 % y 100 %. */
enum class BudgetTone { ON_TRACK, WARNING, OVER }

fun budgetTone(percentage: Int): BudgetTone = when {
    percentage >= 100 -> BudgetTone.OVER
    percentage >= 80 -> BudgetTone.WARNING
    else -> BudgetTone.ON_TRACK
}

/** Porcentaje redondeado, como lo escribe la web. */
fun budgetPercentage(budget: BudgetDto): Int = budget.usagePercentage.roundToInt()

/** Cuantos siguen dentro del limite, para el resumen "1 de 2 dentro del limite". */
fun budgetsWithinLimit(budgets: List<BudgetDto>): Int =
    budgets.count { it.remainingAmount >= 0 }
