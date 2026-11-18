package com.antoniocompany.financetracker.data.model

/** Cuerpo de POST /api/Users/login. */
data class LoginRequest(
    val email: String,
    val password: String,
    /** Dispositivo de confianza del MFA, si lo hay: con uno valido no se pide el codigo. */
    val trustedDeviceToken: String? = null
)

/**
 * Cuerpo de POST /api/Users/google: el ID token que da Google al elegir la
 * cuenta y el idioma ("es" o "en") por si la API tiene que crear la cuenta.
 */
data class GoogleLoginRequest(
    val idToken: String,
    val language: String,
    val trustedDeviceToken: String? = null
)

/**
 * Cuerpo de POST /api/Users/register. Mismos limites que RegisterUserDto en
 * la API: nombre hasta 100, correo hasta 150 y contrasena de al menos 6.
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    /** "es" o "en": la API siembra las categorias de partida en ese idioma. */
    val language: String
)

/**
 * Respuesta de POST /api/Users/login.
 *
 * El registro, en cambio, devuelve el usuario y no un token: despues de
 * registrar hay que encadenar el login, como hace la web.
 */
data class LoginResponse(
    /** null cuando la cuenta tiene MFA y falta el codigo (mfaRequired). */
    val token: String?,
    val expiration: String?,
    val user: UserDto?,
    /**
     * true si la contraseña es correcta pero la cuenta tiene verificacion en
     * dos pasos: no hay sesion todavia, solo mfaToken para enviar el codigo.
     */
    val mfaRequired: Boolean = false,
    val mfaToken: String? = null,
    /** Solo tras verificar el codigo con rememberDevice: se guarda en TrustedDeviceStore. */
    val trustedDeviceToken: String? = null
)

data class UserDto(
    val id: Int,
    val name: String?,
    val email: String,
    val mfaEnabled: Boolean = false
)

/** Cuerpo de POST /api/users/mfa/verify: codigo TOTP o de recuperacion. */
data class MfaVerifyRequest(
    val mfaToken: String,
    val code: String,
    /** "No volver a pedir el codigo en este dispositivo durante 30 dias". */
    val rememberDevice: Boolean = false
)

/** Cuerpo de confirm y disable: los 6 digitos de la app autenticadora. */
data class MfaCodeRequest(
    val code: String
)

/** Respuesta de POST /api/users/mfa/setup. */
data class MfaSetupResponse(
    val secret: String,
    val otpAuthUri: String
)

/** Respuesta de POST /api/users/mfa/confirm. Se muestran una sola vez. */
data class MfaRecoveryCodesResponse(
    val recoveryCodes: List<String>
)
