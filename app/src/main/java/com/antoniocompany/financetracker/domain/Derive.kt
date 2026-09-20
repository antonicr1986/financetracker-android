package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType

/** Totales de un mes. */
data class MonthSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)

/**
 * "2026-09-04T00:00:00" -> "2026-09".
 *
 * Se recorta la cadena en lugar de construir una fecha: pasar por un tipo de
 * fecha aplicaria la zona horaria del dispositivo y agruparia el dia 1 de un
 * mes en el anterior para quien este al oeste de Greenwich.
 */
fun monthKey(isoDate: String): String = isoDate.take(7)

/** Meses presentes en los datos, del mas antiguo al mas reciente. */
fun availableMonths(transactions: List<TransactionDto>): List<String> =
    transactions.map { monthKey(it.date) }.distinct().sorted()

/** Movimientos de un mes, del mas reciente al mas antiguo. */
fun transactionsOfMonth(
    transactions: List<TransactionDto>,
    key: String
): List<TransactionDto> =
    transactions
        .filter { monthKey(it.date) == key }
        .sortedByDescending { it.date }

fun summaryOf(transactions: List<TransactionDto>): MonthSummary {
    val income = transactions
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }

    val expense = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    return MonthSummary(
        totalIncome = income,
        totalExpense = expense,
        balance = income - expense
    )
}
