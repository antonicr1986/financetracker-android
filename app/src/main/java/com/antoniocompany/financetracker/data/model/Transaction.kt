package com.antoniocompany.financetracker.data.model

import com.google.gson.annotations.SerializedName

/**
 * La API serializa el enum como texto ("Income"/"Expense"), no como numero.
 * Se anota explicitamente en vez de confiar en que el nombre de la constante
 * coincida: asi se puede seguir el convenio de Kotlin en mayusculas sin que se
 * rompa la conversion.
 */
enum class TransactionType {
    @SerializedName("Income")
    INCOME,

    @SerializedName("Expense")
    EXPENSE
}

data class TransactionDto(
    val id: Int,
    val description: String,
    val amount: Double,
    /** ISO completo, p.ej. "2026-02-14T00:00:00". Se recorta, no se parsea. */
    val date: String,
    val type: TransactionType,
    val categoryId: Int?,
    val categoryName: String?
)

/**
 * Una categoria del usuario. El tipo importa: la API rechaza un gasto con
 * categoria de ingresos, asi que el formulario solo ofrece las que encajan.
 */
data class CategoryDto(
    val id: Int,
    val name: String,
    val type: TransactionType
)

/**
 * Cuerpo de POST /api/Transactions.
 *
 * `date` viaja como "AAAA-MM-DD". La API lo recibe en un DateTime y lo
 * interpreta a medianoche, que es justo lo que queremos: un movimiento tiene
 * dia, no hora.
 */
data class TransactionInput(
    val description: String,
    val amount: Double,
    val date: String,
    val type: TransactionType,
    val categoryId: Int
)

/** Cuerpo de POST /api/Categories. La API limita el nombre a 100 caracteres. */
data class CategoryInput(
    val name: String,
    val type: TransactionType
)

/** Envoltorio de los listados. Refleja PagedResult<T> de la API. */
data class PagedResult<T>(
    val items: List<T>?,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int
)
