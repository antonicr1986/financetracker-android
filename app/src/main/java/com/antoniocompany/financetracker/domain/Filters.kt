package com.antoniocompany.financetracker.domain

import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType

/**
 * Filtros de la lista de movimientos, igual que en la web: buscador, tipo y
 * categoria, en cliente sobre los movimientos del mes ya cargado. Sin
 * peticion nueva a la API.
 *
 * La etiqueta de "sin categoria" la pasa quien llama, ya traducida: aqui no
 * hay Android, y por tanto tampoco `getString`.
 */

/** Nombre a mostrar de la categoria de un movimiento, o la etiqueta de "sin categoria". */
fun categoryLabel(transaction: TransactionDto, noCategoryLabel: String): String =
    transaction.categoryName ?: noCategoryLabel

/**
 * Nombres de categoria presentes en una lista de movimientos, en orden
 * alfabetico, para rellenar el desplegable del filtro.
 */
fun categoryNamesOf(transactions: List<TransactionDto>, noCategoryLabel: String): List<String> =
    transactions.map { categoryLabel(it, noCategoryLabel) }.distinct().sorted()

/**
 * `type` y `categoryName` en null significan "todos"/"todas". `search` se
 * recorta y compara en minusculas contra la descripcion, como en la web.
 */
fun filterTransactions(
    transactions: List<TransactionDto>,
    type: TransactionType?,
    categoryName: String?,
    search: String,
    noCategoryLabel: String
): List<TransactionDto> {
    val needle = search.trim().lowercase()

    return transactions.filter { transaction ->
        if (type != null && transaction.type != type) return@filter false

        if (categoryName != null && categoryLabel(transaction, noCategoryLabel) != categoryName) {
            return@filter false
        }

        if (needle.isNotEmpty() && !transaction.description.lowercase().contains(needle)) {
            return@filter false
        }

        true
    }
}
