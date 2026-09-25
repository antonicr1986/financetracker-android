package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType

/** Una categoria y lo gastado en ella en un mes, para el desglose. */
data class CategoryBreakdown(val categoryName: String, val amount: Double)

/**
 * Solo gastos, agrupados por categoria y de mayor a menor gasto -igual que
 * "Gastos por categoria" en la web. La categoria con mas gasto (la que se ve
 * en el resumen plegado) es siempre la primera del resultado.
 */
fun breakdownOf(transactions: List<TransactionDto>, noCategoryLabel: String): List<CategoryBreakdown> {
    val totals = LinkedHashMap<String, Double>()

    transactions
        .filter { it.type == TransactionType.EXPENSE }
        .forEach { transaction ->
            val name = transaction.categoryName ?: noCategoryLabel
            totals[name] = (totals[name] ?: 0.0) + transaction.amount
        }

    return totals
        .map { (name, amount) -> CategoryBreakdown(name, amount) }
        .sortedByDescending { it.amount }
}
