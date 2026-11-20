package com.antoniocompany.financetracker.data

import android.content.Context

/**
 * Guarda el token y el correo de la sesion.
 *
 * En SharedPreferences normales, no cifradas: Google deprecio
 * EncryptedSharedPreferences en 2025 en favor de las APIs de la plataforma. El
 * almacenamiento privado de la aplicacion ya esta aislado del resto por el
 * sandbox de Android; lo que anadia el cifrado era proteccion frente a copias
 * de seguridad y extraccion fisica del dispositivo. Lo primero se cubre con
 * android:allowBackup="false" en el manifiesto, y frente a lo segundo la
 * mitigacion real es que el token caduca en 60 minutos.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
        }.apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_EMAIL) else putString(KEY_EMAIL, value)
        }.apply()

    /** Si la cuenta tiene el MFA activo, para mostrarlo en su pantalla. */
    var mfaEnabled: Boolean
        get() = prefs.getBoolean(KEY_MFA_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_MFA_ENABLED, value).apply()

    /** Ver UserDto.hasPassword. Por defecto true: sesiones guardadas antes de existir. */
    var hasPassword: Boolean
        get() = prefs.getBoolean(KEY_HAS_PASSWORD, true)
        set(value) = prefs.edit().putBoolean(KEY_HAS_PASSWORD, value).apply()

    val hasSession: Boolean
        get() = token != null

    fun clear() {
        prefs.edit().clear().apply()
        DashboardCache.clear()
    }

    private companion object {
        const val FILE_NAME = "financetracker.session"
        const val KEY_TOKEN = "token"
        const val KEY_EMAIL = "email"
        const val KEY_MFA_ENABLED = "mfa_enabled"
        const val KEY_HAS_PASSWORD = "has_password"
    }
}
