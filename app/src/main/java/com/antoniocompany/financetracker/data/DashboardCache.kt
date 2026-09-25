package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.BudgetDto
import com.antoniocompany.financetracker.data.model.TransactionDto
import com.antoniocompany.financetracker.data.model.TransactionType

/**
 * Lo ultimo que cargo el panel. Al cambiar de tema o idioma el panel se
 * reinicia, y con esto se pinta al momento en vez de pedir otra vez los
 * movimientos a la API y ensenar la rueda de carga.
 *
 * Solo se usa en ese reinicio; cualquier otra entrada al panel pide datos
 * frescos. Se borra al cerrar sesion (SessionStore.clear) para que otra
 * cuenta nunca vea los datos de la anterior.
 */
object DashboardCache {
    var transactions: List<TransactionDto>? = null
    var selectedMonth: String? = null
    var budgets: List<BudgetDto>? = null

    /** Si la tarjeta de presupuestos esta desplegada. Se recuerda en la sesion. */
    var budgetsExpanded: Boolean = true

    /** Filtros de la lista de movimientos. Null/vacio significa "todos". */
    var filterType: TransactionType? = null
    var filterCategory: String? = null
    var filterSearch: String = ""

    /**
     * Si la tarjeta de filtros esta desplegada. Empieza plegada, a
     * diferencia de los presupuestos: es una herramienta que se abre cuando
     * hace falta, no algo que se mira nada mas entrar.
     */
    var filtersExpanded: Boolean = false

    fun clear() {
        transactions = null
        selectedMonth = null
        budgets = null
        budgetsExpanded = true
        filterType = null
        filterCategory = null
        filterSearch = ""
        filtersExpanded = false
    }
}
