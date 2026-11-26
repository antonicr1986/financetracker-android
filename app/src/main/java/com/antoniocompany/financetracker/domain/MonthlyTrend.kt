package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Ingresos y gastos de un mes ("2026-09"), para el grafico de evolucion mensual. */
data class MonthlyPoint(val month: String, val income: Double, val expense: Double)

/** Un punto por mes con datos, del mas antiguo al mas reciente, como en la web. */
fun monthlySeries(transactions: List<TransactionDto>): List<MonthlyPoint> =
    availableMonths(transactions).map { month ->
        val summary = summaryOf(transactionsOfMonth(transactions, month))
        MonthlyPoint(month, summary.totalIncome, summary.totalExpense)
    }

/** Tope del eje: el valor redondeado hacia arriba a un numero "redondo" (1234 -> 2000). */
fun axisMax(value: Double): Double {
    if (value <= 0.0) return 0.0

    // Potencia de 10 del primer digito (1234 -> 1000), sin logaritmos.
    var magnitude = 1.0
    while (magnitude * 10 <= value) magnitude *= 10
    while (magnitude > value) magnitude /= 10

    // El margen evita que un error de coma flotante (5,0000000001) suba un escalon.
    return ceil(value / magnitude - 1e-9) * magnitude
}

/** Alto de una barra, de 0 a 100, respecto al tope del eje. */
fun barPercent(amount: Double, axisMax: Double): Int =
    if (axisMax <= 0.0) 0 else (amount / axisMax * 100).roundToInt().coerceIn(0, 100)
