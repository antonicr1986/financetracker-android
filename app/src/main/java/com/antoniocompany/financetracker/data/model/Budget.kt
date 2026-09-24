package com.antoniocompany.financetracker.data.model

/**
 * Respuesta de GET /api/Budgets.
 *
 * spentAmount, remainingAmount y usagePercentage **los calcula la API**
 * cruzando el presupuesto con los movimientos: no se derivan en la app, igual
 * que en la web. categoryId a null significa "todas las categorias del tipo",
 * no "sin categoria".
 */
/**
 * Cuerpo de POST y PUT /api/Budgets (CreateBudgetDto y UpdateBudgetDto son
 * identicos). categoryId null = todas las categorias del tipo; Gson omite los
 * null y la API lo toma como null.
 */
data class BudgetInput(
    val name: String,
    val amount: Double,
    val month: Int,
    val year: Int,
    val type: TransactionType,
    val categoryId: Int?
)

data class BudgetDto(
    val id: Int,
    val name: String,
    val amount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val usagePercentage: Double,
    val month: Int,
    val year: Int,
    val type: TransactionType,
    val categoryId: Int?,
    val categoryName: String?
)
