package com.antoniocompany.financetracker.data

import android.content.Context

/**
 * Token de dispositivo de confianza del MFA ("no volver a pedir el codigo en
 * este dispositivo durante 30 dias").
 *
 * En su propio archivo y no en SessionStore a proposito: cerrar sesion borra
 * la sesion entera, pero lo que es de confianza es el dispositivo, no la
 * sesion. Lo borran "Olvidar todos los dispositivos" y desactivar el MFA.
 * Mismo almacenamiento privado que la sesion (ver SessionStore).
 */
class TrustedDeviceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
        }.apply()

    private companion object {
        const val FILE_NAME = "financetracker.device"
        const val KEY_TOKEN = "trusted_device"
    }
}
