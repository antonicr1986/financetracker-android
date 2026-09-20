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

/** Envoltorio de los listados. Refleja PagedResult<T> de la API. */
data class PagedResult<T>(
    val items: List<T>?,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int
)
