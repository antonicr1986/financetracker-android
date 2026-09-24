package com.antoniocompany.financetracker.data

import com.antoniocompany.financetracker.data.model.TransactionDto

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

    fun clear() {
        transactions = null
        selectedMonth = null
    }
}
